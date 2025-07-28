package com.example.ainovel.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.ainovel.dto.ConceptionResponse;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.StoryCard;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service implementation for interacting with the Google Gemini API.
 */
@Service("gemini")
public class GeminiService extends AbstractAiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private final RestTemplate restTemplate;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent}")
    private String geminiApiUrl;

    // DTOs for Gemini API Response
    private static class GeminiResponse {
        public List<Candidate> candidates;
    }

    private static class Candidate {
        public Content content;
    }

    private static class Content {
        public List<Part> parts;
    }

    private static class Part {
        public String text;
    }

    // DTO for the combined conception response from Gemini
    private static class GeminiConceptionResponse {
        @JsonProperty("storyCard")
        public StoryCard storyCard;
        @JsonProperty("characterCards")
        public List<CharacterCard> characterCards;
    }

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.restTemplate = restTemplate;
    }

    @Override
    @Retryable(value = {RestClientException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String generate(String prompt, String apiKey) {
        try {
            return callGeminiApi(prompt, apiKey, false);
        } catch (JsonProcessingException e) {
            log.error("Error calling Gemini API for text generation", e);
            throw new RuntimeException("Failed to generate text from Gemini.", e);
        }
    }

    @Override
    protected String callApiForJson(String prompt, String apiKey) throws JsonProcessingException {
        return callGeminiApi(prompt, apiKey, true);
    }

    @Override
    protected ConceptionResponse parseConceptionResponse(String jsonResponse) throws JsonProcessingException {
        GeminiConceptionResponse conception = objectMapper.readValue(jsonResponse, GeminiConceptionResponse.class);
        return new ConceptionResponse(conception.storyCard, conception.characterCards);
    }

    private String callGeminiApi(String prompt, String apiKey, boolean jsonMode) throws JsonProcessingException {
        String url = geminiApiUrl + "?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(textPart));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", Collections.singletonList(content));

        if (jsonMode) {
            Map<String, String> generationConfig = new HashMap<>();
            generationConfig.put("response_mime_type", "application/json");
            body.put("generationConfig", generationConfig);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();

            GeminiResponse geminiResponse = objectMapper.readValue(responseBody, GeminiResponse.class);

            if (geminiResponse == null || geminiResponse.candidates == null || geminiResponse.candidates.isEmpty() ||
                geminiResponse.candidates.get(0).content == null || geminiResponse.candidates.get(0).content.parts == null ||
                geminiResponse.candidates.get(0).content.parts.isEmpty() || geminiResponse.candidates.get(0).content.parts.get(0).text == null) {
                throw new RuntimeException("Invalid response structure from Gemini.");
            }

            String text = geminiResponse.candidates.get(0).content.parts.get(0).text;

            if (text.trim().isEmpty()) {
                throw new RuntimeException("AI response content is empty.");
            }
            // Gemini might wrap JSON in ```json ... ```, so we need to extract it.
            if (jsonMode && text.contains("{") && text.contains("}")) {
                return text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1);
            }
            return text;
        } catch (RestClientException e) {
            log.error("Error calling Gemini API: " + e.getMessage(), e);
            throw new RuntimeException("Failed to get a valid response from Gemini.", e);
        }
    }

    @Override
    public boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}

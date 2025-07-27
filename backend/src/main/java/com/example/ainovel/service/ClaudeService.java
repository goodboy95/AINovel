package com.example.ainovel.service;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;
import com.example.ainovel.dto.RefineRequest;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.StoryCard;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service implementation for interacting with the Anthropic Claude API.
 */
@Service("claude")
public class ClaudeService extends AbstractAiService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeService.class);
    private final RestTemplate restTemplate;

    @Value("${claude.api.url:https://api.anthropic.com/v1}")
    private String claudeApiUrl;

    @Value("${claude.model.default:claude-3-opus-20240229}")
    private String defaultModel;

    @Value("${claude.model.validation:claude-3-haiku-20240307}")
    private String validationModel;

    // DTOs for Claude API Response
    private static class ClaudeResponse {
        public List<ContentBlock> content;
    }

    private static class ContentBlock {
        public String type;
        public String text;
    }

    // DTO for the combined conception response from Claude
    private static class ClaudeConceptionResponse {
        @JsonProperty("storyCard")
        public StoryCard storyCard;
        @JsonProperty("characterCards")
        public List<CharacterCard> characterCards;
    }
    public ClaudeService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.restTemplate = restTemplate;
    }

    @Override
    public String generate(String prompt, String apiKey) {
        try {
            return callClaudeApi(prompt, apiKey, defaultModel);
        } catch (JsonProcessingException e) {
            log.error("Error calling Claude API for text generation", e);
            throw new RuntimeException("Failed to generate text from Claude.", e);
        }
    }

    @Override
    protected String callApiForJson(String prompt, String apiKey) throws JsonProcessingException {
        return callClaudeApi(prompt, apiKey, defaultModel);
    }

    @Override
    protected ConceptionResponse parseConceptionResponse(String jsonResponse) throws JsonProcessingException {
        ClaudeConceptionResponse conception = objectMapper.readValue(jsonResponse, ClaudeConceptionResponse.class);
        return new ConceptionResponse(conception.storyCard, conception.characterCards);
    }

    private String callClaudeApi(String prompt, String apiKey, String model) throws JsonProcessingException {
        String url = claudeApiUrl + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(message));
        body.put("max_tokens", 4096);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();

            ClaudeResponse claudeResponse = objectMapper.readValue(responseBody, ClaudeResponse.class);

            if (claudeResponse == null || claudeResponse.content == null || claudeResponse.content.isEmpty() ||
                claudeResponse.content.get(0).text == null) {
                throw new RuntimeException("Invalid response structure from Claude.");
            }

            String text = claudeResponse.content.get(0).text;
            if (text.trim().isEmpty()) {
                throw new RuntimeException("AI response content is empty.");
            }
            // Claude might wrap JSON in ```json ... ```, so we need to extract it.
            if (text.contains("{") && text.contains("}")) {
                return text.substring(text.indexOf('{'), text.lastIndexOf('}') + 1);
            }
            return text;
        } catch (RestClientException e) {
            log.error("Error calling Claude API: " + e.getMessage(), e);
            throw new RuntimeException("Failed to get a valid response from Claude.", e);
        }
    }

    @Override
    public boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        String url = claudeApiUrl + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        headers.setContentType(MediaType.APPLICATION_JSON);

        // To validate, we send a very small, cheap request.
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", "hello");

        Map<String, Object> body = new HashMap<>();
        body.put("model", "claude-3-haiku-20240307"); // Use the cheapest model for validation
        body.put("messages", List.of(message));
        body.put("max_tokens", 10);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}

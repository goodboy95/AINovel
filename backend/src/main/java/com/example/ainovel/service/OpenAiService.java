package com.example.ainovel.service;

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
 * Service implementation for interacting with the OpenAI API.
 */
@Service("openai")
public class OpenAiService extends AbstractAiService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);
    private final RestTemplate restTemplate;

    // DTOs for OpenAI Chat Completion Response
    private static class OpenAiChatCompletionResponse {
        public List<Choice> choices;
    }

    private static class Choice {
        public Message message;
    }

    private static class Message {
        public String role;
        public String content;
    }

    // DTO for the combined conception response
    private static class OpenAiConceptionResponse {
        @JsonProperty("storyCard")
        public StoryCard storyCard;
        @JsonProperty("characterCards")
        public List<CharacterCard> characterCards;
    }
    @Value("${openai.api.url:https://api.openai.com/v1}")
    private String openaiApiUrl;

    @Value("${openai.model.default:gpt-4-turbo}")
    private String defaultModel;

    public OpenAiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        super(objectMapper);
        this.restTemplate = restTemplate;
    }

    @Override
    @Retryable(value = {RestClientException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String generate(String prompt, String apiKey) {
        try {
            return callOpenAi(prompt, apiKey, false);
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON for OpenAI text generation", e);
            throw new RuntimeException("Failed to process JSON for OpenAI text generation.", e);
        }
    }

    @Override
    protected String callApiForJson(String prompt, String apiKey) throws JsonProcessingException {
        return callOpenAi(prompt, apiKey, true);
    }

    @Override
    protected ConceptionResponse parseConceptionResponse(String jsonResponse) throws JsonProcessingException {
        OpenAiConceptionResponse conception = objectMapper.readValue(jsonResponse, OpenAiConceptionResponse.class);
        return new ConceptionResponse(conception.storyCard, conception.characterCards);
    }

    private String callOpenAi(String prompt, String apiKey, boolean jsonMode) throws JsonProcessingException {
        String url = openaiApiUrl + "/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", defaultModel);
        body.put("messages", List.of(message));
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();

            OpenAiChatCompletionResponse completionResponse = objectMapper.readValue(responseBody, OpenAiChatCompletionResponse.class);

            if (completionResponse == null || completionResponse.choices == null || completionResponse.choices.isEmpty() ||
                completionResponse.choices.get(0).message == null || completionResponse.choices.get(0).message.content == null) {
                throw new RuntimeException("Invalid response structure from OpenAI.");
            }
            String content = completionResponse.choices.get(0).message.content;

            if (content.trim().isEmpty()) {
                throw new RuntimeException("AI response content is empty.");
            }
            if (jsonMode) {
                log.info("Raw AI JSON response: " + content);
            }
            return content;
        } catch (RestClientException e) {
            log.error("Error calling OpenAI API", e);
            throw new RuntimeException("Failed to get a valid response from OpenAI.", e);
        }
    }

    @Override
    public boolean validateApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }

        String url = openaiApiUrl + "/models";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            // Any exception (e.g., 401 Unauthorized) means the key is invalid or the service is unreachable.
            return false;
        }
    }
}

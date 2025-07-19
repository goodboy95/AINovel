package com.example.ainovel.service;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.StoryCard;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("openai")
public class OpenAiService implements AiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Value("${openai.api.url:https://api.openai.com/v1}")
    private String openaiApiUrl;

    @Override
    public ConceptionResponse generateStory(ConceptionRequest request, String apiKey) {
        String url = openaiApiUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String prompt = String.format(
            "你是一个富有想象力的故事作家。请根据以下信息，为我构思一个故事。\n" +
            "用户想法: \"%s\"\n" +
            "故事类型: \"%s\"\n" +
            "故事基调: \"%s\"\n\n" +
            "请以JSON格式返回，包含两个键: \"storyCard\" 和 \"characterCards\"。\n" +
            "\"storyCard\" 的值应包含 \"title\", \"synopsis\", \"storyArc\"。\n" +
            "\"characterCards\" 的值应为一个数组，包含至少2个主要角色。每个角色对象应包含 \"name\", \"synopsis\" (性别、年龄、外貌、性格), \"details\" (背景故事), \"relationships\"。",
            request.getIdea(), request.getGenre(), request.getTone()
        );

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4-turbo"); // Or any other suitable model
        body.put("messages", List.of(message));
        body.put("response_format", Map.of("type", "json_object"));


        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();

            // Extract the JSON content from the response
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            String jsonContent = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");

            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                throw new RuntimeException("AI response content is empty.");
            }

            // Find the start and end of the JSON object to handle potential markdown wrappers
            int startIndex = jsonContent.indexOf('{');
            int endIndex = jsonContent.lastIndexOf('}');

            if (startIndex == -1 || endIndex == -1 || startIndex > endIndex) {
                throw new RuntimeException("Could not find a valid JSON object in the AI response: " + jsonContent);
            }

            String extractedJson = jsonContent.substring(startIndex, endIndex + 1);

            // Deserialize the JSON content into our DTO
            ConceptionResponse conceptionResponse = objectMapper.readValue(extractedJson, ConceptionResponse.class);
            
            // The genre and tone are from the request, not the AI response, so we set them here.
            if (conceptionResponse.getStoryCard() != null) {
                conceptionResponse.getStoryCard().setGenre(request.getGenre());
                conceptionResponse.getStoryCard().setTone(request.getTone());
            }

            return conceptionResponse;

        } catch (Exception e) {
            // Log the error and throw a custom exception or return a default response
            System.err.println("Error calling OpenAI API: " + e.getMessage());
            throw new RuntimeException("Failed to generate story from OpenAI.", e);
        }
    }

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
        } catch (Exception e) {
            // Any exception (e.g., 401 Unauthorized) means the key is invalid or the service is unreachable.
            return false;
        }
    }
}

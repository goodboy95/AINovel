package com.example.ainovel.service;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("gemini")
public class GeminiService implements AiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent}")
    private String geminiApiUrl;

    public GeminiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ConceptionResponse generateStory(ConceptionRequest request, String apiKey) {
        String url = geminiApiUrl + "?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String prompt = String.format(
            "你是一个富有想象力的故事作家。请根据以下信息，为我构思一个故事。\n" +
            "用户想法: \"%s\"\n" +
            "故事类型: \"%s\"\n" +
            "故事基调: \"%s\"\n\n" +
            "请以JSON格式返回，包含两个键: \"storyCard\" 和 \"characterCards\"。\n" +
            "\"storyCard\" 的值应包含 \"title\", \"synopsis\", \"storyArc\"。\n" +
            "\"characterCards\" 的值应为一个数组，包含至少2个主要角色。每个角色对象应包含 \"name\", \"synopsis\" (性别、年龄、外貌、性格), \"details\" (背景故事), \"relationships\"。\n\n" +
            "请只返回JSON对象，不要包含任何额外的解释或markdown格式。",
            request.getIdea(), request.getGenre(), request.getTone()
        );

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);

        Map<String, Object> content = new HashMap<>();
        content.put("parts", Collections.singletonList(textPart));

        Map<String, Object> body = new HashMap<>();
        body.put("contents", Collections.singletonList(content));
        
        // Adding generationConfig to ask for a JSON response
        Map<String, String> generationConfig = new HashMap<>();
        generationConfig.put("response_mime_type", "application/json");
        body.put("generationConfig", generationConfig);


        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();

            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseMap.get("candidates");
            Map<String, Object> candidateContent = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) candidateContent.get("parts");
            String jsonContent = (String) parts.get(0).get("text");

            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                throw new RuntimeException("AI response content is empty.");
            }

            int startIndex = jsonContent.indexOf('{');
            int endIndex = jsonContent.lastIndexOf('}');

            if (startIndex == -1 || endIndex == -1 || startIndex > endIndex) {
                throw new RuntimeException("Could not find a valid JSON object in the AI response: " + jsonContent);
            }

            String extractedJson = jsonContent.substring(startIndex, endIndex + 1);

            // Manually process the JSON to handle the 'relationships' field
            Map<String, Object> conceptionMap = objectMapper.readValue(extractedJson, Map.class);
            
            if (conceptionMap.containsKey("characterCards")) {
                List<Map<String, Object>> characterCardsList = (List<Map<String, Object>>) conceptionMap.get("characterCards");
                for (Map<String, Object> characterCard : characterCardsList) {
                    if (characterCard.containsKey("relationships") && characterCard.get("relationships") instanceof Map) {
                        String relationshipsJson = objectMapper.writeValueAsString(characterCard.get("relationships"));
                        characterCard.put("relationships", relationshipsJson);
                    }
                }
            }

            ConceptionResponse conceptionResponse = objectMapper.convertValue(conceptionMap, ConceptionResponse.class);

            if (conceptionResponse.getStoryCard() != null) {
                conceptionResponse.getStoryCard().setGenre(request.getGenre());
                conceptionResponse.getStoryCard().setTone(request.getTone());
            }

            return conceptionResponse;

        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            throw new RuntimeException("Failed to generate story from Gemini.", e);
        }
    }

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

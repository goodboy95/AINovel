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
    public String generate(String prompt, String apiKey) {
        String url = openaiApiUrl + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4-turbo");
        body.put("messages", List.of(message));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();

            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            return (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");
        } catch (Exception e) {
            System.err.println("Error calling OpenAI API for text generation: " + e.getMessage());
            throw new RuntimeException("Failed to generate text from OpenAI.", e);
        }
    }

    @Override
    public ConceptionResponse generateConception(ConceptionRequest request, String apiKey) {
        try {
            // Step 1: Generate StoryCard
            StoryCard storyCard = generateStoryCard(request, apiKey);
            storyCard.setGenre(request.getGenre());
            storyCard.setTone(request.getTone());

            // Step 2: Generate CharacterCards based on the StoryCard
            List<CharacterCard> characterCards = generateCharacterCards(storyCard, apiKey);

            return new ConceptionResponse(storyCard, characterCards);

        } catch (Exception e) {
            System.err.println("Error during conception generation process: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate full story conception from OpenAI.", e);
        }
    }

    private StoryCard generateStoryCard(ConceptionRequest request, String apiKey) throws Exception {
        String prompt = String.format(
            "你是一个富有想象力的故事作家。请根据以下信息，为我构思一个故事的核心概念。请使用简体中文进行创作。\n" +
            "用户想法: \"%s\"\n" +
            "故事类型: \"%s\"\n" +
            "故事基调: \"%s\"\n\n" +
            "请以JSON格式返回一个故事卡对象，该对象应包含以下键: \"title\", \"synopsis\", \"storyArc\"。",
            request.getIdea(), request.getGenre(), request.getTone()
        );

        String jsonContent = callOpenAi(prompt, apiKey);
        return objectMapper.readValue(jsonContent, StoryCard.class);
    }

    private List<CharacterCard> generateCharacterCards(StoryCard storyCard, String apiKey) throws Exception {
        String prompt = String.format(
            "你是一位角色设计师。基于以下故事概念，请设计至少2个主要角色。请使用简体中文进行创作。\n\n" +
            "故事标题: %s\n" +
            "故事概要: %s\n\n" +
            "请以JSON格式返回一个角色卡数组。每个角色对象应包含以下键: \"name\", \"synopsis\" (性别、年龄、外貌、性格), \"details\" (背景故事), \"relationships\"。",
            storyCard.getTitle(), storyCard.getSynopsis()
        );

        String jsonContent = callOpenAi(prompt, apiKey);
        return objectMapper.readValue(jsonContent, objectMapper.getTypeFactory().constructCollectionType(List.class, CharacterCard.class));
    }

    private String callOpenAi(String prompt, String apiKey) {
        String url = openaiApiUrl + "/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "gpt-4-turbo");
        body.put("messages", List.of(message));
        body.put("response_format", Map.of("type", "json_object"));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();

            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            String jsonContent = (String) ((Map<String, Object>) choices.get(0).get("message")).get("content");

            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                throw new RuntimeException("AI response content is empty.");
            }
            System.out.println("Raw AI JSON response: " + jsonContent);
            return jsonContent;
        } catch (Exception e) {
            System.err.println("Error calling OpenAI API: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get a valid response from OpenAI.", e);
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

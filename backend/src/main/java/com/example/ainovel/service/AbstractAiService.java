package com.example.ainovel.service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestClientException;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;
import com.example.ainovel.dto.RefineRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AbstractAiService.class);
    protected final ObjectMapper objectMapper;

    protected AbstractAiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @Retryable(value = {RestClientException.class, IOException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String generateJson(String prompt, String apiKey) {
        try {
            return callApiForJson(prompt, apiKey);
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON response from AI service", e);
            throw new RuntimeException("Failed to obtain structured JSON from AI service.", e);
        }
    }

    @Override
    @Retryable(value = {RestClientException.class, IOException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String generateJson(String prompt, String apiKey, String baseUrl, String model) {
        try {
            return callApiForJson(prompt, apiKey, baseUrl, model);
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON response from AI service", e);
            throw new RuntimeException("Failed to obtain structured JSON from AI service.", e);
        }
    }

    @Override
    @Retryable(value = {RestClientException.class, IOException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ConceptionResponse generateConception(ConceptionRequest request, String apiKey) {
        String prompt = buildConceptionPrompt(request);
        try {
            log.info("Attempting to generate conception with prompt: {}", prompt);
            String jsonContent = callApiForJson(prompt, apiKey);
            ConceptionResponse conception = parseConceptionResponse(jsonContent);

            if (conception == null || conception.getStoryCard() == null || conception.getCharacterCards() == null) {
                throw new RuntimeException("AI response is missing required fields for conception.");
            }

            conception.getStoryCard().setGenre(request.getGenre());
            conception.getStoryCard().setTone(request.getTone());

            return conception;
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON for conception generation", e);
            throw new RuntimeException("Failed to process JSON for conception generation.", e);
        } catch (Exception e) {
            log.error("Error during conception generation process", e);
            throw new RuntimeException("Failed to generate full story conception.", e);
        }
    }

    @Override
    @Retryable(value = {RestClientException.class, IOException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String refineText(RefineRequest request, String apiKey) {
        String prompt = buildRefinePrompt(request);
        log.info("Attempting to refine text with prompt: {}", prompt);
        return generate(prompt, apiKey);
    }

    protected abstract String callApiForJson(String prompt, String apiKey) throws JsonProcessingException;

    protected abstract ConceptionResponse parseConceptionResponse(String jsonResponse) throws JsonProcessingException;

    private String buildConceptionPrompt(ConceptionRequest request) {
        return String.format(
            "你是一个世界级的小说家。请根据用户输入：'%s'，生成一个详细的故事构思。请严格按照以下JSON格式返回：\n" +
            "{\n" +
            "  \"storyCard\": {\n" +
            "    \"title\": \"故事标题\",\n" +
            "    \"synopsis\": \"(要求：详细、丰富的故事梗概，长度不少于400字)\",\n" +
            "    \"worldview\": \"(要求：基于梗概生成详细的世界观设定)\"\n" +
            "  },\n" +
            "  \"characterCards\": [\n" +
            "    {\n" +
            "      \"name\": \"角色姓名\",\n" +
            "      \"synopsis\": \"角色简介（年龄、性别、外貌、性格等）\",\n" +
            "      \"details\": \"角色的详细背景故事和设定\",\n" +
            "      \"relationships\": \"角色与其他主要角色的关系\",\n" +
            "      \"avatarUrl\": \"（可选）角色的头像URL\"\n" +
            "    }\n" +
            "  ]\n" +
            "}\n" +
            "characterCards要基于梗概生成3-5个主要角色描述。只返回JSON对象，不要包含任何额外的解释或markdown格式。",
            request.getIdea()
        );
    }

    private String buildRefinePrompt(RefineRequest request) {
        String contextInstruction = "";
        if (request.getContextType() != null && !request.getContextType().isBlank()) {
            contextInstruction = String.format("这是一个关于“%s”的文本。\n", request.getContextType());
        }

        if (request.getInstruction() != null && !request.getInstruction().trim().isEmpty()) {
            return String.format(
                "你是一个专业的编辑。请根据我的修改意见，优化以下文本。%s请只返回优化后的文本内容，不要包含任何解释性文字或Markdown格式。\n\n" +
                "原始文本:\n\"%s\"\n\n" +
                "我的意见:\n\"%s\"",
                contextInstruction, request.getText(), request.getInstruction()
            );
        } else {
            return String.format(
                "你是一个专业的编辑。请优化以下文本，使其更生动、更具吸引力。%s请只返回优化后的文本内容，不要包含任何解释性文字或Markdown格式。\n\n" +
                "原始文本:\n\"%s\"",
                contextInstruction, request.getText()
            );
        }
    }
    // Extended variants to support per-user baseUrl and model
    @Override
    @Retryable(value = {RestClientException.class, IOException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ConceptionResponse generateConception(ConceptionRequest request, String apiKey, String baseUrl, String model) {
        String prompt = buildConceptionPrompt(request);
        try {
            log.info("Attempting to generate conception with prompt: {}", prompt);
            String jsonContent = callApiForJson(prompt, apiKey, baseUrl, model);
            ConceptionResponse conception = parseConceptionResponse(jsonContent);

            if (conception == null || conception.getStoryCard() == null || conception.getCharacterCards() == null) {
                throw new RuntimeException("AI response is missing required fields for conception.");
            }

            conception.getStoryCard().setGenre(request.getGenre());
            conception.getStoryCard().setTone(request.getTone());

            return conception;
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON for conception generation", e);
            throw new RuntimeException("Failed to process JSON for conception generation.", e);
        } catch (Exception e) {
            log.error("Error during conception generation process", e);
            throw new RuntimeException("Failed to generate full story conception.", e);
        }
    }

    @Override
    @Retryable(value = {RestClientException.class, IOException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String refineText(RefineRequest request, String apiKey, String baseUrl, String model) {
        String prompt = buildRefinePrompt(request);
        log.info("Attempting to refine text with prompt: {}", prompt);
        return generate(prompt, apiKey, baseUrl, model);
    }

    // Overload for subclasses that support baseUrl/model (defaults to legacy two-arg behavior)
    protected String callApiForJson(String prompt, String apiKey, String baseUrl, String model) throws JsonProcessingException {
        return callApiForJson(prompt, apiKey);
    }
}

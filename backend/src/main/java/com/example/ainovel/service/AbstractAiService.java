package com.example.ainovel.service;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;
import com.example.ainovel.dto.RefineRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestClientException;

import java.io.IOException;

public abstract class AbstractAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AbstractAiService.class);
    protected final ObjectMapper objectMapper;

    protected AbstractAiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    @Retryable(value = {RestClientException.class, IOException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
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
    @Retryable(value = {RestClientException.class, IOException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public String refineText(RefineRequest request, String apiKey) {
        String prompt = buildRefinePrompt(request);
        log.info("Attempting to refine text with prompt: {}", prompt);
        return generate(prompt, apiKey);
    }

    protected abstract String callApiForJson(String prompt, String apiKey) throws JsonProcessingException;

    protected abstract ConceptionResponse parseConceptionResponse(String jsonResponse) throws JsonProcessingException;

    private String buildConceptionPrompt(ConceptionRequest request) {
        return String.format(
            "你是一个富有想象力的故事作家。请根据以下信息，为我构思一个故事。请使用简体中文进行创作。\n" +
            "用户想法: \"%s\"\n" +
            "故事类型: \"%s\" (此字段必须使用简体中文)。\n" +
            "故事基调: \"%s\" (此字段必须使用简体中文)。\n\n" +
            "请以JSON格式返回，包含两个键: \"storyCard\" 和 \"characterCards\"。\n" +
            "\"storyCard\" 的值应包含 \"title\", \"synopsis\" (生成一段更长的故事梗概，至少300字), \"storyArc\"。\n" +
            "\"characterCards\" 的值应为一个数组，包含至少3个角色。请确保角色之间存在明确的关系（如亲情、友情、爱情、敌对等），并在 \"relationships\" 字段中详细描述这些关系。\n" +
            "每个角色对象应包含 \"name\", \"synopsis\" (性别、年龄、外貌、性格), \"details\" (背景故事), \"relationships\"。\n" +
            "请只返回JSON对象，不要包含任何额外的解释或markdown格式。",
            request.getIdea(), request.getGenre(), request.getTone()
        );
    }

    private String buildRefinePrompt(RefineRequest request) {
        if (request.getUserFeedback() != null && !request.getUserFeedback().trim().isEmpty()) {
            return String.format(
                "你是一个专业的编辑。请根据我的修改意见，优化以下文本。请只返回优化后的文本内容，不要包含任何解释性文字或Markdown格式。\n\n" +
                "原始文本:\n\"%s\"\n\n" +
                "我的意见:\n\"%s\"",
                request.getOriginalText(), request.getUserFeedback()
            );
        } else {
            return String.format(
                "你是一个专业的编辑。请优化以下文本，使其更生动、更具吸引力。请只返回优化后的文本内容，不要包含任何解释性文字或Markdown格式。\n\n" +
                "原始文本:\n\"%s\"",
                request.getOriginalText()
            );
        }
    }
}
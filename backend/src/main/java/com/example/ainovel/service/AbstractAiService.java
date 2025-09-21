package com.example.ainovel.service;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestClientException;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;
import com.example.ainovel.dto.RefineRequest;
import com.example.ainovel.prompt.PromptTemplateService;
import com.example.ainovel.prompt.PromptType;
import com.example.ainovel.prompt.context.PromptContextFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AbstractAiService.class);
    protected final ObjectMapper objectMapper;
    protected final PromptTemplateService promptTemplateService;
    protected final PromptContextFactory promptContextFactory;

    protected AbstractAiService(ObjectMapper objectMapper,
                                PromptTemplateService promptTemplateService,
                                PromptContextFactory promptContextFactory) {
        this.objectMapper = objectMapper;
        this.promptTemplateService = promptTemplateService;
        this.promptContextFactory = promptContextFactory;
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
        return generateConception(null, request, apiKey, null, null);
    }

    @Override
    @Retryable(value = {RestClientException.class, IOException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ConceptionResponse generateConception(Long userId, ConceptionRequest request, String apiKey, String baseUrl, String model) {
        Map<String, Object> context = promptContextFactory.buildStoryCreationContext(request);
        String prompt = promptTemplateService.render(PromptType.STORY_CREATION, userId, context);
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
    public String refineText(RefineRequest request, String apiKey) {
        return refineText(null, request, apiKey, null, null);
    }

    protected abstract String callApiForJson(String prompt, String apiKey) throws JsonProcessingException;

    protected abstract ConceptionResponse parseConceptionResponse(String jsonResponse) throws JsonProcessingException;

    @Override
    @Retryable(value = {RestClientException.class, IOException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ConceptionResponse generateConception(ConceptionRequest request, String apiKey, String baseUrl, String model) {
        return generateConception(null, request, apiKey, baseUrl, model);
    }

    @Override
    @Retryable(value = {RestClientException.class, IOException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String refineText(RefineRequest request, String apiKey, String baseUrl, String model) {
        return refineText(null, request, apiKey, baseUrl, model);
    }

    @Override
    @Retryable(value = {RestClientException.class, IOException.class, RuntimeException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String refineText(Long userId, RefineRequest request, String apiKey, String baseUrl, String model) {
        Map<String, Object> context = promptContextFactory.buildRefineContext(request);
        PromptType type = (request.getInstruction() != null && !request.getInstruction().trim().isEmpty())
                ? PromptType.REFINE_WITH_INSTRUCTION
                : PromptType.REFINE_WITHOUT_INSTRUCTION;
        String prompt = promptTemplateService.render(type, userId, context);
        log.info("Attempting to refine text with prompt: {}", prompt);
        return generate(prompt, apiKey, baseUrl, model);
    }

    // Overload for subclasses that support baseUrl/model (defaults to legacy two-arg behavior)
    protected String callApiForJson(String prompt, String apiKey, String baseUrl, String model) throws JsonProcessingException {
        return callApiForJson(prompt, apiKey);
    }
}

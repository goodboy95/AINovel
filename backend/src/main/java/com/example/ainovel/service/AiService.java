package com.example.ainovel.service;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;
import com.example.ainovel.dto.RefineRequest;

public interface AiService {
    // Legacy minimal signatures (kept for backward compatibility)
    String generate(String prompt, String apiKey);

    default String generate(String prompt, String apiKey, String baseUrl, String model) {
        return generate(prompt, apiKey);
    }

    String generateJson(String prompt, String apiKey);

    default String generateJson(String prompt, String apiKey, String baseUrl, String model) {
        return generateJson(prompt, apiKey);
    }

    ConceptionResponse generateConception(ConceptionRequest request, String apiKey);

    default ConceptionResponse generateConception(ConceptionRequest request, String apiKey, String baseUrl, String model) {
        return generateConception(request, apiKey);
    }

    String refineText(RefineRequest request, String apiKey);

    default String refineText(RefineRequest request, String apiKey, String baseUrl, String model) {
        return refineText(request, apiKey);
    }

    boolean validateApiKey(String apiKey);

    default boolean validateApiKey(String apiKey, String baseUrl) {
        return validateApiKey(apiKey);
    }
}

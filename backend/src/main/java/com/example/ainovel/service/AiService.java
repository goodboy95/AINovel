package com.example.ainovel.service;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;
import com.example.ainovel.dto.RefineRequest;

public interface AiService {
    String generate(String prompt, String apiKey);

    ConceptionResponse generateConception(ConceptionRequest request, String apiKey);

    String refineText(RefineRequest request, String apiKey);

    boolean validateApiKey(String apiKey);
}

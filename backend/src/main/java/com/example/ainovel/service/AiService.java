package com.example.ainovel.service;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;

public interface AiService {
    String generate(String prompt, String apiKey);

    ConceptionResponse generateConception(ConceptionRequest request, String apiKey);
}

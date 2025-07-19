package com.example.ainovel.service;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;

public interface AiService {
    ConceptionResponse generateStory(ConceptionRequest request, String apiKey);
}

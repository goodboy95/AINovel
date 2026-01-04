package com.ainovel.app.ai.dto;

import java.util.List;

public record AiChatRequest(List<Message> messages, String modelId, Object context) {
    public record Message(String role, String content) {}
}


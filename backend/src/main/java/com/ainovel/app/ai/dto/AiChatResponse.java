package com.ainovel.app.ai.dto;

public record AiChatResponse(String role, String content, AiUsageDto usage, double remainingCredits) {}


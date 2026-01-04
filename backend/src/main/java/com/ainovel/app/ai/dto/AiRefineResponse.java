package com.ainovel.app.ai.dto;

public record AiRefineResponse(String result, AiUsageDto usage, double remainingCredits) {}


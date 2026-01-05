package com.ainovel.app.user.dto;

public record UserSummaryResponse(
        long novelCount,
        long worldCount,
        long totalWords,
        long totalEntries
) {}


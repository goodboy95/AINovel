package com.ainovel.app.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record CreditLogDto(
        UUID id,
        UUID userId,
        double amount,
        String reason,
        String details,
        Instant createdAt
) {}


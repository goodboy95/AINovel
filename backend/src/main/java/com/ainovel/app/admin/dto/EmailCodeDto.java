package com.ainovel.app.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record EmailCodeDto(
        UUID id,
        String email,
        String code,
        String purpose,
        boolean used,
        Instant expiresAt,
        Instant createdAt,
        Instant usedAt
) {}


package com.ainovel.app.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record RedeemCodeDto(
        UUID id,
        String code,
        int amount,
        boolean isUsed,
        String usedBy,
        Instant expiresAt
) {}


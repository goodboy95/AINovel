package com.ainovel.app.ai.dto;

import java.util.UUID;

public record AiModelDto(
        UUID id,
        String name,
        String displayName,
        double inputMultiplier,
        double outputMultiplier,
        String poolId,
        boolean isEnabled
) {}


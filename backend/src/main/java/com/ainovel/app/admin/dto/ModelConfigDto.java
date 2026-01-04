package com.ainovel.app.admin.dto;

import java.util.UUID;

public record ModelConfigDto(
        UUID id,
        String name,
        String displayName,
        double inputMultiplier,
        double outputMultiplier,
        String poolId,
        boolean isEnabled
) {}


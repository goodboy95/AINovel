package com.ainovel.app.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String email,
        String avatar,
        String role,
        double credits,
        boolean isBanned,
        Instant lastCheckIn
) {}


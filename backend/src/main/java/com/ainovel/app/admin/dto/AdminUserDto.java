package com.ainovel.app.admin.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminUserDto(
        UUID id,
        String username,
        String email,
        String role,
        double credits,
        boolean isBanned,
        Instant lastCheckIn
) {}


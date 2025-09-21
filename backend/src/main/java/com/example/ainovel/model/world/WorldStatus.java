package com.example.ainovel.model.world;

import java.util.Locale;
import java.util.Optional;

public enum WorldStatus {
    DRAFT,
    GENERATING,
    ACTIVE,
    ARCHIVED;

    public static Optional<WorldStatus> fromString(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (WorldStatus status : values()) {
            if (status.name().equals(normalized)) {
                return Optional.of(status);
            }
        }
        return Optional.empty();
    }
}

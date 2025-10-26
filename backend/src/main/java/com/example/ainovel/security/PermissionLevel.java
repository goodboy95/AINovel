package com.example.ainovel.security;

/**
 * Supported permission levels for ACL checks.
 */
public enum PermissionLevel {
    READ(1),
    WRITE(2),
    ADMIN(3);

    private final int weight;

    PermissionLevel(int weight) {
        this.weight = weight;
    }

    public boolean satisfies(PermissionLevel required) {
        return weight >= required.weight;
    }

    public static PermissionLevel fromString(String value) {
        if (value == null) {
            return READ;
        }
        for (PermissionLevel level : values()) {
            if (level.name().equalsIgnoreCase(value)) {
                return level;
            }
        }
        return READ;
    }
}


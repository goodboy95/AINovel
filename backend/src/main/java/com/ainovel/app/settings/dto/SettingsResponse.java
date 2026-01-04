package com.ainovel.app.settings.dto;

public record SettingsResponse(
        String baseUrl,
        String modelName,
        boolean apiKeyIsSet,
        boolean registrationEnabled,
        boolean maintenanceMode,
        int checkInMinPoints,
        int checkInMaxPoints
) {}

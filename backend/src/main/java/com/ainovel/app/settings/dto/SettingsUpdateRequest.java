package com.ainovel.app.settings.dto;

public record SettingsUpdateRequest(
        String baseUrl,
        String modelName,
        String apiKey,
        Boolean registrationEnabled,
        Boolean maintenanceMode,
        Integer checkInMinPoints,
        Integer checkInMaxPoints
) {}

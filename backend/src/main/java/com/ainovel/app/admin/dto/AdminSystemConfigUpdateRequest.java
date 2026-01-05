package com.ainovel.app.admin.dto;

public record AdminSystemConfigUpdateRequest(
        Boolean registrationEnabled,
        Boolean maintenanceMode,
        Integer checkInMinPoints,
        Integer checkInMaxPoints,
        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        String smtpPassword,
        String llmBaseUrl,
        String llmModelName,
        String llmApiKey
) {}


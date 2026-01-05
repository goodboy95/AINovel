package com.ainovel.app.admin.dto;

public record AdminSystemConfigResponse(
        boolean registrationEnabled,
        boolean maintenanceMode,
        int checkInMinPoints,
        int checkInMaxPoints,
        String smtpHost,
        Integer smtpPort,
        String smtpUsername,
        boolean smtpPasswordIsSet,
        String llmBaseUrl,
        String llmModelName,
        boolean llmApiKeyIsSet
) {}


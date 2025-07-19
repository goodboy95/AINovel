package com.ainovel.app.settings.dto;

import jakarta.validation.constraints.NotBlank;

public record SettingsUpdateRequest(String baseUrl, String modelName, String apiKey) {}

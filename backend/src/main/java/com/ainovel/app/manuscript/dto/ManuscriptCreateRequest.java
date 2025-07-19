package com.ainovel.app.manuscript.dto;

import jakarta.validation.constraints.NotBlank;

public record ManuscriptCreateRequest(@NotBlank String title, String worldId) {}

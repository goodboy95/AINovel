package com.ainovel.app.story.dto;

import jakarta.validation.constraints.NotBlank;

public record CharacterRequest(@NotBlank String name,
                               String synopsis,
                               String details,
                               String relationships) {}

package com.ainovel.app.story.dto;

import jakarta.validation.constraints.NotBlank;

public record StoryCreateRequest(@NotBlank String title,
                                 String synopsis,
                                 String genre,
                                 String tone,
                                 String worldId) {}

package com.ainovel.app.story.dto;

import java.time.Instant;
import java.util.UUID;

public record StoryDto(UUID id, String title, String synopsis, String genre, String tone, String status, String worldId, Instant updatedAt) {}

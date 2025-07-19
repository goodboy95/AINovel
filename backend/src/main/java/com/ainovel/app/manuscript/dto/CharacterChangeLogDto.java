package com.ainovel.app.manuscript.dto;

import java.time.Instant;
import java.util.UUID;

public record CharacterChangeLogDto(UUID id, UUID characterId, String summary, Instant createdAt) {}

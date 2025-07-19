package com.ainovel.app.manuscript.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ManuscriptDto(UUID id,
                            UUID outlineId,
                            String title,
                            String worldId,
                            Map<String, String> sections,
                            Instant updatedAt) {}

package com.ainovel.app.world.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record WorldDetailDto(UUID id,
                             String name,
                             String tagline,
                             String status,
                             String version,
                             List<String> themes,
                             String creativeIntent,
                             String notes,
                             Map<String, Map<String, String>> modules,
                             Map<String, String> moduleProgress,
                             Instant updatedAt) {}

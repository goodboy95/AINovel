package com.ainovel.app.world.dto;

import java.time.Instant;
import java.util.UUID;

public record WorldDto(UUID id, String name, String tagline, String status, String version, Instant updatedAt) {}

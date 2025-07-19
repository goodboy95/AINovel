package com.ainovel.app.material.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MaterialDto(UUID id,
                          String title,
                          String type,
                          String summary,
                          String content,
                          List<String> tags,
                          String status,
                          Instant createdAt) {}

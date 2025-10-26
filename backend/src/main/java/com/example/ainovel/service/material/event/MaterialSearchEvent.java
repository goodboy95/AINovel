package com.example.ainovel.service.material.event;

public record MaterialSearchEvent(Long workspaceId,
                                  Long userId,
                                  String query,
                                  long latencyMs,
                                  boolean usedInGeneration) {
}


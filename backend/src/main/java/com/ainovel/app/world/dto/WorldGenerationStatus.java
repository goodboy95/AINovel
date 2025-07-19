package com.ainovel.app.world.dto;

import java.util.List;

public record WorldGenerationStatus(List<ModuleStatus> queue) {
    public record ModuleStatus(String moduleKey, String status, int attempts, String error) {}
}

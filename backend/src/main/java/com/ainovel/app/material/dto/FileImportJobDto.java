package com.ainovel.app.material.dto;

import java.util.UUID;

public record FileImportJobDto(UUID id, String fileName, String status, int progress, String message) {}

package com.ainovel.app.world.dto;

import java.util.List;

public record WorldPublishPreviewResponse(List<String> missingModules, List<String> modulesToGenerate) {}

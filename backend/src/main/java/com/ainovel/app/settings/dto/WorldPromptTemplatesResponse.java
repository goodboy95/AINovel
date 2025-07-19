package com.ainovel.app.settings.dto;

import java.util.Map;

public record WorldPromptTemplatesResponse(Map<String, String> modules,
                                           Map<String, String> finalTemplates,
                                           String fieldRefine) {}

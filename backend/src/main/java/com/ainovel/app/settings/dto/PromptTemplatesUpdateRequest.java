package com.ainovel.app.settings.dto;

public record PromptTemplatesUpdateRequest(String storyCreation,
                                           String outlineChapter,
                                           String manuscriptSection,
                                           String refineWithInstruction,
                                           String refineWithoutInstruction) {}

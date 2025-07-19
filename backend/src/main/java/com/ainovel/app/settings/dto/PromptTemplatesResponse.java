package com.ainovel.app.settings.dto;

public record PromptTemplatesResponse(String storyCreation,
                                      String outlineChapter,
                                      String manuscriptSection,
                                      String refineWithInstruction,
                                      String refineWithoutInstruction) {}

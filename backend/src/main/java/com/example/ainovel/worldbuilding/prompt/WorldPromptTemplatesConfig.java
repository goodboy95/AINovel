package com.example.ainovel.worldbuilding.prompt;

import java.util.Map;

public record WorldPromptTemplatesConfig(Map<String, String> draftTemplates,
                                         Map<String, String> finalTemplates,
                                         String fieldRefineTemplate,
                                         Map<String, Map<String, String>> focusNotes) {
}

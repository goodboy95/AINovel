package com.example.ainovel.worldbuilding.prompt;

import java.util.Map;

public record WorldPromptTemplatesConfig(Map<String, ModuleTemplate> modules,
                                         String fieldRefineTemplate,
                                         Map<String, Map<String, String>> focusNotes) {

    public record ModuleTemplate(String draft,
                                 @com.fasterxml.jackson.annotation.JsonProperty("final") String finalTemplate) {
    }
}

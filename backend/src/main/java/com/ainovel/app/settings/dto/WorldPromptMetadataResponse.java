package com.ainovel.app.settings.dto;

import java.util.List;

public record WorldPromptMetadataResponse(List<Variable> variables,
                                          List<FunctionItem> functions,
                                          List<Module> modules,
                                          List<String> examples) {
    public record Variable(String name, String valueType, String description) {}
    public record FunctionItem(String name, String description, String usage) {}
    public record Module(String key, String label, List<ModuleField> fields) {}
    public record ModuleField(String key, String label, Integer recommendedLength) {}
}

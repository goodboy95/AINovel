package com.ainovel.app.settings.dto;

import java.util.List;

public record PromptMetadataResponse(List<SyntaxTip> syntaxTips,
                                     List<PromptFunction> functions,
                                     List<TemplateMetadata> templates,
                                     List<String> examples) {

    public record SyntaxTip(String title, String description) {}
    public record PromptFunction(String name, String description, String usage) {}
    public record TemplateMetadata(String key, List<Variable> variables) {}
    public record Variable(String name, String valueType, String description) {}
}

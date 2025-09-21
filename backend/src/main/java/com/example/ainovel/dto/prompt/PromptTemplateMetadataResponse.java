package com.example.ainovel.dto.prompt;

import java.util.List;

public class PromptTemplateMetadataResponse {

    private List<PromptTypeMetadataDto> templates;
    private List<PromptFunctionMetadataDto> functions;
    private List<String> syntaxTips;
    private List<String> examples;

    public List<PromptTypeMetadataDto> getTemplates() {
        return templates;
    }

    public PromptTemplateMetadataResponse setTemplates(List<PromptTypeMetadataDto> templates) {
        this.templates = templates;
        return this;
    }

    public List<PromptFunctionMetadataDto> getFunctions() {
        return functions;
    }

    public PromptTemplateMetadataResponse setFunctions(List<PromptFunctionMetadataDto> functions) {
        this.functions = functions;
        return this;
    }

    public List<String> getSyntaxTips() {
        return syntaxTips;
    }

    public PromptTemplateMetadataResponse setSyntaxTips(List<String> syntaxTips) {
        this.syntaxTips = syntaxTips;
        return this;
    }

    public List<String> getExamples() {
        return examples;
    }

    public PromptTemplateMetadataResponse setExamples(List<String> examples) {
        this.examples = examples;
        return this;
    }
}

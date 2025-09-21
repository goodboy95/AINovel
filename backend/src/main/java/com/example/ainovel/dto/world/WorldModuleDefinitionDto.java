package com.example.ainovel.dto.world;

import java.util.List;

public class WorldModuleDefinitionDto {

    private String key;
    private String label;
    private String description;
    private List<WorldFieldDefinitionDto> fields;
    private String aiGenerationTemplate;
    private String finalTemplate;

    public String getKey() {
        return key;
    }

    public WorldModuleDefinitionDto setKey(String key) {
        this.key = key;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public WorldModuleDefinitionDto setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public WorldModuleDefinitionDto setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<WorldFieldDefinitionDto> getFields() {
        return fields;
    }

    public WorldModuleDefinitionDto setFields(List<WorldFieldDefinitionDto> fields) {
        this.fields = fields;
        return this;
    }

    public String getAiGenerationTemplate() {
        return aiGenerationTemplate;
    }

    public WorldModuleDefinitionDto setAiGenerationTemplate(String aiGenerationTemplate) {
        this.aiGenerationTemplate = aiGenerationTemplate;
        return this;
    }

    public String getFinalTemplate() {
        return finalTemplate;
    }

    public WorldModuleDefinitionDto setFinalTemplate(String finalTemplate) {
        this.finalTemplate = finalTemplate;
        return this;
    }
}

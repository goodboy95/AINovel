package com.example.ainovel.dto.prompt;

public class PromptVariableMetadataDto {

    private String name;
    private String valueType;
    private String description;

    public PromptVariableMetadataDto() {
    }

    public PromptVariableMetadataDto(String name, String valueType, String description) {
        this.name = name;
        this.valueType = valueType;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public PromptVariableMetadataDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getValueType() {
        return valueType;
    }

    public PromptVariableMetadataDto setValueType(String valueType) {
        this.valueType = valueType;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PromptVariableMetadataDto setDescription(String description) {
        this.description = description;
        return this;
    }
}

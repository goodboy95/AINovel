package com.example.ainovel.dto.world;

public class WorldPromptVariableMetadataDto {

    private String name;
    private String valueType;
    private String description;

    public WorldPromptVariableMetadataDto() {
    }

    public WorldPromptVariableMetadataDto(String name, String valueType, String description) {
        this.name = name;
        this.valueType = valueType;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public WorldPromptVariableMetadataDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getValueType() {
        return valueType;
    }

    public WorldPromptVariableMetadataDto setValueType(String valueType) {
        this.valueType = valueType;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public WorldPromptVariableMetadataDto setDescription(String description) {
        this.description = description;
        return this;
    }
}

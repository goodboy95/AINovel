package com.example.ainovel.dto.world;

public class WorldPromptFunctionMetadataDto {

    private String name;
    private String description;
    private String usage;

    public WorldPromptFunctionMetadataDto() {
    }

    public WorldPromptFunctionMetadataDto(String name, String description, String usage) {
        this.name = name;
        this.description = description;
        this.usage = usage;
    }

    public String getName() {
        return name;
    }

    public WorldPromptFunctionMetadataDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public WorldPromptFunctionMetadataDto setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getUsage() {
        return usage;
    }

    public WorldPromptFunctionMetadataDto setUsage(String usage) {
        this.usage = usage;
        return this;
    }
}

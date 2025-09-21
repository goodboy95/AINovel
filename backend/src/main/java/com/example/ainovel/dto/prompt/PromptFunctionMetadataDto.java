package com.example.ainovel.dto.prompt;

public class PromptFunctionMetadataDto {

    private String name;
    private String description;
    private String usage;

    public PromptFunctionMetadataDto() {
    }

    public PromptFunctionMetadataDto(String name, String description, String usage) {
        this.name = name;
        this.description = description;
        this.usage = usage;
    }

    public String getName() {
        return name;
    }

    public PromptFunctionMetadataDto setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PromptFunctionMetadataDto setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getUsage() {
        return usage;
    }

    public PromptFunctionMetadataDto setUsage(String usage) {
        this.usage = usage;
        return this;
    }
}

package com.example.ainovel.dto.world;

import java.util.List;

public class WorldPromptContextDefinitionDto {

    private String description;
    private String example;
    private List<String> notes;

    public String getDescription() {
        return description;
    }

    public WorldPromptContextDefinitionDto setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getExample() {
        return example;
    }

    public WorldPromptContextDefinitionDto setExample(String example) {
        this.example = example;
        return this;
    }

    public List<String> getNotes() {
        return notes;
    }

    public WorldPromptContextDefinitionDto setNotes(List<String> notes) {
        this.notes = notes;
        return this;
    }
}

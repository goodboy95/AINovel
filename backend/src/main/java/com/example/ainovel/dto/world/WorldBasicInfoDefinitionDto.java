package com.example.ainovel.dto.world;

import java.util.List;

public class WorldBasicInfoDefinitionDto {

    private String description;
    private List<WorldFieldDefinitionDto> fields;

    public String getDescription() {
        return description;
    }

    public WorldBasicInfoDefinitionDto setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<WorldFieldDefinitionDto> getFields() {
        return fields;
    }

    public WorldBasicInfoDefinitionDto setFields(List<WorldFieldDefinitionDto> fields) {
        this.fields = fields;
        return this;
    }
}

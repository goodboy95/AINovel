package com.example.ainovel.dto.world;

import java.util.ArrayList;
import java.util.List;

public class WorldPromptModuleMetadataDto {

    private String key;
    private String label;
    private List<WorldPromptModuleFieldMetadataDto> fields = new ArrayList<>();

    public WorldPromptModuleMetadataDto() {
    }

    public WorldPromptModuleMetadataDto(String key, String label, List<WorldPromptModuleFieldMetadataDto> fields) {
        this.key = key;
        this.label = label;
        this.fields = fields;
    }

    public String getKey() {
        return key;
    }

    public WorldPromptModuleMetadataDto setKey(String key) {
        this.key = key;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public WorldPromptModuleMetadataDto setLabel(String label) {
        this.label = label;
        return this;
    }

    public List<WorldPromptModuleFieldMetadataDto> getFields() {
        return fields;
    }

    public WorldPromptModuleMetadataDto setFields(List<WorldPromptModuleFieldMetadataDto> fields) {
        this.fields = fields;
        return this;
    }
}

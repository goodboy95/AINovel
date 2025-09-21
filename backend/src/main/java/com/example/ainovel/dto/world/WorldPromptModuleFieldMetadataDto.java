package com.example.ainovel.dto.world;

public class WorldPromptModuleFieldMetadataDto {

    private String key;
    private String label;
    private String recommendedLength;

    public WorldPromptModuleFieldMetadataDto() {
    }

    public WorldPromptModuleFieldMetadataDto(String key, String label, String recommendedLength) {
        this.key = key;
        this.label = label;
        this.recommendedLength = recommendedLength;
    }

    public String getKey() {
        return key;
    }

    public WorldPromptModuleFieldMetadataDto setKey(String key) {
        this.key = key;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public WorldPromptModuleFieldMetadataDto setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getRecommendedLength() {
        return recommendedLength;
    }

    public WorldPromptModuleFieldMetadataDto setRecommendedLength(String recommendedLength) {
        this.recommendedLength = recommendedLength;
        return this;
    }
}

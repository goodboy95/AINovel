package com.example.ainovel.dto.world;

public class WorldFieldDefinitionDto {

    private String key;
    private String label;
    private String description;
    private String tooltip;
    private String validation;
    private String recommendedLength;
    private String aiFocus;

    public String getKey() {
        return key;
    }

    public WorldFieldDefinitionDto setKey(String key) {
        this.key = key;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public WorldFieldDefinitionDto setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public WorldFieldDefinitionDto setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getTooltip() {
        return tooltip;
    }

    public WorldFieldDefinitionDto setTooltip(String tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public String getValidation() {
        return validation;
    }

    public WorldFieldDefinitionDto setValidation(String validation) {
        this.validation = validation;
        return this;
    }

    public String getRecommendedLength() {
        return recommendedLength;
    }

    public WorldFieldDefinitionDto setRecommendedLength(String recommendedLength) {
        this.recommendedLength = recommendedLength;
        return this;
    }

    public String getAiFocus() {
        return aiFocus;
    }

    public WorldFieldDefinitionDto setAiFocus(String aiFocus) {
        this.aiFocus = aiFocus;
        return this;
    }
}

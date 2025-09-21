package com.example.ainovel.dto.world;

public class WorldPromptTemplateItemDto {

    private String content;
    private boolean defaultTemplate;

    public WorldPromptTemplateItemDto() {
    }

    public WorldPromptTemplateItemDto(String content, boolean defaultTemplate) {
        this.content = content;
        this.defaultTemplate = defaultTemplate;
    }

    public String getContent() {
        return content;
    }

    public WorldPromptTemplateItemDto setContent(String content) {
        this.content = content;
        return this;
    }

    public boolean isDefaultTemplate() {
        return defaultTemplate;
    }

    public WorldPromptTemplateItemDto setDefaultTemplate(boolean defaultTemplate) {
        this.defaultTemplate = defaultTemplate;
        return this;
    }
}

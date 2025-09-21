package com.example.ainovel.dto.prompt;

public class PromptTemplateItemDto {

    private String content;
    private boolean isDefault;

    public PromptTemplateItemDto() {
    }

    public PromptTemplateItemDto(String content, boolean isDefault) {
        this.content = content;
        this.isDefault = isDefault;
    }

    public String getContent() {
        return content;
    }

    public PromptTemplateItemDto setContent(String content) {
        this.content = content;
        return this;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public PromptTemplateItemDto setDefault(boolean aDefault) {
        isDefault = aDefault;
        return this;
    }
}

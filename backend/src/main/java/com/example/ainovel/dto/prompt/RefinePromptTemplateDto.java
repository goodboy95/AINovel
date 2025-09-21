package com.example.ainovel.dto.prompt;

public class RefinePromptTemplateDto {

    private PromptTemplateItemDto withInstruction;
    private PromptTemplateItemDto withoutInstruction;

    public PromptTemplateItemDto getWithInstruction() {
        return withInstruction;
    }

    public RefinePromptTemplateDto setWithInstruction(PromptTemplateItemDto withInstruction) {
        this.withInstruction = withInstruction;
        return this;
    }

    public PromptTemplateItemDto getWithoutInstruction() {
        return withoutInstruction;
    }

    public RefinePromptTemplateDto setWithoutInstruction(PromptTemplateItemDto withoutInstruction) {
        this.withoutInstruction = withoutInstruction;
        return this;
    }
}

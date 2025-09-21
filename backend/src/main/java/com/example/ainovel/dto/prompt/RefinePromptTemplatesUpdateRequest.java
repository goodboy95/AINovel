package com.example.ainovel.dto.prompt;

public class RefinePromptTemplatesUpdateRequest {

    private String withInstruction;
    private String withoutInstruction;

    public String getWithInstruction() {
        return withInstruction;
    }

    public RefinePromptTemplatesUpdateRequest setWithInstruction(String withInstruction) {
        this.withInstruction = withInstruction;
        return this;
    }

    public String getWithoutInstruction() {
        return withoutInstruction;
    }

    public RefinePromptTemplatesUpdateRequest setWithoutInstruction(String withoutInstruction) {
        this.withoutInstruction = withoutInstruction;
        return this;
    }
}

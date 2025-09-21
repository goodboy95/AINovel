package com.example.ainovel.model.prompt;

public class RefinePromptSettings {

    private String withInstruction;
    private String withoutInstruction;

    public String getWithInstruction() {
        return withInstruction;
    }

    public RefinePromptSettings setWithInstruction(String withInstruction) {
        this.withInstruction = withInstruction;
        return this;
    }

    public String getWithoutInstruction() {
        return withoutInstruction;
    }

    public RefinePromptSettings setWithoutInstruction(String withoutInstruction) {
        this.withoutInstruction = withoutInstruction;
        return this;
    }
}

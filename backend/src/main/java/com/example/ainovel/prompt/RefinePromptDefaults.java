package com.example.ainovel.prompt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RefinePromptDefaults {

    private String withInstruction;
    private String withoutInstruction;

    public String getWithInstruction() {
        return withInstruction;
    }

    public void setWithInstruction(String withInstruction) {
        this.withInstruction = withInstruction;
    }

    public String getWithoutInstruction() {
        return withoutInstruction;
    }

    public void setWithoutInstruction(String withoutInstruction) {
        this.withoutInstruction = withoutInstruction;
    }

    public void validate() {
        if (withInstruction == null || withInstruction.isBlank()) {
            throw new IllegalStateException("Default refine-with-instruction prompt must not be empty");
        }
        if (withoutInstruction == null || withoutInstruction.isBlank()) {
            throw new IllegalStateException("Default refine-without-instruction prompt must not be empty");
        }
    }
}

package com.example.ainovel.dto.prompt;

import java.util.List;

public class PromptTypeMetadataDto {

    private String type;
    private String label;
    private List<PromptVariableMetadataDto> variables;

    public PromptTypeMetadataDto() {
    }

    public PromptTypeMetadataDto(String type, String label, List<PromptVariableMetadataDto> variables) {
        this.type = type;
        this.label = label;
        this.variables = variables;
    }

    public String getType() {
        return type;
    }

    public PromptTypeMetadataDto setType(String type) {
        this.type = type;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public PromptTypeMetadataDto setLabel(String label) {
        this.label = label;
        return this;
    }

    public List<PromptVariableMetadataDto> getVariables() {
        return variables;
    }

    public PromptTypeMetadataDto setVariables(List<PromptVariableMetadataDto> variables) {
        this.variables = variables;
        return this;
    }
}

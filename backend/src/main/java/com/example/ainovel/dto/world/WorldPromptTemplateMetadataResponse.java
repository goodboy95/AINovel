package com.example.ainovel.dto.world;

import java.util.ArrayList;
import java.util.List;

public class WorldPromptTemplateMetadataResponse {

    private List<WorldPromptModuleMetadataDto> modules = new ArrayList<>();
    private List<WorldPromptVariableMetadataDto> variables = new ArrayList<>();
    private List<WorldPromptFunctionMetadataDto> functions = new ArrayList<>();
    private List<String> examples = new ArrayList<>();

    public List<WorldPromptModuleMetadataDto> getModules() {
        return modules;
    }

    public WorldPromptTemplateMetadataResponse setModules(List<WorldPromptModuleMetadataDto> modules) {
        this.modules = modules;
        return this;
    }

    public List<WorldPromptVariableMetadataDto> getVariables() {
        return variables;
    }

    public WorldPromptTemplateMetadataResponse setVariables(List<WorldPromptVariableMetadataDto> variables) {
        this.variables = variables;
        return this;
    }

    public List<WorldPromptFunctionMetadataDto> getFunctions() {
        return functions;
    }

    public WorldPromptTemplateMetadataResponse setFunctions(List<WorldPromptFunctionMetadataDto> functions) {
        this.functions = functions;
        return this;
    }

    public List<String> getExamples() {
        return examples;
    }

    public WorldPromptTemplateMetadataResponse setExamples(List<String> examples) {
        this.examples = examples;
        return this;
    }
}

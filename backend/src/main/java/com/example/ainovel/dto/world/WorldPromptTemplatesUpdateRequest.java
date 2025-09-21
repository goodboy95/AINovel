package com.example.ainovel.dto.world;

import java.util.Map;

public class WorldPromptTemplatesUpdateRequest {

    private Map<String, String> modules;
    private Map<String, String> finalTemplates;
    private String fieldRefine;

    public Map<String, String> getModules() {
        return modules;
    }

    public WorldPromptTemplatesUpdateRequest setModules(Map<String, String> modules) {
        this.modules = modules;
        return this;
    }

    public Map<String, String> getFinalTemplates() {
        return finalTemplates;
    }

    public WorldPromptTemplatesUpdateRequest setFinalTemplates(Map<String, String> finalTemplates) {
        this.finalTemplates = finalTemplates;
        return this;
    }

    public String getFieldRefine() {
        return fieldRefine;
    }

    public WorldPromptTemplatesUpdateRequest setFieldRefine(String fieldRefine) {
        this.fieldRefine = fieldRefine;
        return this;
    }
}

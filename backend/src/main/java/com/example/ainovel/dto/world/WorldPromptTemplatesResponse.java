package com.example.ainovel.dto.world;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorldPromptTemplatesResponse {

    private Map<String, WorldPromptTemplateItemDto> modules = new LinkedHashMap<>();
    private Map<String, WorldPromptTemplateItemDto> finalTemplates = new LinkedHashMap<>();
    private WorldPromptTemplateItemDto fieldRefine;

    public Map<String, WorldPromptTemplateItemDto> getModules() {
        return modules;
    }

    public WorldPromptTemplatesResponse setModules(Map<String, WorldPromptTemplateItemDto> modules) {
        this.modules = modules;
        return this;
    }

    public Map<String, WorldPromptTemplateItemDto> getFinalTemplates() {
        return finalTemplates;
    }

    public WorldPromptTemplatesResponse setFinalTemplates(Map<String, WorldPromptTemplateItemDto> finalTemplates) {
        this.finalTemplates = finalTemplates;
        return this;
    }

    public WorldPromptTemplateItemDto getFieldRefine() {
        return fieldRefine;
    }

    public WorldPromptTemplatesResponse setFieldRefine(WorldPromptTemplateItemDto fieldRefine) {
        this.fieldRefine = fieldRefine;
        return this;
    }
}

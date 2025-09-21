package com.example.ainovel.dto.world;

import java.util.List;

public class WorldBuildingDefinitionResponse {

    private WorldBasicInfoDefinitionDto basicInfo;
    private List<WorldModuleDefinitionDto> modules;
    private WorldFieldRefineTemplateDto fieldRefineTemplate;
    private WorldPromptContextDefinitionDto promptContext;

    public WorldBasicInfoDefinitionDto getBasicInfo() {
        return basicInfo;
    }

    public WorldBuildingDefinitionResponse setBasicInfo(WorldBasicInfoDefinitionDto basicInfo) {
        this.basicInfo = basicInfo;
        return this;
    }

    public List<WorldModuleDefinitionDto> getModules() {
        return modules;
    }

    public WorldBuildingDefinitionResponse setModules(List<WorldModuleDefinitionDto> modules) {
        this.modules = modules;
        return this;
    }

    public WorldFieldRefineTemplateDto getFieldRefineTemplate() {
        return fieldRefineTemplate;
    }

    public WorldBuildingDefinitionResponse setFieldRefineTemplate(WorldFieldRefineTemplateDto fieldRefineTemplate) {
        this.fieldRefineTemplate = fieldRefineTemplate;
        return this;
    }

    public WorldPromptContextDefinitionDto getPromptContext() {
        return promptContext;
    }

    public WorldBuildingDefinitionResponse setPromptContext(WorldPromptContextDefinitionDto promptContext) {
        this.promptContext = promptContext;
        return this;
    }
}

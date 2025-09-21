package com.example.ainovel.service.world;

import com.example.ainovel.worldbuilding.definition.WorldModuleDefinitionRegistry;
import com.example.ainovel.worldbuilding.prompt.WorldPromptTemplatesConfig;
import com.example.ainovel.worldbuilding.prompt.WorldPromptTemplatesConfig.ModuleTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.example.ainovel.prompt.TemplateEngine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class WorldPromptTemplateService {

    private final TemplateEngine templateEngine;
    private final WorldModuleDefinitionRegistry definitionRegistry;
    private final Map<String, ModuleTemplate> moduleTemplates;
    private final Map<String, Map<String, String>> focusNotes;
    private final String fieldRefineTemplate;

    public WorldPromptTemplateService(TemplateEngine templateEngine,
                                      WorldModuleDefinitionRegistry definitionRegistry,
                                      @Value("classpath:worldbuilding/prompts.yml") Resource resource) {
        this.templateEngine = templateEngine;
        this.definitionRegistry = definitionRegistry;
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.findAndRegisterModules();
        try (InputStream inputStream = resource.getInputStream()) {
            WorldPromptTemplatesConfig config = objectMapper.readValue(inputStream, WorldPromptTemplatesConfig.class);
            this.moduleTemplates = config.modules() == null ? Collections.emptyMap() : new HashMap<>(config.modules());
            this.focusNotes = config.focusNotes() == null ? Collections.emptyMap()
                    : new HashMap<>(config.focusNotes());
            this.fieldRefineTemplate = config.fieldRefineTemplate();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load world prompt templates", e);
        }
    }

    public String renderDraftTemplate(String moduleKey, Map<String, Object> context) {
        ModuleTemplate template = requireModuleTemplate(moduleKey);
        return templateEngine.render(template.draft(), contextWithDefinitions(moduleKey, context));
    }

    public String renderFinalTemplate(String moduleKey, Map<String, Object> context) {
        ModuleTemplate template = requireModuleTemplate(moduleKey);
        return templateEngine.render(template.finalTemplate(), contextWithDefinitions(moduleKey, context));
    }

    public String renderFieldRefineTemplate(Map<String, Object> context) {
        if (fieldRefineTemplate == null || fieldRefineTemplate.isBlank()) {
            throw new IllegalStateException("Field refine template is not configured");
        }
        return templateEngine.render(fieldRefineTemplate, context);
    }

    public String resolveFocusNote(String moduleKey, String fieldKey) {
        Map<String, String> moduleNotes = focusNotes.get(moduleKey);
        if (moduleNotes == null) {
            return null;
        }
        return moduleNotes.get(fieldKey);
    }

    private ModuleTemplate requireModuleTemplate(String moduleKey) {
        ModuleTemplate template = moduleTemplates.get(moduleKey);
        if (template == null) {
            throw new IllegalArgumentException("未配置模块 " + moduleKey + " 的提示词模板");
        }
        return template;
    }

    private Map<String, Object> contextWithDefinitions(String moduleKey, Map<String, Object> context) {
        Map<String, Object> enriched = new HashMap<>(context == null ? Map.of() : context);
        enriched.putIfAbsent("definition", definitionRegistry.requireModule(moduleKey));
        return enriched;
    }
}

package com.example.ainovel.worldbuilding.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class WorldPromptDefaults {

    private final Map<String, String> draftTemplates;
    private final Map<String, String> finalTemplates;
    private final Map<String, Map<String, String>> focusNotes;
    private final String fieldRefineTemplate;

    public WorldPromptDefaults(@Value("classpath:prompts/world-defaults.yaml") Resource resource) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.findAndRegisterModules();
        try (InputStream inputStream = resource.getInputStream()) {
            WorldPromptTemplatesConfig config = objectMapper.readValue(inputStream, WorldPromptTemplatesConfig.class);
            this.draftTemplates = toUnmodifiableMap(config == null ? null : config.draftTemplates());
            this.finalTemplates = toUnmodifiableMap(config == null ? null : config.finalTemplates());
            this.focusNotes = toNestedUnmodifiableMap(config == null ? null : config.focusNotes());
            this.fieldRefineTemplate = config == null ? null : config.fieldRefineTemplate();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load world prompt defaults", e);
        }
    }

    public Map<String, String> getDraftTemplates() {
        return draftTemplates;
    }

    public Map<String, String> getFinalTemplates() {
        return finalTemplates;
    }

    public Map<String, Map<String, String>> getFocusNotes() {
        return focusNotes;
    }

    public String getFieldRefineTemplate() {
        return fieldRefineTemplate;
    }

    public String getDraftTemplate(String moduleKey) {
        return draftTemplates.get(moduleKey);
    }

    public String getFinalTemplate(String moduleKey) {
        return finalTemplates.get(moduleKey);
    }

    public String getFocusNote(String moduleKey, String fieldKey) {
        Map<String, String> moduleNotes = focusNotes.get(moduleKey);
        if (moduleNotes == null) {
            return null;
        }
        return moduleNotes.get(fieldKey);
    }

    private Map<String, String> toUnmodifiableMap(Map<String, String> source) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    private Map<String, Map<String, String>> toNestedUnmodifiableMap(Map<String, Map<String, String>> source) {
        if (CollectionUtils.isEmpty(source)) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : source.entrySet()) {
            Map<String, String> value = entry.getValue();
            if (value == null) {
                result.put(entry.getKey(), Collections.emptyMap());
            } else {
                result.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(value)));
            }
        }
        return Collections.unmodifiableMap(result);
    }
}

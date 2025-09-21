package com.example.ainovel.worldbuilding.definition;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WorldModuleDefinitionRegistry {

    private final Map<String, WorldModuleDefinition> definitionMap = new ConcurrentHashMap<>();
    private final List<WorldModuleDefinition> orderedDefinitions;

    public WorldModuleDefinitionRegistry(@Value("classpath:worldbuilding/modules.yml") Resource resource) {
        Objects.requireNonNull(resource, "Module definition resource must not be null");
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.findAndRegisterModules();
        try (InputStream inputStream = resource.getInputStream()) {
            WorldModuleDefinitionsConfig config = objectMapper.readValue(inputStream, WorldModuleDefinitionsConfig.class);
            if (config == null || config.modules() == null || config.modules().isEmpty()) {
                throw new IllegalStateException("World module definitions must not be empty");
            }
            orderedDefinitions = config.modules().stream()
                    .sorted(Comparator.comparingInt(WorldModuleDefinition::order))
                    .toList();
            for (WorldModuleDefinition definition : orderedDefinitions) {
                definitionMap.put(definition.key(), definition);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load world module definitions", e);
        }
    }

    public List<WorldModuleDefinition> getAll() {
        return orderedDefinitions;
    }

    public Optional<WorldModuleDefinition> find(String key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitionMap.get(key));
    }

    public WorldModuleDefinition requireModule(String key) {
        return find(key).orElseThrow(() -> new IllegalArgumentException("Unknown module key: " + key));
    }

    public WorldModuleDefinition.FieldDefinition requireField(String moduleKey, String fieldKey) {
        WorldModuleDefinition moduleDefinition = requireModule(moduleKey);
        return moduleDefinition.fields().stream()
                .filter(field -> field.key().equals(fieldKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown field key: " + fieldKey + " for module " + moduleKey));
    }

    public String resolveLabel(String moduleKey) {
        return find(moduleKey).map(WorldModuleDefinition::label)
                .orElse(moduleKey);
    }

    public Optional<String> resolveFieldLabel(String moduleKey, String fieldKey) {
        return find(moduleKey).flatMap(def -> def.fields().stream()
                .filter(field -> field.key().equals(fieldKey))
                .map(WorldModuleDefinition.FieldDefinition::label)
                .findFirst());
    }
}

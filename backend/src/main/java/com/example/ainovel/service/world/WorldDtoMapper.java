package com.example.ainovel.service.world;

import com.example.ainovel.dto.world.WorldDetailResponse;
import com.example.ainovel.dto.world.WorldModuleResponse;
import com.example.ainovel.dto.world.WorldModuleSummary;
import com.example.ainovel.dto.world.WorldFullResponse;
import com.example.ainovel.dto.world.WorldFullResponse.WorldInfo;
import com.example.ainovel.dto.world.WorldFullResponse.WorldModuleFull;
import com.example.ainovel.dto.world.WorldPublishPreviewResponse;
import com.example.ainovel.dto.world.WorldSummaryResponse;
import com.example.ainovel.model.world.World;
import com.example.ainovel.model.world.WorldModule;
import com.example.ainovel.model.world.WorldModuleStatus;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinition;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinitionRegistry;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class WorldDtoMapper {

    private final WorldModuleDefinitionRegistry definitionRegistry;

    public WorldDtoMapper(WorldModuleDefinitionRegistry definitionRegistry) {
        this.definitionRegistry = definitionRegistry;
    }

    public WorldDetailResponse toDetail(World world, List<WorldModule> modules) {
        WorldDetailResponse.WorldInfo info = new WorldDetailResponse.WorldInfo()
                .setId(world.getId())
                .setName(world.getName())
                .setTagline(world.getTagline())
                .setThemes(world.getThemes())
                .setCreativeIntent(world.getCreativeIntent())
                .setNotes(world.getNotes())
                .setStatus(world.getStatus())
                .setVersion(world.getVersion())
                .setPublishedAt(world.getPublishedAt())
                .setUpdatedAt(world.getUpdatedAt())
                .setCreatedAt(world.getCreatedAt())
                .setLastEditedAt(world.getLastEditedAt());

        List<WorldModuleResponse> moduleResponses = sortModules(modules).stream()
                .map(this::toModuleResponse)
                .collect(Collectors.toList());

        return new WorldDetailResponse()
                .setWorld(info)
                .setModules(moduleResponses);
    }

    public WorldSummaryResponse toSummary(World world, List<WorldModule> modules) {
        Map<String, WorldModuleStatus> progress = new LinkedHashMap<>();
        definitionRegistry.getAll().forEach(def -> progress.put(def.key(), WorldModuleStatus.EMPTY));
        for (WorldModule module : modules) {
            progress.put(module.getModuleKey(), module.getStatus());
        }
        return new WorldSummaryResponse()
                .setId(world.getId())
                .setName(world.getName())
                .setTagline(world.getTagline())
                .setThemes(world.getThemes())
                .setStatus(world.getStatus())
                .setVersion(world.getVersion())
                .setUpdatedAt(world.getUpdatedAt())
                .setPublishedAt(world.getPublishedAt())
                .setModuleProgress(progress);
    }

    public WorldModuleResponse toModuleResponse(WorldModule module) {
        Map<String, String> fieldsCopy = module.getFields() == null ? new LinkedHashMap<>()
                : new LinkedHashMap<>(module.getFields());
        return new WorldModuleResponse()
                .setKey(module.getModuleKey())
                .setLabel(definitionRegistry.resolveLabel(module.getModuleKey()))
                .setStatus(module.getStatus())
                .setFields(fieldsCopy)
                .setContentHash(module.getContentHash())
                .setFullContent(module.getFullContent())
                .setFullContentUpdatedAt(module.getFullContentUpdatedAt());
    }

    public WorldModuleSummary toModuleSummary(String moduleKey) {
        return new WorldModuleSummary(moduleKey, definitionRegistry.resolveLabel(moduleKey));
    }

    public List<WorldModuleSummary> toModuleSummaries(List<WorldModule> modules) {
        return sortModules(modules).stream()
                .map(module -> new WorldModuleSummary(module.getModuleKey(),
                        definitionRegistry.resolveLabel(module.getModuleKey())))
                .collect(Collectors.toList());
    }

    public List<WorldModuleResponse> toModuleResponses(List<WorldModule> modules) {
        return sortModules(modules).stream()
                .map(this::toModuleResponse)
                .collect(Collectors.toList());
    }

    public WorldFullResponse toFullResponse(World world, List<WorldModule> modules) {
        WorldInfo info = new WorldInfo()
                .setId(world.getId())
                .setName(world.getName())
                .setTagline(world.getTagline())
                .setThemes(world.getThemes())
                .setCreativeIntent(world.getCreativeIntent())
                .setVersion(world.getVersion())
                .setPublishedAt(world.getPublishedAt());

        List<WorldModuleFull> moduleDtos = sortModules(modules).stream()
                .map(module -> new WorldModuleFull()
                        .setKey(module.getModuleKey())
                        .setLabel(definitionRegistry.resolveLabel(module.getModuleKey()))
                        .setFullContent(module.getFullContent())
                        .setExcerpt(buildExcerpt(module.getFullContent()))
                        .setUpdatedAt(module.getFullContentUpdatedAt() == null
                                ? null
                                : module.getFullContentUpdatedAt().atZone(ZoneId.systemDefault()).toInstant()))
                .collect(Collectors.toList());

        return new WorldFullResponse()
                .setWorld(info)
                .setModules(moduleDtos);
    }

    public List<WorldPublishPreviewResponse.MissingField> buildMissingFields(
            Map<String, List<String>> missingByModule) {
        List<WorldPublishPreviewResponse.MissingField> results = new ArrayList<>();
        missingByModule.forEach((moduleKey, fields) -> {
            String moduleLabel = definitionRegistry.resolveLabel(moduleKey);
            for (String fieldKey : fields) {
                String fieldLabel = definitionRegistry.resolveFieldLabel(moduleKey, fieldKey).orElse(fieldKey);
                results.add(new WorldPublishPreviewResponse.MissingField()
                        .setModuleKey(moduleKey)
                        .setModuleLabel(moduleLabel)
                        .setFieldKey(fieldKey)
                        .setFieldLabel(fieldLabel));
            }
        });
        return results;
    }

    private List<WorldModule> sortModules(List<WorldModule> modules) {
        Map<String, Integer> orderMap = definitionRegistry.getAll().stream()
                .collect(Collectors.toMap(WorldModuleDefinition::key, WorldModuleDefinition::order));
        return modules.stream()
                .sorted(Comparator.comparingInt(module -> orderMap.getOrDefault(module.getModuleKey(), Integer.MAX_VALUE)))
                .collect(Collectors.toList());
    }

    private String buildExcerpt(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.trim();
        if (normalized.length() <= 200) {
            return normalized;
        }
        return normalized.substring(0, 200);
    }
}

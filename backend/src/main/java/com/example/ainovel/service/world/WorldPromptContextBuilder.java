package com.example.ainovel.service.world;

import com.example.ainovel.model.world.World;
import com.example.ainovel.model.world.WorldModule;
import com.example.ainovel.model.world.WorldModuleStatus;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinition;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinitionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class WorldPromptContextBuilder {

    private static final String EMPTY_PLACEHOLDER = "(空)";
    private static final int SUMMARY_LIMIT = 220;

    private final WorldModuleDefinitionRegistry definitionRegistry;

    public WorldPromptContextBuilder(WorldModuleDefinitionRegistry definitionRegistry) {
        this.definitionRegistry = definitionRegistry;
    }

    public Map<String, Object> buildModuleContext(World world, WorldModule module, List<WorldModule> allModules) {
        Map<String, Object> context = new HashMap<>();
        context.put("world", buildWorldInfo(world));
        context.put("module", buildModuleInfo(module));
        context.put("relatedModules", buildRelatedModules(module, allModules));
        context.put("helper", buildHelper(module));
        return context;
    }

    public Map<String, Object> buildFieldRefineContext(World world,
                                                       WorldModule module,
                                                       String fieldKey,
                                                       String focusNote,
                                                       String originalText,
                                                       String instruction,
                                                       List<WorldModule> allModules) {
        Map<String, Object> context = buildModuleContext(world, module, allModules);
        Map<String, Object> helper = safeHelper(context);
        Map<String, Object> fieldContext = new LinkedHashMap<>();
        WorldModuleDefinition.FieldDefinition fieldDefinition = definitionRegistry.requireField(module.getModuleKey(), fieldKey);
        fieldContext.put("key", fieldKey);
        fieldContext.put("label", fieldDefinition.label());
        fieldContext.put("recommendedLength", formatRecommendedLength(fieldDefinition));
        fieldContext.put("focusNote", focusNote == null ? "" : focusNote);
        fieldContext.put("originalText", originalText == null ? EMPTY_PLACEHOLDER : originalText);
        helper.put("field", fieldContext);
        if (StringUtils.hasText(instruction)) {
            helper.put("instruction", "- 额外指令：" + instruction.trim());
        }
        return context;
    }

    private Map<String, Object> buildWorldInfo(World world) {
        Map<String, Object> info = new LinkedHashMap<>();
        if (world != null) {
            info.put("id", world.getId());
            info.put("name", world.getName());
            info.put("tagline", world.getTagline());
            info.put("themes", world.getThemes() == null ? List.of() : world.getThemes());
            info.put("creativeIntent", world.getCreativeIntent());
            info.put("notes", world.getNotes());
            info.put("status", world.getStatus());
            info.put("version", world.getVersion());
            if (world.getPublishedAt() != null) {
                info.put("publishedAt", world.getPublishedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        }
        return info;
    }

    private Map<String, Object> buildModuleInfo(WorldModule module) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("key", module.getModuleKey());
        data.put("label", definitionRegistry.resolveLabel(module.getModuleKey()));
        Map<String, String> fields = module.getFields() == null ? new LinkedHashMap<>()
                : new LinkedHashMap<>(module.getFields());
        data.put("fields", fields);
        List<String> emptyFields = fields.entrySet().stream()
                .filter(entry -> !StringUtils.hasText(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        data.put("emptyFields", emptyFields);
        data.put("dirtyFields", Collections.emptyList());
        data.put("previousFullContent", module.getFullContent());
        data.put("status", module.getStatus());
        data.put("contentHash", module.getContentHash());
        return data;
    }

    private List<Map<String, Object>> buildRelatedModules(WorldModule current, List<WorldModule> allModules) {
        if (allModules == null) {
            return List.of();
        }
        List<Map<String, Object>> related = new ArrayList<>();
        for (WorldModule module : allModules) {
            if (module == null || Objects.equals(module.getId(), current.getId())
                    || Objects.equals(module.getModuleKey(), current.getModuleKey())) {
                continue;
            }
            if (!isModuleReadyForContext(module)) {
                continue;
            }
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("key", module.getModuleKey());
            info.put("label", definitionRegistry.resolveLabel(module.getModuleKey()));
            info.put("status", module.getStatus());
            info.put("summary", buildModuleSummary(module));
            related.add(info);
        }
        return related;
    }

    private boolean isModuleReadyForContext(WorldModule module) {
        WorldModuleStatus status = module.getStatus();
        if (status == WorldModuleStatus.COMPLETED || status == WorldModuleStatus.READY) {
            return true;
        }
        return StringUtils.hasText(module.getFullContent());
    }

    private String buildModuleSummary(WorldModule module) {
        if (StringUtils.hasText(module.getFullContent())) {
            return trimLength(module.getFullContent(), SUMMARY_LIMIT);
        }
        if (module.getFields() == null || module.getFields().isEmpty()) {
            return EMPTY_PLACEHOLDER;
        }
        String joined = module.getFields().values().stream()
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("；"));
        if (!StringUtils.hasText(joined)) {
            return EMPTY_PLACEHOLDER;
        }
        return trimLength(joined, SUMMARY_LIMIT);
    }

    private Map<String, Object> buildHelper(WorldModule module) {
        Map<String, Object> helper = new LinkedHashMap<>();
        helper.put("markdownAllowed", Boolean.FALSE);
        List<Map<String, Object>> definitions = new ArrayList<>();
        WorldModuleDefinition definition = definitionRegistry.requireModule(module.getModuleKey());
        Map<String, String> fields = module.getFields() == null ? Map.of() : module.getFields();
        for (WorldModuleDefinition.FieldDefinition field : definition.fields()) {
            Map<String, Object> fieldInfo = new LinkedHashMap<>();
            fieldInfo.put("key", field.key());
            fieldInfo.put("label", field.label());
            fieldInfo.put("required", field.required());
            fieldInfo.put("recommendedLength", formatRecommendedLength(field));
            String value = fields.get(field.key());
            fieldInfo.put("value", StringUtils.hasText(value) ? value : EMPTY_PLACEHOLDER);
            definitions.add(fieldInfo);
        }
        helper.put("fieldDefinitions", definitions);
        return helper;
    }

    private Map<String, Object> safeHelper(Map<String, Object> context) {
        Object helperObj = context.computeIfAbsent("helper", key -> new LinkedHashMap<>());
        if (helperObj instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        Map<String, Object> helper = new LinkedHashMap<>();
        context.put("helper", helper);
        return helper;
    }

    private String formatRecommendedLength(WorldModuleDefinition.FieldDefinition field) {
        int min = field.minLength();
        int max = field.maxLength();
        if (min > 0 && max > 0) {
            return min + "-" + max + " 字";
        }
        if (min > 0) {
            return "≥" + min + " 字";
        }
        if (max > 0) {
            return "≤" + max + " 字";
        }
        return "建议 200-400 字";
    }

    private String trimLength(String text, int limit) {
        if (!StringUtils.hasText(text)) {
            return EMPTY_PLACEHOLDER;
        }
        String normalized = text.replaceAll("\s+", " ").trim();
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit) + "…";
    }
}

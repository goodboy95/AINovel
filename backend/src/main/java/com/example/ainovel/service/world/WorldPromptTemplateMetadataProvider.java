package com.example.ainovel.service.world;

import com.example.ainovel.dto.world.WorldPromptFunctionMetadataDto;
import com.example.ainovel.dto.world.WorldPromptModuleFieldMetadataDto;
import com.example.ainovel.dto.world.WorldPromptModuleMetadataDto;
import com.example.ainovel.dto.world.WorldPromptTemplateMetadataResponse;
import com.example.ainovel.dto.world.WorldPromptVariableMetadataDto;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinition;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinitionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WorldPromptTemplateMetadataProvider {

    private final WorldModuleDefinitionRegistry definitionRegistry;

    public WorldPromptTemplateMetadataProvider(WorldModuleDefinitionRegistry definitionRegistry) {
        this.definitionRegistry = definitionRegistry;
    }

    public WorldPromptTemplateMetadataResponse getMetadata() {
        WorldPromptTemplateMetadataResponse response = new WorldPromptTemplateMetadataResponse();
        response.setModules(buildModuleMetadata());
        response.setVariables(buildVariables());
        response.setFunctions(buildFunctions());
        response.setExamples(buildExamples());
        return response;
    }

    private List<WorldPromptModuleMetadataDto> buildModuleMetadata() {
        return definitionRegistry.getAll().stream()
                .map(definition -> new WorldPromptModuleMetadataDto()
                        .setKey(definition.key())
                        .setLabel(definition.label())
                        .setFields(buildFieldMetadata(definition)))
                .collect(Collectors.toList());
    }

    private List<WorldPromptModuleFieldMetadataDto> buildFieldMetadata(WorldModuleDefinition definition) {
        if (CollectionUtils.isEmpty(definition.fields())) {
            return List.of();
        }
        List<WorldPromptModuleFieldMetadataDto> fields = new ArrayList<>(definition.fields().size());
        for (WorldModuleDefinition.FieldDefinition field : definition.fields()) {
            fields.add(new WorldPromptModuleFieldMetadataDto()
                    .setKey(field.key())
                    .setLabel(field.label())
                    .setRecommendedLength(formatRecommendedLength(field.minLength(), field.maxLength())));
        }
        return fields;
    }

    private List<WorldPromptVariableMetadataDto> buildVariables() {
        return List.of(
                new WorldPromptVariableMetadataDto("${world.name}", "string", "世界名称。"),
                new WorldPromptVariableMetadataDto("${world.tagline}", "string", "一句话概述。"),
                new WorldPromptVariableMetadataDto("${world.themes}", "string[]", "主题标签数组，可配合 `[index]` 或 `[*]` 使用。"),
                new WorldPromptVariableMetadataDto("${world.creativeIntent}", "string", "创作意图与目标。"),
                new WorldPromptVariableMetadataDto("${module.key}", "string", "当前模块的标识。"),
                new WorldPromptVariableMetadataDto("${module.label}", "string", "当前模块的显示名称。"),
                new WorldPromptVariableMetadataDto("${module.fields.FIELD_KEY}", "string", "模块中具体字段的原始文本。"),
                new WorldPromptVariableMetadataDto("${module.emptyFields}", "string[]", "仍为空的字段 key 列表。"),
                new WorldPromptVariableMetadataDto("${module.dirtyFields}", "string[]", "自上次发布以来被修改的字段。"),
                new WorldPromptVariableMetadataDto("${module.previousFullContent}", "string", "上一次生成的完整信息。"),
                new WorldPromptVariableMetadataDto("${relatedModules[*].label}", "string", "其他模块的名称，可配合 `map()` 生成列表。"),
                new WorldPromptVariableMetadataDto("${relatedModules[*].summary}", "string", "其他模块的摘要文本。"),
                new WorldPromptVariableMetadataDto("${helper.fieldDefinitions[*].label}", "string", "字段中文名称列表。"),
                new WorldPromptVariableMetadataDto("${helper.fieldDefinitions[*].recommendedLength}", "string", "字段推荐字数。")
        );
    }

    private List<WorldPromptFunctionMetadataDto> buildFunctions() {
        return List.of(
                new WorldPromptFunctionMetadataDto("default(value)", "当当前值为空时返回备用值。", "${module.fields.cosmos_structure|default(\"尚未填写\")}"),
                new WorldPromptFunctionMetadataDto("join(separator)", "将数组元素按分隔符拼接。", "${world.themes[*]|join(\" / \")}"),
                new WorldPromptFunctionMetadataDto("upper()", "将字符串转为大写。", "${module.label|upper()}"),
                new WorldPromptFunctionMetadataDto("lower()", "将字符串转为小写。", "${module.key|lower()}"),
                new WorldPromptFunctionMetadataDto("trim()", "去除首尾空白。", "${module.previousFullContent|trim()}"),
                new WorldPromptFunctionMetadataDto("json()", "将值序列化为 JSON 字符串，便于调试。", "${module|json()}"),
                new WorldPromptFunctionMetadataDto("map(alias -> expr)", "遍历列表并对每个元素执行表达式。", "${relatedModules[*]|map(m -> \"- \" + m.label + \"摘要：\" + m.summary)|join(\"\\n\")}"),
                new WorldPromptFunctionMetadataDto("headline()", "转为醒目的标题格式（首字母大写，去除多余空格）。", "${world.tagline|headline()}"),
                new WorldPromptFunctionMetadataDto("truncate(n)", "截断字符串并追加省略号。", "${module.previousFullContent|truncate(200)}")
        );
    }

    private List<String> buildExamples() {
        return List.of(
                "世界背景：\n- 核心概念：${world.tagline}\n- 主题标签：${world.themes[*]|join(\" / \")}\n${relatedModules[*]|map(m -> \"- \" + m.label + \"摘要：\" + m.summary)|join(\"\\n\")}",
                "请严格输出以下 JSON：\n{\n  \"cosmos_structure\": \"……\",\n  \"space_time\": \"……\"\n}",
                "${helper.fieldDefinitions[*]|map(f -> \"- \" + f.label + \"：\" + f.recommendedLength)|join(\"\\n\")}" 
        );
    }

    private String formatRecommendedLength(int min, int max) {
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
}

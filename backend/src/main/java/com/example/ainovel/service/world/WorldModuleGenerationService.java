package com.example.ainovel.service.world;

import com.example.ainovel.exception.ResourceNotFoundException;
import com.example.ainovel.model.world.World;
import com.example.ainovel.model.world.WorldModule;
import com.example.ainovel.model.world.WorldStatus;
import com.example.ainovel.repository.WorldModuleRepository;
import com.example.ainovel.repository.WorldRepository;
import com.example.ainovel.service.AiService;
import com.example.ainovel.service.SettingsService;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinition;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinitionRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class WorldModuleGenerationService {

    private static final TypeReference<Map<String, String>> FIELD_MAP_TYPE = new TypeReference<>() {
    };

    private final WorldRepository worldRepository;
    private final WorldModuleRepository worldModuleRepository;
    private final WorldModuleDefinitionRegistry definitionRegistry;
    private final WorldPromptTemplateService templateService;
    private final WorldPromptContextBuilder contextBuilder;
    private final AiService aiService;
    private final SettingsService settingsService;
    private final ObjectMapper objectMapper;

    public WorldModuleGenerationService(WorldRepository worldRepository,
                                        WorldModuleRepository worldModuleRepository,
                                        WorldModuleDefinitionRegistry definitionRegistry,
                                        WorldPromptTemplateService templateService,
                                        WorldPromptContextBuilder contextBuilder,
                                        AiService aiService,
                                        SettingsService settingsService,
                                        ObjectMapper objectMapper) {
        this.worldRepository = worldRepository;
        this.worldModuleRepository = worldModuleRepository;
        this.definitionRegistry = definitionRegistry;
        this.templateService = templateService;
        this.contextBuilder = contextBuilder;
        this.aiService = aiService;
        this.settingsService = settingsService;
        this.objectMapper = objectMapper;
    }

    public WorldModule generateModule(Long worldId, String moduleKey, Long userId) {
        World world = loadWorld(worldId, userId);
        if (world.getStatus() == WorldStatus.GENERATING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "世界正在生成中，无法调用 AI 自动补全");
        }
        WorldModule module = worldModuleRepository.findByWorldIdAndModuleKey(worldId, moduleKey)
                .orElseThrow(() -> new ResourceNotFoundException("指定模块不存在"));
        List<WorldModule> modules = worldModuleRepository.findByWorldId(worldId);
        Map<String, Object> context = contextBuilder.buildModuleContext(world, module, modules);
        String prompt = templateService.renderDraftTemplate(moduleKey, context);

        AiCredentials credentials = resolveCredentials(userId);
        String aiResult = aiService.generateJson(prompt, credentials.apiKey(), credentials.baseUrl(), credentials.model());
        Map<String, String> generatedFields = parseFields(aiResult);

        LinkedHashMap<String, String> merged = module.getFields() == null ? new LinkedHashMap<>()
                : new LinkedHashMap<>(module.getFields());
        WorldModuleDefinition definition = definitionRegistry.requireModule(moduleKey);
        boolean changed = false;
        for (WorldModuleDefinition.FieldDefinition field : definition.fields()) {
            String value = generatedFields.get(field.key());
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String trimmed = value.trim();
            String existing = merged.get(field.key());
            if (!Objects.equals(existing, trimmed)) {
                merged.put(field.key(), trimmed);
                changed = true;
            }
        }
        if (!changed) {
            return module;
        }
        module.setFields(merged);
        String newHash = WorldModuleContentHelper.computeContentHash(merged);
        boolean contentChanged = !Objects.equals(module.getContentHash(), newHash);
        module.setContentHash(newHash);
        module.setLastEditedBy(userId);
        LocalDateTime now = LocalDateTime.now();
        module.setLastEditedAt(now);
        module.setStatus(WorldModuleContentHelper.determineStatus(definitionRegistry, world, moduleKey, merged, contentChanged));

        if (contentChanged) {
            world.setLastEditedBy(userId);
            world.setLastEditedAt(now);
            if (world.getStatus() == WorldStatus.ACTIVE) {
                world.setStatus(WorldStatus.DRAFT);
            }
        }

        worldModuleRepository.save(module);
        worldRepository.save(world);
        return module;
    }

    private World loadWorld(Long worldId, Long userId) {
        return worldRepository.findByIdAndUserId(worldId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("世界不存在或无权访问"));
    }

    private Map<String, String> parseFields(String aiResult) {
        if (!StringUtils.hasText(aiResult)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回结果为空");
        }
        String sanitized = stripCodeFences(aiResult.trim());
        try {
            JsonNode node = objectMapper.readTree(sanitized);
            if (node.isObject()) {
                return objectMapper.convertValue(node, FIELD_MAP_TYPE);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回内容不是 JSON 对象");
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法解析 AI 返回的 JSON", e);
        }
    }

    private String stripCodeFences(String content) {
        if (content.startsWith("```")) {
            int first = content.indexOf('\n');
            int lastFence = content.lastIndexOf("```");
            if (first > 0 && lastFence > first) {
                return content.substring(first + 1, lastFence).trim();
            }
        }
        return content;
    }

    private AiCredentials resolveCredentials(Long userId) {
        try {
            String apiKey = settingsService.getDecryptedApiKeyByUserId(userId);
            String baseUrl = null;
            String model = null;
            try {
                baseUrl = settingsService.getBaseUrlByUserId(userId);
            } catch (Exception ignored) {
            }
            try {
                model = settingsService.getModelNameByUserId(userId);
            } catch (Exception ignored) {
            }
            return new AiCredentials(apiKey, baseUrl, model);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "请先在用户设置中配置有效的 AI Key", e);
        }
    }

    private record AiCredentials(String apiKey, String baseUrl, String model) {
    }
}

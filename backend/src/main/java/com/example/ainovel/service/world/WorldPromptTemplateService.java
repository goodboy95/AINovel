package com.example.ainovel.service.world;

import com.example.ainovel.dto.world.WorldPromptTemplateItemDto;
import com.example.ainovel.dto.world.WorldPromptTemplateMetadataResponse;
import com.example.ainovel.dto.world.WorldPromptTemplatesResetRequest;
import com.example.ainovel.dto.world.WorldPromptTemplatesResponse;
import com.example.ainovel.dto.world.WorldPromptTemplatesUpdateRequest;
import com.example.ainovel.model.User;
import com.example.ainovel.model.world.WorldPromptSettings;
import com.example.ainovel.prompt.PromptTemplateException;
import com.example.ainovel.prompt.TemplateEngine;
import com.example.ainovel.repository.UserRepository;
import com.example.ainovel.repository.WorldPromptSettingsRepository;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinitionRegistry;
import com.example.ainovel.worldbuilding.prompt.WorldPromptDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Transactional
public class WorldPromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(WorldPromptTemplateService.class);
    private static final int MAX_TEMPLATE_LENGTH = 6000;

    private final TemplateEngine templateEngine;
    private final WorldModuleDefinitionRegistry definitionRegistry;
    private final WorldPromptSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final WorldPromptDefaults defaults;
    private final WorldPromptTemplateMetadataProvider metadataProvider;

    public WorldPromptTemplateService(TemplateEngine templateEngine,
                                      WorldModuleDefinitionRegistry definitionRegistry,
                                      WorldPromptSettingsRepository settingsRepository,
                                      UserRepository userRepository,
                                      WorldPromptDefaults defaults,
                                      WorldPromptTemplateMetadataProvider metadataProvider) {
        this.templateEngine = templateEngine;
        this.definitionRegistry = definitionRegistry;
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
        this.defaults = defaults;
        this.metadataProvider = metadataProvider;
    }

    @Transactional(readOnly = true)
    public String renderDraftTemplate(String moduleKey, Map<String, Object> context, Long userId) {
        String defaultTemplate = requireDefaultDraftTemplate(moduleKey);
        String userTemplate = resolveModuleTemplate(userId, moduleKey, true);
        Map<String, Object> enriched = contextWithDefinitions(moduleKey, context);
        return renderSafely("draft", moduleKey, userTemplate, defaultTemplate, enriched);
    }

    @Transactional(readOnly = true)
    public String renderFinalTemplate(String moduleKey, Map<String, Object> context, Long userId) {
        String defaultTemplate = requireDefaultFinalTemplate(moduleKey);
        String userTemplate = resolveModuleTemplate(userId, moduleKey, false);
        Map<String, Object> enriched = contextWithDefinitions(moduleKey, context);
        return renderSafely("final", moduleKey, userTemplate, defaultTemplate, enriched);
    }

    @Transactional(readOnly = true)
    public String renderFieldRefineTemplate(String moduleKey,
                                            String fieldKey,
                                            Map<String, Object> context,
                                            Long userId) {
        String defaultTemplate = requireDefaultFieldRefineTemplate();
        String userTemplate = resolveFieldRefineTemplate(userId);
        Map<String, Object> enriched = contextWithDefinitions(moduleKey, context);
        return renderSafely("fieldRefine(" + fieldKey + ")", moduleKey, userTemplate, defaultTemplate, enriched);
    }

    @Transactional(readOnly = true)
    public String resolveFocusNote(String moduleKey, String fieldKey) {
        return defaults.getFocusNote(moduleKey, fieldKey);
    }

    @Transactional(readOnly = true)
    public WorldPromptTemplatesResponse getEffectiveTemplates(Long userId) {
        Map<String, WorldPromptTemplateItemDto> modules = new LinkedHashMap<>();
        Map<String, WorldPromptTemplateItemDto> finals = new LinkedHashMap<>();
        Optional<WorldPromptSettings> settingsOpt = findSettings(userId);
        Map<String, String> moduleOverrides = settingsOpt.map(WorldPromptSettings::getModuleTemplates).orElse(Map.of());
        Map<String, String> finalOverrides = settingsOpt.map(WorldPromptSettings::getFinalTemplates).orElse(Map.of());
        for (var definition : definitionRegistry.getAll()) {
            String key = definition.key();
            String defaultDraft = defaults.getDraftTemplate(key);
            String draftContent = moduleOverrides.getOrDefault(key, defaultDraft);
            boolean draftIsDefault = !moduleOverrides.containsKey(key);
            modules.put(key, new WorldPromptTemplateItemDto(draftContent, draftIsDefault));

            String defaultFinal = defaults.getFinalTemplate(key);
            String finalContent = finalOverrides.getOrDefault(key, defaultFinal);
            boolean finalIsDefault = !finalOverrides.containsKey(key);
            finals.put(key, new WorldPromptTemplateItemDto(finalContent, finalIsDefault));
        }
        String defaultField = requireDefaultFieldRefineTemplate();
        String fieldContent = settingsOpt.map(WorldPromptSettings::getFieldRefineTemplate).filter(StringUtils::hasText)
                .orElse(defaultField);
        boolean fieldIsDefault = settingsOpt.map(WorldPromptSettings::getFieldRefineTemplate)
                .map(StringUtils::hasText)
                .map(hasText -> !hasText)
                .orElse(true);

        return new WorldPromptTemplatesResponse()
                .setModules(modules)
                .setFinalTemplates(finals)
                .setFieldRefine(new WorldPromptTemplateItemDto(fieldContent, fieldIsDefault));
    }

    public void saveTemplates(Long userId, WorldPromptTemplatesUpdateRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null when saving world prompt templates");
        }
        if (request == null) {
            return;
        }
        WorldPromptSettings settings = getOrCreateSettings(userId);
        boolean changed = false;
        if (!CollectionUtils.isEmpty(request.getModules())) {
            changed |= applyModuleUpdates(settings, request.getModules(), true);
        }
        if (!CollectionUtils.isEmpty(request.getFinalTemplates())) {
            changed |= applyModuleUpdates(settings, request.getFinalTemplates(), false);
        }
        if (request.getFieldRefine() != null) {
            changed |= applyFieldRefineUpdate(settings, request.getFieldRefine());
        }
        if (!changed) {
            return;
        }
        normalizeSettings(settings);
        settingsRepository.save(settings);
    }

    public void resetTemplates(Long userId, WorldPromptTemplatesResetRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null when resetting world prompt templates");
        }
        if (request == null || CollectionUtils.isEmpty(request.getKeys())) {
            return;
        }
        Optional<WorldPromptSettings> settingsOpt = findSettings(userId);
        if (settingsOpt.isEmpty()) {
            return;
        }
        WorldPromptSettings settings = settingsOpt.get();
        boolean changed = false;
        for (String key : request.getKeys()) {
            if (!StringUtils.hasText(key)) {
                continue;
            }
            if (key.startsWith("modules.")) {
                String moduleKey = key.substring("modules.".length());
                changed |= removeModuleOverride(settings.getModuleTemplates(), moduleKey);
            } else if (key.startsWith("final.")) {
                String moduleKey = key.substring("final.".length());
                changed |= removeModuleOverride(settings.getFinalTemplates(), moduleKey);
            } else if ("fieldRefine".equals(key)) {
                if (StringUtils.hasText(settings.getFieldRefineTemplate())) {
                    settings.setFieldRefineTemplate(null);
                    changed = true;
                }
            }
        }
        if (!changed) {
            return;
        }
        normalizeSettings(settings);
        settingsRepository.save(settings);
    }

    @Transactional(readOnly = true)
    public WorldPromptTemplateMetadataResponse getMetadata() {
        return metadataProvider.getMetadata();
    }

    private Optional<WorldPromptSettings> findSettings(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return settingsRepository.findByUserId(userId);
    }

    private WorldPromptSettings getOrCreateSettings(Long userId) {
        return settingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                    WorldPromptSettings settings = new WorldPromptSettings();
                    settings.setUser(user);
                    return settings;
                });
    }

    private boolean applyModuleUpdates(WorldPromptSettings settings, Map<String, String> updates, boolean draft) {
        boolean changed = false;
        Map<String, String> target = draft ? ensureMap(settings.getModuleTemplates())
                : ensureMap(settings.getFinalTemplates());
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String moduleKey = entry.getKey();
            if (!StringUtils.hasText(moduleKey)) {
                continue;
            }
            String normalized = normalizeTemplate(entry.getValue());
            validateLength(normalized);
            String defaultTemplate = draft ? defaults.getDraftTemplate(moduleKey) : defaults.getFinalTemplate(moduleKey);
            if (!StringUtils.hasText(defaultTemplate)) {
                log.warn("Ignoring update for unknown module template key: {}", moduleKey);
                continue;
            }
            if (!StringUtils.hasText(normalized) || Objects.equals(normalized, normalizeTemplate(defaultTemplate))) {
                changed |= removeModuleOverride(target, moduleKey);
            } else {
                String previous = target.put(moduleKey, normalized);
                changed |= !Objects.equals(previous, normalized);
            }
        }
        if (draft) {
            settings.setModuleTemplates(target);
        } else {
            settings.setFinalTemplates(target);
        }
        return changed;
    }

    private boolean applyFieldRefineUpdate(WorldPromptSettings settings, String template) {
        String normalized = normalizeTemplate(template);
        validateLength(normalized);
        String defaultTemplate = requireDefaultFieldRefineTemplate();
        if (!StringUtils.hasText(normalized) || Objects.equals(normalized, normalizeTemplate(defaultTemplate))) {
            if (StringUtils.hasText(settings.getFieldRefineTemplate())) {
                settings.setFieldRefineTemplate(null);
                return true;
            }
            return false;
        }
        if (!Objects.equals(settings.getFieldRefineTemplate(), normalized)) {
            settings.setFieldRefineTemplate(normalized);
            return true;
        }
        return false;
    }

    private boolean removeModuleOverride(Map<String, String> overrides, String moduleKey) {
        if (CollectionUtils.isEmpty(overrides)) {
            return false;
        }
        return overrides.remove(moduleKey) != null;
    }

    private void normalizeSettings(WorldPromptSettings settings) {
        if (settings.getModuleTemplates() != null && settings.getModuleTemplates().isEmpty()) {
            settings.setModuleTemplates(null);
        }
        if (settings.getFinalTemplates() != null && settings.getFinalTemplates().isEmpty()) {
            settings.setFinalTemplates(null);
        }
        if (!StringUtils.hasText(settings.getFieldRefineTemplate())) {
            settings.setFieldRefineTemplate(null);
        }
    }

    private Map<String, Object> contextWithDefinitions(String moduleKey, Map<String, Object> context) {
        Map<String, Object> enriched = new LinkedHashMap<>(context == null ? Map.of() : context);
        enriched.putIfAbsent("definition", definitionRegistry.requireModule(moduleKey));
        return enriched;
    }

    private String renderSafely(String category,
                                String moduleKey,
                                String template,
                                String fallback,
                                Map<String, Object> context) {
        String effective = template != null ? template : fallback;
        try {
            return templateEngine.render(effective, context);
        } catch (PromptTemplateException ex) {
            if (template != null) {
                log.warn("Failed to render user world prompt template [{}:{}], falling back to default. Error: {}",
                        category, moduleKey, ex.getMessage());
                return templateEngine.render(fallback, context);
            }
            throw ex;
        }
    }

    private String resolveModuleTemplate(Long userId, String moduleKey, boolean draft) {
        if (userId == null) {
            return null;
        }
        return findSettings(userId)
                .map(settings -> draft ? settings.getModuleTemplates() : settings.getFinalTemplates())
                .map(map -> map == null ? null : map.get(moduleKey))
                .orElse(null);
    }

    private String resolveFieldRefineTemplate(Long userId) {
        if (userId == null) {
            return null;
        }
        return findSettings(userId)
                .map(WorldPromptSettings::getFieldRefineTemplate)
                .orElse(null);
    }

    private String requireDefaultDraftTemplate(String moduleKey) {
        String template = defaults.getDraftTemplate(moduleKey);
        if (!StringUtils.hasText(template)) {
            throw new IllegalArgumentException("未配置模块 " + moduleKey + " 的草稿提示词");
        }
        return template;
    }

    private String requireDefaultFinalTemplate(String moduleKey) {
        String template = defaults.getFinalTemplate(moduleKey);
        if (!StringUtils.hasText(template)) {
            throw new IllegalArgumentException("未配置模块 " + moduleKey + " 的完整信息提示词");
        }
        return template;
    }

    private String requireDefaultFieldRefineTemplate() {
        String template = defaults.getFieldRefineTemplate();
        if (!StringUtils.hasText(template)) {
            throw new IllegalStateException("Field refine template is not configured");
        }
        return template;
    }

    private Map<String, String> ensureMap(Map<String, String> source) {
        if (source == null) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(source);
    }

    private String normalizeTemplate(String template) {
        if (template == null) {
            return null;
        }
        String normalized = template.replace("\r\n", "\n").replace('\r', '\n');
        return normalized;
    }

    private void validateLength(String template) {
        if (template != null && template.length() > MAX_TEMPLATE_LENGTH) {
            throw new IllegalArgumentException("模板长度超过限制（" + MAX_TEMPLATE_LENGTH + " 字符）");
        }
    }
}

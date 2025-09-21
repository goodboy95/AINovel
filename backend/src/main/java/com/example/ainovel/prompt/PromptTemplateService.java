package com.example.ainovel.prompt;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.example.ainovel.dto.prompt.PromptTemplateItemDto;
import com.example.ainovel.dto.prompt.PromptTemplateMetadataResponse;
import com.example.ainovel.dto.prompt.PromptTemplatesResetRequest;
import com.example.ainovel.dto.prompt.PromptTemplatesResponse;
import com.example.ainovel.dto.prompt.PromptTemplatesUpdateRequest;
import com.example.ainovel.dto.prompt.RefinePromptTemplateDto;
import com.example.ainovel.dto.prompt.RefinePromptTemplatesUpdateRequest;
import com.example.ainovel.model.prompt.PromptSettings;
import com.example.ainovel.model.prompt.RefinePromptSettings;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateService.class);

    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptDefaults promptDefaults;
    private final TemplateEngine templateEngine;
    private final PromptTemplateMetadataProvider metadataProvider;

    public PromptTemplatesResponse getEffectiveTemplates(Long userId) {
        PromptSettings userSettings = userId == null
                ? null
                : promptTemplateRepository.findByUserId(userId).orElse(null);
        return buildResponse(userSettings);
    }

    public void saveTemplates(Long userId, PromptTemplatesUpdateRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null when saving templates");
        }
        PromptSettings settings = promptTemplateRepository.findByUserId(userId)
                .orElseGet(PromptSettings::new);
        applyUpdate(settings, request);
        if (isEmpty(settings)) {
            promptTemplateRepository.save(userId, null);
        } else {
            promptTemplateRepository.save(userId, settings);
        }
    }

    public void resetTemplates(Long userId, PromptTemplatesResetRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User id must not be null when resetting templates");
        }
        if (request == null || CollectionUtils.isEmpty(request.getKeys())) {
            return;
        }
        PromptSettings settings = promptTemplateRepository.findByUserId(userId)
                .orElseGet(PromptSettings::new);
        boolean changed = false;
        for (String key : request.getKeys()) {
            PromptType type = PromptType.fromKey(key);
            if (type == null) {
                continue;
            }
            changed |= clearTemplate(settings, type);
        }
        if (!changed) {
            return;
        }
        if (isEmpty(settings)) {
            promptTemplateRepository.save(userId, null);
        } else {
            promptTemplateRepository.save(userId, settings);
        }
    }

    public String render(PromptType type, Long userId, Map<String, Object> context) {
        String defaultTemplate = getDefaultTemplate(type);
        if (userId == null) {
            return renderTemplate(defaultTemplate, context);
        }
        PromptSettings settings = promptTemplateRepository.findByUserId(userId).orElse(null);
        String userTemplate = getUserTemplate(settings, type);
        if (userTemplate == null) {
            return renderTemplate(defaultTemplate, context);
        }
        try {
            return renderTemplate(userTemplate, context);
        } catch (PromptTemplateException ex) {
            log.warn("Failed to render user template for type {} and user {}. Falling back to default. Error: {}",
                    type, userId, ex.getMessage());
            return renderTemplate(defaultTemplate, context);
        }
    }

    public String renderWithFallback(PromptType type, Map<String, Object> context) {
        return renderTemplate(getDefaultTemplate(type), context);
    }

    public PromptTemplateMetadataResponse getMetadata() {
        return metadataProvider.getMetadata();
    }

    private void applyUpdate(PromptSettings settings, PromptTemplatesUpdateRequest request) {
        if (request == null) {
            return;
        }
        if (request.getStoryCreation() != null) {
            settings.setStoryCreation(normalizeLineEndings(request.getStoryCreation()));
        }
        if (request.getOutlineChapter() != null) {
            settings.setOutlineChapter(normalizeLineEndings(request.getOutlineChapter()));
        }
        if (request.getManuscriptSection() != null) {
            settings.setManuscriptSection(normalizeLineEndings(request.getManuscriptSection()));
        }
        RefinePromptTemplatesUpdateRequest refine = request.getRefine();
        if (refine != null) {
            RefinePromptSettings refineSettings = settings.ensureRefine();
            if (refine.getWithInstruction() != null) {
                refineSettings.setWithInstruction(normalizeLineEndings(refine.getWithInstruction()));
            }
            if (refine.getWithoutInstruction() != null) {
                refineSettings.setWithoutInstruction(normalizeLineEndings(refine.getWithoutInstruction()));
            }
        }
    }

    private boolean clearTemplate(PromptSettings settings, PromptType type) {
        switch (type) {
            case STORY_CREATION:
                if (settings.getStoryCreation() != null) {
                    settings.setStoryCreation(null);
                    return true;
                }
                return false;
            case OUTLINE_CHAPTER:
                if (settings.getOutlineChapter() != null) {
                    settings.setOutlineChapter(null);
                    return true;
                }
                return false;
            case MANUSCRIPT_SECTION:
                if (settings.getManuscriptSection() != null) {
                    settings.setManuscriptSection(null);
                    return true;
                }
                return false;
            case REFINE_WITH_INSTRUCTION:
                if (settings.getRefine() != null && settings.getRefine().getWithInstruction() != null) {
                    settings.getRefine().setWithInstruction(null);
                    return true;
                }
                return false;
            case REFINE_WITHOUT_INSTRUCTION:
                if (settings.getRefine() != null && settings.getRefine().getWithoutInstruction() != null) {
                    settings.getRefine().setWithoutInstruction(null);
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    private PromptTemplatesResponse buildResponse(PromptSettings settings) {
        PromptTemplatesResponse response = new PromptTemplatesResponse();
        response.setStoryCreation(buildItem(PromptType.STORY_CREATION, settings));
        response.setOutlineChapter(buildItem(PromptType.OUTLINE_CHAPTER, settings));
        response.setManuscriptSection(buildItem(PromptType.MANUSCRIPT_SECTION, settings));
        RefinePromptTemplateDto refine = new RefinePromptTemplateDto()
                .setWithInstruction(buildItem(PromptType.REFINE_WITH_INSTRUCTION, settings))
                .setWithoutInstruction(buildItem(PromptType.REFINE_WITHOUT_INSTRUCTION, settings));
        response.setRefine(refine);
        return response;
    }

    private PromptTemplateItemDto buildItem(PromptType type, PromptSettings settings) {
        String userTemplate = getUserTemplate(settings, type);
        String defaultTemplate = getDefaultTemplate(type);
        boolean isDefault = userTemplate == null;
        return new PromptTemplateItemDto(isDefault ? defaultTemplate : userTemplate, isDefault);
    }

    private boolean isEmpty(PromptSettings settings) {
        if (settings == null) {
            return true;
        }
        boolean baseEmpty = isBlank(settings.getStoryCreation())
                && isBlank(settings.getOutlineChapter())
                && isBlank(settings.getManuscriptSection());
        RefinePromptSettings refine = settings.getRefine();
        boolean refineEmpty = refine == null
                || (isBlank(refine.getWithInstruction()) && isBlank(refine.getWithoutInstruction()));
        if (baseEmpty && refineEmpty) {
            return true;
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeLineEndings(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String getDefaultTemplate(PromptType type) {
        return switch (type) {
            case STORY_CREATION -> promptDefaults.getStoryCreation();
            case OUTLINE_CHAPTER -> promptDefaults.getOutlineChapter();
            case MANUSCRIPT_SECTION -> promptDefaults.getManuscriptSection();
            case REFINE_WITH_INSTRUCTION -> promptDefaults.getRefine().getWithInstruction();
            case REFINE_WITHOUT_INSTRUCTION -> promptDefaults.getRefine().getWithoutInstruction();
        };
    }

    private String getUserTemplate(PromptSettings settings, PromptType type) {
        if (settings == null) {
            return null;
        }
        return switch (type) {
            case STORY_CREATION -> settings.getStoryCreation();
            case OUTLINE_CHAPTER -> settings.getOutlineChapter();
            case MANUSCRIPT_SECTION -> settings.getManuscriptSection();
            case REFINE_WITH_INSTRUCTION -> settings.getRefine() != null ? settings.getRefine().getWithInstruction() : null;
            case REFINE_WITHOUT_INSTRUCTION -> settings.getRefine() != null ? settings.getRefine().getWithoutInstruction() : null;
        };
    }

    private String renderTemplate(String template, Map<String, Object> context) {
        try {
            return templateEngine.render(template, context);
        } catch (PromptTemplateException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new PromptTemplateException("Failed to render prompt template", ex);
        }
    }
}

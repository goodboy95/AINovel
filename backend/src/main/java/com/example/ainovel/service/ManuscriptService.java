package com.example.ainovel.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ainovel.exception.ResourceNotFoundException;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.ManuscriptSection;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.OutlineChapter;
import com.example.ainovel.model.OutlineScene;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.TemporaryCharacter;
import com.example.ainovel.repository.CharacterCardRepository;
import com.example.ainovel.repository.ManuscriptSectionRepository;
import com.example.ainovel.repository.OutlineCardRepository;
import com.example.ainovel.repository.OutlineSceneRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for managing manuscript content, including generation and updates.
 */
@Service
@RequiredArgsConstructor
public class ManuscriptService {

    private final ManuscriptSectionRepository manuscriptSectionRepository;
    private final OutlineService outlineService;
    private final OutlineSceneRepository outlineSceneRepository;
    private final CharacterCardRepository characterCardRepository;
    private final OpenAiService openAiService;
    private final SettingsService settingsService;
    private final OutlineCardRepository outlineCardRepository;

    /**
     * Retrieves the manuscript for a given outline, verifying user access.
     *
     * @param outlineId The ID of the outline.
     * @param userId    The ID of the user requesting the manuscript.
     * @return A map of scene IDs to their corresponding manuscript sections.
     */
    public Map<Long, ManuscriptSection> getManuscriptForOutline(Long outlineId, Long userId) {
        validateOutlineAccess(outlineId, userId);
        List<OutlineChapter> chapters = outlineService.getChaptersForOutline(outlineId);
        List<Long> chapterIds = chapters.stream().map(OutlineChapter::getId).collect(Collectors.toList());
        List<OutlineScene> scenes = outlineService.getScenesForChapters(chapterIds);
        List<Long> sceneIds = scenes.stream().map(OutlineScene::getId).collect(Collectors.toList());
        List<ManuscriptSection> sections = manuscriptSectionRepository.findByScene_IdIn(sceneIds);
        return sections.stream()
            .filter(section -> section.getIsActive() != null && section.getIsActive())
            .collect(Collectors.toMap(
                ManuscriptSection::getSceneId,
                section -> section,
                (existing, replacement) -> {
                    if (existing.getCreatedAt() != null && replacement.getCreatedAt() != null) {
                        return existing.getCreatedAt().isAfter(replacement.getCreatedAt()) ? existing : replacement;
                    }
                    // Fallback if createdAt is null, though it shouldn't be with @CreationTimestamp
                    return replacement;
                }
            ));
    }

    /**
     * Generates content for a specific scene, verifying user access.
     *
     * @param sceneId The ID of the scene.
     * @param userId  The ID of the user requesting generation.
     * @return The newly generated and saved manuscript section.
     */
    @Transactional
    @Retryable(backoff = @Backoff(delay = 1000), maxAttempts = 3)
    public ManuscriptSection generateSceneContent(Long sceneId, Long userId) {
        OutlineScene scene = findSceneById(sceneId);
        validateSceneAccess(scene, userId);

        // Deactivate old sections and find the latest version number
        List<ManuscriptSection> oldSections = manuscriptSectionRepository.findByScene_Id(sceneId);
        int maxVersion = 0;
        for (ManuscriptSection oldSection : oldSections) {
            if (oldSection.getVersion() != null && oldSection.getVersion() > maxVersion) {
                maxVersion = oldSection.getVersion();
            }
            oldSection.setIsActive(false);
        }

        StoryCard story = getStoryCardFromScene(scene);
        // 获取全部角色的“完整信息”实体对象
        List<CharacterCard> characters = characterCardRepository.findByStoryCardId(story.getId());
        // 获取本节的临时角色
        List<TemporaryCharacter> temporaryCharacters = scene.getTemporaryCharacters();

        String baseUrl = settingsService.getBaseUrlByUserId(userId);
        String model = settingsService.getModelNameByUserId(userId);
        String apiKey = settingsService.getDecryptedApiKeyByUserId(userId);

        // 计算位置相关信息
        OutlineChapter currentChapter = scene.getOutlineChapter();
        OutlineCard outline = currentChapter.getOutlineCard();
        int chapterNumber = currentChapter.getChapterNumber();
        int totalChapters = outline.getChapters().size();
        int sceneNumber = scene.getSceneNumber();
        int totalScenesInChapter = currentChapter.getScenes().size();

        // 构建增强上下文（原始信息，而非AI概括）
        String previousSectionContent = getPreviousSectionContent(scene); // 上一节完整内容（若存在）
        List<OutlineScene> previousChapterOutline = getPreviousChapterOutline(scene); // 上一章全部小节大纲（若存在）
        List<OutlineScene> currentChapterOutline = currentChapter.getScenes(); // 本章全部小节大纲

        // 构建新的 Prompt，整合所有上下文与写作规范
        String prompt = buildGenerationPrompt(
                scene,
                story,
                characters,
                temporaryCharacters,
                previousSectionContent,
                previousChapterOutline,
                currentChapterOutline,
                chapterNumber,
                totalChapters,
                sceneNumber,
                totalScenesInChapter
        );

        String generatedContent = openAiService.generate(prompt, apiKey, baseUrl, model);

        // Create a new, active section
        ManuscriptSection newSection = new ManuscriptSection();
        newSection.setScene(scene);
        newSection.setContent(generatedContent);
        newSection.setVersion(maxVersion + 1);
        newSection.setIsActive(true);

        return manuscriptSectionRepository.save(newSection);
    }

    /**
     * Updates the content of a manuscript section, verifying user access.
     *
     * @param sectionId The ID of the manuscript section.
     * @param content   The new content.
     * @param userId    The ID of the user making the update.
     * @return The updated manuscript section.
     */
    @Transactional
    @Retryable(backoff = @Backoff(delay = 1000), maxAttempts = 3)
    public ManuscriptSection updateSectionContent(Long sectionId, String content, Long userId) {
        ManuscriptSection section = manuscriptSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("ManuscriptSection not found with id " + sectionId));
        validateSceneAccess(section.getScene(), userId);
        section.setContent(content);
        return manuscriptSectionRepository.save(section);
    }

    private String buildGenerationPrompt(
            OutlineScene scene,
            StoryCard story,
            List<CharacterCard> allCharacters,
            List<TemporaryCharacter> temporaryCharacters,
            String previousSectionContent,
            List<OutlineScene> previousChapterOutline,
            List<OutlineScene> currentChapterOutline,
            int chapterNumber,
            int totalChapters,
            int sceneNumber,
            int totalScenesInChapter
    ) {
        String tempCharactersInfo = formatTemporaryCharacters(temporaryCharacters);
        String allCharactersInfo = formatCharacters(allCharacters);
        String prevChapterOutlineText = formatOutlineScenes(previousChapterOutline);
        String currentChapterOutlineText = formatOutlineScenes(currentChapterOutline);
        OutlineCard oc = scene.getOutlineChapter().getOutlineCard();
        String pointOfView = (oc != null && oc.getPointOfView() != null && !oc.getPointOfView().trim().isEmpty())
                ? oc.getPointOfView().trim()
                : "第三人称有限视角";

        return String.format(
                "你是一位资深的中文小说家，请使用简体中文进行创作。你的文风细腻而有张力，擅长通过细节与内心戏推动剧情。\n\n" +
                "【故事核心设定】\n- 类型/基调: %s / %s\n- 叙事视角: %s\n- 故事简介: %s\n\n" +
                "【全部角色档案】\n%s\n\n" +
                "【上一章大纲回顾】\n%s\n\n" +
                "【上一节正文（原文）】\n%s\n\n" +
                "【本章完整大纲】\n%s\n\n" +
                "【当前创作位置】\n- 章节: 第 %d/%d 章\n- 小节: 第 %d/%d 节\n\n" +
                "【本节创作蓝图】\n- 梗概: %s\n- 核心出场人物: %s\n- 核心人物状态与行动: %s\n- 临时出场人物:\n%s\n\n" +
                "【写作规则】\n" +
                "1. 钩子与悬念: 开篇30-80字设置强钩子；本节结尾制造悬念或情绪余韵。\n" +
                "2. 伏笔与回收: 合理埋设或回收伏笔，贴合剧情逻辑，避免突兀。\n" +
                "3. 人物弧光: 通过对话与行动自然呈现人物内心变化，拒绝直白说教。\n" +
                "4. 节奏与张力: 结合当前进度（第 %d/%d 章，第 %d/%d 节）控制信息密度与冲突烈度。\n" +
                "5. 细节与画面: 强化感官细节、空间调度与象征性意象，避免模板化。\n" +
                "6. 忠于大纲，高于大纲: 不改变核心走向与设定，可进行合理艺术加工使剧情更佳。\n" +
                "7. 风格统一: 始终保持故事既定基调与叙事视角。\n" +
                "8. 输出要求: 直接输出本节“正文”，约 %d 字；不要输出标题、注释或总结。\n\n" +
                "开始创作。",
                nullToNA(story.getGenre()),
                nullToNA(story.getTone()),
                pointOfView,
                nullToNA(story.getSynopsis()),
                allCharactersInfo,
                (prevChapterOutlineText == null || prevChapterOutlineText.isEmpty()) ? "无" : prevChapterOutlineText,
                nullToNA(previousSectionContent),
                currentChapterOutlineText,
                chapterNumber, totalChapters,
                sceneNumber, totalScenesInChapter,
                nullToNA(scene.getSynopsis()),
                nullToNA(scene.getPresentCharacters()),
                nullToNA(scene.getCharacterStates()),
                tempCharactersInfo,
                chapterNumber, totalChapters,
                sceneNumber, totalScenesInChapter,
                (scene.getExpectedWords() != null ? scene.getExpectedWords() : 1200)
        );
    }

    /**
     * 获取上一章的全部小节大纲（如果当前不是第一章）。
     */
    private List<OutlineScene> getPreviousChapterOutline(OutlineScene currentScene) {
        OutlineChapter currentChapter = currentScene.getOutlineChapter();
        if (currentChapter == null || currentChapter.getChapterNumber() == null || currentChapter.getChapterNumber() <= 1) {
            return Collections.emptyList();
        }
        OutlineCard outline = currentChapter.getOutlineCard();
        if (outline == null || outline.getChapters() == null) {
            return Collections.emptyList();
        }
        return outline.getChapters().stream()
                .filter(ch -> ch.getChapterNumber() != null && ch.getChapterNumber() == currentChapter.getChapterNumber() - 1)
                .findFirst()
                .map(OutlineChapter::getScenes)
                .orElse(Collections.emptyList());
    }

    /**
     * 将角色列表格式化为可读文本，包含完整信息。
     */
    private String formatCharacters(List<CharacterCard> allCharacters) {
        if (allCharacters == null || allCharacters.isEmpty()) {
            return "无";
        }
        return allCharacters.stream()
                .map(c -> String.format(
                        "- %s\n  - 概述: %s\n  - 详细背景: %s\n  - 关系: %s",
                        c.getName(),
                        nullToNA(c.getSynopsis()),
                        nullToNA(c.getDetails()),
                        nullToNA(c.getRelationships())
                ))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 将大纲场景列表格式化为可读文本。
     */
    private String formatOutlineScenes(List<OutlineScene> scenes) {
        if (scenes == null || scenes.isEmpty()) {
            return "无";
        }
        return scenes.stream()
                .map(sc -> String.format(
                        "第 %d 节:\n- 梗概: %s\n- 核心出场人物: %s\n- 人物状态与行动: %s",
                        sc.getSceneNumber(),
                        nullToNA(sc.getSynopsis()),
                        nullToNA(sc.getPresentCharacters()),
                        nullToNA(sc.getCharacterStates())
                ))
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * 将临时角色信息格式化为可读文本（包含各字段）。
     */
    private String formatTemporaryCharacters(List<TemporaryCharacter> temporaryCharacters) {
        if (temporaryCharacters == null || temporaryCharacters.isEmpty()) {
            return "无";
        }
        return temporaryCharacters.stream()
                .map(tc -> String.format(
                        "- %s\n  - 概要: %s\n  - 详情: %s\n  - 关系: %s\n  - 在本节中的状态: %s\n  - 在本节中的心情: %s\n  - 在本节中的行动: %s",
                        tc.getName(),
                        nullToNA(tc.getSummary()),
                        nullToNA(tc.getDetails()),
                        nullToNA(tc.getRelationships()),
                        nullToNA(tc.getStatusInScene()),
                        nullToNA(tc.getMoodInScene()),
                        nullToNA(tc.getActionsInScene())
                ))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 空值与空白安全处理。
     */
    private String nullToNA(String s) {
        return (s == null || s.trim().isEmpty()) ? "无" : s.trim();
    }

    private String getPreviousChapterContent(OutlineScene currentScene) {
        OutlineChapter currentChapter = currentScene.getOutlineChapter();
        if (currentChapter.getChapterNumber() <= 1) {
            return "";
        }
        return outlineService.getChaptersForOutline(currentChapter.getOutlineCard().getId()).stream()
            .filter(ch -> ch.getChapterNumber() == currentChapter.getChapterNumber() - 1)
            .findFirst()
            .map(this::getAllContentForChapter)
            .orElse("");
    }

    private String getPreviousSectionContent(OutlineScene currentScene) {
        return outlineSceneRepository.findFirstByOutlineChapterIdAndSceneNumberLessThanOrderBySceneNumberDesc(
                        currentScene.getOutlineChapter().getId(), currentScene.getSceneNumber())
                .flatMap(previousScene -> manuscriptSectionRepository.findFirstByScene_IdAndIsActiveTrueOrderByVersionDesc(previousScene.getId()))
                .map(ManuscriptSection::getContent)
                .orElse("无");
    }

    private String getCurrentChapterPreviousContent(OutlineScene currentScene) {
        List<OutlineScene> previousScenesInChapter = outlineSceneRepository
                .findByOutlineChapterIdAndSceneNumberLessThanOrderBySceneNumberAsc(currentScene.getOutlineChapter().getId(), currentScene.getSceneNumber());
        return getAllContentForScenes(previousScenesInChapter);
    }

    private String getAllContentForChapter(OutlineChapter chapter) {
        return getAllContentForScenes(chapter.getScenes());
    }

    private String getAllContentForScenes(List<OutlineScene> scenes) {
        if (scenes == null || scenes.isEmpty()) {
            return "";
        }
        List<Long> sceneIds = scenes.stream().map(OutlineScene::getId).collect(Collectors.toList());
        List<ManuscriptSection> sections = manuscriptSectionRepository.findByScene_IdIn(sceneIds);
        Map<Long, String> sceneContentMap = sections.stream()
            .filter(s -> s.getIsActive() != null && s.getIsActive())
            .collect(Collectors.toMap(
                ManuscriptSection::getSceneId,
                ManuscriptSection::getContent,
                (existingContent, newContent) -> newContent
            ));
        return scenes.stream()
            .map(scene -> sceneContentMap.getOrDefault(scene.getId(), ""))
            .filter(content -> content != null && !content.isEmpty())
            .collect(Collectors.joining("\n\n"));
    }

    private OutlineScene findSceneById(Long sceneId) {
        return outlineSceneRepository.findById(sceneId)
                .orElseThrow(() -> new ResourceNotFoundException("Scene not found with id: " + sceneId));
    }

    private StoryCard getStoryCardFromScene(OutlineScene scene) {
        return Optional.ofNullable(scene.getOutlineChapter())
                .map(OutlineChapter::getOutlineCard)
                .map(OutlineCard::getStoryCard)
                .orElseThrow(() -> new IllegalStateException("Scene " + scene.getId() + " is not properly linked to a story."));
    }

    private void validateOutlineAccess(Long outlineId, Long userId) {
        OutlineCard outline = outlineCardRepository.findById(outlineId)
                .orElseThrow(() -> new ResourceNotFoundException("Outline not found with id: " + outlineId));
        if (!outline.getStoryCard().getUser().getId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to access this outline.");
        }
    }

    private void validateSceneAccess(OutlineScene scene, Long userId) {
        StoryCard story = getStoryCardFromScene(scene);
        if (!story.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to access this scene.");
        }
    }
}

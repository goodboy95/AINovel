package com.example.ainovel.service;

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
    private final Map<String, AiService> aiServices;
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
        List<CharacterCard> characters = characterCardRepository.findByStoryCardId(story.getId());
        // Fetch temporary characters associated with the scene
        List<TemporaryCharacter> temporaryCharacters = scene.getTemporaryCharacters();

        String provider = settingsService.getProviderByUserId(userId);
        AiService aiService = aiServices.get(provider);
        if (aiService == null) {
            throw new IllegalStateException("Unsupported AI provider: " + provider);
        }

        String apiKey = settingsService.getDecryptedApiKeyByUserId(userId);

        // New context summarization step
        String contextSummary = summarizeContext(scene, aiService, apiKey);

        // Calculate position information
        OutlineChapter currentChapter = scene.getOutlineChapter();
        OutlineCard outline = currentChapter.getOutlineCard();
        int chapterNumber = currentChapter.getChapterNumber();
        int totalChapters = outline.getChapters().size();
        int sceneNumber = scene.getSceneNumber();
        int totalScenesInChapter = currentChapter.getScenes().size();

        String prompt = buildGenerationPrompt(scene, story, characters, temporaryCharacters, contextSummary, chapterNumber, totalChapters, sceneNumber, totalScenesInChapter);
        String generatedContent = aiService.generate(prompt, apiKey);

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

    private String buildGenerationPrompt(OutlineScene scene, StoryCard story, List<CharacterCard> characters, List<TemporaryCharacter> temporaryCharacters, String contextSummary, int chapterNumber, int totalChapters, int sceneNumber, int totalScenesInChapter) {
        String temporaryCharactersInfo = temporaryCharacters.stream()
                .map(tc -> String.format("- %s: %s", tc.getName(), tc.getSummary()))
                .collect(Collectors.joining("\n"));
        if (temporaryCharactersInfo.isEmpty()) {
            temporaryCharactersInfo = "无";
        }

        return String.format(
            "你是一位才华横溢、情感细腻的小说家。你的文字拥有直击人心的力量。现在，请你将灵魂注入以下场景，创作出能让读者沉浸其中的精彩故事。\n\n" +
            "**故事背景:**\n- 故事类型/基调: %s / %s\n- 故事简介: %s\n\n" +
            "**主要角色设定:**\n%s\n\n" +
            "**上下文回顾:**\n- 前情提要 (AI总结): %s\n" +
            "- 当前位置: 这是故事的 **第 %d/%d 章** 的 **第 %d/%d 节**。请根据这个位置把握好创作的节奏和情绪的烈度。\n\n" +
            "**本节创作蓝图 (大纲):**\n" +
            "- 梗概: %s\n" +
            "- 核心出场人物: %s\n" +
            "- 核心人物状态与行动: %s\n" +
            "- 临时出场人物详情:\n%s\n\n" +
            "**你的创作要求:**\n" +
            "1.  **沉浸式写作:** 请勿平铺直叙。运用感官描写、心理活动和精妙的比喻，让读者完全代入。\n" +
            "2.  **对话与行动:** 对话要符合人物性格，行动要体现人物动机。允许你在大纲基础上，丰富对话和细节，让人物“活”起来。\n" +
            "3.  **自然地融入主题:** 如果需要传递正向价值，请通过人物的选择和成长来体现，避免任何形式的说教。\n" +
            "4.  **忠于大纲，但高于大纲:** 你必须遵循大纲的核心情节和人物状态，但你有权进行合理的艺术加工，让故事更精彩。\n" +
            "5.  **直接输出正文:** 请直接开始创作本节的故事正文，字数在 %d 字左右。不要包含任何前言、标题或总结。\n\n" +
            "现在，请开始你的创作。",
            story.getGenre(), story.getTone(), story.getSynopsis(),
            characters.stream().map(c -> "- " + c.getName() + ": " + c.getSynopsis()).collect(Collectors.joining("\n")),
            contextSummary,
            chapterNumber, totalChapters, sceneNumber, totalScenesInChapter,
            scene.getSynopsis(),
            scene.getPresentCharacters(),
            scene.getCharacterStates(),
            temporaryCharactersInfo,
            scene.getExpectedWords()
        );
    }

    private String summarizeContext(OutlineScene currentScene, AiService aiService, String apiKey) {
        String previousChapterContent = getPreviousChapterContent(currentScene);
        String currentChapterPreviousSectionsContent = getCurrentChapterPreviousContent(currentScene);

        String rawContext = previousChapterContent + "\n\n" + currentChapterPreviousSectionsContent;
        if (rawContext.trim().isEmpty()) {
            return "无";
        }

        // Limit context size to avoid excessive token usage for summarization
        if (rawContext.length() > 4000) {
            rawContext = rawContext.substring(rawContext.length() - 4000);
        }

        String prompt = String.format("请用一两句话概括以下小说内容，抓住核心冲突和情节进展。\n内容: \"%s\"", rawContext);
        return aiService.generate(prompt, apiKey);
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

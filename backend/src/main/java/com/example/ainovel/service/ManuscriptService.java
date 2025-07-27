package com.example.ainovel.service;

import com.example.ainovel.model.*;
import com.example.ainovel.repository.*;
import com.example.ainovel.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        String prompt = buildGenerationPrompt(scene, story, characters);

        String provider = settingsService.getProviderByUserId(userId);
        AiService aiService = aiServices.get(provider);
        if (aiService == null) {
            throw new IllegalStateException("Unsupported AI provider: " + provider);
        }

        String apiKey = settingsService.getDecryptedApiKeyByUserId(userId);
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
    public ManuscriptSection updateSectionContent(Long sectionId, String content, Long userId) {
        ManuscriptSection section = manuscriptSectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("ManuscriptSection not found with id " + sectionId));
        validateSceneAccess(section.getScene(), userId);
        section.setContent(content);
        return manuscriptSectionRepository.save(section);
    }

    private String buildGenerationPrompt(OutlineScene scene, StoryCard story, List<CharacterCard> characters) {
        String previousSectionContent = getPreviousSectionContent(scene);
        String currentChapterPreviousContent = getCurrentChapterPreviousContent(scene);

        return String.format(
            "你是一位才华横溢的小说家。现在请你接续创作故事。\n\n" +
            "**全局信息:**\n- 故事简介: %s\n- 叙事视角: %s\n\n" +
            "**主要角色设定:**\n%s\n\n" +
            "**本章梗概:**\n%s\n\n" +
            "**本节梗概:**\n%s\n\n" +
            "**上下文:**\n- 上一节内容: \"%s\"\n- 本章前面内容: \"%s\"\n\n" +
            "请根据以上所有信息，创作本节的详细内容，字数在 %d 字左右。文笔要生动，符合故事基调和人物性格。请直接开始写正文，不要包含任何解释性文字。",
            story.getSynopsis(),
            scene.getOutlineChapter().getOutlineCard().getPointOfView(),
            characters.stream().map(c -> "- " + c.getName() + ": " + c.getSynopsis()).collect(Collectors.joining("\n")),
            scene.getOutlineChapter().getSynopsis(),
            scene.getSynopsis(),
            previousSectionContent,
            currentChapterPreviousContent,
            scene.getExpectedWords()
        );
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
        if (previousScenesInChapter.isEmpty()) {
            return "无";
        }
        List<Long> sceneIds = previousScenesInChapter.stream().map(OutlineScene::getId).collect(Collectors.toList());
        List<ManuscriptSection> sections = manuscriptSectionRepository.findByScene_IdIn(sceneIds);
        Map<Long, String> sceneContentMap = sections.stream()
                .filter(s -> s.getIsActive() != null && s.getIsActive())
                .collect(Collectors.toMap(
                        ManuscriptSection::getSceneId,
                        ManuscriptSection::getContent,
                        (existingContent, newContent) -> newContent
                ));
        return previousScenesInChapter.stream()
                .map(scene -> sceneContentMap.getOrDefault(scene.getId(), ""))
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

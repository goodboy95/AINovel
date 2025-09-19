package com.example.ainovel.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ainovel.exception.ResourceNotFoundException;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.CharacterChangeLog;
import com.example.ainovel.model.ManuscriptSection;
import com.example.ainovel.model.Manuscript;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.OutlineChapter;
import com.example.ainovel.model.OutlineScene;
import com.example.ainovel.model.SceneCharacter;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.TemporaryCharacter;
import com.example.ainovel.model.User;
import com.example.ainovel.repository.CharacterCardRepository;
import com.example.ainovel.repository.ManuscriptSectionRepository;
import com.example.ainovel.repository.ManuscriptRepository;
import com.example.ainovel.repository.OutlineCardRepository;
import com.example.ainovel.repository.OutlineSceneRepository;
import com.example.ainovel.repository.CharacterChangeLogRepository;
import com.example.ainovel.dto.ManuscriptDto;
import com.example.ainovel.dto.ManuscriptWithSectionsDto;
import com.example.ainovel.dto.AnalyzeCharacterChangesRequest;
import com.example.ainovel.dto.CharacterChangeLogDto;
import com.example.ainovel.dto.RelationshipChangeDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ManuscriptRepository manuscriptRepository;
    private final CharacterChangeLogRepository characterChangeLogRepository;
    private final ObjectMapper objectMapper;
    private static final TypeReference<List<RelationshipChangeDto>> RELATIONSHIP_CHANGE_LIST_TYPE = new TypeReference<>() {};

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
        List<ManuscriptSection> sections = manuscriptSectionRepository.findBySceneIdIn(sceneIds);
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
        List<ManuscriptSection> oldSections = manuscriptSectionRepository.findBySceneId(sceneId);
        int maxVersion = 0;
        for (ManuscriptSection oldSection : oldSections) {
            if (oldSection.getVersion() != null && oldSection.getVersion() > maxVersion) {
                maxVersion = oldSection.getVersion();
            }
            oldSection.setIsActive(false);
        }
        // persist deactivation to ensure consistency
        manuscriptSectionRepository.saveAll(oldSections);

        StoryCard story = getStoryCardFromScene(scene);
        // 获取全部角色的“完整信息”实体对象
        List<CharacterCard> characters = characterCardRepository.findByStoryCardId(story.getId());
        // 获取本节的临时角色
        List<TemporaryCharacter> temporaryCharacters = scene.getTemporaryCharacters();
        List<SceneCharacter> sceneCharacters = scene.getSceneCharacters();

        String baseUrl = settingsService.getBaseUrlByUserId(userId);
        String model = settingsService.getModelNameByUserId(userId);
        String apiKey = settingsService.getDecryptedApiKeyByUserId(userId);

        // 计算位置相关信息
        OutlineChapter currentChapter = scene.getOutlineChapter();
        OutlineCard outline = currentChapter.getOutlineCard();

        // Determine target Manuscript (use latest for this outline or create default)
        Manuscript manuscript = manuscriptRepository.findFirstByOutlineCardIdOrderByCreatedAtDesc(outline.getId())
                .orElseGet(() -> {
                    Manuscript m = new Manuscript();
                    m.setOutlineCard(outline);
                    m.setUser(story.getUser());
                    m.setTitle("默认稿件");
                    return manuscriptRepository.save(m);
                });

        int chapterNumber = currentChapter.getChapterNumber();
        int totalChapters = outline.getChapters().size();
        int sceneNumber = scene.getSceneNumber();
        int totalScenesInChapter = currentChapter.getScenes().size();

        // 构建增强上下文（原始信息，而非AI概括）
        String previousSectionContent = getPreviousSectionContent(scene); // 上一节完整内容（若存在）
        List<OutlineScene> previousChapterOutline = getPreviousChapterOutline(scene); // 上一章全部小节大纲（若存在）
        List<OutlineScene> currentChapterOutline = currentChapter.getScenes(); // 本章全部小节大纲

        Map<Long, CharacterChangeLog> latestCharacterLogs = characters == null ? Collections.emptyMap()
                : characters.stream()
                        .collect(Collectors.toMap(
                                CharacterCard::getId,
                                character -> characterChangeLogRepository
                                        .findFirstByManuscript_IdAndCharacter_IdOrderByCreatedAtDesc(
                                                manuscript.getId(), character.getId())
                                        .orElse(null)
                        ));

        // 构建新的 Prompt，整合所有上下文与写作规范
        String prompt = buildGenerationPrompt(
                scene,
                story,
                characters,
                sceneCharacters,
                temporaryCharacters,
                previousSectionContent,
                previousChapterOutline,
                currentChapterOutline,
                chapterNumber,
                totalChapters,
                sceneNumber,
                totalScenesInChapter,
                latestCharacterLogs
        );

        String generatedContent = openAiService.generate(prompt, apiKey, baseUrl, model);

        // Create a new, active section
        ManuscriptSection newSection = new ManuscriptSection();
        newSection.setManuscript(manuscript);
        newSection.setSceneId(sceneId);
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
        OutlineScene sc = findSceneById(section.getSceneId());
        validateSceneAccess(sc, userId);
        section.setContent(content);
        return manuscriptSectionRepository.save(section);
    }

    private String buildGenerationPrompt(
            OutlineScene scene,
            StoryCard story,
            List<CharacterCard> allCharacters,
            List<SceneCharacter> sceneCharacters,
            List<TemporaryCharacter> temporaryCharacters,
            String previousSectionContent,
            List<OutlineScene> previousChapterOutline,
            List<OutlineScene> currentChapterOutline,
            int chapterNumber,
            int totalChapters,
            int sceneNumber,
            int totalScenesInChapter,
            Map<Long, CharacterChangeLog> latestCharacterLogs
    ) {
        String tempCharactersInfo = formatTemporaryCharacters(temporaryCharacters);
        String allCharactersInfo = formatCharacters(allCharacters, latestCharacterLogs);
        String prevChapterOutlineText = formatOutlineScenes(previousChapterOutline);
        String currentChapterOutlineText = formatOutlineScenes(currentChapterOutline);
        String presentCharacterNames = formatPresentCharacters(sceneCharacters, scene.getPresentCharacters());
        String coreSceneCharacters = formatSceneCharacters(sceneCharacters);
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
                "【本节创作蓝图】\n- 梗概: %s\n- 核心出场人物: %s\n- 核心人物状态卡:\n%s\n- 临时出场人物:\n%s\n\n" +
                "【写作规则】\n" +
                "1. 钩子与悬念: 开篇30-80字设置强钩子；本节结尾制造悬念或情绪余韵。\n" +
                "2. 伏笔与回收: 合理埋设或回收伏笔，贴合剧情逻辑，避免突兀。\n" +
                "3. 人物弧光: 通过对话与行动自然呈现人物内心变化，拒绝直白说教。\n" +
                "4. 节奏与张力: 结合当前进度（第 %d/%d 章，第 %d/%d 节）控制信息密度与冲突烈度。\n" +
                "5. 细节与画面: 强化感官细节、空间调度与象征性意象，避免模板化。\n" +
                "6. 忠于大纲，高于大纲: 不改变核心走向与设定，可进行合理艺术加工使剧情更佳。\n" +
                "7. 风格统一: 始终保持故事既定基调与叙事视角。\n" +
                "8. 输出要求: 直接输出本节“正文”，约 %d 字；不要输出标题、注释或总结。\n" +
                "9. 语言节制: 减少比喻、排比等修辞手法，仅在确有必要时少量使用。\n" +
                "10. 剧情新鲜: 避免套路化的表达和桥段，保持情节的惊喜度与原创性。\n" +
                "11. 自然质感: 让叙述与对话贴近人物的真实语感，避免机械化陈述。\n" +
                "12. 段落饱满: 每个自然段尽量写满多句内容，避免“一句话一个段落”的碎片化写法。\n\n" +
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
                nullToNA(presentCharacterNames),
                coreSceneCharacters,
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
    private String formatCharacters(List<CharacterCard> allCharacters, Map<Long, CharacterChangeLog> latestLogs) {
        if (allCharacters == null || allCharacters.isEmpty()) {
            return "无";
        }
        return allCharacters.stream()
                .map(c -> String.format(
                        "- %s\n  - 概述: %s\n  - 详细背景: %s\n  - 关系: %s\n  - 最近成长轨迹: %s\n  - 最新关系图谱: %s",
                        c.getName(),
                        nullToNA(c.getSynopsis()),
                        nullToNA(c.getDetails()),
                        nullToNA(c.getRelationships()),
                        summarizeCharacterGrowth(latestLogs != null ? latestLogs.get(c.getId()) : null),
                        summarizeRelationshipMap(latestLogs != null ? latestLogs.get(c.getId()) : null)
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
                        "第 %d 节:\n- 梗概: %s\n- 核心出场人物: %s\n- 核心人物状态卡:\n%s\n- 临时人物:\n%s",
                        sc.getSceneNumber(),
                        nullToNA(sc.getSynopsis()),
                        nullToNA(formatPresentCharacters(sc.getSceneCharacters(), sc.getPresentCharacters())),
                        formatSceneCharacters(sc.getSceneCharacters()),
                        formatTemporaryCharacters(sc.getTemporaryCharacters())
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
                        "- %s\n  - 概要: %s\n  - 详情: %s\n  - 关系: %s\n  - 在本节中的状态: %s\n  - 在本节中的想法: %s\n  - 在本节中的行动: %s",
                        tc.getName(),
                        nullToNA(tc.getSummary()),
                        nullToNA(tc.getDetails()),
                        nullToNA(tc.getRelationships()),
                        nullToNA(tc.getStatus()),
                        nullToNA(tc.getThought()),
                        nullToNA(tc.getAction())
                ))
                .collect(Collectors.joining("\n"));
    }

    private String formatSceneCharacters(List<SceneCharacter> sceneCharacters) {
        if (sceneCharacters == null || sceneCharacters.isEmpty()) {
            return "无";
        }
        return sceneCharacters.stream()
                .map(sc -> String.format(
                        "- %s\n  - 状态: %s\n  - 想法: %s\n  - 行动: %s",
                        defaultString(sc.getCharacterName(), "未知角色"),
                        nullToNA(sc.getStatus()),
                        nullToNA(sc.getThought()),
                        nullToNA(sc.getAction())
                ))
                .collect(Collectors.joining("\n"));
    }

    private String formatPresentCharacters(List<SceneCharacter> sceneCharacters, String fallback) {
        if (sceneCharacters != null && !sceneCharacters.isEmpty()) {
            String names = sceneCharacters.stream()
                    .map(SceneCharacter::getCharacterName)
                    .filter(name -> name != null && !name.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));
            if (!names.isBlank()) {
                return names;
            }
        }
        return fallback;
    }

    private String summarizeCharacterGrowth(CharacterChangeLog log) {
        if (log == null) {
            return "暂无最新变化记录";
        }
        String combined = java.util.stream.Stream.of(log.getCharacterChanges(), log.getNewlyKnownInfo())
                .filter(text -> text != null && !text.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" / "));
        if (combined == null || combined.isBlank()) {
            combined = nullToNA(log.getCharacterDetailsAfter());
        }
        return nullToNA(combined);
    }

    private String summarizeRelationshipMap(CharacterChangeLog log) {
        if (log == null) {
            return "暂无新的关系变更";
        }
        List<RelationshipChangeDto> changes = parseRelationshipChanges(log.getRelationshipChangesJson());
        if (changes.isEmpty()) {
            return "暂无新的关系变更";
        }
        return changes.stream()
                .map(change -> String.format(
                        "与角色ID %d: %s -> %s（原因: %s）",
                        change.getTargetCharacterId(),
                        defaultString(change.getPreviousRelationship(), "未知"),
                        defaultString(change.getCurrentRelationship(), "未知"),
                        defaultString(change.getChangeReason(), "未说明")
                ))
                .collect(Collectors.joining("；"));
    }

    /**
     * 空值与空白安全处理。
     */
    private String nullToNA(String s) {
        return (s == null || s.trim().isEmpty()) ? "无" : s.trim();
    }

    private String getPreviousSectionContent(OutlineScene currentScene) {
        return outlineSceneRepository.findFirstByOutlineChapterIdAndSceneNumberLessThanOrderBySceneNumberDesc(
                        currentScene.getOutlineChapter().getId(), currentScene.getSceneNumber())
                .flatMap(previousScene -> manuscriptSectionRepository.findFirstBySceneIdAndIsActiveTrueOrderByVersionDesc(previousScene.getId()))
                .map(ManuscriptSection::getContent)
                .orElse("无");
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

    private void validateManuscriptAccess(Manuscript manuscript, Long userId) {
        Long ownerFromManuscript = manuscript.getUser() != null ? manuscript.getUser().getId() : null;
        Long ownerFromOutline = (manuscript.getOutlineCard() != null && manuscript.getOutlineCard().getUser() != null)
                ? manuscript.getOutlineCard().getUser().getId() : null;
        Long owner = ownerFromManuscript != null ? ownerFromManuscript : ownerFromOutline;
        if (owner == null || !owner.equals(userId)) {
            throw new AccessDeniedException("User does not have permission to access this manuscript.");
        }
    }

    private ManuscriptDto toDto(Manuscript m) {
        return new ManuscriptDto()
                .setId(m.getId())
                .setTitle(m.getTitle())
                .setOutlineId(m.getOutlineCard() != null ? m.getOutlineCard().getId() : null)
                .setCreatedAt(m.getCreatedAt())
                .setUpdatedAt(m.getUpdatedAt());
    }

    /**
     * Lists manuscripts under an outline, after permission validation.
     */
    public List<ManuscriptDto> getManuscriptsForOutline(Long outlineId, Long userId) {
        validateOutlineAccess(outlineId, userId);
        List<Manuscript> list = manuscriptRepository.findByOutlineCardId(outlineId);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Creates a manuscript under an outline.
     */
    @Transactional
    public ManuscriptDto createManuscript(Long outlineId, ManuscriptDto request, Long userId) {
        OutlineCard outline = outlineCardRepository.findById(outlineId)
                .orElseThrow(() -> new ResourceNotFoundException("Outline not found with id: " + outlineId));
        validateOutlineAccess(outline.getId(), userId);

        Manuscript m = new Manuscript();
        m.setOutlineCard(outline);
        // prefer outline.getUser() if present; fallback to story's user
        User owner = outline.getUser() != null ? outline.getUser() : outline.getStoryCard().getUser();
        m.setUser(owner);
        String title = (request != null && request.getTitle() != null && !request.getTitle().trim().isEmpty())
                ? request.getTitle().trim()
                : "新小说稿件";
        m.setTitle(title);

        Manuscript saved = manuscriptRepository.save(m);
        return toDto(saved);
    }

    /**
     * Retrieves a manuscript with its active sections keyed by sceneId.
     */
    @Transactional(readOnly = true)
    public ManuscriptWithSectionsDto getManuscriptWithSections(Long manuscriptId, Long userId) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found with id: " + manuscriptId));
        validateManuscriptAccess(manuscript, userId);

        List<ManuscriptSection> sections = manuscriptSectionRepository.findByManuscript_Id(manuscriptId);
        Map<Long, ManuscriptSection> activeByScene = sections.stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsActive()))
                .collect(Collectors.toMap(
                        ManuscriptSection::getSceneId,
                        s -> s,
                        (a, b) -> {
                            // prefer higher version; fallback to newer createdAt
                            int av = a.getVersion() != null ? a.getVersion() : 0;
                            int bv = b.getVersion() != null ? b.getVersion() : 0;
                            if (av != bv) return av > bv ? a : b;
                            if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                                return a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b;
                            }
                            return b;
                        }
                ));

        return new ManuscriptWithSectionsDto(toDto(manuscript), activeByScene);
    }

    /**
     * Deletes a manuscript (cascade will remove its sections).
     */
    @Transactional
    public void deleteManuscript(Long manuscriptId, Long userId) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found with id: " + manuscriptId));
        validateManuscriptAccess(manuscript, userId);
        manuscriptRepository.delete(manuscript);
    }
    /**
     * Analyzes a manuscript section for character evolution, persists the change logs, and returns the saved records.
     */
    @Transactional
    public List<CharacterChangeLogDto> analyzeCharacterChanges(Long manuscriptId,
                                                                AnalyzeCharacterChangesRequest request,
                                                                Long userId) {
        if (request == null) {
            throw new IllegalArgumentException("Request payload cannot be null.");
        }
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found with id: " + manuscriptId));
        validateManuscriptAccess(manuscript, userId);

        Integer chapterNumber = request.getChapterNumber();
        Integer sectionNumber = request.getSectionNumber();
        if (chapterNumber == null || sectionNumber == null) {
            throw new IllegalArgumentException("chapterNumber and sectionNumber must be provided.");
        }

        List<Long> characterIds = Optional.ofNullable(request.getCharacterIds())
                .orElse(Collections.emptyList())
                .stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (characterIds.isEmpty()) {
            throw new IllegalArgumentException("At least one characterId is required for analysis.");
        }

        List<CharacterCard> characters = characterCardRepository.findAllById(characterIds);
        if (characters.size() != characterIds.size()) {
            throw new ResourceNotFoundException("One or more characters could not be found for the given ids.");
        }

        StoryCard storyCard = Optional.ofNullable(manuscript.getOutlineCard())
                .map(OutlineCard::getStoryCard)
                .orElseThrow(() -> new IllegalStateException("Manuscript is not linked to a story card."));
        Long storyId = storyCard.getId();

        for (CharacterCard character : characters) {
            if (!character.getStoryCard().getId().equals(storyId)) {
                throw new AccessDeniedException("Character " + character.getId() + " does not belong to the manuscript's story.");
            }
        }

        Map<Long, String> storyCharacterNames = characterCardRepository.findByStoryCardId(storyId).stream()
                .collect(Collectors.toMap(CharacterCard::getId, CharacterCard::getName));

        List<CharacterAnalysisContext> contexts = buildAnalysisContexts(manuscript, characters, storyCharacterNames);
        Map<Long, CharacterAnalysisContext> contextById = contexts.stream()
                .collect(Collectors.toMap(ctx -> ctx.getCharacter().getId(), ctx -> ctx));

        String prompt = buildCharacterChangePrompt(
                manuscript,
                chapterNumber,
                sectionNumber,
                defaultString(request.getSectionContent(), "无内容提供。"),
                contexts,
                storyCharacterNames
        );

        String apiKey = settingsService.getDecryptedApiKeyByUserId(userId);
        String baseUrl = settingsService.getBaseUrlByUserId(userId);
        String model = settingsService.getModelNameByUserId(userId);

        String jsonResponse = openAiService.generateJson(prompt, apiKey, baseUrl, model);
        List<AiCharacterChangeItem> aiItems = parseAiCharacterChangeResponse(jsonResponse);

        List<CharacterChangeLog> toPersist = new ArrayList<>();
        for (AiCharacterChangeItem item : aiItems) {
            if (item.getCharacterId() == null) {
                continue;
            }
            CharacterAnalysisContext context = contextById.get(item.getCharacterId());
            if (context == null) {
                continue;
            }

            boolean noChange = Boolean.TRUE.equals(item.getNoChange());
            String detailsAfter = noChange
                    ? context.getPreviousDetails()
                    : defaultString(item.getCharacterDetailsAfter(), context.getPreviousDetails());
            if (detailsAfter == null || detailsAfter.isBlank()) {
                detailsAfter = context.getPreviousDetails();
            }

            CharacterChangeLog log = new CharacterChangeLog();
            log.setCharacter(context.getCharacter());
            log.setManuscript(manuscript);
            log.setOutline(manuscript.getOutlineCard());
            log.setChapterNumber(chapterNumber);
            log.setSectionNumber(sectionNumber);
            log.setNewlyKnownInfo(noChange ? null : safeTrim(item.getNewlyKnownInfo()));
            log.setCharacterChanges(noChange ? null : safeTrim(item.getCharacterChanges()));
            log.setCharacterDetailsAfter(detailsAfter);
            log.setIsAutoCopied(noChange || Boolean.TRUE.equals(item.getIsAutoCopied()));
            log.setIsTurningPoint(Boolean.TRUE.equals(item.getIsTurningPoint()));

            String relationshipJson = serializeRelationshipChanges(item.getRelationshipChanges());
            log.setRelationshipChangesJson(relationshipJson);

            toPersist.add(log);
        }

        if (toPersist.isEmpty()) {
            return Collections.emptyList();
        }

        List<CharacterChangeLog> saved = characterChangeLogRepository.saveAll(toPersist);
        return saved.stream()
                .sorted(Comparator.comparing(log -> log.getCharacter().getId()))
                .map(this::toCharacterChangeLogDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all character change logs for a manuscript in chronological order.
     */
    @Transactional(readOnly = true)
    public List<CharacterChangeLogDto> getCharacterChangeLogs(Long manuscriptId, Long userId) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found with id: " + manuscriptId));
        validateManuscriptAccess(manuscript, userId);

        List<CharacterChangeLog> logs = characterChangeLogRepository.findByManuscript_IdOrderByChapterNumberAscSectionNumberAscCreatedAtAsc(manuscriptId);
        return logs.stream()
                .map(this::toCharacterChangeLogDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves character change logs for a specific character in a manuscript.
     */
    @Transactional(readOnly = true)
    public List<CharacterChangeLogDto> getCharacterChangeLogsForCharacter(Long manuscriptId, Long characterId, Long userId) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found with id: " + manuscriptId));
        validateManuscriptAccess(manuscript, userId);

        CharacterCard character = characterCardRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + characterId));
        StoryCard storyCard = Optional.ofNullable(manuscript.getOutlineCard())
                .map(OutlineCard::getStoryCard)
                .orElseThrow(() -> new IllegalStateException("Manuscript is not linked to a story card."));
        if (!character.getStoryCard().getId().equals(storyCard.getId())) {
            throw new AccessDeniedException("Character does not belong to the manuscript's story.");
        }

        List<CharacterChangeLog> logs = characterChangeLogRepository.findByManuscript_IdAndCharacter_IdOrderByCreatedAtDesc(manuscriptId, characterId);
        List<CharacterChangeLog> chronological = new ArrayList<>(logs);
        Collections.reverse(chronological);
        return chronological.stream()
                .map(this::toCharacterChangeLogDto)
                .collect(Collectors.toList());
    }

    private List<CharacterAnalysisContext> buildAnalysisContexts(Manuscript manuscript,
                                                                 List<CharacterCard> characters,
                                                                 Map<Long, String> storyCharacterNames) {
        List<CharacterAnalysisContext> contexts = new ArrayList<>();
        for (CharacterCard character : characters) {
            List<CharacterChangeLog> history = characterChangeLogRepository
                    .findByManuscript_IdAndCharacter_IdOrderByCreatedAtDesc(manuscript.getId(), character.getId());

            String previousDetails = history.stream()
                    .findFirst()
                    .map(CharacterChangeLog::getCharacterDetailsAfter)
                    .map(this::safeTrim)
                    .orElseGet(() -> defaultString(character.getDetails(), character.getSynopsis()));
            if (previousDetails == null) {
                previousDetails = "暂无记录。";
            }

            String relationshipSummary = buildRelationshipSummary(character, history, storyCharacterNames);
            List<String> memoryHighlights = buildMemoryHighlights(history);

            contexts.add(new CharacterAnalysisContext(character, previousDetails, relationshipSummary, memoryHighlights));
        }
        return contexts;
    }

    private String buildRelationshipSummary(CharacterCard character,
                                            List<CharacterChangeLog> history,
                                            Map<Long, String> storyCharacterNames) {
        List<String> summary = new ArrayList<>();
        String baseRelationships = safeTrim(character.getRelationships());
        if (baseRelationships != null) {
            summary.add("初始关系: " + baseRelationships);
        }

        int recorded = 0;
        for (CharacterChangeLog log : history) {
            List<RelationshipChangeDto> changes = parseRelationshipChanges(log.getRelationshipChangesJson());
            if (changes.isEmpty()) {
                continue;
            }
            String sectionLabel = String.format("第%d章第%d节", log.getChapterNumber(), log.getSectionNumber());
            for (RelationshipChangeDto change : changes) {
                String targetName = storyCharacterNames.getOrDefault(change.getTargetCharacterId(), "角色" + change.getTargetCharacterId());
                String previousState = defaultString(change.getPreviousRelationship(), "未知");
                String currentState = defaultString(change.getCurrentRelationship(), "未知");
                String reason = defaultString(change.getChangeReason(), "未说明原因");
                summary.add(String.format("[%s] 与%s的关系由“%s”变为“%s”，原因：%s", sectionLabel, targetName, previousState, currentState, reason));
            }
            recorded++;
            if (recorded >= 3) {
                break;
            }
        }
        return summary.isEmpty() ? "暂无过往关系变化。" : String.join("；", summary);
    }

    private List<String> buildMemoryHighlights(List<CharacterChangeLog> history) {
        List<String> highlights = new ArrayList<>();
        for (CharacterChangeLog log : history) {
            String info = safeTrim(log.getNewlyKnownInfo());
            if (info != null) {
                highlights.add(String.format("第%d章第%d节：%s", log.getChapterNumber(), log.getSectionNumber(), info));
            }
            if (highlights.size() >= 5) {
                break;
            }
        }
        return highlights;
    }

    private String buildCharacterChangePrompt(Manuscript manuscript,
                                              Integer chapterNumber,
                                              Integer sectionNumber,
                                              String sectionContent,
                                              List<CharacterAnalysisContext> contexts,
                                              Map<Long, String> storyCharacterNames) {
        String storyTitle = Optional.ofNullable(manuscript.getOutlineCard())
                .map(OutlineCard::getStoryCard)
                .map(StoryCard::getTitle)
                .orElse("未知故事");
        String outlineTitle = Optional.ofNullable(manuscript.getOutlineCard())
                .map(OutlineCard::getTitle)
                .orElse("未知大纲");

        StringBuilder builder = new StringBuilder();
        builder.append("你是一名小说角色演化分析助手。请根据提供的章节内容，判断角色的认知、情绪、目标和关系是否发生变化，并严格输出JSON。");
        builder.append("返回内容必须完全符合后文给出的JSON结构，不得包含额外文字。\n\n");

        builder.append("故事标题: ").append(storyTitle).append('\n');
        builder.append("所属大纲: ").append(outlineTitle).append('\n');
        builder.append(String.format("章节: 第%d章 第%d节\n", chapterNumber, sectionNumber));
        builder.append("本节正文:\n").append(sectionContent).append("\n\n");

        builder.append("本节出场角色:\n");
        for (CharacterAnalysisContext context : contexts) {
            builder.append(String.format("- 角色ID %d，名称 %s\n", context.getCharacter().getId(), context.getCharacter().getName()));
            builder.append("  既往详情: ").append(context.getPreviousDetails()).append('\n');
            builder.append("  既往关系脉络: ").append(context.getRelationshipSummary()).append('\n');
            if (!context.getMemoryHighlights().isEmpty()) {
                builder.append("  近期重要记忆:\n");
                for (String memory : context.getMemoryHighlights()) {
                    builder.append("    · ").append(memory).append('\n');
                }
            }
        }

        builder.append("\n所有故事角色（供参考）: ");
        builder.append(storyCharacterNames.entrySet().stream()
                .map(entry -> String.format("%d:%s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", ")));
        builder.append("\n\n");

        builder.append("请输出如下JSON结构:\n");
        builder.append("{\n");
        builder.append("  \"characters\": [\n");
        builder.append("    {\n");
        builder.append("      \"character_id\": 101,\n");
        builder.append("      \"newly_known_info\": \"...\",\n");
        builder.append("      \"character_changes\": \"...\",\n");
        builder.append("      \"character_details_after\": \"...\",\n");
        builder.append("      \"relationship_changes\": [\n");
        builder.append("        {\n");
        builder.append("          \"target_character_id\": 102,\n");
        builder.append("          \"previous_relationship\": \"...\",\n");
        builder.append("          \"current_relationship\": \"...\",\n");
        builder.append("          \"change_reason\": \"...\"\n");
        builder.append("        }\n");
        builder.append("      ],\n");
        builder.append("      \"is_turning_point\": false,\n");
        builder.append("      \"is_auto_copied\": false\n");
        builder.append("    }\n");
        builder.append("  ]\n");
        builder.append("}\n");
        builder.append("若角色没有任何变化，返回对象 {\"character_id\": 101, \"no_change\": true}。若无关系变化，请返回空数组。仅返回JSON字符串，无说明文字。\n");

        return builder.toString();
    }

    private List<AiCharacterChangeItem> parseAiCharacterChangeResponse(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            throw new IllegalStateException("AI did not return any content for character analysis.");
        }
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode charactersNode = root.get("characters");
            if (charactersNode == null || !charactersNode.isArray()) {
                throw new IllegalStateException("AI response must contain a 'characters' array.");
            }
            List<AiCharacterChangeItem> items = new ArrayList<>();
            for (JsonNode node : charactersNode) {
                items.add(objectMapper.treeToValue(node, AiCharacterChangeItem.class));
            }
            return items;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse AI character change response.", e);
        }
    }

    private String serializeRelationshipChanges(List<RelationshipChangeDto> changes) {
        if (changes == null || changes.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(changes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize relationship changes.", e);
        }
    }

    private List<RelationshipChangeDto> parseRelationshipChanges(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, RELATIONSHIP_CHANGE_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse relationship changes.", e);
        }
    }

    private CharacterChangeLogDto toCharacterChangeLogDto(CharacterChangeLog log) {
        List<RelationshipChangeDto> relationshipChanges = parseRelationshipChanges(log.getRelationshipChangesJson());
        return new CharacterChangeLogDto()
                .setId(log.getId())
                .setCharacterId(log.getCharacter().getId())
                .setCharacterName(log.getCharacter().getName())
                .setManuscriptId(log.getManuscript().getId())
                .setOutlineId(log.getOutline() != null ? log.getOutline().getId() : null)
                .setChapterNumber(log.getChapterNumber())
                .setSectionNumber(log.getSectionNumber())
                .setNewlyKnownInfo(log.getNewlyKnownInfo())
                .setCharacterChanges(log.getCharacterChanges())
                .setCharacterDetailsAfter(log.getCharacterDetailsAfter())
                .setIsAutoCopied(Boolean.TRUE.equals(log.getIsAutoCopied()))
                .setRelationshipChanges(relationshipChanges)
                .setIsTurningPoint(Boolean.TRUE.equals(log.getIsTurningPoint()))
                .setCreatedAt(log.getCreatedAt())
                .setUpdatedAt(log.getUpdatedAt());
    }

    private String defaultString(String value, String fallback) {
        String trimmed = safeTrim(value);
        return trimmed != null ? trimmed : fallback;
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static class CharacterAnalysisContext {
        private final CharacterCard character;
        private final String previousDetails;
        private final String relationshipSummary;
        private final List<String> memoryHighlights;

        CharacterAnalysisContext(CharacterCard character,
                                 String previousDetails,
                                 String relationshipSummary,
                                 List<String> memoryHighlights) {
            this.character = character;
            this.previousDetails = previousDetails;
            this.relationshipSummary = relationshipSummary;
            this.memoryHighlights = memoryHighlights;
        }

        CharacterCard getCharacter() {
            return character;
        }

        String getPreviousDetails() {
            return previousDetails;
        }

        String getRelationshipSummary() {
            return relationshipSummary;
        }

        List<String> getMemoryHighlights() {
            return memoryHighlights == null ? Collections.emptyList() : memoryHighlights;
        }
    }

    private static class AiCharacterChangeItem {
        @JsonProperty("character_id")
        private Long characterId;
        @JsonProperty("newly_known_info")
        private String newlyKnownInfo;
        @JsonProperty("character_changes")
        private String characterChanges;
        @JsonProperty("character_details_after")
        private String characterDetailsAfter;
        @JsonProperty("relationship_changes")
        private List<RelationshipChangeDto> relationshipChanges;
        @JsonProperty("is_turning_point")
        private Boolean isTurningPoint;
        @JsonProperty("is_auto_copied")
        private Boolean isAutoCopied;
        @JsonProperty("no_change")
        private Boolean noChange;

        Long getCharacterId() {
            return characterId;
        }

        String getNewlyKnownInfo() {
            return newlyKnownInfo;
        }

        String getCharacterChanges() {
            return characterChanges;
        }

        String getCharacterDetailsAfter() {
            return characterDetailsAfter;
        }

        List<RelationshipChangeDto> getRelationshipChanges() {
            return relationshipChanges == null ? Collections.emptyList() : relationshipChanges;
        }

        Boolean getIsTurningPoint() {
            return isTurningPoint;
        }

        Boolean getIsAutoCopied() {
            return isAutoCopied;
        }

        Boolean getNoChange() {
            return noChange;
        }
    }
}


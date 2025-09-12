package com.example.ainovel.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ainovel.dto.ChapterDto;
import com.example.ainovel.dto.GenerateChapterRequest;
import com.example.ainovel.dto.OutlineDto;
import com.example.ainovel.dto.OutlineRequest;
import com.example.ainovel.dto.RefineRequest;
import com.example.ainovel.dto.RefineResponse;
import com.example.ainovel.dto.SceneDto;
import com.example.ainovel.dto.TemporaryCharacterDto;
import com.example.ainovel.exception.ResourceNotFoundException;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.OutlineChapter;
import com.example.ainovel.model.OutlineScene;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.TemporaryCharacter;
import com.example.ainovel.model.User;
import com.example.ainovel.model.UserSetting;
import com.example.ainovel.repository.OutlineCardRepository;
import com.example.ainovel.repository.OutlineChapterRepository;
import com.example.ainovel.repository.OutlineSceneRepository;
import com.example.ainovel.repository.StoryCardRepository;
import com.example.ainovel.repository.TemporaryCharacterRepository;
import com.example.ainovel.repository.UserRepository;
import com.example.ainovel.repository.UserSettingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * Service for managing story outlines, including AI-powered generation and CRUD operations.
 */
@Service
@RequiredArgsConstructor
public class OutlineService {

    private static final Logger logger = LoggerFactory.getLogger(OutlineService.class);

    private final OutlineCardRepository outlineCardRepository;
    private final StoryCardRepository storyCardRepository;
    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final EncryptionService encryptionService;
    private final SettingsService settingsService;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final OutlineChapterRepository outlineChapterRepository;
    private final OutlineSceneRepository outlineSceneRepository;

    private final TemporaryCharacterRepository temporaryCharacterRepository;

    /**
     * Creates a new story outline using an AI service.
     *
     * @param outlineRequest The request DTO containing creation parameters.
     * @param userId         The ID of the user creating the outline.
     * @return A DTO representing the newly created outline.
     */
    @Transactional
    @Deprecated
    public OutlineDto createOutline(OutlineRequest outlineRequest, Long userId) {
        throw new UnsupportedOperationException("This method is deprecated in V2.");
    }

    /**
     * Creates a new, empty outline for a given story.
     *
     * @param storyCardId The ID of the story card.
     * @param userId      The ID of the user creating the outline.
     * @return A DTO representing the newly created empty outline.
     */
    @Transactional
    @Retryable(backoff = @Backoff(delay = 1000), maxAttempts = 3)
    public OutlineDto createEmptyOutline(Long storyCardId, Long userId) {
        StoryCard storyCard = findStoryCardById(storyCardId);
        validateStoryCardAccess(storyCard, userId);

        User user = findUserById(userId);

        // Get the count of existing outlines for this story to create a default title
        long existingOutlineCount = outlineCardRepository.countByStoryCardId(storyCardId);

        OutlineCard newOutline = new OutlineCard();
        newOutline.setTitle("新大纲 " + (existingOutlineCount + 1)); // e.g., "新大纲 1"
        newOutline.setStoryCard(storyCard);
        newOutline.setUser(user);
        newOutline.setChapters(new ArrayList<>()); // Initialize with an empty list

        OutlineCard savedOutline = outlineCardRepository.save(newOutline);
        return convertToDto(savedOutline);
    }

    /**
     * Retrieves a single outline by its ID, ensuring the user has permission.
     *
     * @param outlineId The ID of the outline.
     * @param userId    The ID of the user requesting the outline.
     * @return A DTO representing the outline.
     */
    public OutlineDto getOutlineById(Long outlineId, Long userId) {
        OutlineCard outlineCard = findOutlineCardById(outlineId);
        validateOutlineAccess(outlineCard, userId);
        return convertToDto(outlineCard);
    }


    private String cleanJson(String rawJson) {
        // Find the first '{' and the last '}' to extract the JSON object.
        int firstBrace = rawJson.indexOf('{');
        int lastBrace = rawJson.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
            return rawJson.substring(firstBrace, lastBrace + 1);
        }
        logger.warn("Could not find a valid JSON object in the response: {}", rawJson);
        return "{}"; // Return empty JSON if cleaning fails
    }

    private OutlineDto convertToDto(OutlineCard outlineCard) {
        // This could be moved to a dedicated mapper class for better separation of concerns.
        OutlineDto dto = new OutlineDto();
        dto.setId(outlineCard.getId());
        dto.setTitle(outlineCard.getTitle());
        dto.setPointOfView(outlineCard.getPointOfView());
        dto.setChapters(outlineCard.getChapters().stream()
                .map(this::convertChapterToDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private ChapterDto convertChapterToDto(OutlineChapter chapter) {
        ChapterDto dto = new ChapterDto();
        dto.setId(chapter.getId());
        dto.setChapterNumber(chapter.getChapterNumber());
        dto.setTitle(chapter.getTitle());
        dto.setSynopsis(chapter.getSynopsis());
        dto.setSettings(chapter.getSettings()); // Add settings
        dto.setScenes(chapter.getScenes().stream()
                .map(this::convertSceneToDto)
                .collect(Collectors.toList()));
        return dto;
    }

    private SceneDto convertSceneToDto(OutlineScene scene) {
        SceneDto dto = new SceneDto();
        dto.setId(scene.getId());
        dto.setSceneNumber(scene.getSceneNumber());
        dto.setSynopsis(scene.getSynopsis());
        dto.setExpectedWords(scene.getExpectedWords());
        dto.setPresentCharacterIds(parsePresentCharacterIds(scene.getPresentCharacters()));
        dto.setCharacterStates(scene.getCharacterStates());
        if (scene.getTemporaryCharacters() != null) {
            dto.setTemporaryCharacters(scene.getTemporaryCharacters().stream()
                .map(this::convertTemporaryCharacterToDto)
                .collect(Collectors.toList()));
        }
        return dto;
    }

    private TemporaryCharacterDto convertTemporaryCharacterToDto(TemporaryCharacter temporaryCharacter) {
        TemporaryCharacterDto dto = new TemporaryCharacterDto();
        dto.setId(temporaryCharacter.getId());
        dto.setName(temporaryCharacter.getName());
        dto.setSummary(temporaryCharacter.getSummary());
        dto.setDetails(temporaryCharacter.getDetails());
        dto.setRelationships(temporaryCharacter.getRelationships());
        dto.setStatusInScene(temporaryCharacter.getStatusInScene());
        dto.setMoodInScene(temporaryCharacter.getMoodInScene());
        dto.setActionsInScene(temporaryCharacter.getActionsInScene());
        return dto;
    }

    /**
     * Retrieves all outlines for a given story card, ensuring the user has permission.
     *
     * @param storyCardId The ID of the story card.
     * @param userId      The ID of the user making the request.
     * @return A list of outline DTOs.
     */
    @Transactional(readOnly = true)
    public List<OutlineDto> getOutlinesByStoryCardId(Long storyCardId, Long userId) {
        if (!storyCardRepository.existsByIdAndUserId(storyCardId, userId)) {
            throw new AccessDeniedException("Unauthorized access or StoryCard not found");
        }
        List<OutlineCard> outlines = outlineCardRepository.findByStoryCardId(storyCardId);
        return outlines.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Updates an existing outline.
     *
     * @param outlineId  The ID of the outline to update.
     * @param outlineDto The DTO with updated data.
     * @param userId     The ID of the user making the request.
     * @return The updated outline DTO.
     */
    @Transactional
    @Retryable(backoff = @Backoff(delay = 1000), maxAttempts = 3)
    public OutlineDto updateOutline(Long outlineId, OutlineDto outlineDto, Long userId) {
        OutlineCard outlineCard = findOutlineCardById(outlineId);
        validateOutlineAccess(outlineCard, userId);

        outlineCard.setTitle(outlineDto.getTitle());
        outlineCard.setPointOfView(outlineDto.getPointOfView());

        // --- Chapter Management ---
        Map<Long, OutlineChapter> existingChaptersMap = outlineCard.getChapters().stream()
                .collect(Collectors.toMap(OutlineChapter::getId, c -> c, (c1, c2) -> c1));
        
        List<OutlineChapter> chaptersToKeep = new ArrayList<>();
        if (outlineDto.getChapters() != null) {
            for (ChapterDto chapterDto : outlineDto.getChapters()) {
                OutlineChapter chapter = existingChaptersMap.remove(chapterDto.getId());
                if (chapter == null) { // New chapter
                    chapter = new OutlineChapter();
                    chapter.setOutlineCard(outlineCard);
                }
                chapter.setChapterNumber(chapterDto.getChapterNumber());
                chapter.setTitle(chapterDto.getTitle());
                chapter.setSynopsis(chapterDto.getSynopsis());
                chapter.setSettings(chapterDto.getSettings());

                // --- Scene Management ---
                Map<Long, OutlineScene> existingScenesMap = chapter.getScenes().stream()
                        .collect(Collectors.toMap(OutlineScene::getId, s -> s, (s1, s2) -> s1));
                
                List<OutlineScene> scenesToKeep = new ArrayList<>();
                if (chapterDto.getScenes() != null) {
                    for (SceneDto sceneDto : chapterDto.getScenes()) {
                        OutlineScene scene = existingScenesMap.remove(sceneDto.getId());
                        if (scene == null) { // New scene
                            scene = new OutlineScene();
                            scene.setOutlineChapter(chapter);
                        }
                        scene.setSceneNumber(sceneDto.getSceneNumber());
                        scene.setSynopsis(sceneDto.getSynopsis());
                        scene.setExpectedWords(sceneDto.getExpectedWords());
                        if (sceneDto.getPresentCharacterIds() != null) {
                            try {
                                scene.setPresentCharacters(objectMapper.writeValueAsString(sceneDto.getPresentCharacterIds()));
                            } catch (JsonProcessingException e) {
                                logger.warn("Failed to serialize presentCharacterIds, storing empty array", e);
                                scene.setPresentCharacters("[]");
                            }
                        }
                        scene.setCharacterStates(sceneDto.getCharacterStates());

                        // --- Temporary Character Management ---
                        if (sceneDto.getTemporaryCharacters() != null) {
                            Map<Long, TemporaryCharacter> existingTempCharsMap = scene.getTemporaryCharacters().stream()
                                    .collect(Collectors.toMap(TemporaryCharacter::getId, tc -> tc, (tc1, tc2) -> tc1));
                            
                            List<TemporaryCharacter> tempCharsToKeep = new ArrayList<>();
                            for (TemporaryCharacterDto tempCharDto : sceneDto.getTemporaryCharacters()) {
                                TemporaryCharacter tempChar = existingTempCharsMap.remove(tempCharDto.getId());
                                if (tempChar == null) { // New temp char
                                    tempChar = new TemporaryCharacter();
                                    tempChar.setScene(scene);
                                }
                                tempChar.setName(tempCharDto.getName());
                                tempChar.setSummary(tempCharDto.getSummary());
                                tempChar.setDetails(tempCharDto.getDetails());
                                tempChar.setRelationships(tempCharDto.getRelationships());
                                tempChar.setStatusInScene(tempCharDto.getStatusInScene());
                                tempChar.setMoodInScene(tempCharDto.getMoodInScene());
                                tempChar.setActionsInScene(tempCharDto.getActionsInScene());
                                tempCharsToKeep.add(tempChar);
                            }
                            scene.getTemporaryCharacters().clear();
                            scene.getTemporaryCharacters().addAll(tempCharsToKeep);
                        }
                        scenesToKeep.add(scene);
                    }
                }
                chapter.getScenes().clear();
                chapter.getScenes().addAll(scenesToKeep);
                chaptersToKeep.add(chapter);
            }
        }
        outlineCard.getChapters().clear();
        outlineCard.getChapters().addAll(chaptersToKeep);

        OutlineCard updatedOutline = outlineCardRepository.save(outlineCard);
        return convertToDto(updatedOutline);
    }

    /**
     * Deletes an outline, ensuring the user has permission.
     *
     * @param outlineId The ID of the outline to delete.
     * @param userId    The ID of the user making the request.
     */
    @Transactional
    @Retryable(backoff = @Backoff(delay = 1000), maxAttempts = 3)
    public void deleteOutline(Long outlineId, Long userId) {
        OutlineCard outlineCard = findOutlineCardById(outlineId);
        validateOutlineAccess(outlineCard, userId);
        outlineCardRepository.delete(outlineCard);
    }

    @Transactional(readOnly = true)
    public List<OutlineChapter> getChaptersForOutline(Long outlineId) {
        return outlineChapterRepository.findByOutlineCardId(outlineId);
    }

    @Transactional(readOnly = true)
    public List<OutlineScene> getScenesForChapters(List<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return new ArrayList<>();
        }
        return outlineSceneRepository.findByOutlineChapterIdIn(chapterIds);
    }

    /**
     * Refines a scene's synopsis using an AI service.
     *
     * @param sceneId The ID of the scene to refine.
     * @param request The refinement request details.
     * @param user    The authenticated user.
     * @return A response DTO with the refined text.
     */
    @Deprecated
    public RefineResponse refineSceneSynopsis(Long sceneId, RefineRequest request, User user) {
        OutlineScene scene = findSceneById(sceneId);
        validateSceneAccess(scene, user.getId());
        // For backward compatibility, we can add a default context type
        if (request.getContextType() == null || request.getContextType().isBlank()) {
            request.setContextType("场景梗概");
        }
        return refineGenericText(request, user);
    }

    /**
     * Provides a generic text refinement service by calling the appropriate AI service.
     *
     * @param request The refinement request details.
     * @param user    The authenticated user.
     * @return A response DTO with the refined text.
     */
    public RefineResponse refineGenericText(RefineRequest request, User user) {
        String apiKey = getDecryptedApiKeyForUser(user);
        String baseUrl = settingsService.getBaseUrlByUserId(user.getId());
        String model = settingsService.getModelNameByUserId(user.getId());

        String refinedText = openAiService.refineText(request, apiKey, baseUrl, model);
        return new RefineResponse(refinedText);
    }

    @Transactional
    @Retryable(backoff = @Backoff(delay = 1000), maxAttempts = 3)
    public ChapterDto generateChapterOutline(Long outlineId, GenerateChapterRequest request) {
        OutlineCard outlineCard = findOutlineCardById(outlineId);
        User user = outlineCard.getUser();
        validateOutlineAccess(outlineCard, user.getId());

        StoryCard storyCard = outlineCard.getStoryCard();
        String apiKey = getDecryptedApiKeyForUser(user);
        String baseUrl = settingsService.getBaseUrlByUserId(user.getId());
        String model = settingsService.getModelNameByUserId(user.getId());

        String prompt = buildChapterPrompt(storyCard, outlineCard, request);
        logger.debug("Generated Prompt for Chapter {}: {}", request.getChapterNumber(), prompt);

        String jsonResponse = openAiService.generate(prompt, apiKey, baseUrl, model);
        logger.info("AI Response JSON for chapter generation: {}", jsonResponse);

        try {
            OutlineChapter chapter = parseAndSaveChapter(jsonResponse, outlineCard, request);
            return convertChapterToDto(chapter);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse AI response for chapter: {}", jsonResponse, e);
            throw new RuntimeException("Failed to parse AI response for chapter", e);
        }
    }

    private String buildChapterPrompt(StoryCard storyCard, OutlineCard outlineCard, GenerateChapterRequest request) {
        String characterProfiles = storyCard.getCharacters().stream()
                .map(c -> String.format("- %s: %s", c.getName(), c.getSynopsis()))
                .collect(Collectors.joining("\n"));

        String previousChapterSynopsis = outlineChapterRepository
            .findByOutlineCardIdAndChapterNumber(outlineCard.getId(), request.getChapterNumber() - 1)
            .map(OutlineChapter::getSynopsis)
            .orElse("无，这是第一章。");

        return String.format(
            """
            你是一位洞悉读者心理、擅长制造“爽点”与“泪点”的顶尖网络小说家。现在，请你以合作者的身份，为我的故事设计接下来的一章。我希望这一章不仅是情节的推进，更是情感的积累和爆发。

            # 故事核心信息
            - **故事简介:** %s
            - **核心主题与基调:** %s / %s
            - **故事长期走向:** %s

            # 主要角色设定
            %s

            # 上下文回顾
            - **上一章梗概:** %s

            # 本章创作任务 (第 %d 章)
            - **预设节数:** %d
            - **预估每节字数:** %d

            # 你的创作目标与自由度
            1.  **情节设计:** 请构思一章充满“钩子”的情节。思考：这一章的结尾，最能让读者好奇地想读下一章的悬念是什么？中间是否可以安排一个小的“情绪爆点”或“情节反转”？
            2.  **人物弧光:** 思考核心人物在本章的经历，他们的内心会产生怎样的变化？他们的信念是会更坚定，还是会受到挑战？
            3.  **伏笔与回收:** 如果有机会，可以埋下一些与长线剧情相关的伏笔。如果前文有伏笔，思考本章是否是回收它的好时机。
            4.  **创作建议 (重要):** 在满足核心要求的前提下，你完全可以提出更有创意的想法。例如，你认为某个临时人物的设定稍微调整一下会更有戏剧性，或者某个情节有更好的表现方式，请大胆地在你的设计中体现出来，并用 `[创作建议]` 标签标注。
            5.  **拒绝平庸:** 请极力避免机械地推进剧情。每一节都应该有其独特的作用，或是塑造人物，或是铺垫情绪，或是揭示信息。

            # 输出格式
            请严格以JSON格式返回。根对象应包含 "title", "synopsis" 和一个 "scenes" 数组。
            每个 scene 对象必须包含:
            - "sceneNumber": (number) 序号。
            - "synopsis": (string) 详细、生动、充满画面感的故事梗概，字数不少于200字。
            - "presentCharacters": (string[]) 核心出场人物姓名列表。
            - "characterStates": (object) 一个对象，键为核心人物姓名，值为该人物在本节中非常详细的状态、内心想法和关键行动的描述。
            - "temporaryCharacters": (object[]) 一个对象数组，用于描写本节新出现的或需要详细刻画的临时人物。如果不需要，则返回空数组[]。每个对象必须包含所有字段: "name", "summary", "details", "relationships", "statusInScene", "moodInScene", "actionsInScene"。
            """,
            storyCard.getSynopsis(),
            storyCard.getGenre(), storyCard.getTone(),
            storyCard.getStoryArc(),
            characterProfiles,
            previousChapterSynopsis,
            request.getChapterNumber(),
            request.getSectionsPerChapter(),
            request.getWordsPerSection()
        );
    }

    private OutlineChapter parseAndSaveChapter(String jsonResponse, OutlineCard outlineCard, GenerateChapterRequest request) throws JsonProcessingException {
        String cleanedJsonResponse = cleanJson(jsonResponse);
        JsonNode chapterNode = objectMapper.readTree(cleanedJsonResponse);

        OutlineChapter chapter = new OutlineChapter();
        chapter.setOutlineCard(outlineCard);
        chapter.setChapterNumber(request.getChapterNumber());
        chapter.setTitle(chapterNode.path("title").asText("未命名章节"));
        chapter.setSynopsis(chapterNode.path("synopsis").asText());
        try {
            chapter.setSettings(objectMapper.writeValueAsString(java.util.Map.of(
                "sectionsPerChapter", request.getSectionsPerChapter(),
                "wordsPerSection", request.getWordsPerSection()
            )));
        } catch (JsonProcessingException e) {
            logger.warn("Could not serialize chapter settings", e);
            chapter.setSettings("{}");
        }

        List<OutlineScene> scenes = new ArrayList<>();
        JsonNode scenesNode = chapterNode.path("scenes");
        for (JsonNode sceneNode : scenesNode) {
            OutlineScene scene = new OutlineScene();
            scene.setOutlineChapter(chapter);
            scene.setSceneNumber(sceneNode.path("sceneNumber").asInt());
            scene.setSynopsis(sceneNode.path("synopsis").asText());
            scene.setExpectedWords(request.getWordsPerSection());

            // Handle presentCharacters as an array of strings
            List<String> presentChars = new ArrayList<>();
            sceneNode.path("presentCharacters").forEach(node -> presentChars.add(node.asText()));
            scene.setPresentCharacters(String.join(", ", presentChars));

            // Handle characterStates as a JSON object string
            scene.setCharacterStates(sceneNode.path("characterStates").toString());

            // Handle temporary characters
            List<TemporaryCharacter> tempChars = new ArrayList<>();
            JsonNode tempCharsNode = sceneNode.path("temporaryCharacters");
            for (JsonNode tempCharNode : tempCharsNode) {
                TemporaryCharacter tempChar = new TemporaryCharacter();
                tempChar.setName(tempCharNode.path("name").asText());
                tempChar.setSummary(tempCharNode.path("description").asText()); // Legacy support for "description" from old prompts
                tempChar.setScene(scene); // Set back-reference
                tempChars.add(tempChar);
            }
            scene.setTemporaryCharacters(tempChars);

            scenes.add(scene);
        }
        chapter.setScenes(scenes);

        return outlineChapterRepository.save(chapter);
    }


    // Helper methods for validation and data retrieval

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    }

    private StoryCard findStoryCardById(Long storyCardId) {
        return storyCardRepository.findById(storyCardId)
                .orElseThrow(() -> new ResourceNotFoundException("StoryCard not found with id: " + storyCardId));
    }

    private OutlineCard findOutlineCardById(Long outlineId) {
        return outlineCardRepository.findById(outlineId)
                .orElseThrow(() -> new ResourceNotFoundException("OutlineCard not found with id: " + outlineId));
    }

    private OutlineScene findSceneById(Long sceneId) {
        return outlineSceneRepository.findById(sceneId)
                .orElseThrow(() -> new ResourceNotFoundException("OutlineScene not found with id: " + sceneId));
    }

    private AiService getAiServiceForUser(User user) {
        // Deprecated provider-based selection removed; always use OpenAI implementation.
        return openAiService;
    }

    private String getDecryptedApiKeyForUser(User user) {
        UserSetting settings = userSettingRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User settings not found. Please configure your AI provider first."));
        String apiKey = encryptionService.decrypt(settings.getApiKey());
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API Key is not configured or is invalid.");
        }
        return apiKey;
    }

    private void validateStoryCardAccess(StoryCard storyCard, Long userId) {
        if (!storyCard.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to access this story card.");
        }
    }

    private void validateOutlineAccess(OutlineCard outlineCard, Long userId) {
        if (!outlineCard.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("User does not have permission to access this outline.");
        }
    }

    private void validateSceneAccess(OutlineScene scene, Long userId) {
        Long ownerId = scene.getOutlineChapter().getOutlineCard().getUser().getId();
        if (!ownerId.equals(userId)) {
            throw new AccessDeniedException("User does not have permission to access this scene.");
        }
    }

    /**
     * Partially updates a Chapter node (title, synopsis, settings, chapterNumber).
     * Only non-null fields in chapterDto will be applied.
     */
    @Transactional
    @Retryable(backoff = @Backoff(delay = 1000), maxAttempts = 3)
    public ChapterDto updateChapter(Long chapterId, ChapterDto chapterDto, Long userId) {
        OutlineChapter chapter = outlineChapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResourceNotFoundException("OutlineChapter not found with id: " + chapterId));

        validateOutlineAccess(chapter.getOutlineCard(), userId);

        if (chapterDto.getChapterNumber() != null) {
            chapter.setChapterNumber(chapterDto.getChapterNumber());
        }
        if (chapterDto.getTitle() != null) {
            chapter.setTitle(chapterDto.getTitle());
        }
        if (chapterDto.getSynopsis() != null) {
            chapter.setSynopsis(chapterDto.getSynopsis());
        }
        if (chapterDto.getSettings() != null) {
            chapter.setSettings(chapterDto.getSettings());
        }

        OutlineChapter saved = outlineChapterRepository.save(chapter);
        return convertChapterToDto(saved);
    }

    /**
     * Partially updates a Scene node (synopsis, expectedWords, presentCharacters, characterStates, sceneNumber, temporaryCharacters).
     * Only non-null fields in sceneDto will be applied. If temporaryCharacters is present, it replaces the entire collection.
     */
    @Transactional
    @Retryable(backoff = @Backoff(delay = 1000), maxAttempts = 3)
    public SceneDto updateScene(Long sceneId, SceneDto sceneDto, Long userId) {
        OutlineScene scene = outlineSceneRepository.findById(sceneId)
                .orElseThrow(() -> new ResourceNotFoundException("OutlineScene not found with id: " + sceneId));

        validateSceneAccess(scene, userId);

        if (sceneDto.getSceneNumber() != null) {
            scene.setSceneNumber(sceneDto.getSceneNumber());
        }
        if (sceneDto.getSynopsis() != null) {
            scene.setSynopsis(sceneDto.getSynopsis());
        }
        if (sceneDto.getExpectedWords() != null) {
            scene.setExpectedWords(sceneDto.getExpectedWords());
        }
        if (sceneDto.getPresentCharacterIds() != null) {
            try {
                scene.setPresentCharacters(objectMapper.writeValueAsString(sceneDto.getPresentCharacterIds()));
            } catch (JsonProcessingException e) {
                logger.warn("Failed to serialize presentCharacterIds, storing empty array", e);
                scene.setPresentCharacters("[]");
            }
        }
        if (sceneDto.getCharacterStates() != null) {
            scene.setCharacterStates(sceneDto.getCharacterStates());
        }

        // Replace temporary characters if provided in DTO
        if (sceneDto.getTemporaryCharacters() != null) {
            List<TemporaryCharacter> existingList = scene.getTemporaryCharacters() != null
                    ? scene.getTemporaryCharacters()
                    : new ArrayList<>();

            Map<Long, TemporaryCharacter> existingTempCharsMap = existingList.stream()
                    .collect(Collectors.toMap(TemporaryCharacter::getId, tc -> tc, (tc1, tc2) -> tc1));

            List<TemporaryCharacter> tempCharsToKeep = new ArrayList<>();
            for (TemporaryCharacterDto tempCharDto : sceneDto.getTemporaryCharacters()) {
                TemporaryCharacter tempChar = tempCharDto.getId() != null
                        ? existingTempCharsMap.remove(tempCharDto.getId())
                        : null;

                if (tempChar == null) {
                    tempChar = new TemporaryCharacter();
                    tempChar.setScene(scene);
                }

                tempChar.setName(tempCharDto.getName());
                tempChar.setSummary(tempCharDto.getSummary());
                tempChar.setDetails(tempCharDto.getDetails());
                tempChar.setRelationships(tempCharDto.getRelationships());
                tempChar.setStatusInScene(tempCharDto.getStatusInScene());
                tempChar.setMoodInScene(tempCharDto.getMoodInScene());
                tempChar.setActionsInScene(tempCharDto.getActionsInScene());

                tempCharsToKeep.add(tempChar);
            }
            scene.setTemporaryCharacters(tempCharsToKeep);
        }

        OutlineScene saved = outlineSceneRepository.save(scene);
        return convertSceneToDto(saved);
    }

    private List<Long> parsePresentCharacterIds(String presentCharacters) {
        if (presentCharacters == null || presentCharacters.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }
        try {
            JsonNode node = objectMapper.readTree(presentCharacters);
            List<Long> ids = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode n : node) {
                    ids.add(n.asLong());
                }
            } else {
                for (String part : presentCharacters.split(",")) {
                    String t = part.trim();
                    if (!t.isEmpty()) {
                        try {
                            ids.add(Long.parseLong(t));
                        } catch (NumberFormatException ignored) { }
                    }
                }
            }
            return ids;
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}

package com.example.ainovel.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;

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
import com.example.ainovel.dto.SceneCharacterDto;
import com.example.ainovel.dto.SceneDto;
import com.example.ainovel.dto.TemporaryCharacterDto;
import com.example.ainovel.exception.ResourceNotFoundException;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.OutlineChapter;
import com.example.ainovel.model.OutlineScene;
import com.example.ainovel.model.SceneCharacter;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.TemporaryCharacter;
import com.example.ainovel.model.User;
import com.example.ainovel.model.UserSetting;
import com.example.ainovel.prompt.PromptTemplateService;
import com.example.ainovel.prompt.PromptType;
import com.example.ainovel.prompt.context.PromptContextFactory;
import com.example.ainovel.repository.CharacterCardRepository;
import com.example.ainovel.repository.OutlineCardRepository;
import com.example.ainovel.repository.OutlineChapterRepository;
import com.example.ainovel.repository.OutlineSceneRepository;
import com.example.ainovel.repository.StoryCardRepository;
import com.example.ainovel.repository.UserRepository;
import com.example.ainovel.repository.UserSettingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.ainovel.service.world.WorldService;

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
    private final CharacterCardRepository characterCardRepository;
    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final EncryptionService encryptionService;
    private final SettingsService settingsService;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final PromptTemplateService promptTemplateService;
    private final PromptContextFactory promptContextFactory;
    private final WorldService worldService;
    private final OutlineChapterRepository outlineChapterRepository;
    private final OutlineSceneRepository outlineSceneRepository;

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
        newOutline.setWorldId(storyCard.getWorldId());

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
        dto.setCreatedAt(outlineCard.getCreatedAt() != null ? outlineCard.getCreatedAt().toString() : null);
        dto.setWorldId(outlineCard.getWorldId());
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
        if (scene.getSceneCharacters() != null && !scene.getSceneCharacters().isEmpty()) {
            List<Long> presentIds = scene.getSceneCharacters().stream()
                    .map(SceneCharacter::getCharacterCard)
                    .filter(Objects::nonNull)
                    .map(CharacterCard::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            dto.setPresentCharacterIds(presentIds);
            String presentNames = scene.getSceneCharacters().stream()
                    .map(SceneCharacter::getCharacterName)
                    .filter(name -> name != null && !name.isBlank())
                    .distinct()
                    .collect(Collectors.joining(", "));
            dto.setPresentCharacters(presentNames.isBlank() ? scene.getPresentCharacters() : presentNames);
        } else {
            dto.setPresentCharacterIds(parsePresentCharacterIds(scene.getPresentCharacters()));
            dto.setPresentCharacters(scene.getPresentCharacters());
        }
        if (scene.getSceneCharacters() != null) {
            dto.setSceneCharacters(scene.getSceneCharacters().stream()
                    .map(this::convertSceneCharacterToDto)
                    .collect(Collectors.toList()));
        }
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
        dto.setStatus(temporaryCharacter.getStatus());
        dto.setThought(temporaryCharacter.getThought());
        dto.setAction(temporaryCharacter.getAction());
        return dto;
    }

    private SceneCharacterDto convertSceneCharacterToDto(SceneCharacter sceneCharacter) {
        SceneCharacterDto dto = new SceneCharacterDto();
        dto.setId(sceneCharacter.getId());
        dto.setCharacterCardId(sceneCharacter.getCharacterCard() != null
                ? sceneCharacter.getCharacterCard().getId()
                : null);
        dto.setCharacterName(sceneCharacter.getCharacterName());
        dto.setStatus(sceneCharacter.getStatus());
        dto.setThought(sceneCharacter.getThought());
        dto.setAction(sceneCharacter.getAction());
        return dto;
    }

    private void applySceneCharacterDtos(OutlineScene scene, List<SceneCharacterDto> sceneCharacterDtos, StoryCard storyCard) {
        List<SceneCharacter> targetList = scene.getSceneCharacters();
        if (targetList == null) {
            targetList = new ArrayList<>();
            scene.setSceneCharacters(targetList);
        }

        Map<Long, SceneCharacter> existingMap = targetList.stream()
                .filter(sc -> sc.getId() != null)
                .collect(Collectors.toMap(SceneCharacter::getId, sc -> sc, (a, _) -> a));

        List<SceneCharacter> charactersToKeep = new ArrayList<>();
        for (SceneCharacterDto dto : sceneCharacterDtos) {
            SceneCharacter entity = dto.getId() != null ? existingMap.remove(dto.getId()) : null;
            if (entity == null) {
                entity = new SceneCharacter();
                entity.setScene(scene);
            }

            CharacterCard linkedCard = resolveCharacterCard(dto.getCharacterCardId(), dto.getCharacterName(), storyCard);
            entity.setCharacterCard(linkedCard);
            if (dto.getCharacterName() != null && !dto.getCharacterName().isBlank()) {
                entity.setCharacterName(dto.getCharacterName().trim());
            } else if (linkedCard != null) {
                entity.setCharacterName(linkedCard.getName());
            }

            entity.setStatus(dto.getStatus());
            entity.setThought(dto.getThought());
            entity.setAction(dto.getAction());

            charactersToKeep.add(entity);
        }

        targetList.clear();
        targetList.addAll(charactersToKeep);
        syncScenePresentCharacters(scene, charactersToKeep, Collections.emptyList());
    }

    private void syncScenePresentCharacters(OutlineScene scene, List<SceneCharacter> sceneCharacters, List<String> fallbackNames) {
        Set<String> uniqueNames = new LinkedHashSet<>();
        if (sceneCharacters != null) {
            sceneCharacters.stream()
                    .map(SceneCharacter::getCharacterName)
                    .filter(name -> name != null && !name.isBlank())
                    .map(String::trim)
                    .forEach(uniqueNames::add);
        }

        if ((uniqueNames.isEmpty()) && fallbackNames != null) {
            fallbackNames.stream()
                    .filter(name -> name != null && !name.isBlank())
                    .map(String::trim)
                    .forEach(uniqueNames::add);
        }

        if (uniqueNames.isEmpty()) {
            scene.setPresentCharacters(null);
        } else {
            scene.setPresentCharacters(String.join(", ", uniqueNames));
        }
    }

    private String buildPresentCharactersStringFromIds(List<Long> characterIds, StoryCard storyCard, String fallback) {
        if (characterIds == null || characterIds.isEmpty()) {
            return (fallback != null && !fallback.isBlank()) ? fallback : null;
        }

        Set<String> resolvedNames = new LinkedHashSet<>();
        if (storyCard != null && storyCard.getCharacters() != null) {
            Map<Long, String> idToName = storyCard.getCharacters().stream()
                    .filter(card -> card.getId() != null && card.getName() != null && !card.getName().isBlank())
                    .collect(Collectors.toMap(
                            CharacterCard::getId,
                            card -> card.getName().trim(),
                            (existing, _) -> existing
                    ));
            for (Long characterId : characterIds) {
                if (characterId == null) {
                    continue;
                }
                String name = idToName.get(characterId);
                if (name != null && !name.isBlank()) {
                    resolvedNames.add(name.trim());
                }
            }
        }

        if (!resolvedNames.isEmpty()) {
            return String.join(", ", resolvedNames);
        }

        return (fallback != null && !fallback.isBlank()) ? fallback : null;
    }

    private CharacterCard resolveCharacterCard(Long characterCardId, String characterName, StoryCard storyCard) {
        if (characterCardId != null) {
            Optional<CharacterCard> byId = characterCardRepository.findById(characterCardId);
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        if (storyCard != null && storyCard.getCharacters() != null && characterName != null) {
            String trimmed = characterName.trim();
            return storyCard.getCharacters().stream()
                    .filter(card -> card.getName() != null && card.getName().trim().equalsIgnoreCase(trimmed))
                    .findFirst()
                    .orElse(null);
        }
        return null;
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
        if (!Objects.equals(outlineCard.getWorldId(), outlineDto.getWorldId())) {
            worldService.ensureSelectableWorld(outlineDto.getWorldId(), userId);
            outlineCard.setWorldId(outlineDto.getWorldId());
        }

        // --- Chapter Management ---
        Map<Long, OutlineChapter> existingChaptersMap = outlineCard.getChapters().stream()
                .collect(Collectors.toMap(OutlineChapter::getId, c -> c, (c1, _) -> c1));
        
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
                        .collect(Collectors.toMap(OutlineScene::getId, s -> s, (s1, _) -> s1));
                
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
                            String resolvedPresentCharacters = buildPresentCharactersStringFromIds(
                                    sceneDto.getPresentCharacterIds(),
                                    outlineCard.getStoryCard(),
                                    sceneDto.getPresentCharacters());
                            scene.setPresentCharacters(resolvedPresentCharacters);
                        }
                        if (sceneDto.getSceneCharacters() != null) {
                            applySceneCharacterDtos(scene, sceneDto.getSceneCharacters(), outlineCard.getStoryCard());
                        }

                        // --- Temporary Character Management ---
                        if (sceneDto.getTemporaryCharacters() != null) {
                            List<TemporaryCharacter> existingList = scene.getTemporaryCharacters() != null
                                    ? scene.getTemporaryCharacters()
                                    : new ArrayList<>();
                            Map<Long, TemporaryCharacter> existingTempCharsMap = existingList.stream()
                                    .collect(Collectors.toMap(TemporaryCharacter::getId, tc -> tc, (tc1, _) -> tc1));

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
                                tempChar.setStatus(tempCharDto.getStatus());
                                tempChar.setThought(tempCharDto.getThought());
                                tempChar.setAction(tempCharDto.getAction());
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
    public RefineResponse refineScene(Long sceneId, RefineRequest request, User user) {
        OutlineScene scene = findSceneById(sceneId);
        validateSceneAccess(scene, user.getId());
        // For backward compatibility, we can add a default context type
        if (request.getContextType() == null || request.getContextType().isBlank()) {
            request.setContextType("场景梗概");
        }
        return refineGenericText(request, user);
    }

    @Deprecated
    public RefineResponse refineSceneSynopsis(Long sceneId, RefineRequest request, User user) {
        return refineScene(sceneId, request, user);
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

        String refinedText = openAiService.refineText(user.getId(), request, apiKey, baseUrl, model);
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

        Long resolvedWorldId = request.getWorldId();
        if (resolvedWorldId == null) {
            resolvedWorldId = outlineCard.getWorldId() != null
                    ? outlineCard.getWorldId()
                    : storyCard.getWorldId();
        }
        if (resolvedWorldId != null) {
            worldService.ensureSelectableWorld(resolvedWorldId, user.getId());
        }
        outlineCard.setWorldId(resolvedWorldId);

        String previousChapterSynopsis = outlineChapterRepository
            .findByOutlineCardIdAndChapterNumber(outlineCard.getId(), request.getChapterNumber() - 1)
            .map(OutlineChapter::getSynopsis)
            .orElse("无，这是第一章。");

        Map<String, Object> context = promptContextFactory.buildOutlineChapterContext(
                user.getId(),
                storyCard,
                outlineCard,
                request,
                previousChapterSynopsis,
                resolvedWorldId
        );

        String prompt = promptTemplateService.render(PromptType.OUTLINE_CHAPTER, user.getId(), context);
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
        StoryCard storyCard = outlineCard.getStoryCard();
        for (JsonNode sceneNode : scenesNode) {
            OutlineScene scene = new OutlineScene();
            scene.setOutlineChapter(chapter);
            scene.setSceneNumber(sceneNode.path("sceneNumber").asInt());
            scene.setSynopsis(sceneNode.path("synopsis").asText());
            scene.setExpectedWords(request.getWordsPerSection());

            // Handle presentCharacters as an array of strings
            List<String> presentChars = new ArrayList<>();
            sceneNode.path("presentCharacters").forEach(node -> presentChars.add(node.asText()));

            // Handle structured scene characters
            List<SceneCharacter> sceneCharacters = new ArrayList<>();
            JsonNode sceneCharactersNode = sceneNode.path("sceneCharacters");
            for (JsonNode characterNode : sceneCharactersNode) {
                SceneCharacter sceneCharacter = new SceneCharacter();
                sceneCharacter.setScene(scene);
                String characterName = characterNode.path("characterName").asText("").trim();
                sceneCharacter.setStatus(characterNode.path("status").asText(null));
                sceneCharacter.setThought(characterNode.path("thought").asText(null));
                sceneCharacter.setAction(characterNode.path("action").asText(null));
                CharacterCard linkedCard = resolveCharacterCard(null, characterName, storyCard);
                sceneCharacter.setCharacterCard(linkedCard);
                if ((characterName == null || characterName.isBlank()) && linkedCard != null) {
                    sceneCharacter.setCharacterName(linkedCard.getName());
                } else {
                    sceneCharacter.setCharacterName(characterName);
                }
                sceneCharacters.add(sceneCharacter);
            }
            scene.setSceneCharacters(sceneCharacters);
            syncScenePresentCharacters(scene, sceneCharacters, presentChars);

            // Handle temporary characters
            List<TemporaryCharacter> tempChars = new ArrayList<>();
            JsonNode tempCharsNode = sceneNode.path("temporaryCharacters");
            for (JsonNode tempCharNode : tempCharsNode) {
                TemporaryCharacter tempChar = new TemporaryCharacter();
                tempChar.setScene(scene); // Set back-reference
                tempChar.setName(tempCharNode.path("name").asText());
                String summary = tempCharNode.hasNonNull("summary")
                        ? tempCharNode.path("summary").asText()
                        : tempCharNode.path("description").asText("");
                tempChar.setSummary(summary);
                tempChar.setDetails(tempCharNode.path("details").asText(null));
                tempChar.setRelationships(tempCharNode.path("relationships").asText(null));
                String status = tempCharNode.hasNonNull("status")
                        ? tempCharNode.path("status").asText()
                        : tempCharNode.path("statusInScene").asText(null);
                String thought = tempCharNode.hasNonNull("thought")
                        ? tempCharNode.path("thought").asText()
                        : tempCharNode.path("moodInScene").asText(null);
                String action = tempCharNode.hasNonNull("action")
                        ? tempCharNode.path("action").asText()
                        : tempCharNode.path("actionsInScene").asText(null);
                tempChar.setStatus(status);
                tempChar.setThought(thought);
                tempChar.setAction(action);
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
            StoryCard storyCard = Optional.ofNullable(scene.getOutlineChapter())
                    .map(OutlineChapter::getOutlineCard)
                    .map(OutlineCard::getStoryCard)
                    .orElse(null);
            String resolvedPresentCharacters = buildPresentCharactersStringFromIds(
                    sceneDto.getPresentCharacterIds(),
                    storyCard,
                    sceneDto.getPresentCharacters());
            scene.setPresentCharacters(resolvedPresentCharacters);
        }
        if (sceneDto.getSceneCharacters() != null) {
            applySceneCharacterDtos(scene, sceneDto.getSceneCharacters(), scene.getOutlineChapter().getOutlineCard().getStoryCard());
        }

        // Replace temporary characters if provided in DTO
        if (sceneDto.getTemporaryCharacters() != null) {
            List<TemporaryCharacter> existingList = scene.getTemporaryCharacters() != null
                    ? scene.getTemporaryCharacters()
                    : new ArrayList<>();

            Map<Long, TemporaryCharacter> existingTempCharsMap = existingList.stream()
                    .collect(Collectors.toMap(TemporaryCharacter::getId, tc -> tc, (tc1, _) -> tc1));

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
                tempChar.setStatus(tempCharDto.getStatus());
                tempChar.setThought(tempCharDto.getThought());
                tempChar.setAction(tempCharDto.getAction());

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

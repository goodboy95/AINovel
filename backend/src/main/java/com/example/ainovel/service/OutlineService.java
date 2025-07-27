package com.example.ainovel.service;

import com.example.ainovel.dto.*;
import com.example.ainovel.model.*;
import com.example.ainovel.repository.*;
import com.example.ainovel.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
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
    public OutlineDto createOutline(OutlineRequest outlineRequest, Long userId) {
        User user = findUserById(userId);
        StoryCard storyCard = findStoryCardById(outlineRequest.getStoryCardId());
        validateStoryCardAccess(storyCard, userId);

        AiService aiService = getAiServiceForUser(user);
        String apiKey = getDecryptedApiKeyForUser(user);

        String prompt = buildOutlinePrompt(storyCard, outlineRequest.getNumberOfChapters(), outlineRequest.getPointOfView());
        String jsonResponse = aiService.generate(prompt, apiKey);
        logger.info("AI Response JSON for outline generation: {}", jsonResponse);

        try {
            OutlineCard outlineCard = parseAndSaveOutline(jsonResponse, user, storyCard, outlineRequest.getPointOfView());
            return convertToDto(outlineCard);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse AI response for outline: {}", jsonResponse, e);
            throw new RuntimeException("Failed to parse AI response", e);
        }
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

    private String buildOutlinePrompt(StoryCard storyCard, int numberOfChapters, String pointOfView) {
        String characterProfiles = storyCard.getCharacters().stream()
                .map(c -> String.format("- %s: %s", c.getName(), c.getSynopsis()))
                .collect(Collectors.joining("\n"));

        return String.format(
            "你是一个专业的小说大纲设计师。请根据以下信息，设计一个详细的故事大纲。\n" +
            "故事信息:\n- 标题: %s\n- 走向: %s\n" +
            "主要角色:\n%s\n" +
            "要求:\n- 总章节数: %d\n- 叙事视角: %s\n\n" +
            "请以JSON格式返回。根对象包含一个 \"chapters\" 键，其值为一个数组。\n" +
            "每个章节对象应包含 \"chapterNumber\", \"title\", \"synopsis\" 和一个 \"scenes\" 数组。\n" +
            "每个场景对象应包含 \"sceneNumber\", \"synopsis\", \"expectedWords\"。\n" +
            "请确保章节和场景的梗概连贯且符合故事走向和角色设定。",
            storyCard.getTitle(), storyCard.getStoryArc(), characterProfiles, numberOfChapters, pointOfView
        );
    }

    private OutlineCard parseAndSaveOutline(String jsonResponse, User user, StoryCard storyCard, String pointOfView) throws JsonProcessingException {
        String cleanedJsonResponse = cleanJson(jsonResponse);
        JsonNode rootNode = objectMapper.readTree(cleanedJsonResponse);
        JsonNode chaptersNode = rootNode.path("chapters");

        OutlineCard outlineCard = new OutlineCard();
        outlineCard.setUser(user);
        outlineCard.setStoryCard(storyCard);
        outlineCard.setTitle(storyCard.getTitle() + " - 大纲");
        outlineCard.setPointOfView(pointOfView);

        List<OutlineChapter> chapters = new ArrayList<>();
        for (JsonNode chapterNode : chaptersNode) {
            OutlineChapter chapter = new OutlineChapter();
            chapter.setOutlineCard(outlineCard);
            chapter.setChapterNumber(chapterNode.path("chapterNumber").asInt());
            chapter.setTitle(chapterNode.path("title").asText());
            chapter.setSynopsis(chapterNode.path("synopsis").asText());

            List<OutlineScene> scenes = new ArrayList<>();
            JsonNode scenesNode = chapterNode.path("scenes");
            for (JsonNode sceneNode : scenesNode) {
                OutlineScene scene = new OutlineScene();
                scene.setOutlineChapter(chapter);
                scene.setSceneNumber(sceneNode.path("sceneNumber").asInt());
                scene.setSynopsis(sceneNode.path("synopsis").asText());
                scene.setExpectedWords(sceneNode.path("expectedWords").asInt(1000)); // Default value
                scenes.add(scene);
            }
            chapter.setScenes(scenes);
            chapters.add(chapter);
        }
        outlineCard.setChapters(chapters);

        return outlineCardRepository.save(outlineCard);
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
    public OutlineDto updateOutline(Long outlineId, OutlineDto outlineDto, Long userId) {
        OutlineCard outlineCard = findOutlineCardById(outlineId);
        validateOutlineAccess(outlineCard, userId);

        // This mapping logic is complex and could be moved to a dedicated mapper class.
        outlineCard.setTitle(outlineDto.getTitle());
        outlineCard.setPointOfView(outlineDto.getPointOfView());
        outlineCard.getChapters().clear(); // Simple strategy: clear and re-add.

        for (ChapterDto chapterDto : outlineDto.getChapters()) {
            OutlineChapter chapter = new OutlineChapter();
            chapter.setId(chapterDto.getId());
            chapter.setOutlineCard(outlineCard);
            chapter.setChapterNumber(chapterDto.getChapterNumber());
            chapter.setTitle(chapterDto.getTitle());
            chapter.setSynopsis(chapterDto.getSynopsis());

            List<OutlineScene> scenes = new ArrayList<>();
            for (SceneDto sceneDto : chapterDto.getScenes()) {
                OutlineScene scene = new OutlineScene();
                scene.setId(sceneDto.getId());
                scene.setOutlineChapter(chapter);
                scene.setSceneNumber(sceneDto.getSceneNumber());
                scene.setSynopsis(sceneDto.getSynopsis());
                scene.setExpectedWords(sceneDto.getExpectedWords());
                scenes.add(scene);
            }
            chapter.setScenes(scenes);
            outlineCard.getChapters().add(chapter);
        }

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
    public RefineResponse refineSceneSynopsis(Long sceneId, RefineRequest request, User user) {
        OutlineScene scene = findSceneById(sceneId);
        validateSceneAccess(scene, user.getId());

        AiService aiService = getAiServiceForUser(user);
        String apiKey = getDecryptedApiKeyForUser(user);

        String refinedText = aiService.refineText(request, apiKey);
        return new RefineResponse(refinedText);
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
        UserSetting settings = userSettingRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User settings not found. Please configure your AI provider first."));
        return (AiService) applicationContext.getBean(settings.getLlmProvider().toLowerCase());
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
}

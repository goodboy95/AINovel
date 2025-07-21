package com.example.ainovel.service;

import com.example.ainovel.dto.*;
import com.example.ainovel.model.*;
import com.example.ainovel.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OutlineService {

    private static final Logger logger = LoggerFactory.getLogger(OutlineService.class);

    @Autowired
    private OutlineCardRepository outlineCardRepository;

    @Autowired
    private StoryCardRepository storyCardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingRepository userSettingRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Transactional
    public OutlineDto createOutline(OutlineRequest outlineRequest, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        StoryCard storyCard = storyCardRepository.findById(outlineRequest.getStoryCardId())
                .orElseThrow(() -> new RuntimeException("StoryCard not found"));

        UserSetting settings = userSettingRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User settings not found. Please configure your AI provider first."));

        String apiKey = encryptionService.decrypt(settings.getApiKey());
        if (apiKey == null) {
            throw new IllegalStateException("API Key is not configured.");
        }
        
        AiService aiService = (AiService) applicationContext.getBean(settings.getLlmProvider().toLowerCase());

        String prompt = buildOutlinePrompt(storyCard, outlineRequest.getNumberOfChapters(), outlineRequest.getPointOfView());
        String jsonResponse = aiService.generate(prompt, apiKey);

        logger.info("AI Response JSON: {}", jsonResponse);

        try {
            OutlineCard outlineCard = parseAndSaveOutline(jsonResponse, user, storyCard, outlineRequest.getPointOfView());
            return convertToDto(outlineCard);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse AI response: {}", jsonResponse, e);
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }

    public OutlineDto getOutlineById(Long outlineId, Long userId) {
        OutlineCard outlineCard = outlineCardRepository.findById(outlineId)
                .orElseThrow(() -> new RuntimeException("OutlineCard not found"));
        if (!outlineCard.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized access to outline card");
        }
        return convertToDto(outlineCard);
    }

    private String buildOutlinePrompt(StoryCard storyCard, int numberOfChapters, String pointOfView) {
        String characterProfiles = storyCard.getCharacters().stream()
                .map(c -> String.format("- %s: %s", c.getName(), c.getSynopsis()))
                .collect(Collectors.joining("\n"));

        return String.format(
                "你是一个专业的小说大纲设计师。请根据以下信息，设计一个详细的故事大纲。\n" +
                        "故事信息:\n" +
                        "- 标题: %s\n" +
                        "- 走向: %s\n" +
                        "主要角色:\n" +
                        "%s\n" +
                        "要求:\n" +
                        "- 总章节数: %d\n" +
                        "- 叙事视角: %s\n\n" +
                        "请以JSON格式返回。根对象包含一个 \"chapters\" 键，其值为一个数组。\n" +
                        "每个章节对象应包含 \"chapterNumber\", \"title\", \"synopsis\" 和一个 \"scenes\" 数组。\n" +
                        "每个场景对象应包含 \"sceneNumber\", \"synopsis\", \"expectedWords\"。\n" +
                        "请确保章节和场景的梗概连贯且符合故事走向和角色设定。",
                storyCard.getTitle(),
                storyCard.getStoryArc(),
                characterProfiles,
                numberOfChapters,
                pointOfView
        );
    }

    private OutlineCard parseAndSaveOutline(String jsonResponse, User user, StoryCard storyCard, String pointOfView) throws JsonProcessingException {
        String cleanedJsonResponse = jsonResponse;
        int firstBrace = cleanedJsonResponse.indexOf('{');
        int lastBrace = cleanedJsonResponse.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
            cleanedJsonResponse = cleanedJsonResponse.substring(firstBrace, lastBrace + 1);
        }
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
                scene.setExpectedWords(sceneNode.path("expectedWords").asInt());
                scenes.add(scene);
            }
            chapter.setScenes(scenes);
            chapters.add(chapter);
        }
        outlineCard.setChapters(chapters);

        return outlineCardRepository.save(outlineCard);
    }

    private OutlineDto convertToDto(OutlineCard outlineCard) {
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
}

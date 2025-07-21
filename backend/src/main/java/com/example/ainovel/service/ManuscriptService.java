package com.example.ainovel.service;

import com.example.ainovel.model.*;
import com.example.ainovel.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ManuscriptService {

    @Autowired
    private ManuscriptSectionRepository manuscriptSectionRepository;

    @Autowired
    private OutlineSceneRepository outlineSceneRepository;
    
    @Autowired
    private OutlineCardRepository outlineCardRepository;

    @Autowired
    private UserSettingRepository userSettingRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Autowired
    private ApplicationContext applicationContext;

    @Transactional
    public ManuscriptSection generateManuscriptForScene(Long sceneId, Long userId) {
        OutlineScene scene = outlineSceneRepository.findById(sceneId)
                .orElseThrow(() -> new RuntimeException("Scene not found with id: " + sceneId));
        
        OutlineChapter chapter = scene.getOutlineChapter();
        OutlineCard outline = chapter.getOutlineCard();
        StoryCard story = outline.getStoryCard();

        if (!story.getUser().getId().equals(userId)) {
            throw new RuntimeException("User not authorized for this story");
        }

        UserSetting settings = userSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("User settings not found. Please configure your AI provider first."));
        
        String apiKey = encryptionService.decrypt(settings.getApiKey());
        AiService aiService = (AiService) applicationContext.getBean(settings.getLlmProvider().toLowerCase());

        String prompt = buildManuscriptPrompt(scene);
        String content = aiService.generate(prompt, apiKey);

        ManuscriptSection section = new ManuscriptSection();
        section.setScene(scene);
        section.setContent(content);
        section.setVersion(1); // Initial version
        section.setActive(true);

        return manuscriptSectionRepository.save(section);
    }

    @Transactional
    public ManuscriptSection updateManuscript(Long sectionId, ManuscriptSection sectionDetails, Long userId) {
        ManuscriptSection section = manuscriptSectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("ManuscriptSection not found with id: " + sectionId));
        
        if (!section.getScene().getOutlineChapter().getOutlineCard().getStoryCard().getUser().getId().equals(userId)) {
            throw new RuntimeException("User not authorized for this manuscript section");
        }

        section.setContent(sectionDetails.getContent());
        return manuscriptSectionRepository.save(section);
    }

    @Transactional(readOnly = true)
    public List<ManuscriptSection> getManuscriptForOutline(Long outlineId, Long userId) {
        OutlineCard outline = outlineCardRepository.findById(outlineId)
                .orElseThrow(() -> new RuntimeException("Outline not found with id: " + outlineId));

        if (!outline.getUser().getId().equals(userId)) {
            throw new RuntimeException("User not authorized for this outline");
        }

        return manuscriptSectionRepository.findBySceneOutlineChapterOutlineCardId(outlineId);
    }

    private String buildManuscriptPrompt(OutlineScene scene) {
        OutlineChapter chapter = scene.getOutlineChapter();
        OutlineCard outline = chapter.getOutlineCard();
        StoryCard story = outline.getStoryCard();

        String characterProfiles = story.getCharacters().stream()
                .map(c -> String.format("- %s: %s", c.getName(), c.getSynopsis()))
                .collect(Collectors.joining("\n"));

        // Simplified context for now. In a real scenario, you might fetch previous sections.
        String previousSectionContent = "This is the beginning of the story.";

        return String.format(
            "你是一位才华横溢的小说家。现在请你接续创作故事。\n" +
            "**全局信息:**\n" +
            "- 故事简介: %s\n" +
            "- 叙事视角: %s\n" +
            "**主要角色设定:**\n" +
            "%s\n" +
            "**本章梗概:**\n" +
            "%s\n" +
            "**本节梗概:**\n" +
            "%s\n" +
            "**上下文:**\n" +
            "- 上一节内容: \"%s\"\n\n" +
            "请根据以上所有信息，创作本节的详细内容，字数在 %d 字左右。文笔要生动，符合故事基调和人物性格。请直接开始写正文，不要包含任何解释性文字。",
            story.getSynopsis(),
            outline.getPointOfView(),
            characterProfiles,
            chapter.getSynopsis(),
            scene.getSynopsis(),
            previousSectionContent,
            scene.getExpectedWords()
        );
    }
}

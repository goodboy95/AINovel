package com.example.ainovel.service;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.example.ainovel.dto.ChapterDto;
import com.example.ainovel.dto.GenerateChapterRequest;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.OutlineChapter;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.User;
import com.example.ainovel.model.UserSetting;
import com.example.ainovel.repository.OutlineCardRepository;
import com.example.ainovel.repository.OutlineChapterRepository;
import com.example.ainovel.repository.UserSettingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OutlineServiceTest {

    @Mock
    private OutlineCardRepository outlineCardRepository;
    @Mock
    private OutlineChapterRepository outlineChapterRepository;
    @Mock
    private UserSettingRepository userSettingRepository;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private AiService aiService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OutlineService outlineService;

    private User user;
    private StoryCard storyCard;
    private OutlineCard outlineCard;
    private GenerateChapterRequest request;

    private final Long USER_ID = 1L;
    private final Long STORY_CARD_ID = 10L;
    private final Long OUTLINE_ID = 100L;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);

        storyCard = new StoryCard();
        storyCard.setId(STORY_CARD_ID);
        storyCard.setUser(user);
        storyCard.setSynopsis("A grand space opera.");
        storyCard.setStoryArc("From rags to riches.");
        CharacterCard mainChar = new CharacterCard();
        mainChar.setName("艾丽莎");
        mainChar.setSynopsis("A brave pilot.");
        storyCard.setCharacters(List.of(mainChar));

        outlineCard = new OutlineCard();
        outlineCard.setId(OUTLINE_ID);
        outlineCard.setUser(user);
        outlineCard.setStoryCard(storyCard);

        request = new GenerateChapterRequest();
        request.setChapterNumber(1);
        request.setSectionsPerChapter(2);
        request.setWordsPerSection(500);
    }

    private String getMockAiResponse() {
        return """
        {
          "title": "第一章：意外的访客",
          "synopsis": "一个神秘的访客打破了主角平静的生活，带来了一个关乎整个星系命运的消息。",
          "scenes": [
            {
              "sceneNumber": 1,
              "synopsis": "在偏远的采矿星球上，主角艾丽莎正在维修她的飞船。突然，一个衣着考究、自称“信使”的老人找到了她，声称带来了她失踪多年父亲的消息。老人看起来非常疲惫，但眼神锐利。",
              "presentCharacters": ["艾丽莎"],
              "characterStates": {
                "艾丽莎": "警惕但又好奇。她对任何与父亲有关的消息都抱有希望，但多年的独自生活让她不轻易相信陌生人。"
              },
              "temporaryCharacters": [
                {
                  "name": "信使",
                  "description": "一位年迈但精神矍铄的老人，身负传递重要信息的使命，身份神秘。"
                }
              ]
            },
            {
              "sceneNumber": 2,
              "synopsis": "信使向艾丽莎展示了一枚刻有家族徽记的信物，并讲述了一个关于古代外星神器和迫在眉睫的星际威胁的故事。艾丽莎的内心在怀疑和继承父亲遗志的使命感之间激烈斗争。",
              "presentCharacters": ["艾丽莎"],
              "characterStates": {
                "艾丽莎": "内心震惊，从最初的怀疑转变为沉重的责任感。她意识到自己的生活将永远改变。"
              },
              "temporaryCharacters": []
            }
          ]
        }
        """;
    }

    @Test
    void testGenerateChapterOutline_Success() throws JsonProcessingException {
        // Arrange
        UserSetting userSetting = new UserSetting();
        userSetting.setLlmProvider("gemini");
        String mockApiKey = "decrypted-api-key";
        String mockJsonResponse = getMockAiResponse();

        when(outlineCardRepository.findById(OUTLINE_ID)).thenReturn(Optional.of(outlineCard));
        when(userSettingRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userSetting));
        when(encryptionService.decrypt(any())).thenReturn(mockApiKey);
        when(applicationContext.getBean("gemini", AiService.class)).thenReturn(aiService);
        when(aiService.generate(anyString(), eq(mockApiKey))).thenReturn(mockJsonResponse);
        when(outlineChapterRepository.findByOutlineCardIdAndChapterNumber(anyLong(), anyInt())).thenReturn(Optional.empty());
        // 让 save 直接返回传入的实体，以便后续断言
        when(outlineChapterRepository.save(any(OutlineChapter.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ChapterDto resultDto = outlineService.generateChapterOutline(OUTLINE_ID, request);

        // Assert
        assertNotNull(resultDto);
        assertEquals("第一章：意外的访客", resultDto.getTitle());
        assertEquals(2, resultDto.getScenes().size());

        // Verify interactions
        verify(outlineCardRepository, times(1)).findById(OUTLINE_ID);
        verify(aiService, times(1)).generate(anyString(), eq(mockApiKey));
        verify(outlineChapterRepository, times(1)).save(any(OutlineChapter.class));

        // Capture the saved chapter and verify its contents
        ArgumentCaptor<OutlineChapter> chapterCaptor = ArgumentCaptor.forClass(OutlineChapter.class);
        verify(outlineChapterRepository).save(chapterCaptor.capture());
        OutlineChapter savedChapter = chapterCaptor.getValue();

        assertNotNull(savedChapter);
        assertEquals("第一章：意外的访客", savedChapter.getTitle());
        assertEquals("一个神秘的访客打破了主角平静的生活，带来了一个关乎整个星系命运的消息。", savedChapter.getSynopsis());
        assertEquals(1, savedChapter.getChapterNumber());
        assertEquals(outlineCard, savedChapter.getOutlineCard());
        assertEquals(2, savedChapter.getScenes().size());

        // Verify first scene
        var scene1 = savedChapter.getScenes().get(0);
        assertEquals(1, scene1.getSceneNumber());
        assertTrue(scene1.getSynopsis().contains("自称“信使”的老人找到了她"));
        assertEquals("艾丽莎", scene1.getPresentCharacters());
        assertNotNull(scene1.getCharacterStates());
        assertEquals(1, scene1.getTemporaryCharacters().size());
        assertEquals("信使", scene1.getTemporaryCharacters().get(0).getName());
        assertEquals("一位年迈但精神矍铄的老人，身负传递重要信息的使命，身份神秘。", scene1.getTemporaryCharacters().get(0).getSummary());
        assertEquals(scene1, scene1.getTemporaryCharacters().get(0).getScene()); // Verify back-reference

        // Verify second scene
        var scene2 = savedChapter.getScenes().get(1);
        assertEquals(2, scene2.getSceneNumber());
        assertTrue(scene2.getSynopsis().contains("古代外星神器"));
        assertTrue(scene2.getTemporaryCharacters().isEmpty());

        // Verify DTO conversion
        assertEquals(1, resultDto.getScenes().get(0).getTemporaryCharacters().size());
        assertEquals("信使", resultDto.getScenes().get(0).getTemporaryCharacters().get(0).getName());
        assertTrue(resultDto.getScenes().get(1).getTemporaryCharacters().isEmpty());
    }
}
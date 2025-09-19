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

import com.example.ainovel.dto.ChapterDto;
import com.example.ainovel.dto.GenerateChapterRequest;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.OutlineChapter;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.User;
import com.example.ainovel.model.UserSetting;
import com.example.ainovel.repository.CharacterCardRepository;
import com.example.ainovel.repository.OutlineCardRepository;
import com.example.ainovel.repository.OutlineChapterRepository;
import com.example.ainovel.repository.OutlineSceneRepository;
import com.example.ainovel.repository.StoryCardRepository;
import com.example.ainovel.repository.UserRepository;
import com.example.ainovel.repository.UserSettingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OutlineServiceTest {

    @Mock
    private OutlineCardRepository outlineCardRepository;
    @Mock
    private StoryCardRepository storyCardRepository;
    @Mock
    private CharacterCardRepository characterCardRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSettingRepository userSettingRepository;
    @Mock
    private EncryptionService encryptionService;
    @Mock
    private SettingsService settingsService;
    @Mock
    private OpenAiService openAiService;

    @Mock
    private OutlineChapterRepository outlineChapterRepository;
    @Mock
    private OutlineSceneRepository outlineSceneRepository;

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
        mainChar.setId(101L);
        mainChar.setName("艾丽莎");
        mainChar.setSynopsis("A brave pilot.");
        storyCard.setCharacters(List.of(mainChar));
        mainChar.setStoryCard(storyCard);

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
              "sceneCharacters": [
                {
                  "characterName": "艾丽莎",
                  "status": "她刚修完飞船，身上带着机油味，精神保持高度警惕。",
                  "thought": "这个陌生人可能掌握父亲的线索，但也可能是危险的陷阱。",
                  "action": "暂时收起扳手，与信使保持距离交谈。"
                }
              ],
              "temporaryCharacters": [
                {
                  "name": "信使",
                  "summary": "一位年迈但精神矍铄的老人，身负传递重要信息的使命，身份神秘。",
                  "details": "他的外套沾着宇宙尘土，似乎长途跋涉而来。",
                  "relationships": "声称认识艾丽莎的父亲。",
                  "status": "旅途劳顿，但仍强打精神。",
                  "thought": "必须尽快把消息传达出去。",
                  "action": "掏出带有徽记的信物，试图取信于她。"
                }
              ]
            },
            {
              "sceneNumber": 2,
              "synopsis": "信使向艾丽莎展示了一枚刻有家族徽记的信物，并讲述了一个关于古代外星神器和迫在眉睫的星际威胁的故事。艾丽莎的内心在怀疑和继承父亲遗志的使命感之间激烈斗争。",
              "presentCharacters": ["艾丽莎"],
              "sceneCharacters": [
                {
                  "characterName": "艾丽莎",
                  "status": "被信使的故事震撼，心跳加速。",
                  "thought": "如果父亲真的陷入危机，我必须接过他的责任。",
                  "action": "握紧信物，准备跟随信使离开矿区。"
                }
              ],
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
        String mockApiKey = "decrypted-api-key";
        String mockJsonResponse = getMockAiResponse();

        when(outlineCardRepository.findById(OUTLINE_ID)).thenReturn(Optional.of(outlineCard));
        when(userSettingRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userSetting));
        when(encryptionService.decrypt(any())).thenReturn(mockApiKey);
        when(settingsService.getBaseUrlByUserId(USER_ID)).thenReturn("https://api.example.com/v1");
        when(settingsService.getModelNameByUserId(USER_ID)).thenReturn("gpt-4");
        when(openAiService.generate(anyString(), eq(mockApiKey), eq("https://api.example.com/v1"), eq("gpt-4"))).thenReturn(mockJsonResponse);
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
        verify(openAiService, times(1)).generate(anyString(), eq(mockApiKey), eq("https://api.example.com/v1"), eq("gpt-4"));
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
        assertEquals(1, scene1.getSceneCharacters().size());
        assertEquals("艾丽莎", scene1.getSceneCharacters().get(0).getCharacterName());
        assertTrue(scene1.getSceneCharacters().get(0).getStatus().contains("机油"));
        assertEquals(1, scene1.getTemporaryCharacters().size());
        assertEquals("信使", scene1.getTemporaryCharacters().get(0).getName());
        assertEquals("一位年迈但精神矍铄的老人，身负传递重要信息的使命，身份神秘。", scene1.getTemporaryCharacters().get(0).getSummary());
        assertEquals("他的外套沾着宇宙尘土，似乎长途跋涉而来。", scene1.getTemporaryCharacters().get(0).getDetails());
        assertEquals("旅途劳顿，但仍强打精神。", scene1.getTemporaryCharacters().get(0).getStatus());
        assertEquals("必须尽快把消息传达出去。", scene1.getTemporaryCharacters().get(0).getThought());
        assertEquals("掏出带有徽记的信物，试图取信于她。", scene1.getTemporaryCharacters().get(0).getAction());
        assertEquals(scene1, scene1.getTemporaryCharacters().get(0).getScene()); // Verify back-reference

        // Verify second scene
        var scene2 = savedChapter.getScenes().get(1);
        assertEquals(2, scene2.getSceneNumber());
        assertTrue(scene2.getSynopsis().contains("古代外星神器"));
        assertEquals(1, scene2.getSceneCharacters().size());
        assertTrue(scene2.getSceneCharacters().get(0).getThought().contains("责任"));
        assertTrue(scene2.getTemporaryCharacters().isEmpty());

        // Verify DTO conversion
        assertEquals(1, resultDto.getScenes().get(0).getTemporaryCharacters().size());
        assertEquals("信使", resultDto.getScenes().get(0).getTemporaryCharacters().get(0).getName());
        assertEquals(1, resultDto.getScenes().get(0).getSceneCharacters().size());
        assertEquals("艾丽莎", resultDto.getScenes().get(0).getSceneCharacters().get(0).getCharacterName());
        assertTrue(resultDto.getScenes().get(1).getTemporaryCharacters().isEmpty());
    }
}
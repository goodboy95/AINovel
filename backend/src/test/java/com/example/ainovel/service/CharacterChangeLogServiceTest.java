package com.example.ainovel.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ainovel.dto.AnalyzeCharacterChangesRequest;
import com.example.ainovel.exception.BadRequestException;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.CharacterChangeLog;
import com.example.ainovel.model.Manuscript;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.OutlineChapter;
import com.example.ainovel.model.OutlineScene;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.User;
import com.example.ainovel.repository.CharacterCardRepository;
import com.example.ainovel.repository.CharacterChangeLogRepository;
import com.example.ainovel.repository.ManuscriptRepository;
import com.example.ainovel.repository.OutlineSceneRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CharacterChangeLogServiceTest {

    @Mock
    private CharacterChangeLogRepository characterChangeLogRepository;
    @Mock
    private ManuscriptRepository manuscriptRepository;
    @Mock
    private CharacterCardRepository characterCardRepository;
    @Mock
    private OutlineSceneRepository outlineSceneRepository;
    @Mock
    private OpenAiService openAiService;
    @Mock
    private SettingsService settingsService;
    @Mock
    private ManuscriptService manuscriptService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CharacterChangeLogService characterChangeLogService;

    private Manuscript manuscript;
    private OutlineScene outlineScene;
    private CharacterCard characterCard;

    private static final Long MANUSCRIPT_ID = 100L;
    private static final Long SCENE_ID = 500L;
    private static final Long CHARACTER_ID = 900L;
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(USER_ID);

        StoryCard storyCard = new StoryCard();
        storyCard.setId(200L);
        storyCard.setUser(user);

        OutlineCard outlineCard = new OutlineCard();
        outlineCard.setId(300L);
        outlineCard.setStoryCard(storyCard);
        outlineCard.setUser(user);

        OutlineChapter chapter = new OutlineChapter();
        chapter.setId(400L);
        chapter.setOutlineCard(outlineCard);
        chapter.setChapterNumber(2);

        outlineScene = new OutlineScene();
        outlineScene.setId(SCENE_ID);
        outlineScene.setOutlineChapter(chapter);
        outlineScene.setSceneNumber(5);
        outlineScene.setPresentCharacters("[" + CHARACTER_ID + "]");

        manuscript = new Manuscript();
        manuscript.setId(MANUSCRIPT_ID);
        manuscript.setOutlineCard(outlineCard);
        manuscript.setUser(user);

        characterCard = new CharacterCard();
        characterCard.setId(CHARACTER_ID);
        characterCard.setStoryCard(storyCard);
        characterCard.setUser(user);
        characterCard.setName("林渊");
        characterCard.setSynopsis("冷静的侦探");
        characterCard.setDetails("初始详情");

        characterChangeLogService = new CharacterChangeLogService(
                characterChangeLogRepository,
                manuscriptRepository,
                characterCardRepository,
                outlineSceneRepository,
                openAiService,
                settingsService,
                manuscriptService,
                objectMapper);
    }

    @Test
    void analyzeAndPersist_shouldPersistLogWhenAiReturnsChanges() {
        AnalyzeCharacterChangesRequest request = new AnalyzeCharacterChangesRequest();
        request.setSceneId(SCENE_ID);
        request.setSectionContent("内容");
        request.setCharacterIds(List.of(CHARACTER_ID));

        when(manuscriptRepository.findById(MANUSCRIPT_ID)).thenReturn(Optional.of(manuscript));
        doNothing().when(manuscriptService).validateManuscriptAccess(manuscript, USER_ID);
        when(outlineSceneRepository.findById(SCENE_ID)).thenReturn(Optional.of(outlineScene));
        when(characterCardRepository.findAllById(request.getCharacterIds())).thenReturn(List.of(characterCard));
        when(settingsService.getDecryptedApiKeyByUserId(USER_ID)).thenReturn("api-key");
        when(settingsService.getBaseUrlByUserId(USER_ID)).thenReturn("https://base");
        when(settingsService.getModelNameByUserId(USER_ID)).thenReturn("gpt-test");
        when(openAiService.generateJson(any(String.class), eq("api-key"), eq("https://base"), eq("gpt-test")))
                .thenReturn("{" +
                        "\"newly_known_info\":\"得知真相\"," +
                        "\"character_changes\":\"情绪转变\"," +
                        "\"character_details_after\":\"最新详情\"," +
                        "\"no_change\":false}" );
        when(characterChangeLogRepository.findFirstByCharacter_IdAndManuscript_IdAndDeletedAtIsNullOrderByChapterNumberDescSectionNumberDescCreatedAtDesc(
                CHARACTER_ID, MANUSCRIPT_ID)).thenReturn(Optional.empty());
        when(characterChangeLogRepository.save(any(CharacterChangeLog.class))).thenAnswer(invocation -> {
            CharacterChangeLog log = invocation.getArgument(0);
            log.setId(1L);
            return log;
        });

        List<CharacterChangeLog> result = characterChangeLogService.analyzeAndPersist(MANUSCRIPT_ID, request, USER_ID);

        assertEquals(1, result.size());
        CharacterChangeLog log = result.get(0);
        assertEquals("得知真相", log.getNewlyKnownInfo());
        assertEquals("情绪转变", log.getCharacterChanges());
        assertEquals("最新详情", log.getCharacterDetailsAfter());
        assertEquals(false, log.getIsAutoCopied());
        assertEquals(Integer.valueOf(2), log.getChapterNumber());
        assertEquals(Integer.valueOf(5), log.getSectionNumber());
    }

    @Test
    void analyzeAndPersist_shouldCopyPreviousDetailsWhenNoChange() {
        AnalyzeCharacterChangesRequest request = new AnalyzeCharacterChangesRequest();
        request.setSceneId(SCENE_ID);
        request.setSectionContent("内容");
        request.setCharacterIds(List.of(CHARACTER_ID));

        CharacterChangeLog previous = new CharacterChangeLog();
        previous.setCharacter(characterCard);
        previous.setManuscript(manuscript);
        previous.setOutline(manuscript.getOutlineCard());
        previous.setSceneId(SCENE_ID);
        previous.setCharacterDetailsAfter("先前详情");

        when(manuscriptRepository.findById(MANUSCRIPT_ID)).thenReturn(Optional.of(manuscript));
        doNothing().when(manuscriptService).validateManuscriptAccess(manuscript, USER_ID);
        when(outlineSceneRepository.findById(SCENE_ID)).thenReturn(Optional.of(outlineScene));
        when(characterCardRepository.findAllById(request.getCharacterIds())).thenReturn(List.of(characterCard));
        when(settingsService.getDecryptedApiKeyByUserId(USER_ID)).thenReturn("api-key");
        when(settingsService.getBaseUrlByUserId(USER_ID)).thenReturn("https://base");
        when(settingsService.getModelNameByUserId(USER_ID)).thenReturn("gpt-test");
        when(openAiService.generateJson(any(String.class), eq("api-key"), eq("https://base"), eq("gpt-test")))
                .thenReturn("{" +
                        "\"newly_known_info\":\"\"," +
                        "\"character_changes\":\"\"," +
                        "\"character_details_after\":\"\"," +
                        "\"no_change\":true}" );
        when(characterChangeLogRepository.findFirstByCharacter_IdAndManuscript_IdAndDeletedAtIsNullOrderByChapterNumberDescSectionNumberDescCreatedAtDesc(
                CHARACTER_ID, MANUSCRIPT_ID)).thenReturn(Optional.of(previous));
        when(characterChangeLogRepository.save(any(CharacterChangeLog.class))).thenAnswer(invocation -> {
            CharacterChangeLog log = invocation.getArgument(0);
            log.setId(2L);
            return log;
        });

        List<CharacterChangeLog> result = characterChangeLogService.analyzeAndPersist(MANUSCRIPT_ID, request, USER_ID);

        CharacterChangeLog log = result.get(0);
        assertEquals("", log.getNewlyKnownInfo());
        assertEquals("", log.getCharacterChanges());
        assertEquals("先前详情", log.getCharacterDetailsAfter());
        assertEquals(true, log.getIsAutoCopied());
    }

    @Test
    void analyzeAndPersist_shouldThrowWhenAiMissingDetails() {
        AnalyzeCharacterChangesRequest request = new AnalyzeCharacterChangesRequest();
        request.setSceneId(SCENE_ID);
        request.setSectionContent("内容");
        request.setCharacterIds(List.of(CHARACTER_ID));

        when(manuscriptRepository.findById(MANUSCRIPT_ID)).thenReturn(Optional.of(manuscript));
        doNothing().when(manuscriptService).validateManuscriptAccess(manuscript, USER_ID);
        when(outlineSceneRepository.findById(SCENE_ID)).thenReturn(Optional.of(outlineScene));
        when(characterCardRepository.findAllById(request.getCharacterIds())).thenReturn(List.of(characterCard));
        when(settingsService.getDecryptedApiKeyByUserId(USER_ID)).thenReturn("api-key");
        when(settingsService.getBaseUrlByUserId(USER_ID)).thenReturn("https://base");
        when(settingsService.getModelNameByUserId(USER_ID)).thenReturn("gpt-test");
        when(openAiService.generateJson(any(String.class), eq("api-key"), eq("https://base"), eq("gpt-test")))
                .thenReturn("{" +
                        "\"newly_known_info\":\"\"," +
                        "\"character_changes\":\"\"," +
                        "\"no_change\":false}" );
        when(characterChangeLogRepository.findFirstByCharacter_IdAndManuscript_IdAndDeletedAtIsNullOrderByChapterNumberDescSectionNumberDescCreatedAtDesc(
                CHARACTER_ID, MANUSCRIPT_ID)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> characterChangeLogService.analyzeAndPersist(MANUSCRIPT_ID, request, USER_ID));
    }

    @Test
    void analyzeAndPersist_shouldThrowWhenSceneNotInOutline() {
        AnalyzeCharacterChangesRequest request = new AnalyzeCharacterChangesRequest();
        request.setSceneId(SCENE_ID);
        request.setSectionContent("内容");
        request.setCharacterIds(List.of(CHARACTER_ID));

        OutlineCard anotherOutline = new OutlineCard();
        anotherOutline.setId(999L);
        OutlineChapter otherChapter = new OutlineChapter();
        otherChapter.setOutlineCard(anotherOutline);
        OutlineScene mismatchedScene = new OutlineScene();
        mismatchedScene.setId(SCENE_ID);
        mismatchedScene.setOutlineChapter(otherChapter);
        mismatchedScene.setSceneNumber(1);

        when(manuscriptRepository.findById(MANUSCRIPT_ID)).thenReturn(Optional.of(manuscript));
        doNothing().when(manuscriptService).validateManuscriptAccess(manuscript, USER_ID);
        when(outlineSceneRepository.findById(SCENE_ID)).thenReturn(Optional.of(mismatchedScene));

        assertThrows(BadRequestException.class,
                () -> characterChangeLogService.analyzeAndPersist(MANUSCRIPT_ID, request, USER_ID));
    }
}


package com.example.ainovel.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import com.example.ainovel.dto.AnalyzeCharacterChangesRequest;
import com.example.ainovel.exception.BadRequestException;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.CharacterChangeLog;
import com.example.ainovel.model.User;
import com.example.ainovel.service.ManuscriptService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = ManuscriptController.class)
@AutoConfigureMockMvc(addFilters = false)
class ManuscriptControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ManuscriptService manuscriptService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setUsername("tester");
        authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    @Test
    void analyzeCharacterChanges_shouldReturnResponseList() throws Exception {
        CharacterCard character = new CharacterCard();
        character.setId(99L);
        character.setName("角色一");

        CharacterChangeLog log = new CharacterChangeLog();
        log.setId(5L);
        log.setCharacter(character);
        log.setChapterNumber(3);
        log.setSectionNumber(2);
        log.setCharacterDetailsAfter("最新详情");
        log.setNewlyKnownInfo("发现秘密");
        log.setCharacterChanges("情绪变化");
        log.setIsAutoCopied(false);

        when(manuscriptService.analyzeCharacterChanges(anyLong(), any(AnalyzeCharacterChangesRequest.class), anyLong()))
                .thenReturn(List.of(log));

        AnalyzeCharacterChangesRequest request = new AnalyzeCharacterChangesRequest();
        request.setSceneId(10L);
        request.setCharacterIds(List.of(99L));
        request.setSectionContent("正文");

        mockMvc.perform(post("/api/v1/manuscripts/1/sections/analyze-character-changes")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].characterId").value(99L))
                .andExpect(jsonPath("$[0].characterDetailsAfter").value("最新详情"));
    }

    @Test
    void analyzeCharacterChanges_shouldReturnBadRequestWhenServiceThrows() throws Exception {
        when(manuscriptService.analyzeCharacterChanges(anyLong(), any(AnalyzeCharacterChangesRequest.class), anyLong()))
                .thenThrow(new BadRequestException("invalid"));

        AnalyzeCharacterChangesRequest request = new AnalyzeCharacterChangesRequest();
        request.setSceneId(10L);
        request.setCharacterIds(List.of(1L));
        request.setSectionContent("正文");

        mockMvc.perform(post("/api/v1/manuscripts/1/sections/analyze-character-changes")
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCharacterChangeLogs_shouldReturnLogs() throws Exception {
        CharacterCard character = new CharacterCard();
        character.setId(77L);

        CharacterChangeLog log = new CharacterChangeLog();
        log.setId(8L);
        log.setCharacter(character);
        log.setChapterNumber(1);
        log.setSectionNumber(1);
        log.setCharacterDetailsAfter("详情");

        when(manuscriptService.getCharacterChangeLogs(anyLong(), anyLong(), anyLong())).thenReturn(List.of(log));

        mockMvc.perform(get("/api/v1/manuscripts/1/sections/2/character-change-logs")
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].logId").value(8L));
    }
}


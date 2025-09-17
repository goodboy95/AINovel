package com.example.ainovel.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.ainovel.dto.AnalyzeCharacterChangesRequest;
import com.example.ainovel.exception.BadRequestException;
import com.example.ainovel.exception.ResourceNotFoundException;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.CharacterChangeLog;
import com.example.ainovel.model.Manuscript;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.OutlineChapter;
import com.example.ainovel.model.OutlineScene;
import com.example.ainovel.repository.CharacterCardRepository;
import com.example.ainovel.repository.CharacterChangeLogRepository;
import com.example.ainovel.repository.ManuscriptRepository;
import com.example.ainovel.repository.OutlineSceneRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CharacterChangeLogService {

    private static final Logger log = LoggerFactory.getLogger(CharacterChangeLogService.class);

    private final CharacterChangeLogRepository characterChangeLogRepository;
    private final ManuscriptRepository manuscriptRepository;
    private final CharacterCardRepository characterCardRepository;
    private final OutlineSceneRepository outlineSceneRepository;
    private final OpenAiService openAiService;
    private final SettingsService settingsService;
    private final ManuscriptService manuscriptService;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<CharacterChangeLog> analyzeAndPersist(Long manuscriptId, AnalyzeCharacterChangesRequest request, Long userId) {
        if (request == null) {
            throw new BadRequestException("请求体不能为空");
        }
        if (request.getSceneId() == null) {
            throw new BadRequestException("sceneId 不能为空");
        }

        List<Long> characterIds = Optional.ofNullable(request.getCharacterIds()).orElse(Collections.emptyList());
        if (characterIds.isEmpty()) {
            throw new BadRequestException("请至少选择一个角色进行分析");
        }

        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found with id: " + manuscriptId));
        manuscriptService.validateManuscriptAccess(manuscript, userId);

        OutlineScene scene = outlineSceneRepository.findById(request.getSceneId())
                .orElseThrow(() -> new ResourceNotFoundException("Scene not found with id: " + request.getSceneId()));

        OutlineChapter chapter = scene.getOutlineChapter();
        OutlineCard outline = chapter != null ? chapter.getOutlineCard() : null;

        OutlineCard manuscriptOutline = manuscript.getOutlineCard();
        if (manuscriptOutline == null) {
            throw new BadRequestException("稿件缺少关联大纲信息");
        }
        if (request.getOutlineId() != null && !Objects.equals(request.getOutlineId(), manuscriptOutline.getId())) {
            throw new BadRequestException("大纲信息与稿件不匹配");
        }
        if (outline == null || !Objects.equals(outline.getId(), manuscriptOutline.getId())) {
            throw new BadRequestException("场景不属于当前稿件所关联的大纲");
        }

        Long storyCardId = outline.getStoryCard() != null ? outline.getStoryCard().getId() : null;

        Map<Long, CharacterCard> characterMap = new HashMap<>();
        List<CharacterCard> characters = characterCardRepository.findAllById(characterIds);
        if (characters.size() != characterIds.size()) {
            List<Long> foundIds = characters.stream().map(CharacterCard::getId).collect(Collectors.toList());
            List<Long> missing = characterIds.stream().filter(id -> !foundIds.contains(id)).collect(Collectors.toList());
            throw new BadRequestException("未找到以下角色: " + missing);
        }
        for (CharacterCard character : characters) {
            if (character.getStoryCard() == null || !Objects.equals(character.getStoryCard().getId(), storyCardId)) {
                throw new BadRequestException("角色 " + character.getName() + " 不属于当前故事");
            }
            characterMap.put(character.getId(), character);
        }

        String apiKey = settingsService.getDecryptedApiKeyByUserId(userId);
        if (apiKey == null || apiKey.isBlank()) {
            throw new BadRequestException("请先在设置中配置有效的 OpenAI API Key");
        }
        String baseUrl = settingsService.getBaseUrlByUserId(userId);
        String model = settingsService.getModelNameByUserId(userId);

        List<CharacterChangeLog> persisted = new ArrayList<>();
        for (Long characterId : characterIds) {
            CharacterCard character = characterMap.get(characterId);
            if (character == null) {
                continue;
            }

            Optional<CharacterChangeLog> previousLog = characterChangeLogRepository
                    .findFirstByCharacter_IdAndManuscript_IdAndDeletedAtIsNullOrderByChapterNumberDescSectionNumberDescCreatedAtDesc(
                            characterId, manuscriptId);

            String previousDetails = previousLog
                    .map(CharacterChangeLog::getCharacterDetailsAfter)
                    .filter(detail -> detail != null && !detail.isBlank())
                    .orElseGet(() -> defaultIfBlank(character.getDetails(), character.getSynopsis()));

            CharacterAnalysisResult analysis = analyzeWithAi(character, previousDetails, request.getSectionContent(), apiKey,
                    baseUrl, model);

            CharacterChangeLog logEntry = new CharacterChangeLog();
            logEntry.setCharacter(character);
            logEntry.setManuscript(manuscript);
            logEntry.setOutline(outline);
            logEntry.setSceneId(scene.getId());
            logEntry.setChapterNumber(chapter != null ? chapter.getChapterNumber() : request.getChapterNumber());
            logEntry.setSectionNumber(scene.getSceneNumber() != null ? scene.getSceneNumber() : request.getSectionNumber());
            logEntry.setNewlyKnownInfo(trimToEmpty(analysis.getNewlyKnownInfo()));
            logEntry.setCharacterChanges(trimToEmpty(analysis.getCharacterChanges()));

            boolean noChange = Boolean.TRUE.equals(analysis.getNoChange());
            if (noChange) {
                logEntry.setNewlyKnownInfo("");
                logEntry.setCharacterChanges("");
                if (previousLog.isPresent()) {
                    logEntry.copyFrom(previousLog.get());
                } else if (previousDetails != null && !previousDetails.isBlank()) {
                    logEntry.setCharacterDetailsAfter(previousDetails);
                    logEntry.setIsAutoCopied(true);
                } else {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "AI 标记角色无变化，但缺少可复制的历史详情");
                }
            } else {
                String detailsAfter = trimToEmpty(analysis.getCharacterDetailsAfter());
                if (detailsAfter.isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                            "AI 响应缺少 character_details_after 字段");
                }
                logEntry.setCharacterDetailsAfter(detailsAfter);
                logEntry.setIsAutoCopied(false);
            }

            CharacterChangeLog saved = characterChangeLogRepository.save(logEntry);
            persisted.add(saved);
        }

        return persisted;
    }

    @Transactional(readOnly = true)
    public List<CharacterChangeLog> getLogsForScene(Long manuscriptId, Long sceneId, Long userId) {
        Manuscript manuscript = manuscriptRepository.findById(manuscriptId)
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found with id: " + manuscriptId));
        manuscriptService.validateManuscriptAccess(manuscript, userId);

        OutlineScene scene = outlineSceneRepository.findById(sceneId)
                .orElseThrow(() -> new ResourceNotFoundException("Scene not found with id: " + sceneId));

        OutlineChapter chapter = scene.getOutlineChapter();
        OutlineCard outline = chapter != null ? chapter.getOutlineCard() : null;
        OutlineCard manuscriptOutline = manuscript.getOutlineCard();
        if (outline == null || manuscriptOutline == null || !Objects.equals(outline.getId(), manuscriptOutline.getId())) {
            throw new BadRequestException("场景不属于当前稿件所关联的大纲");
        }

        return characterChangeLogRepository
                .findByManuscript_IdAndSceneIdAndDeletedAtIsNullOrderByCharacter_Id(manuscriptId, sceneId);
    }

    private CharacterAnalysisResult analyzeWithAi(CharacterCard character, String previousDetails, String sectionContent,
            String apiKey, String baseUrl, String model) {
        String prompt = buildPrompt(character, previousDetails, sectionContent);
        try {
            String json = openAiService.generateJson(prompt, apiKey, baseUrl, model);
            return objectMapper.readValue(json, CharacterAnalysisResult.class);
        } catch (JsonProcessingException e) {
            log.error("解析角色 {} 的 AI 响应失败", character.getName(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "无法解析 AI 响应");
        } catch (RuntimeException e) {
            log.error("调用 AI 服务分析角色 {} 时出错", character.getName(), e);
            throw e;
        }
    }

    private String buildPrompt(CharacterCard character, String previousDetails, String sectionContent) {
        String synopsis = defaultIfBlank(character.getSynopsis(), "无");
        String baseline = defaultIfBlank(previousDetails, "无");
        String content = defaultIfBlank(sectionContent, "无");
        return "你是小说编辑，负责追踪角色状态。请根据提供的信息判断角色是否在本小节发生变化。\n"
                + "【角色既有设定】\n"
                + "- 角色名称: " + defaultIfBlank(character.getName(), "未知角色") + "\n"
                + "- 基础档案: " + synopsis + "\n"
                + "- 上一次记录的角色详情: " + baseline + "\n"
                + "【本节正文】\n"
                + content + "\n"
                + "任务：\n"
                + "1. 分析角色在本节新增认知或信息，使用第一人称/第三人称均可。\n"
                + "2. 总结角色状态（心理、生理、外貌、人际态度等）发生的变化。\n"
                + "3. 输出本节结束后的最新角色详情，需完整覆盖角色核心设定，允许保留未变化的描述。\n"
                + "输出格式必须是 JSON\n"
                + "{\n"
                + "  \"newly_known_info\": \"string，可为空字符串\",\n"
                + "  \"character_changes\": \"string，可为空字符串\",\n"
                + "  \"character_details_after\": \"string\",\n"
                + "  \"no_change\": true/false\n"
                + "}\n"
                + "若确无变化，将 newly_known_info 和 character_changes 置为 \"\"，并将 no_change 设为 true。";
    }

    private String trimToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class CharacterAnalysisResult {
        @JsonProperty("newly_known_info")
        private String newlyKnownInfo;
        @JsonProperty("character_changes")
        private String characterChanges;
        @JsonProperty("character_details_after")
        private String characterDetailsAfter;
        @JsonProperty("no_change")
        private Boolean noChange;

        public String getNewlyKnownInfo() {
            return newlyKnownInfo;
        }

        public String getCharacterChanges() {
            return characterChanges;
        }

        public String getCharacterDetailsAfter() {
            return characterDetailsAfter;
        }

        public Boolean getNoChange() {
            return noChange;
        }
    }
}


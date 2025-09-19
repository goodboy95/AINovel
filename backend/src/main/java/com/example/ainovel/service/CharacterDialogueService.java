package com.example.ainovel.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.example.ainovel.dto.CharacterDialogueRequest;
import com.example.ainovel.dto.CharacterDialogueResponse;
import com.example.ainovel.exception.ResourceNotFoundException;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.CharacterChangeLog;
import com.example.ainovel.model.Manuscript;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.repository.CharacterCardRepository;
import com.example.ainovel.repository.CharacterChangeLogRepository;
import com.example.ainovel.repository.ManuscriptRepository;

import lombok.RequiredArgsConstructor;

/**
 * Generates character dialogue responses guided by recent memories and current scene context.
 */
@Service
@RequiredArgsConstructor
public class CharacterDialogueService {

    private final ManuscriptRepository manuscriptRepository;
    private final CharacterCardRepository characterCardRepository;
    private final CharacterChangeLogRepository characterChangeLogRepository;
    private final SettingsService settingsService;
    private final OpenAiService openAiService;

    public CharacterDialogueResponse generateDialogue(CharacterDialogueRequest request, Long userId) {
        if (request == null) {
            throw new IllegalArgumentException("Dialogue generation request cannot be null.");
        }
        Manuscript manuscript = manuscriptRepository.findById(request.getManuscriptId())
                .orElseThrow(() -> new ResourceNotFoundException("Manuscript not found with id: " + request.getManuscriptId()));
        validateAccess(manuscript, userId);

        CharacterCard character = characterCardRepository.findById(request.getCharacterId())
                .orElseThrow(() -> new ResourceNotFoundException("Character not found with id: " + request.getCharacterId()));

        StoryCard storyCard = Optional.ofNullable(manuscript.getOutlineCard())
                .map(OutlineCard::getStoryCard)
                .orElseThrow(() -> new IllegalStateException("Manuscript is not linked to a story card."));
        if (!character.getStoryCard().getId().equals(storyCard.getId())) {
            throw new AccessDeniedException("Character does not belong to the manuscript's story.");
        }

        List<CharacterChangeLog> history = characterChangeLogRepository
                .findByManuscript_IdAndCharacter_IdOrderByCreatedAtDesc(manuscript.getId(), character.getId());
        List<String> keyMemories = history.stream()
                .map(CharacterChangeLog::getNewlyKnownInfo)
                .map(this::safeTrim)
                .filter(s -> s != null)
                .limit(5)
                .collect(Collectors.toCollection(ArrayList::new));

        String profile = safeTrim(character.getDetails());
        if (profile == null) {
            profile = safeTrim(character.getSynopsis());
        }
        if (profile == null) {
            profile = "暂无角色档案。";
        }

        String prompt = buildDialoguePrompt(
                character.getName(),
                profile,
                safeTrim(request.getCurrentSceneDescription()),
                safeTrim(request.getDialogueTopic()),
                keyMemories
        );

        String apiKey = settingsService.getDecryptedApiKeyByUserId(userId);
        String baseUrl = settingsService.getBaseUrlByUserId(userId);
        String model = settingsService.getModelNameByUserId(userId);

        String dialogue = openAiService.generate(prompt, apiKey, baseUrl, model);
        return new CharacterDialogueResponse(dialogue != null ? dialogue.trim() : "");
    }

    private void validateAccess(Manuscript manuscript, Long userId) {
        Long ownerFromManuscript = Optional.ofNullable(manuscript.getUser()).map(u -> u.getId()).orElse(null);
        Long ownerFromOutline = Optional.ofNullable(manuscript.getOutlineCard())
                .map(OutlineCard::getUser)
                .map(user -> user.getId())
                .orElse(null);
        Long owner = ownerFromManuscript != null ? ownerFromManuscript : ownerFromOutline;
        if (owner == null || !owner.equals(userId)) {
            throw new AccessDeniedException("User does not have permission to access this manuscript.");
        }
    }

    private String buildDialoguePrompt(String characterName,
                                       String characterProfile,
                                       String sceneDescription,
                                       String dialogueTopic,
                                       List<String> keyMemories) {
        StringBuilder builder = new StringBuilder();
        builder.append("你是一位擅长刻画人物的小说家，请以第一人称代入角色\"")
                .append(characterName)
                .append("\"，根据以下情境创作一段自然的对白回答。\n\n");
        builder.append("【角色设定】\n").append(characterProfile).append("\n\n");
        if (keyMemories != null && !keyMemories.isEmpty()) {
            builder.append("【角色重要记忆】\n");
            for (String memory : keyMemories) {
                builder.append("- ").append(memory).append("\n");
            }
            builder.append("\n");
        }
        if (sceneDescription != null) {
            builder.append("【当前场景】\n").append(sceneDescription).append("\n\n");
        }
        if (dialogueTopic != null) {
            builder.append("【对话主题】\n").append(dialogueTopic).append("\n\n");
        }
        builder.append("请输出角色的一段连续台词，保持语气、思考方式与其记忆一致，不要添加旁白。\n");
        return builder.toString();
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

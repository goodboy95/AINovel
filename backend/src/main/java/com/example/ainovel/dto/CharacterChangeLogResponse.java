package com.example.ainovel.dto;

import java.time.LocalDateTime;

import com.example.ainovel.model.CharacterChangeLog;

import lombok.Data;

@Data
public class CharacterChangeLogResponse {
    private Long logId;
    private Long characterId;
    private Integer chapterNumber;
    private Integer sectionNumber;
    private String newlyKnownInfo;
    private String characterChanges;
    private String characterDetailsAfter;
    private boolean autoCopied;
    private LocalDateTime createdAt;

    public static CharacterChangeLogResponse from(CharacterChangeLog log) {
        CharacterChangeLogResponse response = new CharacterChangeLogResponse();
        response.setLogId(log.getId());
        response.setCharacterId(log.getCharacter() != null ? log.getCharacter().getId() : null);
        response.setChapterNumber(log.getChapterNumber());
        response.setSectionNumber(log.getSectionNumber());
        response.setNewlyKnownInfo(log.getNewlyKnownInfo());
        response.setCharacterChanges(log.getCharacterChanges());
        response.setCharacterDetailsAfter(log.getCharacterDetailsAfter());
        response.setAutoCopied(Boolean.TRUE.equals(log.getIsAutoCopied()));
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}


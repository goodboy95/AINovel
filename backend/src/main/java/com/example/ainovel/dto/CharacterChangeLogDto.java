package com.example.ainovel.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * DTO exposed to clients for character change log entries.
 */
@Data
@Accessors(chain = true)
public class CharacterChangeLogDto {

    private Long id;
    private Long characterId;
    private String characterName;
    private Long manuscriptId;
    private Long outlineId;
    private Integer chapterNumber;
    private Integer sectionNumber;
    private String newlyKnownInfo;
    private String characterChanges;
    private String characterDetailsAfter;
    private Boolean isAutoCopied;
    private List<RelationshipChangeDto> relationshipChanges;
    private Boolean isTurningPoint;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

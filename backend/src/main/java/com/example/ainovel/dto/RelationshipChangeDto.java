package com.example.ainovel.dto;

import lombok.Data;

/**
 * Describes how the relationship between two characters changes within a section.
 */
@Data
public class RelationshipChangeDto {
    private Long targetCharacterId;
    private String previousRelationship;
    private String currentRelationship;
    private String changeReason;
}

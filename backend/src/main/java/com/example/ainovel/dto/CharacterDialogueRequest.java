package com.example.ainovel.dto;

import lombok.Data;

/**
 * Request payload for memory-driven dialogue generation.
 */
@Data
public class CharacterDialogueRequest {
    private Long characterId;
    private Long manuscriptId;
    private String currentSceneDescription;
    private String dialogueTopic;
}

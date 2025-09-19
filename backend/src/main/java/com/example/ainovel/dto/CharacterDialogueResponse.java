package com.example.ainovel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response payload for dialogue generation.
 */
@Data
@AllArgsConstructor
public class CharacterDialogueResponse {
    private String dialogue;
}

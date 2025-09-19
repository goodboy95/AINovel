package com.example.ainovel.dto;

import java.util.List;

import lombok.Data;

@Data
public class SceneDto {
    private Long id;
    private Integer sceneNumber;
    private String synopsis;
    private Integer expectedWords;
    // Store core present characters as CharacterCard IDs (JSON array string in entity)
    private List<Long> presentCharacterIds;
    private String presentCharacters;
    private List<SceneCharacterDto> sceneCharacters;
    private List<TemporaryCharacterDto> temporaryCharacters;
}

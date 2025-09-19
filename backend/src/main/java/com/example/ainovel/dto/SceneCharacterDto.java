package com.example.ainovel.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SceneCharacterDto {
    private Long id;
    private Long characterCardId;
    private String characterName;
    private String status;
    private String thought;
    private String action;
}

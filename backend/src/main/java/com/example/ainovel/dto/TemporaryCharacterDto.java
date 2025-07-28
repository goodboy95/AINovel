package com.example.ainovel.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TemporaryCharacterDto {
    private Long id;
    private String name;
    private String summary;
    private String details;
    private String relationships;
    private String statusInScene;
    private String moodInScene;
    private String actionsInScene;
}
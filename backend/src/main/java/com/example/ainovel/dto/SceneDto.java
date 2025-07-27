package com.example.ainovel.dto;

import lombok.Data;

@Data
public class SceneDto {
   private Long id;
   private Integer sceneNumber;
   private String synopsis;
   private Integer expectedWords;
   private String presentCharacters;
   private String characterStates;
}

package com.example.ainovel.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TemporaryCharacterDto {
    private Long id;
    private String name;
    private String description;
}
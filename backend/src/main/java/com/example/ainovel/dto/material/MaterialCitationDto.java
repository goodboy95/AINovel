package com.example.ainovel.dto.material;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MaterialCitationDto {

    private Long id;
    private Long materialId;
    private Long chunkId;
    private Integer chunkSeq;
    private String documentType;
    private Long documentId;
    private Long userId;
    private String usageContext;
    private LocalDateTime createdAt;
}


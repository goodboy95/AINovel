package com.example.ainovel.dto;

import java.util.List;

import lombok.Data;

@Data
public class AnalyzeCharacterChangesRequest {
    private Long sceneId;
    private Integer chapterNumber;
    private Integer sectionNumber;
    private String sectionContent;
    private List<Long> characterIds;
    private Long outlineId;
}


package com.example.ainovel.dto;

import java.util.List;

import lombok.Data;

/**
 * Request payload for analyzing character changes within a manuscript section.
 */
@Data
public class AnalyzeCharacterChangesRequest {
    private Integer chapterNumber;
    private Integer sectionNumber;
    private String sectionContent;
    private List<Long> characterIds;
}

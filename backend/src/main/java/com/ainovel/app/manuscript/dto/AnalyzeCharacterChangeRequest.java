package com.ainovel.app.manuscript.dto;

import java.util.List;

public record AnalyzeCharacterChangeRequest(Integer chapterNumber, Integer sectionNumber, String sectionContent, List<String> characterIds) {}

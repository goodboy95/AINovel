package com.example.ainovel.dto;

import lombok.Data;

@Data
public class GenerateChapterRequest {
    private int chapterNumber;
    private int sectionsPerChapter;
    private int wordsPerSection;
}
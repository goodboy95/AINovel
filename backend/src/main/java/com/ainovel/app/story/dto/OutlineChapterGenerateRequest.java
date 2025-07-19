package com.ainovel.app.story.dto;

public record OutlineChapterGenerateRequest(Integer chapterNumber, Integer sectionsPerChapter, Integer wordsPerSection, String worldId) {}

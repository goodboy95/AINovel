package com.ainovel.app.story.dto;

public record ChapterUpdateRequest(
        String title,
        String summary,
        Integer order
) {}


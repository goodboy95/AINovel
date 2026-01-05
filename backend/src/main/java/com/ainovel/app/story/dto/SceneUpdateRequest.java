package com.ainovel.app.story.dto;

public record SceneUpdateRequest(
        String title,
        String summary,
        String content,
        Integer order
) {}


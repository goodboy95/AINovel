package com.ainovel.app.story.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OutlineDto(UUID id,
                         UUID storyId,
                         String title,
                         String worldId,
                         List<ChapterDto> chapters,
                         Instant updatedAt) {
    public record ChapterDto(UUID id, String title, String summary, Integer order, List<SceneDto> scenes) {}
    public record SceneDto(UUID id, String title, String summary, String content, Integer order) {}
}

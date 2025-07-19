package com.ainovel.app.story.dto;

import java.util.List;
import java.util.UUID;

public record OutlineSaveRequest(String title, String worldId, List<ChapterPayload> chapters) {
    public record ChapterPayload(UUID id, String title, String summary, Integer order, List<ScenePayload> scenes) {}
    public record ScenePayload(UUID id, String title, String summary, String content, Integer order) {}
}

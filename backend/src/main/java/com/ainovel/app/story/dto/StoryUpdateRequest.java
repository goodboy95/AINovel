package com.ainovel.app.story.dto;

public record StoryUpdateRequest(String title,
                                 String synopsis,
                                 String genre,
                                 String tone,
                                 String status,
                                 String worldId) {}

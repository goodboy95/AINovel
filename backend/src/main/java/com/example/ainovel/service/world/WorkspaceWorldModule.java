package com.example.ainovel.service.world;

import java.time.Instant;

public class WorkspaceWorldModule {

    private final String key;
    private final String label;
    private final String fullContent;
    private final String excerpt;
    private final Instant updatedAt;

    public WorkspaceWorldModule(String key, String label, String fullContent, String excerpt, Instant updatedAt) {
        this.key = key;
        this.label = label;
        this.fullContent = fullContent;
        this.excerpt = excerpt;
        this.updatedAt = updatedAt;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getFullContent() {
        return fullContent;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

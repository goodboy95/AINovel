package com.example.ainovel.dto.world;

import com.example.ainovel.model.world.WorldModuleStatus;

import java.time.LocalDateTime;
import java.util.Map;

public class WorldModuleResponse {

    private String key;
    private String label;
    private WorldModuleStatus status;
    private Map<String, String> fields;
    private String contentHash;
    private String fullContent;
    private LocalDateTime fullContentUpdatedAt;

    public String getKey() {
        return key;
    }

    public WorldModuleResponse setKey(String key) {
        this.key = key;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public WorldModuleResponse setLabel(String label) {
        this.label = label;
        return this;
    }

    public WorldModuleStatus getStatus() {
        return status;
    }

    public WorldModuleResponse setStatus(WorldModuleStatus status) {
        this.status = status;
        return this;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public WorldModuleResponse setFields(Map<String, String> fields) {
        this.fields = fields;
        return this;
    }

    public String getContentHash() {
        return contentHash;
    }

    public WorldModuleResponse setContentHash(String contentHash) {
        this.contentHash = contentHash;
        return this;
    }

    public String getFullContent() {
        return fullContent;
    }

    public WorldModuleResponse setFullContent(String fullContent) {
        this.fullContent = fullContent;
        return this;
    }

    public LocalDateTime getFullContentUpdatedAt() {
        return fullContentUpdatedAt;
    }

    public WorldModuleResponse setFullContentUpdatedAt(LocalDateTime fullContentUpdatedAt) {
        this.fullContentUpdatedAt = fullContentUpdatedAt;
        return this;
    }
}

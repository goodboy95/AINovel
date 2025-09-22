package com.example.ainovel.dto;

import java.time.LocalDateTime;

public class ManuscriptDto {
    private Long id;
    private String title;
    private Long outlineId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long worldId;

    public ManuscriptDto() {}

    public ManuscriptDto(Long id, String title, Long outlineId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.outlineId = outlineId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public ManuscriptDto(Long id, String title, Long outlineId, LocalDateTime createdAt, LocalDateTime updatedAt, Long worldId) {
        this(id, title, outlineId, createdAt, updatedAt);
        this.worldId = worldId;
    }

    // Getters / Setters
    public Long getId() {
        return id;
    }

    public ManuscriptDto setId(Long id) {
        this.id = id;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public ManuscriptDto setTitle(String title) {
        this.title = title;
        return this;
    }

    public Long getOutlineId() {
        return outlineId;
    }

    public ManuscriptDto setOutlineId(Long outlineId) {
        this.outlineId = outlineId;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ManuscriptDto setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public ManuscriptDto setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Long getWorldId() {
        return worldId;
    }

    public ManuscriptDto setWorldId(Long worldId) {
        this.worldId = worldId;
        return this;
    }
}
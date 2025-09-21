package com.example.ainovel.dto.world;

import com.example.ainovel.model.world.WorldModuleStatus;
import com.example.ainovel.model.world.WorldStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class WorldSummaryResponse {

    private Long id;
    private String name;
    private String tagline;
    private List<String> themes;
    private WorldStatus status;
    private Integer version;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private Map<String, WorldModuleStatus> moduleProgress;

    public Long getId() {
        return id;
    }

    public WorldSummaryResponse setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public WorldSummaryResponse setName(String name) {
        this.name = name;
        return this;
    }

    public String getTagline() {
        return tagline;
    }

    public WorldSummaryResponse setTagline(String tagline) {
        this.tagline = tagline;
        return this;
    }

    public List<String> getThemes() {
        return themes;
    }

    public WorldSummaryResponse setThemes(List<String> themes) {
        this.themes = themes;
        return this;
    }

    public WorldStatus getStatus() {
        return status;
    }

    public WorldSummaryResponse setStatus(WorldStatus status) {
        this.status = status;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public WorldSummaryResponse setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public WorldSummaryResponse setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public WorldSummaryResponse setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
        return this;
    }

    public Map<String, WorldModuleStatus> getModuleProgress() {
        return moduleProgress;
    }

    public WorldSummaryResponse setModuleProgress(Map<String, WorldModuleStatus> moduleProgress) {
        this.moduleProgress = moduleProgress;
        return this;
    }
}

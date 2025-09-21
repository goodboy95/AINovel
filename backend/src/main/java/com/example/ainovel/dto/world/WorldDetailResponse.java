package com.example.ainovel.dto.world;

import com.example.ainovel.model.world.WorldStatus;

import java.time.LocalDateTime;
import java.util.List;

public class WorldDetailResponse {

    private WorldInfo world;
    private List<WorldModuleResponse> modules;

    public WorldInfo getWorld() {
        return world;
    }

    public WorldDetailResponse setWorld(WorldInfo world) {
        this.world = world;
        return this;
    }

    public List<WorldModuleResponse> getModules() {
        return modules;
    }

    public WorldDetailResponse setModules(List<WorldModuleResponse> modules) {
        this.modules = modules;
        return this;
    }

    public static class WorldInfo {
        private Long id;
        private String name;
        private String tagline;
        private List<String> themes;
        private String creativeIntent;
        private String notes;
        private WorldStatus status;
        private Integer version;
        private LocalDateTime publishedAt;
        private LocalDateTime updatedAt;
        private LocalDateTime createdAt;
        private LocalDateTime lastEditedAt;

        public Long getId() {
            return id;
        }

        public WorldInfo setId(Long id) {
            this.id = id;
            return this;
        }

        public String getName() {
            return name;
        }

        public WorldInfo setName(String name) {
            this.name = name;
            return this;
        }

        public String getTagline() {
            return tagline;
        }

        public WorldInfo setTagline(String tagline) {
            this.tagline = tagline;
            return this;
        }

        public List<String> getThemes() {
            return themes;
        }

        public WorldInfo setThemes(List<String> themes) {
            this.themes = themes;
            return this;
        }

        public String getCreativeIntent() {
            return creativeIntent;
        }

        public WorldInfo setCreativeIntent(String creativeIntent) {
            this.creativeIntent = creativeIntent;
            return this;
        }

        public String getNotes() {
            return notes;
        }

        public WorldInfo setNotes(String notes) {
            this.notes = notes;
            return this;
        }

        public WorldStatus getStatus() {
            return status;
        }

        public WorldInfo setStatus(WorldStatus status) {
            this.status = status;
            return this;
        }

        public Integer getVersion() {
            return version;
        }

        public WorldInfo setVersion(Integer version) {
            this.version = version;
            return this;
        }

        public LocalDateTime getPublishedAt() {
            return publishedAt;
        }

        public WorldInfo setPublishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public WorldInfo setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public WorldInfo setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public LocalDateTime getLastEditedAt() {
            return lastEditedAt;
        }

        public WorldInfo setLastEditedAt(LocalDateTime lastEditedAt) {
            this.lastEditedAt = lastEditedAt;
            return this;
        }
    }
}

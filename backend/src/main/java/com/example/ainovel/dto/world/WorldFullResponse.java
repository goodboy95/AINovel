package com.example.ainovel.dto.world;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public class WorldFullResponse {

    private WorldInfo world;
    private List<WorldModuleFull> modules;

    public WorldInfo getWorld() {
        return world;
    }

    public WorldFullResponse setWorld(WorldInfo world) {
        this.world = world;
        return this;
    }

    public List<WorldModuleFull> getModules() {
        return modules;
    }

    public WorldFullResponse setModules(List<WorldModuleFull> modules) {
        this.modules = modules;
        return this;
    }

    public static class WorldInfo {
        private Long id;
        private String name;
        private String tagline;
        private List<String> themes;
        private String creativeIntent;
        private Integer version;
        private LocalDateTime publishedAt;

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
    }

    public static class WorldModuleFull {
        private String key;
        private String label;
        private String fullContent;
        private String excerpt;
        private Instant updatedAt;

        public String getKey() {
            return key;
        }

        public WorldModuleFull setKey(String key) {
            this.key = key;
            return this;
        }

        public String getLabel() {
            return label;
        }

        public WorldModuleFull setLabel(String label) {
            this.label = label;
            return this;
        }

        public String getFullContent() {
            return fullContent;
        }

        public WorldModuleFull setFullContent(String fullContent) {
            this.fullContent = fullContent;
            return this;
        }

        public String getExcerpt() {
            return excerpt;
        }

        public WorldModuleFull setExcerpt(String excerpt) {
            this.excerpt = excerpt;
            return this;
        }

        public Instant getUpdatedAt() {
            return updatedAt;
        }

        public WorldModuleFull setUpdatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
    }
}

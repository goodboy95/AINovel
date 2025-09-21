package com.example.ainovel.model.world;

import com.example.ainovel.model.world.converter.StringMapJsonConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "world_modules", uniqueConstraints = {
        @UniqueConstraint(name = "uk_world_module_key", columnNames = {"world_id", "module_key"})
})
public class WorldModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "world_id", nullable = false)
    @JsonIgnore
    private World world;

    @Column(name = "module_key", length = 32, nullable = false)
    private String moduleKey;

    @Column(columnDefinition = "JSON")
    @Convert(converter = StringMapJsonConverter.class)
    private Map<String, String> fields = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorldModuleStatus status = WorldModuleStatus.EMPTY;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Lob
    @Column(name = "full_content", columnDefinition = "LONGTEXT")
    private String fullContent;

    @Column(name = "full_content_updated_at")
    private LocalDateTime fullContentUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_edited_by")
    private Long lastEditedBy;

    @Column(name = "last_edited_at")
    private LocalDateTime lastEditedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public String getModuleKey() {
        return moduleKey;
    }

    public void setModuleKey(String moduleKey) {
        this.moduleKey = moduleKey;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    public void setFields(Map<String, String> fields) {
        this.fields = fields;
    }

    public WorldModuleStatus getStatus() {
        return status;
    }

    public void setStatus(WorldModuleStatus status) {
        this.status = status;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getFullContent() {
        return fullContent;
    }

    public void setFullContent(String fullContent) {
        this.fullContent = fullContent;
    }

    public LocalDateTime getFullContentUpdatedAt() {
        return fullContentUpdatedAt;
    }

    public void setFullContentUpdatedAt(LocalDateTime fullContentUpdatedAt) {
        this.fullContentUpdatedAt = fullContentUpdatedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getLastEditedBy() {
        return lastEditedBy;
    }

    public void setLastEditedBy(Long lastEditedBy) {
        this.lastEditedBy = lastEditedBy;
    }

    public LocalDateTime getLastEditedAt() {
        return lastEditedAt;
    }

    public void setLastEditedAt(LocalDateTime lastEditedAt) {
        this.lastEditedAt = lastEditedAt;
    }
}

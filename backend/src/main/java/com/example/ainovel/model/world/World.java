package com.example.ainovel.model.world;

import com.example.ainovel.model.User;
import com.example.ainovel.model.world.converter.StringListJsonConverter;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "worlds")
@SQLDelete(sql = "UPDATE worlds SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class World {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 180, nullable = false)
    private String tagline;

    @Column(columnDefinition = "JSON")
    @Convert(converter = StringListJsonConverter.class)
    private List<String> themes = new ArrayList<>();

    @Column(name = "creative_intent", columnDefinition = "TEXT", nullable = false)
    private String creativeIntent;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorldStatus status = WorldStatus.DRAFT;

    @Column(nullable = false)
    private Integer version = 0;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "last_edited_by")
    private Long lastEditedBy;

    @Column(name = "last_edited_at")
    private LocalDateTime lastEditedAt;

    @OneToMany(mappedBy = "world", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorldModule> modules = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public List<String> getThemes() {
        return themes;
    }

    public void setThemes(List<String> themes) {
        this.themes = themes;
    }

    public String getCreativeIntent() {
        return creativeIntent;
    }

    public void setCreativeIntent(String creativeIntent) {
        this.creativeIntent = creativeIntent;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public WorldStatus getStatus() {
        return status;
    }

    public void setStatus(WorldStatus status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
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

    public List<WorldModule> getModules() {
        return modules;
    }

    public void setModules(List<WorldModule> modules) {
        this.modules = modules;
    }
}

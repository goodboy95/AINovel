package com.example.ainovel.model.world;

import com.example.ainovel.model.User;
import com.example.ainovel.model.world.converter.StringMapJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "world_prompt_settings")
public class WorldPromptSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "module_templates", columnDefinition = "JSON")
    @Convert(converter = StringMapJsonConverter.class)
    private Map<String, String> moduleTemplates = new LinkedHashMap<>();

    @Column(name = "final_templates", columnDefinition = "JSON")
    @Convert(converter = StringMapJsonConverter.class)
    private Map<String, String> finalTemplates = new LinkedHashMap<>();

    @Lob
    @Column(name = "field_refine_template", columnDefinition = "LONGTEXT")
    private String fieldRefineTemplate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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

    public Map<String, String> getModuleTemplates() {
        return moduleTemplates;
    }

    public void setModuleTemplates(Map<String, String> moduleTemplates) {
        this.moduleTemplates = moduleTemplates;
    }

    public Map<String, String> getFinalTemplates() {
        return finalTemplates;
    }

    public void setFinalTemplates(Map<String, String> finalTemplates) {
        this.finalTemplates = finalTemplates;
    }

    public String getFieldRefineTemplate() {
        return fieldRefineTemplate;
    }

    public void setFieldRefineTemplate(String fieldRefineTemplate) {
        this.fieldRefineTemplate = fieldRefineTemplate;
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
}

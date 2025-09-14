package com.example.ainovel.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a section of a Manuscript. Each section corresponds to a single OutlineScene,
 * but is now associated to a Manuscript entity (many sections belong to one manuscript).
 */
@Data
@Entity
@Table(name = "manuscript_sections")
public class ManuscriptSection {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Belongs-to Manuscript.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manuscript_id", nullable = false)
    private Manuscript manuscript;

    /**
     * Keep a direct foreign key to the OutlineScene for convenient querying and to preserve linkage.
     */
    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    /**
     * Full text content of this section.
     */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    /**
     * Revision number.
     */
    private Integer version = 0;

    /**
     * Whether this section is the active version for the scene.
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * Creation timestamp.
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Backward-compatibility for old frontend code: expose a synthetic "scene" object with only id.
     */
    @Transient
    public OutlineScene getScene() {
        if (sceneId == null) return null;
        OutlineScene s = new OutlineScene();
        s.setId(sceneId);
        return s;
    }
}

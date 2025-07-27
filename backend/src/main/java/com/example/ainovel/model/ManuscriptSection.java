package com.example.ainovel.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

/**
 * Represents a section of the manuscript, corresponding to a single scene in the outline.
 */
@Data
@Entity
@Table(name = "manuscript_sections")
public class ManuscriptSection {

    /**
     * The unique identifier for the manuscript section.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The outline scene this manuscript section is based on.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_id", nullable = false)
    private OutlineScene scene;

    /**
     * The full text content of the manuscript section.
     */
    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    /**
     * The version number of this section, for tracking revisions.
     */
    private Integer version = 0;

    /**
     * Indicates if this is the currently active version of the section.
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * Timestamp of when the section was created.
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * A transient getter to easily access the scene ID without loading the entire scene object.
     * @return The ID of the associated scene.
     */
    @JsonIgnore
    public Long getSceneId() {
        if (this.scene != null) {
            return this.scene.getId();
        }
        return null;
    }
}

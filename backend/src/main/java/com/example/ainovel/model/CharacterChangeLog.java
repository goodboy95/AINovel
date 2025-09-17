package com.example.ainovel.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Where;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Entity that records character state changes for each manuscript section.
 */
@Data
@Entity
@Table(name = "character_change_logs", indexes = {
        @Index(name = "idx_ccl_character", columnList = "character_id,manuscript_id"),
        @Index(name = "idx_ccl_scene", columnList = "manuscript_id,scene_id,chapter_number,section_number"),
        @Index(name = "idx_ccl_turning", columnList = "manuscript_id,character_id,deleted_at")
})
@SQLDelete(sql = "UPDATE character_change_logs SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class CharacterChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private CharacterCard character;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manuscript_id", nullable = false)
    private Manuscript manuscript;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outline_id", nullable = false)
    private OutlineCard outline;

    @Column(name = "scene_id", nullable = false)
    private Long sceneId;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(name = "section_number", nullable = false)
    private Integer sectionNumber;

    @Column(name = "newly_known_info", columnDefinition = "TEXT")
    private String newlyKnownInfo;

    @Column(name = "character_changes", columnDefinition = "TEXT")
    private String characterChanges;

    @Lob
    @Column(name = "character_details_after", nullable = false, columnDefinition = "LONGTEXT")
    private String characterDetailsAfter;

    @Column(name = "is_auto_copied", nullable = false)
    private Boolean isAutoCopied = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Copy the character details from the previous log when no change occurs.
     *
     * @param previous the previous log to copy from
     */
    public CharacterChangeLog copyFrom(CharacterChangeLog previous) {
        if (previous == null) {
            throw new IllegalArgumentException("Previous log must not be null when copying");
        }
        this.characterDetailsAfter = previous.getCharacterDetailsAfter();
        this.isAutoCopied = true;
        return this;
    }

    /**
     * Build a composite key for ordering within a manuscript timeline.
     *
     * @return a string combining chapter and section numbers
     */
    public String buildTimelineKey() {
        Integer chapter = this.chapterNumber != null ? this.chapterNumber : 0;
        Integer section = this.sectionNumber != null ? this.sectionNumber : 0;
        return String.format("%d-%d", chapter, section);
    }
}


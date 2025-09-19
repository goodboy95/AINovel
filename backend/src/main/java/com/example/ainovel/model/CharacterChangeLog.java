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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Records character evolution snapshots after each manuscript section analysis.
 */
@Data
@Entity
@Table(name = "character_change_log")
@SQLDelete(sql = "UPDATE character_change_log SET deleted_at = NOW() WHERE id = ?")
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
    @JoinColumn(name = "outline_id")
    private OutlineCard outline;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    @Column(name = "section_number", nullable = false)
    private Integer sectionNumber;

    @Lob
    @Column(name = "newly_known_info")
    private String newlyKnownInfo;

    @Lob
    @Column(name = "character_changes")
    private String characterChanges;

    @Lob
    @Column(name = "character_details_after", nullable = false)
    private String characterDetailsAfter;

    @Column(name = "is_auto_copied", nullable = false)
    private Boolean isAutoCopied = Boolean.FALSE;

    @Column(name = "relationship_changes", columnDefinition = "JSON")
    private String relationshipChangesJson;

    @Column(name = "is_turning_point", nullable = false)
    private Boolean isTurningPoint = Boolean.FALSE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}

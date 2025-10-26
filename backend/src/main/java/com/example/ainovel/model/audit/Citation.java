package com.example.ainovel.model.audit;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "citation", indexes = {
    @Index(name = "idx_citation_workspace", columnList = "workspace_id"),
    @Index(name = "idx_citation_material", columnList = "material_id")
})
public class Citation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "material_id", nullable = false)
    private Long materialId;

    @Column(name = "chunk_id")
    private Long chunkId;

    @Column(name = "chunk_seq")
    private Integer chunkSeq;

    @Column(name = "usage_context", columnDefinition = "TEXT")
    private String usageContext;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}


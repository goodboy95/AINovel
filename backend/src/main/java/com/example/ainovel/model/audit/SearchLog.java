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
@Table(name = "search_log", indexes = {
    @Index(name = "idx_search_log_workspace", columnList = "workspace_id"),
    @Index(name = "idx_search_log_user", columnList = "user_id")
})
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 500)
    private String query;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "used_in_generation")
    private Boolean usedInGeneration;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}


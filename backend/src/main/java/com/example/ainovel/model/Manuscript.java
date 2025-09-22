package com.example.ainovel.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "manuscripts")
public class Manuscript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // 例如："初稿", "重修版 v1.1"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outline_card_id", nullable = false)
    private OutlineCard outlineCard; // 关联到大纲

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 关联到用户

    // 一个 Manuscript 包含多个 ManuscriptSection
    @OneToMany(mappedBy = "manuscript", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ManuscriptSection> sections;

    @Column(name = "world_id")
    private Long worldId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
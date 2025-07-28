package com.example.ainovel.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

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
import lombok.NoArgsConstructor;

@Entity
@Table(name = "temporary_characters")
@Data
@NoArgsConstructor
public class TemporaryCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_id", nullable = false)
    @JsonBackReference
    private OutlineScene scene;

    @Column(nullable = false)
    private String name;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary; // 概要 (新字段，替代原description)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String details; // 详情 (新字段)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String relationships; // 与核心人物的关系 (新字段)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String statusInScene; // 在本节中的状态 (新字段)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String moodInScene; // 在本节中的心情 (新字段)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String actionsInScene; // 在本节中的核心行动 (新字段)
}
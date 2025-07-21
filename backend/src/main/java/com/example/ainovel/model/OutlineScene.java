package com.example.ainovel.model;

import jakarta.persistence.*;

@Entity
@Table(name = "outline_scenes")
public class OutlineScene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private OutlineChapter outlineChapter;

    @Column(name = "scene_number", nullable = false)
    private Integer sceneNumber;

    @Lob
    private String synopsis;

    @Column(name = "expected_words")
    private Integer expectedWords;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OutlineChapter getOutlineChapter() {
        return outlineChapter;
    }

    public void setOutlineChapter(OutlineChapter outlineChapter) {
        this.outlineChapter = outlineChapter;
    }

    public Integer getSceneNumber() {
        return sceneNumber;
    }

    public void setSceneNumber(Integer sceneNumber) {
        this.sceneNumber = sceneNumber;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public Integer getExpectedWords() {
        return expectedWords;
    }

    public void setExpectedWords(Integer expectedWords) {
        this.expectedWords = expectedWords;
    }
}

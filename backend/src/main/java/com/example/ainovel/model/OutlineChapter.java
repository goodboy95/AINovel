package com.example.ainovel.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "outline_chapters")
public class OutlineChapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outline_card_id", nullable = false)
    private OutlineCard outlineCard;

    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    private String title;

    @Lob
    private String synopsis;

    @OneToMany(mappedBy = "outlineChapter", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sceneNumber ASC")
    private List<OutlineScene> scenes;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OutlineCard getOutlineCard() {
        return outlineCard;
    }

    public void setOutlineCard(OutlineCard outlineCard) {
        this.outlineCard = outlineCard;
    }

    public Integer getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(Integer chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public List<OutlineScene> getScenes() {
        return scenes;
    }

    public void setScenes(List<OutlineScene> scenes) {
        this.scenes = scenes;
    }
}

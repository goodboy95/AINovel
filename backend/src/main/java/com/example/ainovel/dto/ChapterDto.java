package com.example.ainovel.dto;

import java.util.List;

public class ChapterDto {
    private Long id;
    private Integer chapterNumber;
    private String title;
    private String synopsis;
    private List<SceneDto> scenes;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<SceneDto> getScenes() {
        return scenes;
    }

    public void setScenes(List<SceneDto> scenes) {
        this.scenes = scenes;
    }
}

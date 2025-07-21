package com.example.ainovel.dto;

public class SceneDto {
    private Long id;
    private Integer sceneNumber;
    private String synopsis;
    private Integer expectedWords;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

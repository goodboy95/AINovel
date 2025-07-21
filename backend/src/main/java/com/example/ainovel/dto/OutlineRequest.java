package com.example.ainovel.dto;

public class OutlineRequest {
    private Long storyCardId;
    private Integer numberOfChapters;
    private String pointOfView;

    // Getters and Setters
    public Long getStoryCardId() {
        return storyCardId;
    }

    public void setStoryCardId(Long storyCardId) {
        this.storyCardId = storyCardId;
    }

    public Integer getNumberOfChapters() {
        return numberOfChapters;
    }

    public void setNumberOfChapters(Integer numberOfChapters) {
        this.numberOfChapters = numberOfChapters;
    }

    public String getPointOfView() {
        return pointOfView;
    }

    public void setPointOfView(String pointOfView) {
        this.pointOfView = pointOfView;
    }
}

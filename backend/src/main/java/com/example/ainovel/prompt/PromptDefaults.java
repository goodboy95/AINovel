package com.example.ainovel.prompt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PromptDefaults {

    private String storyCreation;
    private String outlineChapter;
    private String manuscriptSection;
    private RefinePromptDefaults refine;

    public String getStoryCreation() {
        return storyCreation;
    }

    public void setStoryCreation(String storyCreation) {
        this.storyCreation = storyCreation;
    }

    public String getOutlineChapter() {
        return outlineChapter;
    }

    public void setOutlineChapter(String outlineChapter) {
        this.outlineChapter = outlineChapter;
    }

    public String getManuscriptSection() {
        return manuscriptSection;
    }

    public void setManuscriptSection(String manuscriptSection) {
        this.manuscriptSection = manuscriptSection;
    }

    public RefinePromptDefaults getRefine() {
        return refine;
    }

    public void setRefine(RefinePromptDefaults refine) {
        this.refine = refine;
    }

    public void validate() {
        if (storyCreation == null || storyCreation.isBlank()) {
            throw new IllegalStateException("Default story creation prompt must not be empty");
        }
        if (outlineChapter == null || outlineChapter.isBlank()) {
            throw new IllegalStateException("Default outline chapter prompt must not be empty");
        }
        if (manuscriptSection == null || manuscriptSection.isBlank()) {
            throw new IllegalStateException("Default manuscript section prompt must not be empty");
        }
        if (refine == null) {
            throw new IllegalStateException("Default refine prompts must be provided");
        }
        refine.validate();
    }
}

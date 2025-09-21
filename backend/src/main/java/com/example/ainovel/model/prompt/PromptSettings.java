package com.example.ainovel.model.prompt;

public class PromptSettings {

    private String storyCreation;
    private String outlineChapter;
    private String manuscriptSection;
    private RefinePromptSettings refine;

    public String getStoryCreation() {
        return storyCreation;
    }

    public PromptSettings setStoryCreation(String storyCreation) {
        this.storyCreation = storyCreation;
        return this;
    }

    public String getOutlineChapter() {
        return outlineChapter;
    }

    public PromptSettings setOutlineChapter(String outlineChapter) {
        this.outlineChapter = outlineChapter;
        return this;
    }

    public String getManuscriptSection() {
        return manuscriptSection;
    }

    public PromptSettings setManuscriptSection(String manuscriptSection) {
        this.manuscriptSection = manuscriptSection;
        return this;
    }

    public RefinePromptSettings getRefine() {
        return refine;
    }

    public PromptSettings setRefine(RefinePromptSettings refine) {
        this.refine = refine;
        return this;
    }

    public RefinePromptSettings ensureRefine() {
        if (this.refine == null) {
            this.refine = new RefinePromptSettings();
        }
        return this.refine;
    }
}

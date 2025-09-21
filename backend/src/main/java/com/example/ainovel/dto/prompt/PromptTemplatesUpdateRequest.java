package com.example.ainovel.dto.prompt;

public class PromptTemplatesUpdateRequest {

    private String storyCreation;
    private String outlineChapter;
    private String manuscriptSection;
    private RefinePromptTemplatesUpdateRequest refine;

    public String getStoryCreation() {
        return storyCreation;
    }

    public PromptTemplatesUpdateRequest setStoryCreation(String storyCreation) {
        this.storyCreation = storyCreation;
        return this;
    }

    public String getOutlineChapter() {
        return outlineChapter;
    }

    public PromptTemplatesUpdateRequest setOutlineChapter(String outlineChapter) {
        this.outlineChapter = outlineChapter;
        return this;
    }

    public String getManuscriptSection() {
        return manuscriptSection;
    }

    public PromptTemplatesUpdateRequest setManuscriptSection(String manuscriptSection) {
        this.manuscriptSection = manuscriptSection;
        return this;
    }

    public RefinePromptTemplatesUpdateRequest getRefine() {
        return refine;
    }

    public PromptTemplatesUpdateRequest setRefine(RefinePromptTemplatesUpdateRequest refine) {
        this.refine = refine;
        return this;
    }
}

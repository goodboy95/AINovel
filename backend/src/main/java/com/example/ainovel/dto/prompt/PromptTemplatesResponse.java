package com.example.ainovel.dto.prompt;

public class PromptTemplatesResponse {

    private PromptTemplateItemDto storyCreation;
    private PromptTemplateItemDto outlineChapter;
    private PromptTemplateItemDto manuscriptSection;
    private RefinePromptTemplateDto refine;

    public PromptTemplateItemDto getStoryCreation() {
        return storyCreation;
    }

    public PromptTemplatesResponse setStoryCreation(PromptTemplateItemDto storyCreation) {
        this.storyCreation = storyCreation;
        return this;
    }

    public PromptTemplateItemDto getOutlineChapter() {
        return outlineChapter;
    }

    public PromptTemplatesResponse setOutlineChapter(PromptTemplateItemDto outlineChapter) {
        this.outlineChapter = outlineChapter;
        return this;
    }

    public PromptTemplateItemDto getManuscriptSection() {
        return manuscriptSection;
    }

    public PromptTemplatesResponse setManuscriptSection(PromptTemplateItemDto manuscriptSection) {
        this.manuscriptSection = manuscriptSection;
        return this;
    }

    public RefinePromptTemplateDto getRefine() {
        return refine;
    }

    public PromptTemplatesResponse setRefine(RefinePromptTemplateDto refine) {
        this.refine = refine;
        return this;
    }
}

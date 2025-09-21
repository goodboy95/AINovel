package com.example.ainovel.dto.world;

import java.util.List;

public class WorldFieldRefineTemplateDto {

    private String description;
    private String template;
    private List<String> usageNotes;

    public String getDescription() {
        return description;
    }

    public WorldFieldRefineTemplateDto setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getTemplate() {
        return template;
    }

    public WorldFieldRefineTemplateDto setTemplate(String template) {
        this.template = template;
        return this;
    }

    public List<String> getUsageNotes() {
        return usageNotes;
    }

    public WorldFieldRefineTemplateDto setUsageNotes(List<String> usageNotes) {
        this.usageNotes = usageNotes;
        return this;
    }
}

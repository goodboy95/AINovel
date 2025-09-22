package com.example.ainovel.service.world;

import java.util.List;

public class WorkspaceWorldContext {

    private final Long id;
    private final String name;
    private final String tagline;
    private final List<String> themes;
    private final String creativeIntent;
    private final WorkspaceWorldModulesView modules;

    public WorkspaceWorldContext(Long id,
                                 String name,
                                 String tagline,
                                 List<String> themes,
                                 String creativeIntent,
                                 WorkspaceWorldModulesView modules) {
        this.id = id;
        this.name = name;
        this.tagline = tagline;
        this.themes = themes;
        this.creativeIntent = creativeIntent;
        this.modules = modules;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTagline() {
        return tagline;
    }

    public List<String> getThemes() {
        return themes;
    }

    public String getCreativeIntent() {
        return creativeIntent;
    }

    public WorkspaceWorldModulesView getModules() {
        return modules;
    }
}

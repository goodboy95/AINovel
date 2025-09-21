package com.example.ainovel.dto.world;

import java.util.List;

public class WorldUpsertRequest {

    private String name;
    private String tagline;
    private List<String> themes;
    private String creativeIntent;
    private String notes;

    public String getName() {
        return name;
    }

    public WorldUpsertRequest setName(String name) {
        this.name = name;
        return this;
    }

    public String getTagline() {
        return tagline;
    }

    public WorldUpsertRequest setTagline(String tagline) {
        this.tagline = tagline;
        return this;
    }

    public List<String> getThemes() {
        return themes;
    }

    public WorldUpsertRequest setThemes(List<String> themes) {
        this.themes = themes;
        return this;
    }

    public String getCreativeIntent() {
        return creativeIntent;
    }

    public WorldUpsertRequest setCreativeIntent(String creativeIntent) {
        this.creativeIntent = creativeIntent;
        return this;
    }

    public String getNotes() {
        return notes;
    }

    public WorldUpsertRequest setNotes(String notes) {
        this.notes = notes;
        return this;
    }
}

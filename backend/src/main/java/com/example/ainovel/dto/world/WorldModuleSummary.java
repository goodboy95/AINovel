package com.example.ainovel.dto.world;

public class WorldModuleSummary {

    private String key;
    private String label;

    public WorldModuleSummary() {
    }

    public WorldModuleSummary(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() {
        return key;
    }

    public WorldModuleSummary setKey(String key) {
        this.key = key;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public WorldModuleSummary setLabel(String label) {
        this.label = label;
        return this;
    }
}

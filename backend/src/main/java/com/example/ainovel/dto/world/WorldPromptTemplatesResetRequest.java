package com.example.ainovel.dto.world;

import java.util.List;

public class WorldPromptTemplatesResetRequest {

    private List<String> keys;

    public List<String> getKeys() {
        return keys;
    }

    public WorldPromptTemplatesResetRequest setKeys(List<String> keys) {
        this.keys = keys;
        return this;
    }
}

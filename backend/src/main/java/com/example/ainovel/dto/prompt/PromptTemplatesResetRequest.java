package com.example.ainovel.dto.prompt;

import java.util.List;

public class PromptTemplatesResetRequest {

    private List<String> keys;

    public List<String> getKeys() {
        return keys;
    }

    public PromptTemplatesResetRequest setKeys(List<String> keys) {
        this.keys = keys;
        return this;
    }
}

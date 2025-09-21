package com.example.ainovel.dto.world;

import java.util.Map;

public class WorldModuleUpdateRequest {

    private Map<String, String> fields;

    public Map<String, String> getFields() {
        return fields;
    }

    public WorldModuleUpdateRequest setFields(Map<String, String> fields) {
        this.fields = fields;
        return this;
    }
}

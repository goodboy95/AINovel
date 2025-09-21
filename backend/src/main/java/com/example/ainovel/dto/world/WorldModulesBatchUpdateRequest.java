package com.example.ainovel.dto.world;

import java.util.List;
import java.util.Map;

public class WorldModulesBatchUpdateRequest {

    private List<ModuleUpdate> modules;

    public List<ModuleUpdate> getModules() {
        return modules;
    }

    public WorldModulesBatchUpdateRequest setModules(List<ModuleUpdate> modules) {
        this.modules = modules;
        return this;
    }

    public static class ModuleUpdate {
        private String key;
        private Map<String, String> fields;

        public String getKey() {
            return key;
        }

        public ModuleUpdate setKey(String key) {
            this.key = key;
            return this;
        }

        public Map<String, String> getFields() {
            return fields;
        }

        public ModuleUpdate setFields(Map<String, String> fields) {
            this.fields = fields;
            return this;
        }
    }
}

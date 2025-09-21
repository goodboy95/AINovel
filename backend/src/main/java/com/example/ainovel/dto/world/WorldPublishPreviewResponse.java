package com.example.ainovel.dto.world;

import com.example.ainovel.model.world.WorldModuleStatus;

import java.util.List;
import java.util.Map;

public class WorldPublishPreviewResponse {

    private boolean ready;
    private Map<String, WorldModuleStatus> moduleStatuses;
    private List<MissingField> missingFields;
    private List<WorldModuleSummary> modulesToGenerate;
    private List<WorldModuleSummary> modulesToReuse;

    public boolean isReady() {
        return ready;
    }

    public WorldPublishPreviewResponse setReady(boolean ready) {
        this.ready = ready;
        return this;
    }

    public Map<String, WorldModuleStatus> getModuleStatuses() {
        return moduleStatuses;
    }

    public WorldPublishPreviewResponse setModuleStatuses(Map<String, WorldModuleStatus> moduleStatuses) {
        this.moduleStatuses = moduleStatuses;
        return this;
    }

    public List<MissingField> getMissingFields() {
        return missingFields;
    }

    public WorldPublishPreviewResponse setMissingFields(List<MissingField> missingFields) {
        this.missingFields = missingFields;
        return this;
    }

    public List<WorldModuleSummary> getModulesToGenerate() {
        return modulesToGenerate;
    }

    public WorldPublishPreviewResponse setModulesToGenerate(List<WorldModuleSummary> modulesToGenerate) {
        this.modulesToGenerate = modulesToGenerate;
        return this;
    }

    public List<WorldModuleSummary> getModulesToReuse() {
        return modulesToReuse;
    }

    public WorldPublishPreviewResponse setModulesToReuse(List<WorldModuleSummary> modulesToReuse) {
        this.modulesToReuse = modulesToReuse;
        return this;
    }

    public static class MissingField {
        private String moduleKey;
        private String moduleLabel;
        private String fieldKey;
        private String fieldLabel;

        public String getModuleKey() {
            return moduleKey;
        }

        public MissingField setModuleKey(String moduleKey) {
            this.moduleKey = moduleKey;
            return this;
        }

        public String getModuleLabel() {
            return moduleLabel;
        }

        public MissingField setModuleLabel(String moduleLabel) {
            this.moduleLabel = moduleLabel;
            return this;
        }

        public String getFieldKey() {
            return fieldKey;
        }

        public MissingField setFieldKey(String fieldKey) {
            this.fieldKey = fieldKey;
            return this;
        }

        public String getFieldLabel() {
            return fieldLabel;
        }

        public MissingField setFieldLabel(String fieldLabel) {
            this.fieldLabel = fieldLabel;
            return this;
        }
    }
}

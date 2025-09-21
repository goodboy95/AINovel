package com.example.ainovel.dto.world;

import java.util.List;

public class WorldPublishResponse {

    private Long worldId;
    private List<WorldModuleSummary> modulesToGenerate;
    private List<WorldModuleSummary> modulesToReuse;

    public Long getWorldId() {
        return worldId;
    }

    public WorldPublishResponse setWorldId(Long worldId) {
        this.worldId = worldId;
        return this;
    }

    public List<WorldModuleSummary> getModulesToGenerate() {
        return modulesToGenerate;
    }

    public WorldPublishResponse setModulesToGenerate(List<WorldModuleSummary> modulesToGenerate) {
        this.modulesToGenerate = modulesToGenerate;
        return this;
    }

    public List<WorldModuleSummary> getModulesToReuse() {
        return modulesToReuse;
    }

    public WorldPublishResponse setModulesToReuse(List<WorldModuleSummary> modulesToReuse) {
        this.modulesToReuse = modulesToReuse;
        return this;
    }
}

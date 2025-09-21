package com.example.ainovel.dto.world;

import com.example.ainovel.model.world.WorldGenerationJobStatus;
import com.example.ainovel.model.world.WorldStatus;

import java.time.LocalDateTime;
import java.util.List;

public class WorldGenerationStatusResponse {

    private Long worldId;
    private WorldStatus status;
    private Integer version;
    private List<JobStatus> queue;

    public Long getWorldId() {
        return worldId;
    }

    public WorldGenerationStatusResponse setWorldId(Long worldId) {
        this.worldId = worldId;
        return this;
    }

    public WorldStatus getStatus() {
        return status;
    }

    public WorldGenerationStatusResponse setStatus(WorldStatus status) {
        this.status = status;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public WorldGenerationStatusResponse setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public List<JobStatus> getQueue() {
        return queue;
    }

    public WorldGenerationStatusResponse setQueue(List<JobStatus> queue) {
        this.queue = queue;
        return this;
    }

    public static class JobStatus {
        private String moduleKey;
        private String moduleLabel;
        private WorldGenerationJobStatus status;
        private Integer attempts;
        private LocalDateTime startedAt;
        private LocalDateTime finishedAt;
        private String error;

        public String getModuleKey() {
            return moduleKey;
        }

        public JobStatus setModuleKey(String moduleKey) {
            this.moduleKey = moduleKey;
            return this;
        }

        public String getModuleLabel() {
            return moduleLabel;
        }

        public JobStatus setModuleLabel(String moduleLabel) {
            this.moduleLabel = moduleLabel;
            return this;
        }

        public WorldGenerationJobStatus getStatus() {
            return status;
        }

        public JobStatus setStatus(WorldGenerationJobStatus status) {
            this.status = status;
            return this;
        }

        public Integer getAttempts() {
            return attempts;
        }

        public JobStatus setAttempts(Integer attempts) {
            this.attempts = attempts;
            return this;
        }

        public LocalDateTime getStartedAt() {
            return startedAt;
        }

        public JobStatus setStartedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public LocalDateTime getFinishedAt() {
            return finishedAt;
        }

        public JobStatus setFinishedAt(LocalDateTime finishedAt) {
            this.finishedAt = finishedAt;
            return this;
        }

        public String getError() {
            return error;
        }

        public JobStatus setError(String error) {
            this.error = error;
            return this;
        }
    }
}

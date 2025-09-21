package com.example.ainovel.model.world;

public enum WorldModuleStatus {
    EMPTY,
    IN_PROGRESS,
    READY,
    AWAITING_GENERATION,
    GENERATING,
    COMPLETED,
    FAILED;

    public boolean isReadyForPublish() {
        return this == READY || this == AWAITING_GENERATION || this == COMPLETED;
    }
}

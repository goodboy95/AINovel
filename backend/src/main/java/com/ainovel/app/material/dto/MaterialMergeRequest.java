package com.ainovel.app.material.dto;

import java.util.UUID;

public record MaterialMergeRequest(UUID sourceMaterialId,
                                   UUID targetMaterialId,
                                   Boolean mergeTags,
                                   Boolean mergeSummaryWhenEmpty,
                                   String note) {}

package com.example.ainovel.dto.material;

import lombok.Data;

@Data
public class MaterialMergeRequest {

    private Long sourceMaterialId;
    private Long targetMaterialId;
    private boolean mergeTags = true;
    private boolean mergeSummaryWhenEmpty = true;
    private String note;
}


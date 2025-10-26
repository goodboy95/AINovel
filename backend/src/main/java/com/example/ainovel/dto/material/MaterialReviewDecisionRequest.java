package com.example.ainovel.dto.material;

import lombok.Data;

/**
 * 人审工作台审批/驳回请求。
 */
@Data
public class MaterialReviewDecisionRequest {

    private String title;
    private String summary;
    private String tags;
    private String entitiesJson;
    private String type;
    private String reviewNotes;
}


package com.example.ainovel.dto.material;

import lombok.Data;

/**
 * 素材更新请求。
 */
@Data
public class MaterialUpdateRequest {
    private String title;
    private String type;
    private String summary;
    private String tags;
    private String content;
    private String status;
    private String entitiesJson;
}


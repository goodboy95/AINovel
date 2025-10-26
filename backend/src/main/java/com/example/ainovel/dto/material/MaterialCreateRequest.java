package com.example.ainovel.dto.material;

import lombok.Data;

/**
 * 手动创建素材的请求体。
 */
@Data
public class MaterialCreateRequest {
    private String title;
    private String type;
    private String summary;
    private String content;
    private String tags;
}


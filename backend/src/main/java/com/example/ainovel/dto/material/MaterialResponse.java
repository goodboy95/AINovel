package com.example.ainovel.dto.material;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 素材创建或查询后的响应对象。
 */
@Data
public class MaterialResponse {
    private Long id;
    private Long workspaceId;
    private String title;
    private String type;
    private String summary;
    private String tags;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


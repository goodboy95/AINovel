package com.example.ainovel.dto.material;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 待审核素材列表项。
 */
@Data
public class MaterialReviewItem {

    private Long id;
    private Long workspaceId;
    private String title;
    private String type;
    private String summary;
    private String tags;
    private String content;
    private String entitiesJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String reviewNotes;
}


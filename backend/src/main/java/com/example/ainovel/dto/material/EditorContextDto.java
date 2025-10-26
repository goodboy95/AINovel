package com.example.ainovel.dto.material;

import lombok.Data;

/**
 * 编辑器上下文，用于自动素材建议。
 */
@Data
public class EditorContextDto {

    private String text;
    private Long workspaceId;
    private Integer limit;
}


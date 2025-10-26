package com.example.ainovel.dto.material;

import lombok.Data;

/**
 * 素材检索返回结果。
 */
@Data
public class MaterialSearchResult {
    private Long materialId;
    private String title;
    private Integer chunkSeq;
    private String snippet;
    private Double score;
}


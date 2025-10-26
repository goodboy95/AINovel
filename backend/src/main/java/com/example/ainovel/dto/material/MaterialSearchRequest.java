package com.example.ainovel.dto.material;

import lombok.Data;

/**
 * 素材检索请求体。
 */
@Data
public class MaterialSearchRequest {
    private String query;
    private Integer limit;
}


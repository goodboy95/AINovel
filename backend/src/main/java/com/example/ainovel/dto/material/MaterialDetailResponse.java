package com.example.ainovel.dto.material;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 素材详情响应，包含正文内容。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MaterialDetailResponse extends MaterialResponse {
    private String content;
}


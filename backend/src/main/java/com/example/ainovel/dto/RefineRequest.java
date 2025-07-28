package com.example.ainovel.dto;

import lombok.Data;

@Data
public class RefineRequest {
    private String text; // 待优化的原文
    private String instruction; // 优化方向
    private String contextType; // (新字段) 文本类型，如 "角色介绍", "大纲梗概"
}

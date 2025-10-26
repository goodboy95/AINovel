package com.example.ainovel.dto.material;

import java.time.LocalDateTime;

import com.example.ainovel.model.material.FileImportStatus;

import lombok.Data;

/**
 * 文件导入作业状态响应体。
 */
@Data
public class FileImportJobResponse {
    private Long id;
    private Long workspaceId;
    private Long uploaderId;
    private String fileName;
    private FileImportStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}


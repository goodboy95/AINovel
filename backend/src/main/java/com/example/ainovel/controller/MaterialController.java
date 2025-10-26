package com.example.ainovel.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.ainovel.dto.material.FileImportJobResponse;
import com.example.ainovel.dto.material.MaterialCreateRequest;
import com.example.ainovel.dto.material.MaterialResponse;
import com.example.ainovel.dto.material.MaterialSearchRequest;
import com.example.ainovel.dto.material.MaterialSearchResult;
import com.example.ainovel.model.User;
import com.example.ainovel.service.material.FileImportService;
import com.example.ainovel.service.material.MaterialService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final FileImportService fileImportService;

    @PostMapping
    public ResponseEntity<?> createMaterial(@RequestBody MaterialCreateRequest request,
                                            @AuthenticationPrincipal User user) {
        try {
            Long workspaceId = user.getId();
            MaterialResponse response = materialService.createMaterial(request, user.getId(), workspaceId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchMaterials(@RequestBody MaterialSearchRequest request,
                                             @AuthenticationPrincipal User user) {
        try {
            Long workspaceId = user.getId();
            int limit = request.getLimit() != null && request.getLimit() > 0 ? Math.min(request.getLimit(), 20) : 10;
            return ResponseEntity.ok(materialService.searchMaterials(workspaceId, request.getQuery(), limit));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMaterial(@RequestParam("file") MultipartFile file,
                                            @AuthenticationPrincipal User user) {
        try {
            Long workspaceId = user.getId();
            FileImportJobResponse job = fileImportService.startFileImportJob(workspaceId, user.getId(), file);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(job);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "文件上传失败", "detail", ex.getMessage()));
        }
    }

    @GetMapping("/upload/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable Long jobId,
                                          @AuthenticationPrincipal User user) {
        try {
            Long workspaceId = user.getId();
            FileImportJobResponse job = fileImportService.getJobStatus(jobId, workspaceId);
            return ResponseEntity.ok(job);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
        }
    }
}


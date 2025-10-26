package com.example.ainovel.controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.ainovel.dto.material.EditorContextDto;
import com.example.ainovel.dto.material.FileImportJobResponse;
import com.example.ainovel.dto.material.MaterialCitationDto;
import com.example.ainovel.dto.material.MaterialCreateRequest;
import com.example.ainovel.dto.material.MaterialDuplicateCandidate;
import com.example.ainovel.dto.material.MaterialMergeRequest;
import com.example.ainovel.dto.material.MaterialResponse;
import com.example.ainovel.dto.material.MaterialReviewDecisionRequest;
import com.example.ainovel.dto.material.MaterialReviewItem;
import com.example.ainovel.dto.material.MaterialSearchRequest;
import com.example.ainovel.dto.material.MaterialSearchResult;
import com.example.ainovel.model.User;
import com.example.ainovel.service.material.DeduplicationService;
import com.example.ainovel.service.material.FileImportService;
import com.example.ainovel.service.material.MaterialAuditQueryService;
import com.example.ainovel.service.material.MaterialService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final FileImportService fileImportService;
    private final DeduplicationService deduplicationService;
    private final MaterialAuditQueryService materialAuditQueryService;

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

    @GetMapping
    public ResponseEntity<List<MaterialResponse>> listMaterials(@AuthenticationPrincipal User user) {
        Long workspaceId = user.getId();
        return ResponseEntity.ok(materialService.listMaterials(workspaceId));
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchMaterials(@RequestBody MaterialSearchRequest request,
                                             @AuthenticationPrincipal User user) {
        try {
            Long workspaceId = user.getId();
            int limit = request.getLimit() != null && request.getLimit() > 0 ? Math.min(request.getLimit(), 20) : 10;
            return ResponseEntity.ok(materialService.searchMaterials(workspaceId, user.getId(), request.getQuery(), limit));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/find-duplicates")
    public ResponseEntity<List<MaterialDuplicateCandidate>> findDuplicates(@AuthenticationPrincipal User user) {
        Long workspaceId = user.getId();
        return ResponseEntity.ok(deduplicationService.findDuplicates(workspaceId));
    }

    @PostMapping("/merge")
    public ResponseEntity<?> mergeMaterials(@RequestBody MaterialMergeRequest request,
                                            @AuthenticationPrincipal User user) {
        try {
            Long workspaceId = user.getId();
            MaterialResponse response = deduplicationService.mergeMaterials(workspaceId, user.getId(), request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException | AccessDeniedException ex) {
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

    @GetMapping("/review/pending")
    public ResponseEntity<?> listPendingReview(@AuthenticationPrincipal User user) {
        Long workspaceId = user.getId();
        return ResponseEntity.ok(materialService.listPendingReview(workspaceId));
    }

    @PostMapping("/{materialId}/review/approve")
    public ResponseEntity<?> approveMaterial(@PathVariable Long materialId,
                                             @RequestBody(required = false) MaterialReviewDecisionRequest request,
                                             @AuthenticationPrincipal User user) {
        try {
            Long workspaceId = user.getId();
            MaterialReviewItem item = materialService.approveMaterial(materialId, workspaceId, user.getId(), request);
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/{materialId}/review/reject")
    public ResponseEntity<?> rejectMaterial(@PathVariable Long materialId,
                                            @RequestBody(required = false) MaterialReviewDecisionRequest request,
                                            @AuthenticationPrincipal User user) {
        try {
            Long workspaceId = user.getId();
            MaterialReviewItem item = materialService.rejectMaterial(materialId, workspaceId, user.getId(), request);
            return ResponseEntity.ok(item);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/editor/auto-hints")
    public ResponseEntity<?> getAutoHints(@RequestBody EditorContextDto context,
                                          @AuthenticationPrincipal User user) {
        if (context == null || !StringUtils.hasText(context.getText())) {
            return ResponseEntity.ok(List.of());
        }
        Long workspaceId = context.getWorkspaceId() != null ? context.getWorkspaceId() : user.getId();
        int limit = context.getLimit() != null && context.getLimit() > 0 ? Math.min(context.getLimit(), 15) : 6;
        String query = normalizeEditorContext(context.getText());
        if (!StringUtils.hasText(query)) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(materialService.searchMaterials(workspaceId, user.getId(), query, limit));
    }

    @GetMapping("/{materialId}/citations")
    public ResponseEntity<?> listCitations(@PathVariable Long materialId,
                                           @AuthenticationPrincipal User user) {
        try {
            Long workspaceId = user.getId();
            List<MaterialCitationDto> citations = materialAuditQueryService.listCitations(workspaceId, materialId);
            return ResponseEntity.ok(citations);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    private String normalizeEditorContext(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        int maxLength = 500;
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(trimmed.length() - maxLength);
    }
}

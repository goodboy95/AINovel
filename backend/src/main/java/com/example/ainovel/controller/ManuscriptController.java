package com.example.ainovel.controller;

import com.example.ainovel.dto.AnalyzeCharacterChangesRequest;
import com.example.ainovel.dto.CharacterChangeLogResponse;
import com.example.ainovel.dto.ManuscriptDto;
import com.example.ainovel.dto.ManuscriptWithSectionsDto;
import com.example.ainovel.dto.UpdateSectionRequest;
import com.example.ainovel.model.ManuscriptSection;
import com.example.ainovel.model.User;
import com.example.ainovel.service.ManuscriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for handling manuscript-related operations.
 * Backward-compatible routes are kept under /manuscript/... while new routes follow the design doc.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ManuscriptController {

    private final ManuscriptService manuscriptService;

    // ----------------- Backward-compatible endpoints -----------------

    /**
     * Retrieves the entire manuscript map (active section per scene) for a given outline.
     * Backward-compatible path: /api/v1/manuscript/outlines/{outlineId}
     */
    @GetMapping("/manuscript/outlines/{outlineId}")
    public ResponseEntity<Map<Long, ManuscriptSection>> getManuscriptForOutline(@PathVariable Long outlineId, @AuthenticationPrincipal User user) {
        Map<Long, ManuscriptSection> manuscript = manuscriptService.getManuscriptForOutline(outlineId, user.getId());
        return ResponseEntity.ok(manuscript);
    }

    /**
     * Generates content for a specific scene into the latest (or default) manuscript of the outline.
     * Backward-compatible path: /api/v1/manuscript/scenes/{sceneId}/generate
     */
    @PostMapping("/manuscript/scenes/{sceneId}/generate")
    public ResponseEntity<ManuscriptSection> generateScene(@PathVariable Long sceneId, @AuthenticationPrincipal User user) {
        ManuscriptSection section = manuscriptService.generateSceneContent(sceneId, user.getId());
        return ResponseEntity.ok(section);
    }

    /**
     * Updates the content of a specific manuscript section.
     * Backward-compatible path: /api/v1/manuscript/sections/{sectionId}
     */
    @PutMapping("/manuscript/sections/{sectionId}")
    public ResponseEntity<ManuscriptSection> updateSection(@PathVariable Long sectionId, @RequestBody UpdateSectionRequest request, @AuthenticationPrincipal User user) {
        ManuscriptSection updatedSection = manuscriptService.updateSectionContent(sectionId, request.getContent(), user.getId());
        return ResponseEntity.ok(updatedSection);
    }

    // ----------------- New endpoints per design -----------------

    /**
     * List manuscripts under an outline.
     * GET /api/v1/outlines/{outlineId}/manuscripts
     */
    @GetMapping("/outlines/{outlineId}/manuscripts")
    public ResponseEntity<List<ManuscriptDto>> listManuscriptsForOutline(@PathVariable Long outlineId, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(manuscriptService.getManuscriptsForOutline(outlineId, user.getId()));
    }

    /**
     * Create a new manuscript under an outline.
     * POST /api/v1/outlines/{outlineId}/manuscripts
     */
    @PostMapping("/outlines/{outlineId}/manuscripts")
    public ResponseEntity<ManuscriptDto> createManuscript(@PathVariable Long outlineId,
                                                          @RequestBody ManuscriptDto request,
                                                          @AuthenticationPrincipal User user) {
        ManuscriptDto created = manuscriptService.createManuscript(outlineId, request, user.getId());
        return ResponseEntity.ok(created);
    }

    /**
     * Get a single manuscript with its active sections map keyed by sceneId.
     * GET /api/v1/manuscripts/{manuscriptId}
     */
    @GetMapping("/manuscripts/{manuscriptId}")
    public ResponseEntity<ManuscriptWithSectionsDto> getManuscriptWithSections(@PathVariable Long manuscriptId,
                                                                               @AuthenticationPrincipal User user) {
        ManuscriptWithSectionsDto dto = manuscriptService.getManuscriptWithSections(manuscriptId, user.getId());
        return ResponseEntity.ok(dto);
    }

    /**
     * Delete a manuscript and cascade delete its sections.
     * DELETE /api/v1/manuscripts/{manuscriptId}
     */
    @DeleteMapping("/manuscripts/{manuscriptId}")
    public ResponseEntity<Void> deleteManuscript(@PathVariable Long manuscriptId, @AuthenticationPrincipal User user) {
        manuscriptService.deleteManuscript(manuscriptId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/manuscripts/{manuscriptId}/sections/analyze-character-changes")
    public ResponseEntity<List<CharacterChangeLogResponse>> analyzeCharacterChanges(
            @PathVariable Long manuscriptId,
            @RequestBody AnalyzeCharacterChangesRequest request,
            @AuthenticationPrincipal User user) {
        List<CharacterChangeLogResponse> responses = manuscriptService
                .analyzeCharacterChanges(manuscriptId, request, user.getId())
                .stream()
                .map(CharacterChangeLogResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/manuscripts/{manuscriptId}/sections/{sceneId}/character-change-logs")
    public ResponseEntity<List<CharacterChangeLogResponse>> getCharacterChangeLogs(
            @PathVariable Long manuscriptId,
            @PathVariable Long sceneId,
            @AuthenticationPrincipal User user) {
        List<CharacterChangeLogResponse> responses = manuscriptService
                .getCharacterChangeLogs(manuscriptId, sceneId, user.getId())
                .stream()
                .map(CharacterChangeLogResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}

package com.example.ainovel.controller;

import com.example.ainovel.dto.UpdateSectionRequest;
import com.example.ainovel.model.ManuscriptSection;
import com.example.ainovel.model.User;
import com.example.ainovel.service.ManuscriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for handling manuscript-related operations.
 */
@RestController
@RequestMapping("/api/v1/manuscript")
@RequiredArgsConstructor
public class ManuscriptController {

    private final ManuscriptService manuscriptService;

    /**
     * Retrieves the entire manuscript for a given outline.
     * @param outlineId The ID of the outline.
     * @param user The authenticated user.
     * @return A map of scene IDs to their corresponding manuscript sections.
     */
    @GetMapping("/outlines/{outlineId}")
    public ResponseEntity<Map<Long, ManuscriptSection>> getManuscriptForOutline(@PathVariable Long outlineId, @AuthenticationPrincipal User user) {
        Map<Long, ManuscriptSection> manuscript = manuscriptService.getManuscriptForOutline(outlineId, user.getId());
        return ResponseEntity.ok(manuscript);
    }

    /**
     * Generates content for a specific scene.
     * @param sceneId The ID of the scene to generate content for.
     * @param user The authenticated user.
     * @return The generated manuscript section.
     */
    @PostMapping("/scenes/{sceneId}/generate")
    public ResponseEntity<ManuscriptSection> generateScene(@PathVariable Long sceneId, @AuthenticationPrincipal User user) {
        ManuscriptSection section = manuscriptService.generateSceneContent(sceneId, user.getId());
        return ResponseEntity.ok(section);
    }

    /**
     * Updates the content of a specific manuscript section.
     * @param sectionId The ID of the section to update.
     * @param request The request object containing the new content.
     * @param user The authenticated user.
     * @return The updated manuscript section.
     */
    @PutMapping("/sections/{sectionId}")
    public ResponseEntity<ManuscriptSection> updateSection(@PathVariable Long sectionId, @RequestBody UpdateSectionRequest request, @AuthenticationPrincipal User user) {
        ManuscriptSection updatedSection = manuscriptService.updateSectionContent(sectionId, request.getContent(), user.getId());
        return ResponseEntity.ok(updatedSection);
    }
}

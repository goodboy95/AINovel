package com.example.ainovel.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ainovel.dto.ChapterDto;
import com.example.ainovel.dto.GenerateChapterRequest;
import com.example.ainovel.dto.OutlineDto;
import com.example.ainovel.dto.SceneDto;
import com.example.ainovel.dto.RefineRequest;
import com.example.ainovel.dto.RefineResponse;
import com.example.ainovel.model.User;
import com.example.ainovel.service.OutlineService;

/**
 * Controller for handling outline-related operations.
 * Provides endpoints for creating, retrieving, updating, and deleting story outlines.
 */
@RestController
@RequestMapping("/api/v1")
public class OutlineController {

    private final OutlineService outlineService;

    /**
     * Constructs an OutlineController with the necessary OutlineService.
     * @param outlineService The service for handling outline business logic.
     */
    public OutlineController(OutlineService outlineService) {
        this.outlineService = outlineService;
    }

    /**
     * Creates a new outline based on the provided request.
     * @param outlineRequest The request object containing details for the new outline.
     * @param user The authenticated user performing the action.
     * @return A ResponseEntity containing the created OutlineDto.
     */
    /**
     * [DEPRECATED in V2] This endpoint is no longer used for generating full outlines.
     * Use POST /outlines/{outlineId}/chapters instead.
     */
    @Deprecated
    @PostMapping("/outlines")
    public ResponseEntity<OutlineDto> createOutline() {
        return ResponseEntity.status(410).build(); // 410 Gone
    }

    /**
     * Generates a single chapter for a given outline.
     * @param outlineId The ID of the outline to add the chapter to.
     * @param request The request object containing chapter generation parameters.
     * @param user The authenticated user.
     * @return A ResponseEntity containing the newly created ChapterDto.
     */
    @PostMapping("/outlines/{outlineId}/chapters")
    public ResponseEntity<ChapterDto> generateChapterForOutline(
            @PathVariable Long outlineId,
            @RequestBody GenerateChapterRequest request) {
        ChapterDto newChapter = outlineService.generateChapterOutline(outlineId, request);
        return ResponseEntity.ok(newChapter);
    }

    /**
     * Retrieves a specific outline by its ID.
     * @param id The ID of the outline to retrieve.
     * @param user The authenticated user performing the action.
     * @return A ResponseEntity containing the requested OutlineDto.
     */
    @GetMapping("/outlines/{id}")
    public ResponseEntity<OutlineDto> getOutlineById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        OutlineDto outline = outlineService.getOutlineById(id, user.getId());
        return ResponseEntity.ok(outline);
    }

    /**
     * Retrieves all outlines associated with a specific story card.
     * @param storyCardId The ID of the story card.
     * @param user The authenticated user performing the action.
     * @return A ResponseEntity containing a list of OutlineDto objects.
     */
    @GetMapping("/story-cards/{storyCardId}/outlines")
    public ResponseEntity<List<OutlineDto>> getOutlinesByStoryCardId(@PathVariable Long storyCardId, @AuthenticationPrincipal User user) {
        List<OutlineDto> outlines = outlineService.getOutlinesByStoryCardId(storyCardId, user.getId());
        return ResponseEntity.ok(outlines);
    }

    /**
     * Creates a new, empty outline for a specific story card.
     * @param storyCardId The ID of the story card for which to create the outline.
     * @param user The authenticated user performing the action.
     * @return A ResponseEntity containing the newly created OutlineDto.
     */
    @PostMapping("/story-cards/{storyCardId}/outlines")
    public ResponseEntity<OutlineDto> createEmptyOutlineForStory(
            @PathVariable Long storyCardId,
            @AuthenticationPrincipal User user) {
        OutlineDto newOutline = outlineService.createEmptyOutline(storyCardId, user.getId());
        return ResponseEntity.ok(newOutline);
    }

    /**
     * Updates an existing outline.
     * @param id The ID of the outline to update.
     * @param outlineDto The DTO containing the updated outline data.
     * @param user The authenticated user performing the action.
     * @return A ResponseEntity containing the updated OutlineDto.
     */
    @PutMapping("/outlines/{id}")
    public ResponseEntity<OutlineDto> updateOutline(@PathVariable Long id, @RequestBody OutlineDto outlineDto, @AuthenticationPrincipal User user) {
        OutlineDto updatedOutline = outlineService.updateOutline(id, outlineDto, user.getId());
        return ResponseEntity.ok(updatedOutline);
    }

    /**
     * Deletes an outline by its ID.
     * @param id The ID of the outline to delete.
     * @param user The authenticated user performing the action.
     * @return A ResponseEntity with no content.
     */
    @DeleteMapping("/outlines/{id}")
    public ResponseEntity<Void> deleteOutline(@PathVariable Long id, @AuthenticationPrincipal User user) {
        outlineService.deleteOutline(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Refines the synopsis of a specific scene within an outline.
     * Note: The endpoint path "/outlines/scenes/{id}/refine" is unconventional.
     * A more RESTful approach might be a PATCH request to the scene resource itself.
     * However, to avoid breaking changes, the existing endpoint is maintained.
     * @param id The ID of the scene to refine.
     * @param request The request object containing the refinement instructions.
     * @param user The authenticated user performing the action.
     * @return A ResponseEntity containing the refined scene information.
     */
    @PostMapping("/outlines/scenes/{id}/refine")
    public ResponseEntity<RefineResponse> refineScene(
            @PathVariable Long id, @RequestBody RefineRequest request, @AuthenticationPrincipal User user) {
        RefineResponse response = outlineService.refineSceneSynopsis(id, request, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Provides a generic text refinement service.
     * @param request The request object containing the text to refine and instructions.
     * @param user The authenticated user performing the action.
     * @return A ResponseEntity containing the refined text.
     */
    @PostMapping("/ai/refine-text")
    public ResponseEntity<RefineResponse> refineGenericText(
            @RequestBody RefineRequest request, @AuthenticationPrincipal User user) {
        RefineResponse response = outlineService.refineGenericText(request, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Partially updates a Chapter node (title, synopsis, settings, etc.).
     * PATCH is used to allow partial updates from the "true" edit mode.
     */
    @PatchMapping("/chapters/{id}")
    public ResponseEntity<ChapterDto> patchChapter(
            @PathVariable Long id,
            @RequestBody ChapterDto chapterDto,
            @AuthenticationPrincipal User user) {
        ChapterDto updated = outlineService.updateChapter(id, chapterDto, user.getId());
        return ResponseEntity.ok(updated);
    }

    /**
     * Partially updates a Scene node (synopsis, expectedWords, presentCharacters, characterStates, temporaryCharacters).
     * PATCH is used to allow partial updates from the "true" edit mode.
     */
    @PatchMapping("/scenes/{id}")
    public ResponseEntity<SceneDto> patchScene(
            @PathVariable Long id,
            @RequestBody SceneDto sceneDto,
            @AuthenticationPrincipal User user) {
        SceneDto updated = outlineService.updateScene(id, sceneDto, user.getId());
        return ResponseEntity.ok(updated);
    }
}

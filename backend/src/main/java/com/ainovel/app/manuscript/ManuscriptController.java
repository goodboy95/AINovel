package com.ainovel.app.manuscript;

import com.ainovel.app.common.RefineRequest;
import com.ainovel.app.manuscript.dto.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class ManuscriptController {
    @Autowired
    private ManuscriptService manuscriptService;

    @GetMapping("/outlines/{outlineId}/manuscripts")
    public List<ManuscriptDto> list(@PathVariable UUID outlineId) { return manuscriptService.listByOutline(outlineId); }

    @PostMapping("/outlines/{outlineId}/manuscripts")
    public ManuscriptDto create(@PathVariable UUID outlineId, @Valid @RequestBody ManuscriptCreateRequest request) {
        return manuscriptService.create(outlineId, request);
    }

    @DeleteMapping("/manuscripts/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { manuscriptService.delete(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/manuscripts/{id}")
    public ManuscriptDto get(@PathVariable UUID id) { return manuscriptService.get(id); }

    @PostMapping("/manuscripts/{id}/scenes/{sceneId}/generate")
    public ManuscriptDto generateSceneForManuscript(@PathVariable UUID id, @PathVariable UUID sceneId) {
        return manuscriptService.generateForScene(id, sceneId);
    }

    @PutMapping("/manuscripts/{id}/sections/{sceneId}")
    public ManuscriptDto saveSectionForManuscript(@PathVariable UUID id, @PathVariable UUID sceneId, @RequestBody SectionUpdateRequest request) {
        return manuscriptService.updateSection(id, sceneId, request);
    }

    @PostMapping("/manuscript/scenes/{sceneId}/generate")
    public ManuscriptDto generateScene(@PathVariable UUID sceneId) { return manuscriptService.generateForScene(sceneId); }

    @PutMapping("/manuscript/sections/{sectionId}")
    public ManuscriptDto saveSection(@PathVariable UUID sectionId, @RequestBody SectionUpdateRequest request) { return manuscriptService.updateSection(sectionId, request); }

    @PostMapping("/manuscripts/{id}/sections/analyze-character-changes")
    public List<CharacterChangeLogDto> analyze(@PathVariable UUID id, @RequestBody AnalyzeCharacterChangeRequest request) { return manuscriptService.analyzeCharacterChanges(id, request); }

    @GetMapping("/manuscripts/{id}/character-change-logs")
    public List<CharacterChangeLogDto> logs(@PathVariable UUID id) { return manuscriptService.listCharacterLogs(id); }

    @GetMapping("/manuscripts/{id}/character-change-logs/{characterId}")
    public List<CharacterChangeLogDto> logsByCharacter(@PathVariable UUID id, @PathVariable UUID characterId) {
        return manuscriptService.listCharacterLogs(id, characterId);
    }

    @PostMapping("/ai/generate-dialogue")
    public ResponseEntity<String> dialogue(@RequestBody RefineRequest request) { return ResponseEntity.ok(manuscriptService.generateDialogue(request)); }
}

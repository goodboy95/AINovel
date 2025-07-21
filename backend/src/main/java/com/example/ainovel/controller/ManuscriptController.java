package com.example.ainovel.controller;

import com.example.ainovel.model.ManuscriptSection;
import com.example.ainovel.model.User;
import com.example.ainovel.service.ManuscriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manuscript")
public class ManuscriptController {

    @Autowired
    private ManuscriptService manuscriptService;

    @PostMapping("/scenes/{sceneId}/generate")
    public ResponseEntity<ManuscriptSection> generateManuscript(@PathVariable Long sceneId, @AuthenticationPrincipal User user) {
        ManuscriptSection manuscriptSection = manuscriptService.generateManuscriptForScene(sceneId, user.getId());
        return ResponseEntity.ok(manuscriptSection);
    }

    @PutMapping("/sections/{sectionId}")
    public ResponseEntity<ManuscriptSection> updateManuscript(@PathVariable Long sectionId, @RequestBody ManuscriptSection sectionDetails, @AuthenticationPrincipal User user) {
        ManuscriptSection updatedSection = manuscriptService.updateManuscript(sectionId, sectionDetails, user.getId());
        return ResponseEntity.ok(updatedSection);
    }

    @GetMapping("/outlines/{outlineId}")
    public ResponseEntity<List<ManuscriptSection>> getManuscriptForOutline(@PathVariable Long outlineId, @AuthenticationPrincipal User user) {
        List<ManuscriptSection> sections = manuscriptService.getManuscriptForOutline(outlineId, user.getId());
        return ResponseEntity.ok(sections);
    }
}

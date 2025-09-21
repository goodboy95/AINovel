package com.example.ainovel.controller;

import com.example.ainovel.dto.world.WorldPromptTemplateMetadataResponse;
import com.example.ainovel.dto.world.WorldPromptTemplatesResetRequest;
import com.example.ainovel.dto.world.WorldPromptTemplatesResponse;
import com.example.ainovel.dto.world.WorldPromptTemplatesUpdateRequest;
import com.example.ainovel.model.User;
import com.example.ainovel.service.world.WorldPromptTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/world-prompts")
@RequiredArgsConstructor
public class WorldPromptTemplateController {

    private final WorldPromptTemplateService templateService;

    @GetMapping
    public WorldPromptTemplatesResponse getTemplates(@AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        return templateService.getEffectiveTemplates(userId);
    }

    @PutMapping
    public ResponseEntity<Void> saveTemplates(@AuthenticationPrincipal User user,
                                              @RequestBody WorldPromptTemplatesUpdateRequest request) {
        templateService.saveTemplates(user.getId(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetTemplates(@AuthenticationPrincipal User user,
                                               @RequestBody WorldPromptTemplatesResetRequest request) {
        templateService.resetTemplates(user.getId(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/metadata")
    public WorldPromptTemplateMetadataResponse getMetadata() {
        return templateService.getMetadata();
    }
}

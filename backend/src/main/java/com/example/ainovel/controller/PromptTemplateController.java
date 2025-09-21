package com.example.ainovel.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ainovel.dto.prompt.PromptTemplateMetadataResponse;
import com.example.ainovel.dto.prompt.PromptTemplatesResetRequest;
import com.example.ainovel.dto.prompt.PromptTemplatesResponse;
import com.example.ainovel.dto.prompt.PromptTemplatesUpdateRequest;
import com.example.ainovel.model.User;
import com.example.ainovel.prompt.PromptTemplateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/prompt-templates")
@RequiredArgsConstructor
public class PromptTemplateController {

    private final PromptTemplateService promptTemplateService;

    @GetMapping
    public PromptTemplatesResponse getTemplates(@AuthenticationPrincipal User user) {
        Long userId = user != null ? user.getId() : null;
        return promptTemplateService.getEffectiveTemplates(userId);
    }

    @PutMapping
    public ResponseEntity<Void> saveTemplates(@AuthenticationPrincipal User user,
                                              @RequestBody PromptTemplatesUpdateRequest request) {
        promptTemplateService.saveTemplates(user.getId(), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetTemplates(@AuthenticationPrincipal User user,
                                               @RequestBody PromptTemplatesResetRequest request) {
        promptTemplateService.resetTemplates(user.getId(), request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/metadata")
    public PromptTemplateMetadataResponse getMetadata() {
        return promptTemplateService.getMetadata();
    }
}

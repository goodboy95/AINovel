package com.ainovel.app.settings;

import com.ainovel.app.settings.dto.*;
import com.ainovel.app.user.User;
import com.ainovel.app.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public class SettingsController {
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private UserRepository userRepository;

    private User currentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
    }

    @GetMapping("/settings")
    public ResponseEntity<SettingsResponse> getSettings(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(settingsService.getSettings(currentUser(principal)));
    }

    @PutMapping("/settings")
    public ResponseEntity<SettingsResponse> updateSettings(@AuthenticationPrincipal UserDetails principal,
                                                           @Valid @RequestBody SettingsUpdateRequest request) {
        return ResponseEntity.ok(settingsService.updateSettings(currentUser(principal), request));
    }

    @PostMapping("/settings/test")
    public ResponseEntity<Boolean> testSettings(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(settingsService.testSettings(currentUser(principal)));
    }

    @GetMapping("/prompt-templates")
    public ResponseEntity<PromptTemplatesResponse> getPrompts(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(settingsService.getPromptTemplates(currentUser(principal)));
    }

    @PutMapping("/prompt-templates")
    public ResponseEntity<PromptTemplatesResponse> updatePrompts(@AuthenticationPrincipal UserDetails principal,
                                                                 @RequestBody PromptTemplatesUpdateRequest request) {
        return ResponseEntity.ok(settingsService.updatePromptTemplates(currentUser(principal), request));
    }

    @PostMapping("/prompt-templates/reset")
    public ResponseEntity<PromptTemplatesResponse> resetPrompts(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(settingsService.resetPromptTemplates(currentUser(principal)));
    }

    @GetMapping("/prompt-templates/metadata")
    public ResponseEntity<PromptMetadataResponse> metadata() {
        return ResponseEntity.ok(settingsService.getPromptMetadata());
    }

    @GetMapping("/world-prompts")
    public ResponseEntity<WorldPromptTemplatesResponse> worldPrompts(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(settingsService.getWorldPromptTemplates(currentUser(principal)));
    }

    @PutMapping("/world-prompts")
    public ResponseEntity<WorldPromptTemplatesResponse> updateWorldPrompts(@AuthenticationPrincipal UserDetails principal,
                                                                           @RequestBody WorldPromptTemplatesUpdateRequest request) {
        return ResponseEntity.ok(settingsService.updateWorldPrompts(currentUser(principal), request));
    }

    @PostMapping("/world-prompts/reset")
    public ResponseEntity<WorldPromptTemplatesResponse> resetWorldPrompts(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(settingsService.resetWorldPrompts(currentUser(principal)));
    }

    @GetMapping("/world-prompts/metadata")
    public ResponseEntity<WorldPromptMetadataResponse> worldPromptMetadata() {
        return ResponseEntity.ok(settingsService.getWorldPromptMetadata());
    }
}

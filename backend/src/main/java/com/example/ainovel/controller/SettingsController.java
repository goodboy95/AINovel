package com.example.ainovel.controller;

import com.example.ainovel.dto.SettingsDto;
import com.example.ainovel.service.SettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<SettingsDto> getSettings(Authentication authentication) {
        String username = (authentication != null) ? authentication.getName() : "duwei";
        SettingsDto settings = settingsService.getSettings(username);
        return ResponseEntity.ok(settings);
    }

    @PutMapping
    public ResponseEntity<Void> updateSettings(Authentication authentication, @RequestBody SettingsDto settingsDto) {
        String username = (authentication != null) ? authentication.getName() : "duwei";
        settingsService.updateSettings(username, settingsDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/test")
    public ResponseEntity<?> testConnection(@RequestBody SettingsDto settingsDto) {
        boolean isSuccess = settingsService.testConnection(settingsDto);
        if (isSuccess) {
            return ResponseEntity.ok(Map.of("message", "Connection successful!"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("message", "Connection test failed. Please check your API key."));
        }
    }
}

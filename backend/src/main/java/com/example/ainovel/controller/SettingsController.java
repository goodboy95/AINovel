package com.example.ainovel.controller;

import com.example.ainovel.dto.SettingsDto;
import com.example.ainovel.model.User;
import com.example.ainovel.service.SettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing user settings.
 */
@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * Retrieves the settings for the authenticated user.
     * @param user The authenticated user.
     * @return A DTO containing the user's settings.
     */
    @GetMapping
    public ResponseEntity<SettingsDto> getSettings(@AuthenticationPrincipal User user) {
        SettingsDto settings = settingsService.getSettings(user.getUsername());
        return ResponseEntity.ok(settings);
    }

    /**
     * Updates the settings for the authenticated user.
     * @param user The authenticated user.
     * @param settingsDto A DTO containing the new settings.
     * @return A response entity indicating success.
     */
    @PutMapping
    public ResponseEntity<Void> updateSettings(@AuthenticationPrincipal User user, @RequestBody SettingsDto settingsDto) {
        settingsService.updateSettings(user.getUsername(), settingsDto);
        return ResponseEntity.ok().build();
    }

    /**
     * Tests the connection to an AI provider with the given settings.
     * @param settingsDto A DTO containing the provider and API key to test.
     * @return A response entity indicating whether the connection was successful.
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testConnection(@RequestBody SettingsDto settingsDto) {
        boolean isSuccess = settingsService.testConnection(settingsDto);
        if (isSuccess) {
            return ResponseEntity.ok(Map.of("message", "Connection successful!"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Connection test failed. Please check your API key and provider."));
    }
}

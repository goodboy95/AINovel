package com.example.ainovel.controller;

import com.example.ainovel.dto.CharacterDialogueRequest;
import com.example.ainovel.dto.CharacterDialogueResponse;
import com.example.ainovel.model.User;
import com.example.ainovel.service.CharacterDialogueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI-focused endpoints that are not tied to a specific legacy controller.
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final CharacterDialogueService characterDialogueService;

    @PostMapping("/generate-dialogue")
    public ResponseEntity<CharacterDialogueResponse> generateDialogue(@RequestBody CharacterDialogueRequest request,
                                                                       @AuthenticationPrincipal User user) {
        CharacterDialogueResponse response = characterDialogueService.generateDialogue(request, user.getId());
        return ResponseEntity.ok(response);
    }
}

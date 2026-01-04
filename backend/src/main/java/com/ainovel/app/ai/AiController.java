package com.ainovel.app.ai;

import com.ainovel.app.ai.dto.*;
import com.ainovel.app.user.User;
import com.ainovel.app.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/ai")
public class AiController {
    @Autowired
    private AiService aiService;
    @Autowired
    private UserRepository userRepository;

    private User currentUser(UserDetails details) {
        return userRepository.findByUsername(details.getUsername()).orElseThrow();
    }

    @GetMapping("/models")
    public List<AiModelDto> models() {
        return aiService.listModels();
    }

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody AiChatRequest request) {
        return ResponseEntity.ok(aiService.chat(currentUser(principal), request));
    }

    @PostMapping("/refine")
    public ResponseEntity<AiRefineResponse> refine(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody AiRefineRequest request) {
        return ResponseEntity.ok(aiService.refine(currentUser(principal), request));
    }
}


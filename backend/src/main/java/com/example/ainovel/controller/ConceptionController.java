package com.example.ainovel.controller;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.User;
import com.example.ainovel.service.ConceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ConceptionController {

    private final ConceptionService conceptionService;

    public ConceptionController(ConceptionService conceptionService) {
        this.conceptionService = conceptionService;
    }

    @PostMapping("/conception")
    public ResponseEntity<ConceptionResponse> generateStory(
            @RequestBody ConceptionRequest request, @AuthenticationPrincipal User user) {
        ConceptionResponse response = conceptionService.generateAndSaveStory(user.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/story-cards")
    public ResponseEntity<List<StoryCard>> getAllStoryCards(@AuthenticationPrincipal User user) {
        List<StoryCard> storyCards = conceptionService.getAllStoryCards(user.getUsername());
        return ResponseEntity.ok(storyCards);
    }

    @GetMapping("/story-cards/{id}")
    public ResponseEntity<StoryCard> getStoryCardById(@PathVariable Long id) {
        StoryCard storyCard = conceptionService.getStoryCardById(id);
        return ResponseEntity.ok(storyCard);
    }

    @GetMapping("/story-cards/{storyId}/character-cards")
    public ResponseEntity<List<CharacterCard>> getCharacterCardsByStoryId(@PathVariable Long storyId) {
        List<CharacterCard> characterCards = conceptionService.getCharacterCardsByStoryId(storyId);
        return ResponseEntity.ok(characterCards);
    }

    @PutMapping("/story-cards/{id}")
    public ResponseEntity<StoryCard> updateStoryCard(@PathVariable Long id, @RequestBody StoryCard storyDetails) {
        StoryCard updatedStoryCard = conceptionService.updateStoryCard(id, storyDetails);
        return ResponseEntity.ok(updatedStoryCard);
    }

    @PutMapping("/character-cards/{id}")
    public ResponseEntity<CharacterCard> updateCharacterCard(@PathVariable Long id, @RequestBody CharacterCard characterDetails) {
        CharacterCard updatedCharacterCard = conceptionService.updateCharacterCard(id, characterDetails);
        return ResponseEntity.ok(updatedCharacterCard);
    }
}

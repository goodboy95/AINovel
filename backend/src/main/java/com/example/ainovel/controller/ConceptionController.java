package com.example.ainovel.controller;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;
import com.example.ainovel.dto.RefineRequest;
import com.example.ainovel.dto.RefineResponse;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.User;
import com.example.ainovel.service.ConceptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for handling story conception and character card operations.
 */
@RestController
@RequestMapping("/api/v1")
public class ConceptionController {

    private final ConceptionService conceptionService;

    /**
     * Constructs a ConceptionController with the necessary ConceptionService.
     * @param conceptionService The service for handling conception logic.
     */
    public ConceptionController(ConceptionService conceptionService) {
        this.conceptionService = conceptionService;
    }

    /**
     * Generates a new story conception based on the user's request.
     * @param request The request object containing the story prompt.
     * @param user The authenticated user.
     * @return A response entity with the generated story and character cards.
     */
    @PostMapping("/conception")
    public ResponseEntity<ConceptionResponse> generateStory(
            @RequestBody ConceptionRequest request, @AuthenticationPrincipal User user) {
        ConceptionResponse response = conceptionService.generateAndSaveStory(user.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all story cards for the authenticated user.
     * @param user The authenticated user.
     * @return A list of story cards.
     */
    @GetMapping("/story-cards")
    public ResponseEntity<List<StoryCard>> getAllStoryCards(@AuthenticationPrincipal User user) {
        List<StoryCard> storyCards = conceptionService.getAllStoryCards(user.getUsername());
        return ResponseEntity.ok(storyCards);
    }

    /**
     * Retrieves a specific story card by its ID.
     * @param id The ID of the story card.
     * @return The requested story card.
     */
    @GetMapping("/story-cards/{id}")
    public ResponseEntity<StoryCard> getStoryCardById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        StoryCard storyCard = conceptionService.getStoryCardById(id, user.getId());
        return ResponseEntity.ok(storyCard);
    }

    /**
     * Retrieves all character cards associated with a specific story.
     * @param storyId The ID of the story.
     * @return A list of character cards.
     */
    @GetMapping("/story-cards/{storyId}/character-cards")
    public ResponseEntity<List<CharacterCard>> getCharacterCardsByStoryId(@PathVariable Long storyId, @AuthenticationPrincipal User user) {
        List<CharacterCard> characterCards = conceptionService.getCharacterCardsByStoryId(storyId, user.getId());
        return ResponseEntity.ok(characterCards);
    }

    /**
     * Updates an existing story card.
     * @param id The ID of the story card to update.
     * @param storyDetails The updated story card data.
     * @return The updated story card.
     */
    @PutMapping("/story-cards/{id}")
    public ResponseEntity<StoryCard> updateStoryCard(@PathVariable Long id, @RequestBody StoryCard storyDetails, @AuthenticationPrincipal User user) {
        StoryCard updatedStoryCard = conceptionService.updateStoryCard(id, storyDetails, user.getId());
        return ResponseEntity.ok(updatedStoryCard);
    }

    /**
     * Updates an existing character card.
     * @param id The ID of the character card to update.
     * @param characterDetails The updated character card data.
     * @return The updated character card.
     */
    @PutMapping("/character-cards/{id}")
    public ResponseEntity<CharacterCard> updateCharacterCard(@PathVariable Long id, @RequestBody CharacterCard characterDetails, @AuthenticationPrincipal User user) {
        CharacterCard updatedCharacterCard = conceptionService.updateCharacterCard(id, characterDetails, user.getId());
        return ResponseEntity.ok(updatedCharacterCard);
    }

    /**
     * Adds a new character card to a story.
     * @param storyCardId The ID of the story to add the character to.
     * @param characterCard The new character card data.
     * @param user The authenticated user.
     * @return The newly created character card.
     */
    @PostMapping("/story-cards/{storyCardId}/characters")
    public ResponseEntity<CharacterCard> addCharacterToStory(
            @PathVariable Long storyCardId, @RequestBody CharacterCard characterCard, @AuthenticationPrincipal User user) {
        CharacterCard newCharacterCard = conceptionService.addCharacterToStory(storyCardId, characterCard, user);
        return ResponseEntity.ok(newCharacterCard);
    }

    /**
     * Deletes a character card by its ID.
     * @param id The ID of the character card to delete.
     * @return A response entity with no content.
     */
    @DeleteMapping("/character-cards/{id}")
    public ResponseEntity<Void> deleteCharacterCard(@PathVariable Long id, @AuthenticationPrincipal User user) {
        conceptionService.deleteCharacterCard(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Refines a specific field of a story card.
     * @param id The ID of the story card.
     * @param request The request object with refinement details.
     * @param user The authenticated user.
     * @return A response entity with the refined content.
     */
    @PostMapping("/story-cards/{id}/refine")
    public ResponseEntity<RefineResponse> refineStoryCard(
            @PathVariable Long id, @RequestBody RefineRequest request, @AuthenticationPrincipal User user) {
        RefineResponse response = conceptionService.refineStoryCardField(id, request, user);
        return ResponseEntity.ok(response);
    }

    /**
     * Refines a specific field of a character card.
     * @param id The ID of the character card.
     * @param request The request object with refinement details.
     * @param user The authenticated user.
     * @return A response entity with the refined content.
     */
    @PostMapping("/character-cards/{id}/refine")
    public ResponseEntity<RefineResponse> refineCharacterCard(
            @PathVariable Long id, @RequestBody RefineRequest request, @AuthenticationPrincipal User user) {
        RefineResponse response = conceptionService.refineCharacterCardField(id, request, user);
        return ResponseEntity.ok(response);
    }
}

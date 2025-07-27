package com.example.ainovel.service;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;
import com.example.ainovel.dto.RefineRequest;
import com.example.ainovel.dto.RefineResponse;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.User;
import com.example.ainovel.model.UserSetting;
import com.example.ainovel.repository.CharacterCardRepository;
import com.example.ainovel.repository.StoryCardRepository;
import com.example.ainovel.repository.UserRepository;
import com.example.ainovel.repository.UserSettingRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.example.ainovel.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

/**
 * Service for handling business logic related to story conception,
 * including generation, retrieval, and modification of story and character cards.
 */
@Service
@RequiredArgsConstructor
public class ConceptionService {

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final StoryCardRepository storyCardRepository;
    private final CharacterCardRepository characterCardRepository;
    private final EncryptionService encryptionService;
    private final ApplicationContext applicationContext;

    /**
     * Generates and saves a new story and associated character cards based on a user's prompt.
     *
     * @param username The username of the user making the request.
     * @param request  The request DTO containing the prompt and other details.
     * @return A DTO containing the newly created story and character cards.
     */
    @Transactional
    public ConceptionResponse generateAndSaveStory(String username, ConceptionRequest request) {
        User user = findUserByUsername(username);
        AiService aiService = getAiServiceForUser(user);
        String apiKey = getApiKeyForUser(user);

        ConceptionResponse responseFromAi = aiService.generateConception(request, apiKey);

        StoryCard storyCard = responseFromAi.getStoryCard();
        if (storyCard == null) {
            throw new IllegalStateException("Failed to generate story card from AI service.");
        }
        storyCard.setUser(user);
        StoryCard savedStoryCard = storyCardRepository.save(storyCard);

        List<CharacterCard> characterCards = responseFromAi.getCharacterCards();
        characterCards.forEach(cc -> {
            cc.setUser(user);
            cc.setStoryCard(savedStoryCard);
        });
        List<CharacterCard> savedCharacterCards = characterCardRepository.saveAll(characterCards);

        return new ConceptionResponse(savedStoryCard, savedCharacterCards);
    }

    /**
     * Retrieves all story cards belonging to a specific user.
     *
     * @param username The username of the user.
     * @return A list of the user's story cards.
     */
    public List<StoryCard> getAllStoryCards(String username) {
        User user = findUserByUsername(username);
        return storyCardRepository.findByUserId(user.getId());
    }

    /**
     * Retrieves a single story card by its ID, ensuring the user has permission.
     *
     * @param id     The ID of the story card.
     * @param userId The ID of the user requesting the card.
     * @return The requested StoryCard.
     * @throws ResourceNotFoundException if the card is not found.
     * @throws AccessDeniedException     if the user does not own the card.
     */
    public StoryCard getStoryCardById(Long id, Long userId) {
        StoryCard storyCard = findStoryCardById(id);
        validateUserPermission(storyCard.getUser(), userId, "story card");
        return storyCard;
    }

    /**
     * Retrieves all character cards for a given story, ensuring the user has permission.
     *
     * @param storyId The ID of the story.
     * @param userId  The ID of the user making the request.
     * @return A list of character cards.
     */
    public List<CharacterCard> getCharacterCardsByStoryId(Long storyId, Long userId) {
        StoryCard storyCard = findStoryCardById(storyId);
        validateUserPermission(storyCard.getUser(), userId, "story card");
        return characterCardRepository.findByStoryCardId(storyId);
    }

    /**
     * Updates an existing story card, ensuring the user has permission.
     *
     * @param cardId       The ID of the story card to update.
     * @param storyDetails A DTO with the updated details.
     * @param userId       The ID of the user making the request.
     * @return The updated StoryCard.
     */
    @Transactional
    public StoryCard updateStoryCard(Long cardId, StoryCard storyDetails, Long userId) {
        StoryCard storyCard = findStoryCardById(cardId);
        validateUserPermission(storyCard.getUser(), userId, "story card");

        storyCard.setTitle(storyDetails.getTitle());
        storyCard.setGenre(storyDetails.getGenre());
        storyCard.setTone(storyDetails.getTone());
        storyCard.setSynopsis(storyDetails.getSynopsis());
        storyCard.setStoryArc(storyDetails.getStoryArc());

        return storyCardRepository.save(storyCard);
    }

    /**
     * Updates an existing character card, ensuring the user has permission.
     *
     * @param cardId           The ID of the character card to update.
     * @param characterDetails A DTO with the updated details.
     * @param userId           The ID of the user making the request.
     * @return The updated CharacterCard.
     */
    @Transactional
    public CharacterCard updateCharacterCard(Long cardId, CharacterCard characterDetails, Long userId) {
        CharacterCard characterCard = findCharacterCardById(cardId);
        validateUserPermission(characterCard.getUser(), userId, "character card");

        characterCard.setName(characterDetails.getName());
        characterCard.setSynopsis(characterDetails.getSynopsis());
        characterCard.setDetails(characterDetails.getDetails());
        characterCard.setRelationships(characterDetails.getRelationships());
        characterCard.setAvatarUrl(characterDetails.getAvatarUrl());

        return characterCardRepository.save(characterCard);
    }

    /**
     * Adds a new character to an existing story, ensuring the user has permission.
     *
     * @param storyCardId   The ID of the story to add the character to.
     * @param characterCard The new character card to add.
     * @param user          The authenticated user.
     * @return The newly saved CharacterCard.
     */
    @Transactional
    public CharacterCard addCharacterToStory(Long storyCardId, CharacterCard characterCard, User user) {
        StoryCard storyCard = findStoryCardById(storyCardId);
        validateUserPermission(storyCard.getUser(), user.getId(), "story card");
        characterCard.setStoryCard(storyCard);
        characterCard.setUser(user);
        return characterCardRepository.save(characterCard);
    }

    /**
     * Deletes a character card, ensuring the user has permission.
     *
     * @param cardId The ID of the character card to delete.
     * @param userId The ID of the user making the request.
     */
    @Transactional
    public void deleteCharacterCard(Long cardId, Long userId) {
        CharacterCard characterCard = findCharacterCardById(cardId);
        validateUserPermission(characterCard.getUser(), userId, "character card");
        characterCardRepository.deleteById(cardId);
    }

    /**
     * Refines a field of a story card using an AI service.
     *
     * @param cardId  The ID of the story card.
     * @param request The refinement request.
     * @param user    The authenticated user.
     * @return A DTO with the refined text.
     */
    public RefineResponse refineStoryCardField(Long cardId, RefineRequest request, User user) {
        StoryCard storyCard = findStoryCardById(cardId);
        validateUserPermission(storyCard.getUser(), user.getId(), "story card");
        return refineTextWithAi(request, user);
    }

    /**
     * Refines a field of a character card using an AI service.
     *
     * @param cardId  The ID of the character card.
     * @param request The refinement request.
     * @param user    The authenticated user.
     * @return A DTO with the refined text.
     */
    public RefineResponse refineCharacterCardField(Long cardId, RefineRequest request, User user) {
        CharacterCard characterCard = findCharacterCardById(cardId);
        validateUserPermission(characterCard.getUser(), user.getId(), "character card");
        return refineTextWithAi(request, user);
    }

    // Helper methods

    private RefineResponse refineTextWithAi(RefineRequest request, User user) {
        AiService aiService = getAiServiceForUser(user);
        String apiKey = getApiKeyForUser(user);
        String refinedText = aiService.refineText(request, apiKey);
        return new RefineResponse(refinedText);
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private StoryCard findStoryCardById(Long id) {
        return storyCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StoryCard not found with id " + id));
    }

    private CharacterCard findCharacterCardById(Long id) {
        return characterCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CharacterCard not found with id " + id));
    }

    private AiService getAiServiceForUser(User user) {
        UserSetting settings = userSettingRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User settings not found. Please configure your AI provider first."));
        return (AiService) applicationContext.getBean(settings.getLlmProvider().toLowerCase());
    }

    private String getApiKeyForUser(User user) {
        UserSetting settings = userSettingRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User settings not found. Please configure your AI provider first."));
        String apiKey = encryptionService.decrypt(settings.getApiKey());
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API Key is not configured or is invalid.");
        }
        return apiKey;
    }

    private void validateUserPermission(User resourceOwner, Long currentUserId, String resourceType) {
        if (!resourceOwner.getId().equals(currentUserId)) {
            throw new AccessDeniedException("User does not have permission to access this " + resourceType + ".");
        }
    }
}

package com.example.ainovel.service;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;
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
import java.util.Optional;

@Service
public class ConceptionService {

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final StoryCardRepository storyCardRepository;
    private final CharacterCardRepository characterCardRepository;
    private final EncryptionService encryptionService;
    private final ApplicationContext applicationContext;

    public ConceptionService(UserRepository userRepository, UserSettingRepository userSettingRepository, StoryCardRepository storyCardRepository, CharacterCardRepository characterCardRepository, EncryptionService encryptionService, ApplicationContext applicationContext) {
        this.userRepository = userRepository;
        this.userSettingRepository = userSettingRepository;
        this.storyCardRepository = storyCardRepository;
        this.characterCardRepository = characterCardRepository;
        this.encryptionService = encryptionService;
        this.applicationContext = applicationContext;
    }

    @Transactional
    public ConceptionResponse generateAndSaveStory(String username, ConceptionRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        UserSetting settings = userSettingRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("User settings not found. Please configure your AI provider first."));

        String apiKey = encryptionService.decrypt(settings.getApiKey());
        if (apiKey == null) {
            throw new IllegalStateException("API Key is not configured.");
        }

        AiService aiService = (AiService) applicationContext.getBean(settings.getLlmProvider().toLowerCase());
        ConceptionResponse responseFromAi = aiService.generateStory(request, apiKey);

        // Save the story card
        StoryCard storyCard = responseFromAi.getStoryCard();
        if (storyCard == null) {
            throw new IllegalStateException("Failed to generate story card from AI service. The response might be malformed or empty.");
        }
        storyCard.setUser(user);
        StoryCard savedStoryCard = storyCardRepository.save(storyCard);

        // Save the character cards
        List<CharacterCard> characterCards = responseFromAi.getCharacterCards();
        characterCards.forEach(cc -> {
            cc.setUser(user);
            cc.setStoryCard(savedStoryCard);
        });
        List<CharacterCard> savedCharacterCards = characterCardRepository.saveAll(characterCards);

        return new ConceptionResponse(savedStoryCard, savedCharacterCards);
    }

    public List<StoryCard> getAllStoryCards(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return storyCardRepository.findByUserId(user.getId());
    }

    @Transactional
    public StoryCard updateStoryCard(Long cardId, StoryCard storyDetails) {
        StoryCard storyCard = storyCardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("StoryCard not found with id " + cardId));
        
        storyCard.setTitle(storyDetails.getTitle());
        storyCard.setGenre(storyDetails.getGenre());
        storyCard.setTone(storyDetails.getTone());
        storyCard.setSynopsis(storyDetails.getSynopsis());
        storyCard.setStoryArc(storyDetails.getStoryArc());

        return storyCardRepository.save(storyCard);
    }

    @Transactional
    public CharacterCard updateCharacterCard(Long cardId, CharacterCard characterDetails) {
        CharacterCard characterCard = characterCardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("CharacterCard not found with id " + cardId));

        characterCard.setName(characterDetails.getName());
        characterCard.setSynopsis(characterDetails.getSynopsis());
        characterCard.setDetails(characterDetails.getDetails());
        characterCard.setRelationships(characterDetails.getRelationships());
        characterCard.setAvatarUrl(characterDetails.getAvatarUrl());
        
        return characterCardRepository.save(characterCard);
    }
}

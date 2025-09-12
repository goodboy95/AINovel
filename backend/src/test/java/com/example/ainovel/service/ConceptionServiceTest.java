package com.example.ainovel.service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.ConceptionResponse;
import com.example.ainovel.dto.RefineRequest;
import com.example.ainovel.dto.RefineResponse;
import com.example.ainovel.exception.ResourceNotFoundException;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.User;
import com.example.ainovel.model.UserSetting;
import com.example.ainovel.repository.CharacterCardRepository;
import com.example.ainovel.repository.StoryCardRepository;
import com.example.ainovel.repository.UserRepository;
import com.example.ainovel.repository.UserSettingRepository;

@ExtendWith(MockitoExtension.class)
class ConceptionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingRepository userSettingRepository;

    @Mock
    private StoryCardRepository storyCardRepository;

    @Mock
    private CharacterCardRepository characterCardRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private SettingsService settingsService;

    @Mock
    private OpenAiService openAiService;

    @InjectMocks
    private ConceptionService conceptionService;

    private User testUser;
    private User otherUser;
    private UserSetting testUserSetting;
    private StoryCard testStoryCard;
    private CharacterCard testCharacterCard;
    private ConceptionRequest testConceptionRequest;
    private ConceptionResponse testConceptionResponse;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("otheruser");

        testUserSetting = new UserSetting();
        testUserSetting.setId(1L);
        testUserSetting.setUser(testUser);
        testUserSetting.setApiKey("encrypted-api-key");

        testStoryCard = new StoryCard();
        testStoryCard.setId(1L);
        testStoryCard.setTitle("Test Story");
        testStoryCard.setUser(testUser);

        testCharacterCard = new CharacterCard();
        testCharacterCard.setId(1L);
        testCharacterCard.setName("Test Character");
        testCharacterCard.setUser(testUser);
        testCharacterCard.setStoryCard(testStoryCard);

        testConceptionRequest = new ConceptionRequest();
        testConceptionRequest.setIdea("A magical adventure");

        testConceptionResponse = new ConceptionResponse(testStoryCard, Arrays.asList(testCharacterCard));
    }

    @Test
    void testGenerateAndSaveStory_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.of(testUserSetting));
        when(encryptionService.decrypt(anyString())).thenReturn("decrypted-api-key");
        when(settingsService.getBaseUrlByUserId(1L)).thenReturn("https://api.example.com/v1");
        when(settingsService.getModelNameByUserId(1L)).thenReturn("gpt-4");
        when(openAiService.generateConception(any(ConceptionRequest.class), anyString(), anyString(), anyString()))
                .thenReturn(testConceptionResponse);
        when(storyCardRepository.save(any(StoryCard.class))).thenReturn(testStoryCard);
        when(characterCardRepository.saveAll(anyList())).thenReturn(Arrays.asList(testCharacterCard));

        // When
        ConceptionResponse result = conceptionService.generateAndSaveStory("testuser", testConceptionRequest);

        // Then
        assertNotNull(result);
        assertEquals(testStoryCard, result.getStoryCard());
        verify(storyCardRepository).save(any(StoryCard.class));
        verify(characterCardRepository).saveAll(anyList());
    }

    @Test
    void testGetAllStoryCards_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(storyCardRepository.findByUserId(1L)).thenReturn(Arrays.asList(testStoryCard));

        // When
        List<StoryCard> result = conceptionService.getAllStoryCards("testuser");

        // Then
        assertEquals(1, result.size());
    }

    @Test
    void testGetStoryCardById_Success() {
        // Given
        when(storyCardRepository.findById(1L)).thenReturn(Optional.of(testStoryCard));

        // When
        StoryCard result = conceptionService.getStoryCardById(1L, 1L);

        // Then
        assertEquals(testStoryCard, result);
    }

    @Test
    void testGetStoryCardById_NotFound() {
        // Given
        when(storyCardRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            conceptionService.getStoryCardById(1L, 1L);
        });
    }

    @Test
    void testGetStoryCardById_AccessDenied() {
        // Given
        when(storyCardRepository.findById(1L)).thenReturn(Optional.of(testStoryCard));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            conceptionService.getStoryCardById(1L, 2L); // otherUser's ID
        });
    }

    @Test
    void testGetCharacterCardsByStoryId_Success() {
        // Given
        when(storyCardRepository.findById(1L)).thenReturn(Optional.of(testStoryCard));
        when(characterCardRepository.findByStoryCardId(1L)).thenReturn(Arrays.asList(testCharacterCard));

        // When
        List<CharacterCard> result = conceptionService.getCharacterCardsByStoryId(1L, 1L);

        // Then
        assertEquals(1, result.size());
    }

    @Test
    void testGetCharacterCardsByStoryId_AccessDenied() {
        // Given
        when(storyCardRepository.findById(1L)).thenReturn(Optional.of(testStoryCard));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            conceptionService.getCharacterCardsByStoryId(1L, 2L);
        });
    }

    @Test
    void testUpdateStoryCard_Success() {
        // Given
        StoryCard updatedDetails = new StoryCard();
        updatedDetails.setTitle("Updated Title");
        when(storyCardRepository.findById(1L)).thenReturn(Optional.of(testStoryCard));
        when(storyCardRepository.save(any(StoryCard.class))).thenReturn(testStoryCard);

        // When
        StoryCard result = conceptionService.updateStoryCard(1L, updatedDetails, 1L);

        // Then
        assertEquals("Updated Title", result.getTitle());
        verify(storyCardRepository).save(testStoryCard);
    }

    @Test
    void testUpdateStoryCard_AccessDenied() {
        // Given
        when(storyCardRepository.findById(1L)).thenReturn(Optional.of(testStoryCard));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            conceptionService.updateStoryCard(1L, new StoryCard(), 2L);
        });
    }

    @Test
    void testUpdateCharacterCard_Success() {
        // Given
        CharacterCard updatedDetails = new CharacterCard();
        updatedDetails.setName("Updated Name");
        when(characterCardRepository.findById(1L)).thenReturn(Optional.of(testCharacterCard));
        when(characterCardRepository.save(any(CharacterCard.class))).thenReturn(testCharacterCard);

        // When
        CharacterCard result = conceptionService.updateCharacterCard(1L, updatedDetails, 1L);

        // Then
        assertEquals("Updated Name", result.getName());
        verify(characterCardRepository).save(testCharacterCard);
    }

    @Test
    void testUpdateCharacterCard_AccessDenied() {
        // Given
        when(characterCardRepository.findById(1L)).thenReturn(Optional.of(testCharacterCard));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            conceptionService.updateCharacterCard(1L, new CharacterCard(), 2L);
        });
    }

    @Test
    void testAddCharacterToStory_Success() {
        // Given
        CharacterCard newCharacter = new CharacterCard();
        when(storyCardRepository.findById(1L)).thenReturn(Optional.of(testStoryCard));
        when(characterCardRepository.save(any(CharacterCard.class))).thenReturn(newCharacter);

        // When
        CharacterCard result = conceptionService.addCharacterToStory(1L, newCharacter, testUser);

        // Then
        assertNotNull(result);
        verify(characterCardRepository).save(newCharacter);
    }

    @Test
    void testAddCharacterToStory_AccessDenied() {
        // Given
        when(storyCardRepository.findById(1L)).thenReturn(Optional.of(testStoryCard));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            conceptionService.addCharacterToStory(1L, new CharacterCard(), otherUser);
        });
    }

    @Test
    void testDeleteCharacterCard_Success() {
        // Given
        when(characterCardRepository.findById(1L)).thenReturn(Optional.of(testCharacterCard));
        doNothing().when(characterCardRepository).deleteById(1L);

        // When
        conceptionService.deleteCharacterCard(1L, 1L);

        // Then
        verify(characterCardRepository).deleteById(1L);
    }

    @Test
    void testDeleteCharacterCard_NotFound() {
        // Given
        when(characterCardRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            conceptionService.deleteCharacterCard(1L, 1L);
        });
    }

    @Test
    void testDeleteCharacterCard_AccessDenied() {
        // Given
        when(characterCardRepository.findById(1L)).thenReturn(Optional.of(testCharacterCard));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            conceptionService.deleteCharacterCard(1L, 2L);
        });
    }

    @Test
    void testRefineStoryCardField_Success() {
        // Given
        RefineRequest refineRequest = new RefineRequest();
        when(storyCardRepository.findById(1L)).thenReturn(Optional.of(testStoryCard));
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.of(testUserSetting));
        when(encryptionService.decrypt(anyString())).thenReturn("decrypted-api-key");
        when(settingsService.getBaseUrlByUserId(1L)).thenReturn("https://api.example.com/v1");
        when(settingsService.getModelNameByUserId(1L)).thenReturn("gpt-4");
        when(openAiService.refineText(any(RefineRequest.class), anyString(), anyString(), anyString()))
                .thenReturn("Refined text");

        // When
        RefineResponse result = conceptionService.refineStoryCardField(1L, refineRequest, testUser);

        // Then
        assertEquals("Refined text", result.getRefinedText());
    }

    @Test
    void testRefineStoryCardField_AccessDenied() {
        // Given
        RefineRequest refineRequest = new RefineRequest();
        when(storyCardRepository.findById(1L)).thenReturn(Optional.of(testStoryCard));

        // When & Then
        assertThrows(AccessDeniedException.class, () -> {
            conceptionService.refineStoryCardField(1L, refineRequest, otherUser);
        });
    }
}

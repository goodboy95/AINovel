package com.example.ainovel.service;

import com.example.ainovel.dto.SettingsDto;
import com.example.ainovel.model.User;
import com.example.ainovel.model.UserSetting;
import com.example.ainovel.repository.UserRepository;
import com.example.ainovel.repository.UserSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSettingRepository userSettingRepository;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private OpenAiService openAiService;

    @Mock
    private ClaudeService claudeService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SettingsService settingsService;

    private User testUser;
    private UserSetting testUserSetting;
    private SettingsDto testSettingsDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");

        testUserSetting = new UserSetting();
        testUserSetting.setId(1L);
        testUserSetting.setUser(testUser);
        testUserSetting.setLlmProvider("openai");
        testUserSetting.setModelName("gpt-4");
        testUserSetting.setApiKey("encrypted-api-key");
        testUserSetting.setCustomPrompt("Custom prompt");

        testSettingsDto = new SettingsDto();
        testSettingsDto.setLlmProvider("openai");
        testSettingsDto.setModelName("gpt-4");
        testSettingsDto.setApiKey("test-api-key");
        testSettingsDto.setCustomPrompt("Custom prompt");

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testuser");
    }

    @Test
    void testGetSettings_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.of(testUserSetting));

        // When
        SettingsDto result = settingsService.getSettings("testuser");

        // Then
        assertNotNull(result);
        assertEquals("openai", result.getLlmProvider());
        assertEquals("gpt-4", result.getModelName());
        assertEquals("Custom prompt", result.getCustomPrompt());
        assertNull(result.getApiKey()); // API key should not be exposed
    }

    @Test
    void testGetSettings_UserNotFound() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, () -> {
            settingsService.getSettings("testuser");
        });
    }

    @Test
    void testGetSettings_NoUserSettings() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // When
        SettingsDto result = settingsService.getSettings("testuser");

        // Then
        assertNotNull(result);
        assertNull(result.getLlmProvider());
        assertNull(result.getModelName());
        assertNull(result.getCustomPrompt());
        assertNull(result.getApiKey());
    }

    @Test
    void testUpdateSettings_NewSettings() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(encryptionService.encrypt("test-api-key")).thenReturn("encrypted-api-key");
        when(userSettingRepository.save(any(UserSetting.class))).thenReturn(testUserSetting);

        // When
        settingsService.updateSettings("testuser", testSettingsDto);

        // Then
        verify(encryptionService).encrypt("test-api-key");
        verify(userSettingRepository).save(any(UserSetting.class));
    }

    @Test
    void testUpdateSettings_ExistingSettings() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.of(testUserSetting));
        when(encryptionService.encrypt("test-api-key")).thenReturn("encrypted-api-key");
        when(userSettingRepository.save(any(UserSetting.class))).thenReturn(testUserSetting);

        // When
        settingsService.updateSettings("testuser", testSettingsDto);

        // Then
        verify(encryptionService).encrypt("test-api-key");
        verify(userSettingRepository).save(testUserSetting);
    }

    @Test
    void testUpdateSettings_NoApiKey() {
        // Given
        testSettingsDto.setApiKey(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.of(testUserSetting));
        when(userSettingRepository.save(any(UserSetting.class))).thenReturn(testUserSetting);

        // When
        settingsService.updateSettings("testuser", testSettingsDto);

        // Then
        verify(encryptionService, never()).encrypt(anyString());
        verify(userSettingRepository).save(testUserSetting);
    }

    @Test
    void testTestConnection_OpenAI_Success() {
        // Given
        when(openAiService.validateApiKey("test-api-key")).thenReturn(true);

        // When
        boolean result = settingsService.testConnection(testSettingsDto);

        // Then
        assertTrue(result);
        verify(openAiService).validateApiKey("test-api-key");
    }

    @Test
    void testTestConnection_Claude_Success() {
        // Given
        testSettingsDto.setLlmProvider("claude");
        when(claudeService.validateApiKey("test-api-key")).thenReturn(true);

        // When
        boolean result = settingsService.testConnection(testSettingsDto);

        // Then
        assertTrue(result);
        verify(claudeService).validateApiKey("test-api-key");
    }

    @Test
    void testTestConnection_InvalidProvider() {
        // Given
        testSettingsDto.setLlmProvider("unknown");

        // When
        boolean result = settingsService.testConnection(testSettingsDto);

        // Then
        assertFalse(result);
        verify(openAiService, never()).validateApiKey(anyString());
        verify(claudeService, never()).validateApiKey(anyString());
    }

    @Test
    void testTestConnection_NoApiKey() {
        // Given
        testSettingsDto.setApiKey(null);

        // When
        boolean result = settingsService.testConnection(testSettingsDto);

        // Then
        assertFalse(result);
        verify(openAiService, never()).validateApiKey(anyString());
    }

    @Test
    void testTestConnection_Exception() {
        // Given
        when(openAiService.validateApiKey("test-api-key")).thenThrow(new RuntimeException("API error"));

        // When
        boolean result = settingsService.testConnection(testSettingsDto);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetDecryptedApiKeyForCurrentUser_Success() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.of(testUserSetting));
        when(encryptionService.decrypt("encrypted-api-key")).thenReturn("decrypted-api-key");

        // When
        String result = settingsService.getDecryptedApiKeyForCurrentUser();

        // Then
        assertEquals("decrypted-api-key", result);
        verify(encryptionService).decrypt("encrypted-api-key");
    }

    @Test
    void testGetDecryptedApiKeyForCurrentUser_UserNotFound() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UsernameNotFoundException.class, () -> {
            settingsService.getDecryptedApiKeyForCurrentUser();
        });
    }

    @Test
    void testGetDecryptedApiKeyForCurrentUser_SettingsNotFound() {
        // Given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            settingsService.getDecryptedApiKeyForCurrentUser();
        });
    }

    @Test
    void testGetDecryptedApiKeyForCurrentUser_NoApiKey() {
        // Given
        testUserSetting.setApiKey(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userSettingRepository.findByUserId(1L)).thenReturn(Optional.of(testUserSetting));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            settingsService.getDecryptedApiKeyForCurrentUser();
        });
    }
}

package com.example.ainovel.service;

import com.example.ainovel.dto.SettingsDto;
import com.example.ainovel.model.User;
import com.example.ainovel.model.UserSetting;
import com.example.ainovel.repository.UserRepository;
import com.example.ainovel.repository.UserSettingRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * Service for managing user settings, including AI provider and API keys.
 */
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final EncryptionService encryptionService;
    private final OpenAiService openAiService;

    /**
     * Retrieves the decrypted API key for the currently authenticated user.
     * @return The decrypted API key.
     */
    @Transactional(readOnly = true)
    public String getDecryptedApiKeyForCurrentUser() {
        User currentUser = getCurrentUser();
        return getDecryptedApiKeyByUserId(currentUser.getId());
    }

    /**
     * Retrieves the LLM provider for the currently authenticated user.
     * @return The name of the LLM provider.
     */
    @Transactional(readOnly = true)
    public String getProviderForCurrentUser() {
        User currentUser = getCurrentUser();
        return getProviderByUserId(currentUser.getId());
    }

    /**
     * Retrieves the decrypted API key for a specific user by their ID.
     * @param userId The ID of the user.
     * @return The decrypted API key.
     */
    @Transactional(readOnly = true)
    public String getDecryptedApiKeyByUserId(Long userId) {
        UserSetting userSetting = findUserSettingByUserId(userId);
        if (!StringUtils.hasText(userSetting.getApiKey())) {
            throw new IllegalStateException("API Key is not configured for user ID: " + userId);
        }
        return encryptionService.decrypt(userSetting.getApiKey());
    }

    /**
     * Retrieves the LLM provider for a specific user by their ID.
     * @param userId The ID of the user.
     * @return The name of the LLM provider.
     */
    @Transactional(readOnly = true)
    public String getProviderByUserId(Long userId) {
        // Backward compatibility: provider is fixed to OpenAI after simplification
        return "openai";
    }

    /**
     * Retrieves the settings for a specific user.
     * @param username The username of the user.
     * @return A DTO with the user's settings.
     */
    @Transactional(readOnly = true)
    public SettingsDto getSettings(String username) {
        User user = findUserByUsername(username);
        UserSetting userSetting = userSettingRepository.findByUserId(user.getId()).orElse(new UserSetting());

        SettingsDto dto = new SettingsDto();
        dto.setBaseUrl(userSetting.getBaseUrl());
        dto.setModelName(userSetting.getModelName());
        dto.setCustomPrompt(userSetting.getCustomPrompt());
        return dto;
    }

    /**
     * Updates the settings for a specific user.
     * @param username The username of the user.
     * @param settingsDto A DTO containing the new settings.
     */
    @Transactional
    public void updateSettings(String username, SettingsDto settingsDto) {
        User user = findUserByUsername(username);
        UserSetting userSetting = userSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserSetting newUserSetting = new UserSetting();
                    newUserSetting.setUser(user);
                    return newUserSetting;
                });

        userSetting.setBaseUrl(settingsDto.getBaseUrl());
        userSetting.setModelName(settingsDto.getModelName());
        userSetting.setCustomPrompt(settingsDto.getCustomPrompt());

        if (StringUtils.hasText(settingsDto.getApiKey())) {
            userSetting.setApiKey(encryptionService.encrypt(settingsDto.getApiKey()));
        }

        userSettingRepository.save(userSetting);
    }

    /**
     * Tests the connection to the configured AI provider with the given API key.
     * @param settingsDto A DTO containing the provider and API key.
     * @return true if the connection is successful, false otherwise.
     */
    public boolean testConnection(SettingsDto settingsDto) {
        if (!StringUtils.hasText(settingsDto.getApiKey())) {
            return false;
        }
        try {
            return openAiService.validateApiKey(settingsDto.getApiKey(), settingsDto.getBaseUrl());
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public String getBaseUrlByUserId(Long userId) {
        UserSetting userSetting = findUserSettingByUserId(userId);
        if (!StringUtils.hasText(userSetting.getBaseUrl())) {
            throw new IllegalStateException("Base URL is not configured for user ID: " + userId);
        }
        return userSetting.getBaseUrl();
    }

    @Transactional(readOnly = true)
    public String getModelNameByUserId(Long userId) {
        UserSetting userSetting = findUserSettingByUserId(userId);
        if (!StringUtils.hasText(userSetting.getModelName())) {
            throw new IllegalStateException("Model name is not configured for user ID: " + userId);
        }
        return userSetting.getModelName();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return findUserByUsername(username);
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    private UserSetting findUserSettingByUserId(Long userId) {
        return userSettingRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("User settings not found for user ID: " + userId));
    }
}

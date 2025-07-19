package com.example.ainovel.service;

import com.example.ainovel.dto.SettingsDto;
import com.example.ainovel.model.User;
import com.example.ainovel.model.UserSetting;
import com.example.ainovel.repository.UserRepository;
import com.example.ainovel.repository.UserSettingRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SettingsService {

    private final UserRepository userRepository;
    private final UserSettingRepository userSettingRepository;
    private final EncryptionService encryptionService;
    private final OpenAiService openAiService;
    private final ClaudeService claudeService;
    private final GeminiService geminiService;

    public SettingsService(UserRepository userRepository, UserSettingRepository userSettingRepository, EncryptionService encryptionService, OpenAiService openAiService, ClaudeService claudeService, GeminiService geminiService) {
        this.userRepository = userRepository;
        this.userSettingRepository = userSettingRepository;
        this.encryptionService = encryptionService;
        this.openAiService = openAiService;
        this.claudeService = claudeService;
        this.geminiService = geminiService;
    }

    @Transactional(readOnly = true)
    public SettingsDto getSettings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        UserSetting userSetting = userSettingRepository.findByUserId(user.getId())
                .orElse(new UserSetting());

        SettingsDto dto = new SettingsDto();
        dto.setLlmProvider(userSetting.getLlmProvider());
        dto.setModelName(userSetting.getModelName());
        dto.setCustomPrompt(userSetting.getCustomPrompt());
        // Do not expose API key
        return dto;
    }

    @Transactional
    public void updateSettings(String username, SettingsDto settingsDto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        UserSetting userSetting = userSettingRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserSetting newUserSetting = new UserSetting();
                    newUserSetting.setUser(user);
                    return newUserSetting;
                });

        userSetting.setLlmProvider(settingsDto.getLlmProvider());
        userSetting.setModelName(settingsDto.getModelName());
        userSetting.setCustomPrompt(settingsDto.getCustomPrompt());

        if (StringUtils.hasText(settingsDto.getApiKey())) {
            userSetting.setApiKey(encryptionService.encrypt(settingsDto.getApiKey()));
        }

        userSettingRepository.save(userSetting);
    }

    public boolean testConnection(SettingsDto settingsDto) {
        if (!StringUtils.hasText(settingsDto.getApiKey())) {
            return false;
        }
        try {
            switch (settingsDto.getLlmProvider().toLowerCase()) {
                case "openai":
                    return openAiService.validateApiKey(settingsDto.getApiKey());
                case "claude":
                    return claudeService.validateApiKey(settingsDto.getApiKey());
                case "gemini":
                    return geminiService.validateApiKey(settingsDto.getApiKey());
                default:
                    return false;
            }
        } catch (Exception e) {
            // Log the exception for debugging purposes
            // logger.error("API key validation failed", e);
            return false;
        }
    }
}

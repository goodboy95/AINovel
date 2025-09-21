package com.example.ainovel.prompt;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.example.ainovel.model.User;
import com.example.ainovel.model.UserSetting;
import com.example.ainovel.model.prompt.PromptSettings;
import com.example.ainovel.repository.UserRepository;
import com.example.ainovel.repository.UserSettingRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PromptTemplateRepository {

    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;

    public Optional<PromptSettings> findByUserId(Long userId) {
        return userSettingRepository.findByUserId(userId)
                .map(UserSetting::getPromptSettings)
                .filter(settings -> settings != null);
    }

    public PromptSettings save(Long userId, PromptSettings promptSettings) {
        UserSetting setting = userSettingRepository.findByUserId(userId)
                .orElseGet(() -> createSettingForUser(userId));
        setting.setPromptSettings(promptSettings);
        userSettingRepository.save(setting);
        return promptSettings;
    }

    public void clearTemplate(Long userId, java.util.function.Consumer<PromptSettings> clearer) {
        UserSetting setting = userSettingRepository.findByUserId(userId)
                .orElseGet(() -> createSettingForUser(userId));
        PromptSettings prompts = setting.getPromptSettings();
        if (prompts == null) {
            prompts = new PromptSettings();
        }
        clearer.accept(prompts);
        setting.setPromptSettings(prompts);
        userSettingRepository.save(setting);
    }

    private UserSetting createSettingForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found with id: " + userId));
        UserSetting setting = new UserSetting();
        setting.setUser(user);
        return setting;
    }
}

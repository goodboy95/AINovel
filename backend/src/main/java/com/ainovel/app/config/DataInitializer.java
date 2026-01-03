package com.ainovel.app.config;

import com.ainovel.app.material.model.Material;
import com.ainovel.app.material.repo.MaterialRepository;
import com.ainovel.app.settings.model.SystemSettings;
import com.ainovel.app.settings.repo.SystemSettingsRepository;
import com.ainovel.app.story.model.Story;
import com.ainovel.app.story.repo.StoryRepository;
import com.ainovel.app.user.User;
import com.ainovel.app.user.UserRepository;
import com.ainovel.app.world.model.World;
import com.ainovel.app.world.repo.WorldRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private WorldRepository worldRepository;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private SystemSettingsRepository systemSettingsRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Value("${app.ai.base-url:https://api.openai.com/v1}")
    private String defaultAiBaseUrl;
    @Value("${app.ai.model:gpt-4o}")
    private String defaultAiModel;
    @Value("${app.ai.api-key:}")
    private String defaultAiApiKey;

    @Override
    public void run(String... args) {
        User user = userRepository.findByUsername("admin").orElseGet(() -> {
            User u = new User();
            u.setUsername("admin");
            u.setEmail("admin@example.com");
            u.setPasswordHash(passwordEncoder.encode("password"));
            u.setRoles(Set.of("ROLE_USER"));
            return userRepository.save(u);
        });

        SystemSettings settings = systemSettingsRepository.findByUser(user).orElseGet(() -> {
            SystemSettings s = new SystemSettings();
            s.setUser(user);
            return s;
        });
        boolean settingsChanged = false;
        if (settings.getBaseUrl() == null || !settings.getBaseUrl().equals(defaultAiBaseUrl)) {
            settings.setBaseUrl(defaultAiBaseUrl);
            settingsChanged = true;
        }
        if (settings.getModelName() == null || !settings.getModelName().equals(defaultAiModel)) {
            settings.setModelName(defaultAiModel);
            settingsChanged = true;
        }
        if (defaultAiApiKey != null && !defaultAiApiKey.isBlank()
                && (settings.getApiKeyEncrypted() == null || !settings.getApiKeyEncrypted().equals(defaultAiApiKey))) {
            settings.setApiKeyEncrypted(defaultAiApiKey);
            settingsChanged = true;
        }
        if (settings.getId() == null || settingsChanged) {
            systemSettingsRepository.save(settings);
        }

        if (storyRepository.count() == 0) {
            Story story = new Story();
            story.setUser(user);
            story.setTitle("赛博侦探：霓虹雨");
            story.setSynopsis("在2077年的新东京，一名失去记忆的侦探试图找回过去");
            story.setGenre("科幻");
            story.setTone("阴郁");
            story.setStatus("draft");
            storyRepository.save(story);
        }

        if (worldRepository.count() == 0) {
            World world = new World();
            world.setUser(user);
            world.setName("新东京 2077");
            world.setTagline("高科技低生活");
            world.setStatus("active");
            world.setVersion("1.0.0");
            world.setModulesJson("{}");
            world.setModuleProgressJson("{}");
            worldRepository.save(world);
        }

        if (materialRepository.count() == 0) {
            Material material = new Material();
            material.setUser(user);
            material.setTitle("赛博义体型号大全");
            material.setType("text");
            material.setSummary("常见义体型号与特点");
            material.setContent("常见义体型号包含军用与民用两大类...");
            material.setTagsJson("[\"设定\",\"科技\"]");
            material.setStatus("approved");
            materialRepository.save(material);
        }
    }
}

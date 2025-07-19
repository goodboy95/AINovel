package com.ainovel.app.settings.repo;

import com.ainovel.app.settings.model.SystemSettings;
import com.ainovel.app.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, UUID> {
    Optional<SystemSettings> findByUser(User user);
}

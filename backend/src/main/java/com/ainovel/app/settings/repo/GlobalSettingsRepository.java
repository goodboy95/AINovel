package com.ainovel.app.settings.repo;

import com.ainovel.app.settings.model.GlobalSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GlobalSettingsRepository extends JpaRepository<GlobalSettings, UUID> {
    Optional<GlobalSettings> findTopByOrderByUpdatedAtDesc();
}


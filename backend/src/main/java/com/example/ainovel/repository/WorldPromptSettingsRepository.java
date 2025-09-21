package com.example.ainovel.repository;

import com.example.ainovel.model.world.WorldPromptSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorldPromptSettingsRepository extends JpaRepository<WorldPromptSettings, Long> {

    Optional<WorldPromptSettings> findByUserId(Long userId);
}

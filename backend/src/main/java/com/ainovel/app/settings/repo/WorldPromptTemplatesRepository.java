package com.ainovel.app.settings.repo;

import com.ainovel.app.settings.model.WorldPromptTemplatesEntity;
import com.ainovel.app.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WorldPromptTemplatesRepository extends JpaRepository<WorldPromptTemplatesEntity, UUID> {
    Optional<WorldPromptTemplatesEntity> findByUser(User user);
}

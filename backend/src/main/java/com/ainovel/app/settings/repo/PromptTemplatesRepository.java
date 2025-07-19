package com.ainovel.app.settings.repo;

import com.ainovel.app.settings.model.PromptTemplatesEntity;
import com.ainovel.app.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromptTemplatesRepository extends JpaRepository<PromptTemplatesEntity, UUID> {
    Optional<PromptTemplatesEntity> findByUser(User user);
}

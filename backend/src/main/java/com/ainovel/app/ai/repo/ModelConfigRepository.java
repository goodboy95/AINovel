package com.ainovel.app.ai.repo;

import com.ainovel.app.ai.model.ModelConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModelConfigRepository extends JpaRepository<ModelConfigEntity, UUID> {
    List<ModelConfigEntity> findByEnabledTrueOrderByDisplayNameAsc();
}


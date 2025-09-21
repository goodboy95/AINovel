package com.example.ainovel.repository;

import com.example.ainovel.model.world.WorldModule;
import com.example.ainovel.model.world.WorldModuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorldModuleRepository extends JpaRepository<WorldModule, Long> {

    List<WorldModule> findByWorldId(Long worldId);

    List<WorldModule> findByWorldIdIn(Collection<Long> worldIds);

    Optional<WorldModule> findByWorldIdAndModuleKey(Long worldId, String moduleKey);

    long countByWorldIdAndStatusIn(Long worldId, Collection<WorldModuleStatus> statuses);
}

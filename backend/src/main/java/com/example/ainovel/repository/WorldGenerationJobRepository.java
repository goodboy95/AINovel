package com.example.ainovel.repository;

import com.example.ainovel.model.world.WorldGenerationJob;
import com.example.ainovel.model.world.WorldGenerationJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorldGenerationJobRepository extends JpaRepository<WorldGenerationJob, Long> {

    @Query(value = "SELECT * FROM world_generation_jobs WHERE status = 'WAITING' ORDER BY sequence ASC, id ASC LIMIT 1 FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    Optional<WorldGenerationJob> fetchNextJobForUpdate();

    List<WorldGenerationJob> findByWorldIdOrderBySequenceAsc(Long worldId);

    long countByWorldIdAndStatusIn(Long worldId, Collection<WorldGenerationJobStatus> statuses);

    Optional<WorldGenerationJob> findByWorldIdAndModuleKey(Long worldId, String moduleKey);

    @Modifying
    @Query("DELETE FROM WorldGenerationJob j WHERE j.world.id = :worldId")
    void deleteByWorldId(@Param("worldId") Long worldId);
}

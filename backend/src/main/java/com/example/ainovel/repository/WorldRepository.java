package com.example.ainovel.repository;

import com.example.ainovel.model.world.World;
import com.example.ainovel.model.world.WorldStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorldRepository extends JpaRepository<World, Long> {

    Optional<World> findByIdAndUserId(Long id, Long userId);

    Optional<World> findByIdAndUserIdAndStatus(Long id, Long userId, WorldStatus status);

    List<World> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    List<World> findAllByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, WorldStatus status);
}

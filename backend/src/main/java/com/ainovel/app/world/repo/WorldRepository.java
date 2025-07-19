package com.ainovel.app.world.repo;

import com.ainovel.app.world.model.World;
import com.ainovel.app.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorldRepository extends JpaRepository<World, UUID> {
    List<World> findByUser(User user);
}

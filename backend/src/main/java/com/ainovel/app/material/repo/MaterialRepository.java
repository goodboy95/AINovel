package com.ainovel.app.material.repo;

import com.ainovel.app.material.model.Material;
import com.ainovel.app.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaterialRepository extends JpaRepository<Material, UUID> {
    List<Material> findByUser(User user);
}

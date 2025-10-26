package com.example.ainovel.repository.material;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ainovel.model.material.Material;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Long> {

    List<Material> findByWorkspaceId(Long workspaceId);

    List<Material> findByWorkspaceIdAndStatus(Long workspaceId, String status);
}

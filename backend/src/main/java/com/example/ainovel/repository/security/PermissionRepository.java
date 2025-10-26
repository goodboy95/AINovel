package com.example.ainovel.repository.security;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ainovel.model.security.Permission;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByResourceTypeAndResourceIdAndUserId(String resourceType, Long resourceId, Long userId);

    List<Permission> findByUserIdAndResourceType(Long userId, String resourceType);

    List<Permission> findByResourceTypeAndResourceId(String resourceType, Long resourceId);
}


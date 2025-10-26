package com.example.ainovel.service.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.ainovel.model.security.Permission;
import com.example.ainovel.repository.security.PermissionRepository;
import com.example.ainovel.security.PermissionLevel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Central service for ACL management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    public static final String RESOURCE_WORKSPACE = "WORKSPACE";

    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public boolean hasPermission(Long userId,
                                 String resourceType,
                                 Long resourceId,
                                 PermissionLevel requiredLevel,
                                 Map<String, Long> context) {
        if (userId == null || resourceId == null) {
            return false;
        }
        String normalizedType = normalizeResourceType(resourceType);
        PermissionLevel required = requiredLevel != null ? requiredLevel : PermissionLevel.READ;
        Optional<Permission> existing =
            permissionRepository.findByResourceTypeAndResourceIdAndUserId(normalizedType, resourceId, userId);
        if (existing.isPresent()) {
            return existing.get().getPermissionLevel().satisfies(required);
        }
        // Fallback to workspace-level permission when available.
        Long workspaceId = context != null ? context.get(RESOURCE_WORKSPACE) : null;
        if (workspaceId != null) {
            Optional<Permission> workspacePermission =
                permissionRepository.findByResourceTypeAndResourceIdAndUserId(RESOURCE_WORKSPACE, workspaceId, userId);
            if (workspacePermission.isPresent()) {
                PermissionLevel level = workspacePermission.get().getPermissionLevel();
                if (level.satisfies(required)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public void assertPermission(Long userId,
                                 String resourceType,
                                 Long resourceId,
                                 PermissionLevel requiredLevel,
                                 Map<String, Long> context) {
        if (!hasPermission(userId, resourceType, resourceId, requiredLevel, context)) {
            throw new AccessDeniedException("当前账号缺少访问权限");
        }
    }

    @Transactional
    public void grantPermission(Long userId,
                                String resourceType,
                                Long resourceId,
                                PermissionLevel level) {
        if (userId == null || resourceId == null || !StringUtils.hasText(resourceType)) {
            return;
        }
        String normalizedType = normalizeResourceType(resourceType);
        PermissionLevel normalized = level != null ? level : PermissionLevel.READ;
        Permission permission = permissionRepository
            .findByResourceTypeAndResourceIdAndUserId(normalizedType, resourceId, userId)
            .orElseGet(() -> {
                Permission entity = new Permission();
                entity.setResourceType(normalizedType);
                entity.setResourceId(resourceId);
                entity.setUserId(userId);
                return entity;
            });
        if (!permission.getPermissionLevel().satisfies(normalized)) {
            permission.setPermissionLevel(normalized);
        }
        permissionRepository.save(permission);
    }

    @Transactional
    public void ensureWorkspaceAdmin(Long userId, Long workspaceId) {
        grantPermission(userId, RESOURCE_WORKSPACE, workspaceId, PermissionLevel.ADMIN);
    }

    @Transactional
    public void syncMaterialOwnership(Long ownerId, Long materialId, Long workspaceId) {
        grantPermission(ownerId, "MATERIAL", materialId, PermissionLevel.ADMIN);
        if (workspaceId != null) {
            ensureWorkspaceAdmin(ownerId, workspaceId);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Set<PermissionLevel>> findPermissionsForWorkspace(Long workspaceId, Long userId) {
        if (workspaceId == null || userId == null) {
            return Collections.emptyMap();
        }
        Map<String, Set<PermissionLevel>> grouped = new ConcurrentHashMap<>();
        permissionRepository.findByResourceTypeAndResourceIdAndUserId(RESOURCE_WORKSPACE, workspaceId, userId)
            .ifPresent(permission -> grouped.put(
                RESOURCE_WORKSPACE + "_" + workspaceId,
                EnumSet.of(permission.getPermissionLevel())
            ));

        List<Permission> materialPermissions =
            permissionRepository.findByUserIdAndResourceType(userId, "MATERIAL");
        for (Permission permission : materialPermissions) {
            String key = "MATERIAL_" + permission.getResourceId();
            grouped.computeIfAbsent(key, k -> EnumSet.noneOf(PermissionLevel.class))
                .add(permission.getPermissionLevel());
        }
        return grouped;
    }

    @Transactional
    public void revokePermissionsForMaterial(Long materialId) {
        if (materialId == null) {
            return;
        }
        List<Permission> permissions =
            permissionRepository.findByResourceTypeAndResourceId("MATERIAL", materialId);
        if (!permissions.isEmpty()) {
            permissionRepository.deleteAll(permissions);
        }
    }

    private String normalizeResourceType(String resourceType) {
        return StringUtils.hasText(resourceType) ? resourceType.toUpperCase() : "";
    }
}

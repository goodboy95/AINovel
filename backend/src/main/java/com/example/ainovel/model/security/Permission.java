package com.example.ainovel.model.security;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.CreationTimestamp;

import com.example.ainovel.security.PermissionLevel;

import lombok.Getter;
import lombok.Setter;

/**
 * ACL permission record for a user and business resource.
 */
@Getter
@Setter
@Entity
@Table(name = "permissions",
    uniqueConstraints = @UniqueConstraint(name = "uk_permission_resource_user",
        columnNames = {"resource_type", "resource_id", "user_id"}),
    indexes = {
        @Index(name = "idx_permission_user", columnList = "user_id"),
        @Index(name = "idx_permission_resource", columnList = "resource_type, resource_id")
    })
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_level", nullable = false, length = 20)
    private PermissionLevel permissionLevel = PermissionLevel.READ;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}


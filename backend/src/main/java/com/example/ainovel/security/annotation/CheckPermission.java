package com.example.ainovel.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.example.ainovel.security.PermissionLevel;

/**
 * Marks service methods that require ACL checks before execution.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckPermission {

    /**
     * Business resource type, e.g. MATERIAL or WORKSPACE.
     */
    String resourceType();

    /**
     * Required permission level to execute the method.
     */
    PermissionLevel level() default PermissionLevel.READ;
}


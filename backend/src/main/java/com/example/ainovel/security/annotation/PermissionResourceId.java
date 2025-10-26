package com.example.ainovel.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method argument as the resource identifier used for ACL validation.
 * <p>
 * When {@code value} is empty, the enclosing {@link CheckPermission#resourceType()} is assumed.
 * Otherwise the specified resource type alias is used to match the annotated parameter to the check.
 * </p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface PermissionResourceId {

    /**
     * Optional explicit resource type that this parameter represents.
     */
    String value() default "";
}


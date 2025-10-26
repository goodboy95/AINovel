package com.example.ainovel.security;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.ainovel.model.User;
import com.example.ainovel.security.annotation.CheckPermission;
import com.example.ainovel.security.annotation.PermissionResourceId;
import com.example.ainovel.service.security.PermissionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles {@link CheckPermission} annotations via Spring AOP.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;

    @Around("@annotation(checkPermission)")
    public Object enforcePermission(ProceedingJoinPoint joinPoint, CheckPermission checkPermission) throws Throwable {
        Long userId = resolveCurrentUserId();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        String targetResourceType = checkPermission.resourceType().toUpperCase();
        Map<String, Long> context = new HashMap<>();
        Long resourceId = extractResourceId(method, args, targetResourceType, context);
        if (resourceId == null) {
            throw new AccessDeniedException("缺少资源标识，无法执行权限校验");
        }

        permissionService.assertPermission(userId,
            targetResourceType,
            resourceId,
            checkPermission.level(),
            context);

        return joinPoint.proceed();
    }

    private Long resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AccessDeniedException("未认证用户");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user && user.getId() != null) {
            return user.getId();
        }
        if (principal instanceof org.springframework.security.core.userdetails.User springUser) {
            log.debug("Spring security user without domain ID detected: {}", springUser.getUsername());
        }
        throw new AccessDeniedException("无法识别当前用户");
    }

    private Long extractResourceId(Method method,
                                   Object[] args,
                                   String targetResourceType,
                                   Map<String, Long> context) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        Long primaryId = null;
        for (int i = 0; i < parameterAnnotations.length; i++) {
            PermissionResourceId resourceIdAnnotation = findAnnotation(parameterAnnotations[i]);
            if (resourceIdAnnotation == null) {
                continue;
            }
            String alias = resourceIdAnnotation.value();
            String normalizedAlias = StringUtils.hasText(alias) ? alias.toUpperCase() : targetResourceType;
            Long value = convertToLong(args[i]);
            if (value == null) {
                continue;
            }
            context.put(normalizedAlias, value);
            if (Objects.equals(normalizedAlias, targetResourceType)) {
                primaryId = value;
            }
        }
        return primaryId;
    }

    private PermissionResourceId findAnnotation(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof PermissionResourceId resourceId) {
                return resourceId;
            }
        }
        return null;
    }

    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ex) {
                log.debug("Failed to convert parameter value to long: {}", str);
            }
        }
        return null;
    }
}


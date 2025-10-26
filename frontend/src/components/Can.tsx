import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import type { PermissionLevel, PermissionMap } from '../types';

const LEVEL_ORDER: Record<PermissionLevel, number> = {
    READ: 1,
    WRITE: 2,
    ADMIN: 3,
};

type CanProps = {
    perform: string;
    on?: number | string | null;
    fallback?: React.ReactNode;
    children: React.ReactNode;
};

const normalizeLevel = (level: string): PermissionLevel => {
    const upper = level.toUpperCase();
    if (upper === 'WRITE') return 'WRITE';
    if (upper === 'ADMIN') return 'ADMIN';
    return 'READ';
};

const hasLevel = (available: string[], required: PermissionLevel) => {
    return available.some(level => {
        const normalized = normalizeLevel(level);
        return LEVEL_ORDER[normalized] >= LEVEL_ORDER[required];
    });
};

const buildKeys = (resource: string, resourceId: string | number | null | undefined, workspaceId?: number | null) => {
    const keys: string[] = [];
    if (resource === 'WORKSPACE') {
        if (resourceId != null) {
            keys.push(`WORKSPACE_${resourceId}`);
        } else if (workspaceId != null) {
            keys.push(`WORKSPACE_${workspaceId}`);
        }
        return keys;
    }
    if (resourceId != null) {
        keys.push(`${resource}_${resourceId}`);
    }
    if (workspaceId != null) {
        keys.push(`WORKSPACE_${workspaceId}`);
    }
    return keys;
};

const checkPermission = (
    permissions: PermissionMap,
    workspaceId: number | null | undefined,
    perform: string,
    on?: string | number | null,
) => {
    if (!permissions) return false;
    const [resourceRaw, levelRaw] = perform.split(':');
    const resource = (resourceRaw ?? '').trim().toUpperCase();
    const requiredLevel = normalizeLevel(levelRaw ?? 'READ');
    const candidateKeys = buildKeys(resource, on ?? null, workspaceId);
    for (const key of candidateKeys) {
        const available = permissions[key];
        if (available && hasLevel(available, requiredLevel)) {
            return true;
        }
    }
    return false;
};

const Can: React.FC<CanProps> = ({ perform, on = null, fallback = null, children }) => {
    const { permissions, workspaceId } = useAuth();
    const allowed = checkPermission(permissions, workspaceId ?? null, perform, on);
    if (!allowed) {
        return <>{fallback}</>;
    }
    return <>{children}</>;
};

export default Can;

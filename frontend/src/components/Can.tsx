import React from 'react';
import { useCanPerform } from '../hooks/useCanPerform';

type CanProps = {
    perform: string;
    on?: number | string | null;
    fallback?: React.ReactNode;
    children: React.ReactNode;
};

const Can: React.FC<CanProps> = ({ perform, on = null, fallback = null, children }) => {
    const allowed = useCanPerform(perform, on);
    if (!allowed) {
        return <>{fallback}</>;
    }
    return <>{children}</>;
};

export default Can;

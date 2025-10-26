import { useCallback, useEffect, useRef, useState } from 'react';
import { getAutoHints } from '../services/api';
import type { MaterialSearchResult } from '../types';

const MIN_LENGTH = 80;
const DEBOUNCE_MS = 1500;

export const useAutoSuggestions = (contextText: string, workspaceId?: number | null, limit = 6) => {
    const [suggestions, setSuggestions] = useState<MaterialSearchResult[]>([]);
    const [loading, setLoading] = useState(false);
    const debounceTimer = useRef<number | null>(null);
    const lastRequestId = useRef(0);

    const cancelPending = useCallback(() => {
        if (debounceTimer.current) {
            window.clearTimeout(debounceTimer.current);
            debounceTimer.current = null;
        }
    }, []);

    useEffect(() => {
        if (!contextText || contextText.trim().length < MIN_LENGTH) {
            cancelPending();
            setSuggestions([]);
            setLoading(false);
            return;
        }

        cancelPending();
        setLoading(true);
        const payloadText = contextText.length > 600 ? contextText.slice(-600) : contextText;
        const requestId = ++lastRequestId.current;

        debounceTimer.current = window.setTimeout(async () => {
            try {
                const results = await getAutoHints({ text: payloadText, workspaceId, limit });
                if (lastRequestId.current === requestId) {
                    setSuggestions(results ?? []);
                }
            } catch (error) {
                if (lastRequestId.current === requestId) {
                    console.warn('自动素材建议失败', error);
                    setSuggestions([]);
                }
            } finally {
                if (lastRequestId.current === requestId) {
                    setLoading(false);
                }
            }
        }, DEBOUNCE_MS);

        return () => {
            cancelPending();
        };
    }, [contextText, workspaceId, limit, cancelPending]);

    return { suggestions, loading, cancelPending };
};


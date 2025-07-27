import { useState, useCallback } from 'react';
import { refineText } from '../services/api';
import type { RefineContext } from '../types';

/**
 * Custom hook for managing the AI text refinement modal.
 * @returns An object containing state and handler functions for the modal.
 */
export const useRefineModal = () => {
    const [isModalVisible, setIsModalVisible] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [originalText, setOriginalText] = useState('');
    const [refinedText, setRefinedText] = useState('');
    const [context, setContext] = useState<RefineContext | null>(null);

    /**
     * Opens the refinement modal with the given text and context.
     * @param {string} text - The original text to be refined.
     * @param {RefineContext} ctx - The context for the refinement, including endpoint and success callback.
     */
    const openModal = useCallback((text: string, ctx: RefineContext) => {
        setOriginalText(text);
        setRefinedText('');
        setContext(ctx);
        setError(null);
        setIsModalVisible(true);
    }, []);

    /**
     * Closes the refinement modal and resets its state.
     */
    const closeModal = useCallback(() => {
        setIsModalVisible(false);
        setContext(null);
    }, []);

    /**
     * Sends the text and user feedback to the server for refinement.
     * @param {string} userFeedback - Optional user feedback to guide the refinement.
     */
    const handleRefine = useCallback(async (userFeedback: string) => {
        if (!context) return;
        setIsLoading(true);
        setError(null);
        try {
            const payload = {
                originalText,
                userFeedback,
                fieldName: context.fieldName,
            };
            const data = await refineText(context.endpoint, payload);
            setRefinedText(data.refinedText);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to refine text.');
        } finally {
            setIsLoading(false);
        }
    }, [context, originalText]);

    /**
     * Accepts the refined text, calls the success callback, and closes the modal.
     */
    const acceptRefinement = useCallback(() => {
        if (context && refinedText) {
            context.onSuccess(refinedText);
        }
        closeModal();
    }, [context, refinedText, closeModal]);

    return {
        isModalVisible,
        isLoading,
        error,
        originalText,
        refinedText,
        openModal,
        closeModal,
        handleRefine,
        acceptRefinement,
    };
};
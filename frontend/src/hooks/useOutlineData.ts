import { useState, useCallback } from 'react';
import {
    fetchOutlinesForStory,
    generateOutline as apiGenerateOutline,
    updateOutline as apiUpdateOutline,
    deleteOutline as apiDeleteOutline,
    fetchOutlineDetails,
} from '../services/api';
import type { Outline } from '../types';

/**
 * Custom hook for managing outline data for a specific story.
 * @param {string | null} storyId - The ID of the story for which to manage outlines.
 * @returns An object containing state and handler functions for outlines.
 */
export const useOutlineData = (storyId: string | null) => {
    const [outlines, setOutlines] = useState<Outline[]>([]);
    const [selectedOutline, setSelectedOutline] = useState<Outline | null>(null);
    const [generatedOutline, setGeneratedOutline] = useState<Outline | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    /**
     * Fetches the list of outlines for the currently selected story.
     */
    const loadOutlines = useCallback(async () => {
        if (!storyId) {
            setOutlines([]);
            return;
        }
        setIsLoading(true);
        setError(null);
        try {
            const data = await fetchOutlinesForStory(storyId);
            setOutlines(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to fetch outlines.');
        } finally {
            setIsLoading(false);
        }
    }, [storyId]);

    /**
     * Generates a new outline for the selected story.
     * @param {{ numberOfChapters: number; pointOfView: string; }} params - The parameters for outline generation.
     */
    const generateOutline = useCallback(async (params: { numberOfChapters: number; pointOfView: string; }) => {
        if (!storyId) {
            setError('A story must be selected to generate an outline.');
            return;
        }
        setIsLoading(true);
        setError(null);
        try {
            const newOutline = await apiGenerateOutline({ storyCardId: storyId, ...params });
            setGeneratedOutline(newOutline);
            await loadOutlines(); // Refresh the list
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to generate outline.');
        } finally {
            setIsLoading(false);
        }
    }, [storyId, loadOutlines]);

    /**
     * Updates an existing outline.
     * @param {Outline} outlineData - The outline data to update.
     */
    const updateOutline = useCallback(async (outlineData: Outline) => {
        setIsLoading(true);
        setError(null);
        try {
            const updatedOutline = await apiUpdateOutline(outlineData);
            setOutlines(prev => prev.map(o => o.id === updatedOutline.id ? { ...o, ...updatedOutline } : o));
            if (selectedOutline?.id === updatedOutline.id) {
                setSelectedOutline(updatedOutline);
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to update outline.');
        } finally {
            setIsLoading(false);
        }
    }, [selectedOutline]);

    /**
     * Deletes an outline.
     * @param {number} outlineId - The ID of the outline to delete.
     */
    const deleteOutline = useCallback(async (outlineId: number) => {
        setIsLoading(true);
        setError(null);
        try {
            await apiDeleteOutline(outlineId);
            setOutlines(prev => prev.filter(o => o.id !== outlineId));
            if (selectedOutline?.id === outlineId) {
                setSelectedOutline(null);
            }
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to delete outline.');
        } finally {
            setIsLoading(false);
        }
    }, [selectedOutline]);

    /**
     * Sets the currently selected outline.
     * @param {Outline | null} outline - The outline to select, or null to deselect.
     */
    const selectOutline = useCallback((outline: Outline | null) => {
        setSelectedOutline(outline);
        setGeneratedOutline(null); // Clear generated outline when selecting an existing one
    }, []);

    /**
     * Fetches the full details of an outline, intended for the manuscript writing view.
     * @param {number} outlineId - The ID of the outline to fetch.
     * @returns {Promise<Outline | null>} The detailed outline or null on failure.
     */
    const getOutlineForWriting = useCallback(async (outlineId: number): Promise<Outline | null> => {
        setIsLoading(true);
        setError(null);
        try {
            const outlineDetails = await fetchOutlineDetails(outlineId);
            return outlineDetails;
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to fetch outline details.');
            return null;
        } finally {
            setIsLoading(false);
        }
    }, []);


    return {
        outlines,
        selectedOutline,
        generatedOutline,
        isLoading,
        error,
        loadOutlines,
        generateOutline,
        updateOutline,
        deleteOutline,
        selectOutline,
        getOutlineForWriting,
        setOutlines,
        setSelectedOutline,
    };
};
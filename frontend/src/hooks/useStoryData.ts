import { useState, useCallback } from 'react';
import {
    fetchStoryList,
    fetchStoryDetails,
    createStoryConception,
    updateStoryCard as apiUpdateStoryCard,
    createCharacterCard as apiCreateCharacter,
    updateCharacterCard as apiUpdateCharacter,
    deleteCharacterCard as apiDeleteCharacter,
    createStory as apiCreateStory,
} from '../services/api';
import type { StoryCard, CharacterCard, ConceptionFormValues } from '../types';

/**
 * Custom hook for managing story and character data.
 * Encapsulates state and logic for fetching, creating, updating, and deleting stories and characters.
 * @returns An object containing state and handler functions.
 */
export const useStoryData = () => {
    const [storyList, setStoryList] = useState<StoryCard[]>([]);
    const [selectedStory, setSelectedStory] = useState<StoryCard | null>(null);
    const [characterCards, setCharacterCards] = useState<CharacterCard[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    /**
     * Fetches the list of all stories from the server.
     */
    const loadStoryList = useCallback(async () => {
        setIsLoading(true);
        setError(null);
        try {
            const data = await fetchStoryList();
            setStoryList(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to fetch story list.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    /**
     * Fetches the details of a specific story and its characters.
     * @param {number} storyId - The ID of the story to view.
     * @returns {Promise<StoryCard | null>} The selected story card or null if not found.
     */
    const viewStory = useCallback(async (storyId: number): Promise<StoryCard | null> => {
        setIsLoading(true);
        setError(null);
        try {
            const { storyCard, characterCards } = await fetchStoryDetails(storyId);
            setSelectedStory(storyCard);
            setCharacterCards(characterCards);
            return storyCard; // Return the story card to allow navigation
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to fetch story details.');
            return null;
        } finally {
            setIsLoading(false);
        }
    }, []);

    /**
     * Generates a new story based on the provided conception values.
     * @param {ConceptionFormValues} values - The form values for story conception.
     * @returns {Promise<StoryCard | null>} The newly created story card or null on failure.
     */
    const generateStory = useCallback(async (values: ConceptionFormValues): Promise<StoryCard | null> => {
        setIsLoading(true);
        setError(null);
        try {
            const { storyCard, characterCards } = await createStoryConception(values);
            setSelectedStory(storyCard);
            setCharacterCards(characterCards);
            await loadStoryList(); // Refresh list
            return storyCard;
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to generate story.');
            return null;
        } finally {
            setIsLoading(false);
        }
    }, [loadStoryList]);

    /**
     * Updates an existing story card.
     * @param {StoryCard} storyData - The story data to update.
     */
    const updateStory = useCallback(async (storyData: StoryCard) => {
        setIsLoading(true);
        setError(null);
        try {
            const updatedStory = await apiUpdateStoryCard(storyData);
            setSelectedStory(updatedStory);
            await loadStoryList();
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to update story.');
        } finally {
            setIsLoading(false);
        }
    }, [loadStoryList]);

    /**
     * Manually create a new story.
     * @param payload - Basic fields for a new story.
     * @returns The created StoryCard or null on failure.
     */
    const createStory = useCallback(async (payload: { title: string; synopsis: string; genre: string; tone: string; worldId?: number | null; }): Promise<StoryCard | null> => {
        setIsLoading(true);
        setError(null);
        try {
            const newStory = await apiCreateStory(payload);
            setSelectedStory(newStory);
            await loadStoryList();
            return newStory;
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to create story.');
            return null;
        } finally {
            setIsLoading(false);
        }
    }, [loadStoryList]);

    /**
     * Creates a new character for a given story.
     * @param {number} storyId - The ID of the story to associate the character with.
     * @param {Omit<CharacterCard, 'id'>} characterData - The data for the new character.
     */
    const createCharacter = useCallback(async (storyId: number, characterData: Omit<CharacterCard, 'id'>) => {
        setIsLoading(true);
        setError(null);
        try {
            const newCharacter = await apiCreateCharacter(storyId, characterData);
            setCharacterCards(prev => [...prev, newCharacter]);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to create character.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    /**
     * Updates an existing character.
     * @param {CharacterCard} characterData - The character data to update.
     */
    const updateCharacter = useCallback(async (characterData: CharacterCard) => {
        setIsLoading(true);
        setError(null);
        try {
            const updatedCharacter = await apiUpdateCharacter(characterData);
            setCharacterCards(prev => prev.map(c => c.id === updatedCharacter.id ? updatedCharacter : c));
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to update character.');
        } finally {
            setIsLoading(false);
        }
    }, []);

    /**
     * Deletes a character.
     * @param {number} characterId - The ID of the character to delete.
     */
    const deleteCharacter = useCallback(async (characterId: number) => {
        setIsLoading(true);
        setError(null);
        try {
            await apiDeleteCharacter(characterId);
            setCharacterCards(prev => prev.filter(c => c.id !== characterId));
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Failed to delete character.');
        } finally {
            setIsLoading(false);
        }
    }, []);


    return {
        storyList,
        selectedStory,
        characterCards,
        isLoading,
        error,
        loadStoryList,
        viewStory,
        generateStory,
        updateStory,
        createStory,
        createCharacter,
        updateCharacter,
        deleteCharacter,
        setSelectedStory,
        setCharacterCards,
    };
};
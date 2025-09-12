import type { StoryCard, CharacterCard, Outline, ConceptionFormValues, Chapter } from '../types';

/**
 * Creates authorization headers for API requests.
 * Retrieves the JWT token from local storage.
 * @returns {HeadersInit} The headers object.
 */
const getAuthHeaders = (): HeadersInit => {
    const token = localStorage.getItem('token');
    const headers: HeadersInit = {
        'Content-Type': 'application/json',
    };
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return headers;
};

/**
 * Handles the response from a fetch request.
 * Checks for errors and parses the JSON response.
 * @param {Response} response - The response object from the fetch call.
 * @returns {Promise<any>} The parsed JSON data.
 * @throws {Error} If the response is not ok.
 */
const handleResponse = async <T>(response: Response): Promise<T> => {
    if (!response.ok) {
        const errorData = await response.json().catch(() => ({ message: 'An unknown error occurred.' }));
        console.error('API request failed:', response.status, errorData);
        throw new Error(errorData.message || `Request failed with status ${response.status}`);
    }
    return response.json() as Promise<T>;
};

// Story Card APIs
/**
 * Fetches the list of all story cards.
 * @returns {Promise<StoryCard[]>} A promise that resolves to an array of story cards.
 */
export const fetchStoryList = (): Promise<StoryCard[]> => {
    return fetch('/api/v1/story-cards', { headers: getAuthHeaders() }).then(res => handleResponse<StoryCard[]>(res));
};

/**
 * Fetches the details for a specific story, including its character cards.
 * @param {number} storyId - The ID of the story to fetch.
 * @returns {Promise<{ storyCard: StoryCard; characterCards: CharacterCard[] }>} A promise that resolves to the story and character data.
 */
export const fetchStoryDetails = async (storyId: number): Promise<{ storyCard: StoryCard; characterCards: CharacterCard[] }> => {
    const storyCard = await fetch(`/api/v1/story-cards/${storyId}`, { headers: getAuthHeaders() }).then(res => handleResponse<StoryCard>(res));
    const characterCards = await fetch(`/api/v1/story-cards/${storyId}/character-cards`, { headers: getAuthHeaders() }).then(res => handleResponse<CharacterCard[]>(res));
    return { storyCard, characterCards };
};

/**
 * Creates a new story conception, including the main story card and associated character cards.
 * @param {ConceptionFormValues} values - The initial idea, genre, and tone for the story.
 * @returns {Promise<{ storyCard: StoryCard; characterCards: CharacterCard[] }>} A promise that resolves to the newly created story data.
 */
export const createStoryConception = (values: ConceptionFormValues): Promise<{ storyCard: StoryCard; characterCards: CharacterCard[] }> => {
    return fetch('/api/v1/conception', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(values),
    }).then(res => handleResponse<{ storyCard: StoryCard; characterCards: CharacterCard[] }>(res));
};

/**
 * Updates an existing story card.
 * @param {StoryCard} storyCard - The story card data to update.
 * @returns {Promise<StoryCard>} A promise that resolves to the updated story card.
 */
export const updateStoryCard = (storyCard: StoryCard): Promise<StoryCard> => {
    return fetch(`/api/v1/story-cards/${storyCard.id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(storyCard),
    }).then(res => handleResponse<StoryCard>(res));
};
// Create a new story (manual creation)
export const createStory = (payload: { title: string; synopsis: string; genre: string; tone: string; }): Promise<StoryCard> => {
    return fetch('/api/v1/stories', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload),
    }).then(res => handleResponse<StoryCard>(res));
};

// Fetch characters for a given story (for multi-select options)
export const fetchCharactersForStory = (storyId: number): Promise<CharacterCard[]> => {
    return fetch(`/api/v1/story-cards/${storyId}/character-cards`, { headers: getAuthHeaders() })
        .then(res => handleResponse<CharacterCard[]>(res));
};

// Character Card APIs
/**
 * Creates a new character card for a given story.
 * @param {number} storyId - The ID of the story to associate the character with.
 * @param {Omit<CharacterCard, 'id'>} characterData - The data for the new character.
 * @returns {Promise<CharacterCard>} A promise that resolves to the newly created character card.
 */
export const createCharacterCard = (storyId: number, characterData: Omit<CharacterCard, 'id'>): Promise<CharacterCard> => {
    return fetch(`/api/v1/story-cards/${storyId}/characters`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(characterData),
    }).then(res => handleResponse<CharacterCard>(res));
};

/**
 * Updates an existing character card.
 * @param {CharacterCard} characterCard - The character card data to update.
 * @returns {Promise<CharacterCard>} A promise that resolves to the updated character card.
 */
export const updateCharacterCard = (characterCard: CharacterCard): Promise<CharacterCard> => {
    return fetch(`/api/v1/character-cards/${characterCard.id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(characterCard),
    }).then(res => handleResponse<CharacterCard>(res));
};

/**
 * Deletes a character card.
 * @param {number} characterId - The ID of the character to delete.
 * @returns {Promise<void>} A promise that resolves when the character is deleted.
 */
export const deleteCharacterCard = (characterId: number): Promise<void> => {
    return fetch(`/api/v1/character-cards/${characterId}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
    }).then(res => handleResponse<void>(res));
};

// Outline APIs
/**
 * Fetches all outlines associated with a specific story.
 * @param {string} storyId - The ID of the story.
 * @returns {Promise<Outline[]>} A promise that resolves to an array of outlines.
 */
export const fetchOutlinesForStory = (storyId: string): Promise<Outline[]> => {
    return fetch(`/api/v1/story-cards/${storyId}/outlines`, { headers: getAuthHeaders() }).then(res => handleResponse<Outline[]>(res));
};

/**
 * Creates a new, empty outline for a specific story.
 * @param {string} storyId - The ID of the story.
 * @returns {Promise<Outline>} A promise that resolves to the newly created outline.
 */
export const createEmptyOutlineForStory = (storyId: string): Promise<Outline> => {
    return fetch(`/api/v1/story-cards/${storyId}/outlines`, {
        method: 'POST',
        headers: getAuthHeaders(),
    }).then(res => handleResponse<Outline>(res));
};

/**
 * @deprecated As of V2, this function is deprecated. Use generateChapter instead.
 * Generates a new outline for a story.
 * @param {{ storyCardId: string; numberOfChapters: number; pointOfView: string; }} params - The parameters for outline generation.
 * @returns {Promise<Outline>} A promise that resolves to the newly generated outline.
 */
export const generateOutline = (params: { storyCardId: string; numberOfChapters: number; pointOfView: string; }): Promise<Outline> => {
    return fetch('/api/v1/outlines', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(params),
    }).then(res => handleResponse<Outline>(res));
};

/**
 * Generates a single chapter for a given outline.
 * @param {number} outlineId - The ID of the outline.
 * @param {{ chapterNumber: number; sectionsPerChapter: number; wordsPerSection: number; }} params - The parameters for chapter generation.
 * @returns {Promise<Chapter>} A promise that resolves to the newly generated chapter.
 */
export const generateChapter = (outlineId: number, params: { chapterNumber: number; sectionsPerChapter: number; wordsPerSection: number; }): Promise<Chapter> => {
    return fetch(`/api/v1/outlines/${outlineId}/chapters`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(params),
    }).then(res => handleResponse<Chapter>(res));
};

/**
 * Fetches the full details of a specific outline.
 * @param {number} outlineId - The ID of the outline to fetch.
 * @returns {Promise<Outline>} A promise that resolves to the detailed outline data.
 */
export const fetchOutlineDetails = (outlineId: number): Promise<Outline> => {
    return fetch(`/api/v1/outlines/${outlineId}`, { headers: getAuthHeaders() }).then(res => handleResponse<Outline>(res));
};

/**
 * Updates an existing outline.
 * @param {Outline} outline - The outline data to update.
 * @returns {Promise<Outline>} A promise that resolves to the updated outline.
 */
export const updateOutline = (outline: Outline): Promise<Outline> => {
    return fetch(`/api/v1/outlines/${outline.id}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify(outline),
    }).then(res => handleResponse<Outline>(res));
};

/**
 * Deletes an outline.
 * @param {number} outlineId - The ID of the outline to delete.
 * @returns {Promise<void>} A promise that resolves when the outline is deleted.
 */
export const deleteOutline = (outlineId: number): Promise<void> => {
    return fetch(`/api/v1/outlines/${outlineId}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
    }).then(res => handleResponse<void>(res));
};

// Refine API
/**
 * Sends text to the AI for refinement.
 * @param {string} endpoint - The API endpoint to use for refinement.
 * @param {{ originalText: string; userFeedback: string; fieldName: string }} payload - The data for the refinement request.
 * @returns {Promise<{ refinedText: string }>} A promise that resolves to the refined text.
 */
export const refineText = (
    payload: { text: string; instruction: string; contextType: string }
): Promise<{ refinedText: string }> => {
    return fetch('/api/v1/ai/refine-text', {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload),
    }).then(res => handleResponse<{ refinedText: string }>(res));
};
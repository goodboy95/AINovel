export interface Scene {
    id: number;
    sceneNumber: number;
    synopsis: string;
    expectedWords: number;
}

export interface Chapter {
    id: number;
    chapterNumber: number;
    title: string;
    synopsis: string;
    scenes: Scene[];
}

export interface Outline {
    id: number;
    title: string;
    pointOfView: string;
    chapters: Chapter[];
    storyCard?: StoryCard; // Optional because it might not always be loaded
}

export interface StoryCard {
    id: number;
    title: string;
    synopsis: string;
    storyArc: string;
    genre: string;
    tone: string;
}

export interface ManuscriptSection {
    id: number;
    content: string;
    version: number;
    active: boolean;
    scene: {
        id: number;
    };
}

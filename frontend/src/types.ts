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
}

export interface StoryCard {
    id: number;
    title: string;
}

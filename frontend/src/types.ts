export interface TemporaryCharacter {
  id: number;
  name: string;
  summary: string;
  details: string;
  relationships: string;
  statusInScene: string;
  moodInScene: string;
  actionsInScene: string;
}

export interface Scene {
    id: number;
    sceneNumber: number;
    synopsis: string;
    expectedWords: number;
    presentCharacters?: string;
    characterStates?: string;
    temporaryCharacters?: TemporaryCharacter[]; // 新增：临时人物
    content?: string; // 正文内容
}

export interface Chapter {
    id: number;
    chapterNumber: number;
    title: string;
    synopsis: string;
    scenes: Scene[];
    settings?: { // 新增
        sectionsPerChapter?: number;
        wordsPerSection?: number;
    };
}

export interface Outline {
    id: number;
    title: string;
    pointOfView: string;
    chapters: Chapter[];
    storySynopsis?: string; // Add optional story synopsis
    storyCard?: StoryCard; // Optional because it might not always be loaded
    created_at: string; // Add created_at for historical view
}

export interface StoryCard {
    id: number;
    title: string;
    synopsis: string;
    storyArc: string;
    genre: string;
    tone: string;
}

export interface CharacterCard {
    id: number;
    name: string;
    synopsis: string;
    details: string;
    relationships: string;
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

export interface ConceptionFormValues {
    idea: string;
    genre: string;
    tone: string;
}

export interface RefineContext {
    endpoint: string;
    fieldName: string;
    onSuccess: (newText: string) => void;
}

export type RefineHandler = (text: string, context: RefineContext) => void;

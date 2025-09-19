export interface TemporaryCharacter {
  id: number;
  name: string;
  summary: string;
  details: string;
  relationships: string;
  status: string;
  thought: string;
  action: string;
}

export interface SceneCharacter {
  id?: number;
  characterCardId?: number;
  characterName: string;
  status: string;
  thought: string;
  action: string;
}

export interface Scene {
    id: number;
    sceneNumber: number;
    synopsis: string;
    expectedWords: number;
    presentCharacterIds?: number[]; // 多选角色ID数组
    presentCharacters?: string; // 兼容旧数据：以逗号分隔的人名
    sceneCharacters?: SceneCharacter[];
    temporaryCharacters?: TemporaryCharacter[]; // 新增：临时人物
    content?: string; // 正文内容
}

export interface Chapter {
    id: number;
    chapterNumber: number;
    title: string;
    synopsis: string;
    scenes: Scene[];
    wordCount?: number; // 预定字数（可选）
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

/**
 * Manuscript DTO from backend.
 * Aligns with backend ManuscriptDto fields.
 */
export interface Manuscript {
    id: number;
    title: string;
    outlineId: number;
    createdAt: string;
    updatedAt: string;
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
    /**
     * Backend field is "isActive" (boolean). Keep "active" for compatibility.
     */
    active?: boolean;
    /**
     * Transient scene object for backward-compat; may be absent.
     */
    scene?: {
        id: number;
    };
    /**
     * New canonical reference for scene link on backend model.
     */
    sceneId?: number;
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

export interface RelationshipChange {
    targetCharacterId: number;
    previousRelationship?: string | null;
    currentRelationship?: string | null;
    changeReason?: string | null;
}

export interface CharacterChangeLog {
    id: number;
    characterId: number;
    characterName: string;
    manuscriptId: number;
    outlineId?: number | null;
    chapterNumber: number;
    sectionNumber: number;
    newlyKnownInfo?: string | null;
    characterChanges?: string | null;
    characterDetailsAfter: string;
    isAutoCopied: boolean;
    relationshipChanges: RelationshipChange[];
    isTurningPoint: boolean;
    createdAt: string;
    updatedAt: string;
}

export interface CharacterDialogueRequestPayload {
    characterId: number;
    manuscriptId: number;
    currentSceneDescription?: string;
    dialogueTopic?: string;
}

export interface CharacterDialogueResponsePayload {
    dialogue: string;
}

export type RefineHandler = (text: string, context: RefineContext) => void;

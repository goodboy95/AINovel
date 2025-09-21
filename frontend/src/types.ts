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
    tags: string[];
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

export interface PromptTemplateItem {
    content: string;
    default: boolean;
}

export interface RefinePromptTemplateGroup {
    withInstruction: PromptTemplateItem;
    withoutInstruction: PromptTemplateItem;
}

export interface PromptTemplatesResponse {
    storyCreation: PromptTemplateItem;
    outlineChapter: PromptTemplateItem;
    manuscriptSection: PromptTemplateItem;
    refine: RefinePromptTemplateGroup;
}

export interface PromptTemplatesUpdatePayload {
    storyCreation?: string;
    outlineChapter?: string;
    manuscriptSection?: string;
    refine?: {
        withInstruction?: string;
        withoutInstruction?: string;
    };
}

export interface PromptVariableMetadata {
    name: string;
    valueType: string;
    description: string;
}

export interface PromptTypeMetadata {
    type: string;
    label: string;
    variables: PromptVariableMetadata[];
}

export interface PromptFunctionMetadata {
    name: string;
    description: string;
    usage: string;
}

export interface WorldFieldDefinition {
    key: string;
    label: string;
    description: string;
    tooltip: string;
    validation: string;
    recommendedLength?: string;
    aiFocus?: string;
}

export interface WorldBasicInfoDefinition {
    description: string;
    fields: WorldFieldDefinition[];
}

export interface WorldModuleDefinition {
    key: string;
    label: string;
    description?: string;
    fields: WorldFieldDefinition[];
    aiGenerationTemplate: string;
    finalTemplate: string;
}

export interface WorldFieldRefineTemplate {
    description: string;
    template: string;
    usageNotes?: string[];
}

export interface WorldPromptContextDefinition {
    description: string;
    example: string;
    notes: string[];
}

export interface WorldBuildingDefinitionsResponse {
    basicInfo: WorldBasicInfoDefinition;
    modules: WorldModuleDefinition[];
    fieldRefineTemplate: WorldFieldRefineTemplate;
    promptContext: WorldPromptContextDefinition;
}

export type WorldStatus = 'DRAFT' | 'GENERATING' | 'ACTIVE' | 'ARCHIVED';

export type WorldModuleStatus =
    | 'EMPTY'
    | 'IN_PROGRESS'
    | 'READY'
    | 'AWAITING_GENERATION'
    | 'GENERATING'
    | 'COMPLETED'
    | 'FAILED';

export type WorldGenerationJobStatus =
    | 'WAITING'
    | 'RUNNING'
    | 'SUCCEEDED'
    | 'FAILED'
    | 'CANCELLED';

export interface WorldSummary {
    id: number;
    name: string;
    tagline: string;
    themes: string[];
    status: WorldStatus;
    version?: number | null;
    updatedAt?: string | null;
    publishedAt?: string | null;
    moduleProgress: Record<string, WorldModuleStatus>;
}

export interface WorldBasicInfo {
    id?: number;
    name: string;
    tagline: string;
    themes: string[];
    creativeIntent: string;
    notes?: string | null;
    status: WorldStatus;
    version?: number | null;
    publishedAt?: string | null;
    updatedAt?: string | null;
    createdAt?: string | null;
    lastEditedAt?: string | null;
}

export interface WorldModule {
    key: string;
    label: string;
    status: WorldModuleStatus;
    fields: Record<string, string>;
    contentHash?: string | null;
    fullContent?: string | null;
    fullContentUpdatedAt?: string | null;
}

export interface WorldDetail {
    world: WorldBasicInfo;
    modules: WorldModule[];
}

export interface WorldModuleSummary {
    key: string;
    label: string;
}

export interface WorldPublishPreviewMissingField {
    moduleKey: string;
    moduleLabel: string;
    fieldKey: string;
    fieldLabel: string;
}

export interface WorldPublishPreview {
    ready: boolean;
    moduleStatuses: Record<string, WorldModuleStatus>;
    missingFields: WorldPublishPreviewMissingField[];
    modulesToGenerate: WorldModuleSummary[];
    modulesToReuse: WorldModuleSummary[];
}

export interface WorldPublishResponse {
    worldId: number;
    modulesToGenerate: WorldModuleSummary[];
    modulesToReuse: WorldModuleSummary[];
}

export interface WorldGenerationJobStatusEntry {
    moduleKey: string;
    moduleLabel: string;
    status: WorldGenerationJobStatus;
    attempts?: number | null;
    startedAt?: string | null;
    finishedAt?: string | null;
    error?: string | null;
}

export interface WorldGenerationStatus {
    worldId: number;
    status: WorldStatus;
    version?: number | null;
    queue: WorldGenerationJobStatusEntry[];
}

export interface PromptTemplateMetadata {
    templates: PromptTypeMetadata[];
    functions: PromptFunctionMetadata[];
    syntaxTips: string[];
    examples: string[];
}

export interface WorldPromptTemplateItem {
    content: string;
    defaultTemplate: boolean;
}

export interface WorldPromptTemplatesResponse {
    modules: Record<string, WorldPromptTemplateItem>;
    finalTemplates: Record<string, WorldPromptTemplateItem>;
    fieldRefine: WorldPromptTemplateItem;
}

export interface WorldPromptTemplatesUpdatePayload {
    modules?: Record<string, string>;
    finalTemplates?: Record<string, string>;
    fieldRefine?: string | null;
}

export interface WorldPromptTemplatesResetPayload {
    keys: string[];
}

export interface WorldPromptModuleFieldMetadata {
    key: string;
    label: string;
    recommendedLength?: string;
}

export interface WorldPromptModuleMetadata {
    key: string;
    label: string;
    fields: WorldPromptModuleFieldMetadata[];
}

export interface WorldPromptVariableMetadata {
    name: string;
    valueType: string;
    description: string;
}

export interface WorldPromptFunctionMetadata {
    name: string;
    description: string;
    usage: string;
}

export interface WorldPromptMetadata {
    modules: WorldPromptModuleMetadata[];
    variables: WorldPromptVariableMetadata[];
    functions: WorldPromptFunctionMetadata[];
    examples: string[];
}

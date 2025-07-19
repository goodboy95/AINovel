export interface User {
  id: string;
  username: string;
  email: string;
  avatar?: string;
}

export interface Story {
  id: string;
  title: string;
  synopsis: string;
  genre: string;
  tone: string;
  status: 'draft' | 'published' | 'archived';
  updatedAt: string;
   worldId?: string;
  cover?: string;
}

export interface Character {
  id: string;
  name: string;
  role: string;
  archetype: string;
  summary: string;
}

export interface World {
  id: string;
  name: string;
  tagline: string;
  status: 'draft' | 'generating' | 'active' | 'archived';
  version: string;
  updatedAt: string;
}

export interface Material {
  id: string;
  title: string;
  type: 'text' | 'image' | 'link';
  content: string;
  summary?: string;
  tags: string[];
  status: 'pending' | 'approved' | 'rejected';
  createdAt?: string;
}

export interface FileImportJob {
  id: string;
  fileName: string;
  status: 'processing' | 'completed' | 'failed';
  progress: number;
  message?: string;
}

export interface Scene {
  id: string;
  title: string;
  summary: string;
  content?: string;
}

export interface Chapter {
  id: string;
  title: string;
  summary: string;
  scenes: Scene[];
}

export interface Outline {
  id: string;
  storyId: string;
  title: string;
  worldId?: string;
  chapters: Chapter[];
  updatedAt: string;
}

export interface Manuscript {
  id: string;
  outlineId: string;
  title: string;
  worldId?: string;
  sections?: Record<string, string>;
  updatedAt: string;
}

// --- World Building Types ---

export interface WorldFieldDefinition {
  key: string;
  label: string;
  type: 'text' | 'textarea' | 'select';
  description?: string;
  placeholder?: string;
}

export interface WorldModuleDefinition {
  key: string;
  label: string;
  description: string;
  fields: WorldFieldDefinition[];
}

export interface WorldModuleData {
  key: string;
  fields: Record<string, string>; // fieldKey -> value
}

export interface WorldDetail extends World {
  themes: string[];
  creativeIntent: string;
  notes: string;
  modules: WorldModuleData[];
}

// --- Settings Types ---

export interface SystemSettings {
  baseUrl: string;
  modelName: string;
  apiKeyIsSet: boolean;
}

export interface PromptTemplates {
  storyCreation: string;
  outlineChapter: string;
  manuscriptSection: string;
  refineWithInstruction: string;
  refineWithoutInstruction: string;
}

export interface WorldPromptTemplates {
  modules: Record<string, string>; // moduleKey -> template
  finalTemplates: Record<string, string>;
  fieldRefine: string;
}

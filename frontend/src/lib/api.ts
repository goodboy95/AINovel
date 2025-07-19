import { Material, Outline, PromptTemplates, Story, SystemSettings, User, World, WorldDetail, WorldModuleDefinition, Manuscript } from "@/types";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "/api";

const normalizeWorldDetail = (data: any): WorldDetail => ({
  ...data,
  modules: data.modules
    ? Object.entries(data.modules).map(([key, fields]) => ({ key, fields }))
    : [],
});

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem("token");
  const headers: HeadersInit = {
    "Content-Type": options.body instanceof FormData ? undefined : "application/json",
    ...(options.headers || {}),
  };
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(text || res.statusText);
  }
  if (res.status === 204) return undefined as T;
  return res.json();
}

export const api = {
  auth: {
    login: async (username: string, password: string) => {
      const res = await request<any>("/v1/auth/login", {
        method: "POST",
        body: JSON.stringify({ username, password }),
      });
      const user: User = { id: res.id, username: res.username, email: res.email };
      return { token: res.token, user };
    },
    register: (data: any) => request<any>("/v1/auth/register", { method: "POST", body: JSON.stringify(data) }),
    validate: async () => {
      const res = await request<any>("/v1/auth/validate");
      return { id: res.id, username: res.username, email: res.email } as User;
    },
  },
  stories: {
    list: () => request<Story[]>("/v1/story-cards"),
    create: (data: Partial<Story>) => request<Story>("/v1/stories", { method: "POST", body: JSON.stringify(data) }),
    get: (id: string) => request<Story>(`/v1/story-cards/${id}`),
  },
  outlines: {
    listByStory: (storyId: string) => request<Outline[]>(`/v1/story-cards/${storyId}/outlines`),
    get: (id: string) => request<Outline>(`/v1/outlines/${id}`),
    save: (id: string, payload: any) => request<Outline>(`/v1/outlines/${id}`, { method: "PUT", body: JSON.stringify(payload) }),
  },
  worlds: {
    list: () => request<World[]>("/v1/worlds"),
    getDefinitions: () => request<WorldModuleDefinition[]>("/v1/world-building/definitions"),
    getDetail: async (id: string) => {
      const res = await request<any>(`/v1/worlds/${id}`);
      return normalizeWorldDetail(res);
    },
    create: async (data: Partial<WorldDetail>) => {
      const payload = { ...data, modules: data.modules ? Object.fromEntries(data.modules.map(m => [m.key, m.fields])) : undefined };
      const res = await request<any>("/v1/worlds", { method: "POST", body: JSON.stringify(payload) });
      return normalizeWorldDetail(res);
    },
    update: async (id: string, data: Partial<WorldDetail>) => {
      const payload = { ...data, modules: data.modules ? Object.fromEntries(data.modules.map(m => [m.key, m.fields])) : undefined };
      const res = await request<any>(`/v1/worlds/${id}`, { method: "PUT", body: JSON.stringify(payload) });
      return normalizeWorldDetail(res);
    },
  },
  materials: {
    list: () => request<Material[]>("/v1/materials"),
    create: (data: any) => request<Material>("/v1/materials", { method: "POST", body: JSON.stringify(data) }),
    upload: async (file: File) => {
      const form = new FormData();
      form.append("file", file);
      return request<any>("/v1/materials/upload", { method: "POST", body: form });
    },
    getUploadStatus: (jobId: string) => request<any>(`/v1/materials/upload/${jobId}`),
    search: (query: string) => request<any[]>("/v1/materials/search", { method: "POST", body: JSON.stringify({ query }) }),
    getPending: () => request<Material[]>("/v1/materials/review/pending", { method: "POST" }),
    review: (id: string, action: "approve" | "reject", data: any) => request<Material>(`/v1/materials/${id}/review/${action}` , { method: "POST", body: JSON.stringify(data) }),
  },
  settings: {
    get: () => request<SystemSettings>("/v1/settings"),
    update: (data: Partial<SystemSettings> & { apiKey?: string }) => request<SystemSettings>("/v1/settings", { method: "PUT", body: JSON.stringify(data) }),
    testConnection: () => request<boolean>("/v1/settings/test", { method: "POST" }),
  },
  prompts: {
    getWorkspace: () => request<PromptTemplates>("/v1/prompt-templates"),
    updateWorkspace: (data: Partial<PromptTemplates>) => request<PromptTemplates>("/v1/prompt-templates", { method: "PUT", body: JSON.stringify(data) }),
    getWorld: () => request<any>("/v1/world-prompts"),
    updateWorld: (data: any) => request<any>("/v1/world-prompts", { method: "PUT", body: JSON.stringify(data) }),
    getWorkspaceMetadata: () => request<any>("/v1/prompt-templates/metadata"),
    getWorldMetadata: () => request<any>("/v1/world-prompts/metadata"),
  },
  manuscripts: {
    listByOutline: (outlineId: string) => request<Manuscript[]>(`/v1/outlines/${outlineId}/manuscripts`),
  }
};

import { Story, User, World, Material, Outline, Manuscript, WorldDetail, WorldModuleDefinition, FileImportJob, SystemSettings, PromptTemplates, WorldPromptTemplates, ModelConfig, AdminDashboardStats, CreditLog } from "@/types";

// 模拟延迟
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

// Mock Data
let MOCK_USER: User = {
  id: "u1",
  username: "WriterOne",
  email: "writer@example.com",
  avatar: "https://github.com/shadcn.png",
  role: "user",
  credits: 500, // 初始积分
  isBanned: false,
  lastCheckIn: "2023-01-01T00:00:00Z", // 很久以前
};

// Admin User for testing
const MOCK_ADMIN: User = {
  id: "admin1",
  username: "AdminUser",
  email: "admin@novel.studio",
  role: "admin",
  credits: 999999,
  isBanned: false,
};

const MOCK_STORIES: Story[] = [
  {
    id: "s1",
    title: "赛博侦探：霓虹雨",
    synopsis: "在2077年的新东京，一名失去记忆的侦探试图找回自己的过去。",
    genre: "科幻/赛博朋克",
    tone: "阴郁、悬疑",
    status: "draft",
    updatedAt: "2024-03-10",
  },
  {
    id: "s2",
    title: "龙之谷的最后守护者",
    synopsis: "当古老的封印破碎，年轻的牧羊人必须拿起剑。",
    genre: "奇幻/史诗",
    tone: "热血、冒险",
    status: "published",
    updatedAt: "2024-02-15",
  },
];

const MOCK_WORLDS: World[] = [
  {
    id: "w1",
    name: "新东京 2077",
    tagline: "高科技，低生活",
    status: "active",
    version: "1.2.0",
    updatedAt: "2024-03-01",
  },
];

const MOCK_WORLD_DEFINITIONS: WorldModuleDefinition[] = [
  {
    key: "geography",
    label: "地理环境",
    description: "定义世界的地形、气候、重要地点。",
    fields: [
      { key: "terrain", label: "地形地貌", type: "textarea", placeholder: "例如：多山、群岛、荒漠..." },
      { key: "climate", label: "气候特征", type: "textarea", placeholder: "例如：终年多雨、极寒..." },
      { key: "locations", label: "重要地点", type: "textarea", placeholder: "列出3-5个关键城市或地标" }
    ]
  },
  {
    key: "society",
    label: "社会体系",
    description: "定义政治、经济、文化结构。",
    fields: [
      { key: "politics", label: "政治体制", type: "textarea", placeholder: "例如：联邦制、君主立宪、公司极权..." },
      { key: "economy", label: "经济来源", type: "textarea", placeholder: "例如：能源出口、高科技制造、农业..." },
      { key: "culture", label: "文化习俗", type: "textarea", placeholder: "例如：崇拜机械、重视荣誉..." }
    ]
  },
];

const MOCK_WORLD_DETAILS: Record<string, WorldDetail> = {
  "w1": {
    id: "w1",
    name: "新东京 2077",
    tagline: "高科技，低生活",
    status: "active",
    version: "1.2.0",
    updatedAt: "2024-03-01",
    themes: ["赛博朋克", "反乌托邦", "人工智能"],
    creativeIntent: "探讨人类在高度机械化社会中的异化。",
    notes: "参考《银翼杀手》的视觉风格。",
    modules: []
  }
};

const MOCK_OUTLINES: Outline[] = [
  {
    id: "o1",
    storyId: "s1",
    title: "主线大纲 V1",
    updatedAt: "2024-03-11",
    chapters: [
      {
        id: "c1",
        title: "第一章：觉醒",
        summary: "主角在垃圾堆中醒来，发现自己只有一只机械臂。",
        scenes: [
          { id: "sc1", title: "垃圾场苏醒", summary: "雨夜，霓虹灯闪烁，主角睁开眼。" },
          { id: "sc2", title: "遭遇拾荒者", summary: "几个不怀好意的拾荒者试图拆解主角。" }
        ]
      }
    ]
  }
];

const MOCK_MATERIALS: Material[] = [
  { id: "m1", title: "赛博义肢型号大全", type: "text", content: "...", tags: ["设定", "科技"], status: "approved", createdAt: "2024-03-01" },
];

let MOCK_SETTINGS: SystemSettings = {
  baseUrl: "https://api.openai.com/v1",
  modelName: "gpt-4-turbo",
  apiKeyIsSet: true,
  registrationEnabled: true,
  maintenanceMode: false,
  checkInMinPoints: 10,
  checkInMaxPoints: 50,
};

let MOCK_PROMPTS: PromptTemplates = {
  storyCreation: "你是一个专业的小说家...",
  outlineChapter: "请根据故事大纲...",
  manuscriptSection: "请根据场景描述...",
  refineWithInstruction: "请根据以下指示...",
  refineWithoutInstruction: "请润色以下文本...",
};

let MOCK_WORLD_PROMPTS: WorldPromptTemplates = {
  modules: {},
  finalTemplates: {},
  fieldRefine: "请优化以下设定字段...",
};

// V2 Mock Data
const MOCK_MODELS: ModelConfig[] = [
  { id: "m1", name: "gpt-3.5-turbo", displayName: "GPT-3.5 (快速)", inputMultiplier: 1, outputMultiplier: 1, poolId: "p1", isEnabled: true },
  { id: "m2", name: "gpt-4-turbo", displayName: "GPT-4 Turbo (智能)", inputMultiplier: 10, outputMultiplier: 30, poolId: "p2", isEnabled: true },
  { id: "m3", name: "claude-3-opus", displayName: "Claude 3 Opus (长文)", inputMultiplier: 15, outputMultiplier: 45, poolId: "p3", isEnabled: true },
];

let MOCK_CREDIT_LOGS: CreditLog[] = [
  { id: "l1", userId: "u1", amount: 50, reason: "check_in", details: "每日签到", createdAt: "2024-03-20T10:00:00Z" },
  { id: "l2", userId: "u1", amount: -120, reason: "generation", details: "Model: GPT-4, In: 500, Out: 200", createdAt: "2024-03-20T10:05:00Z" },
];

let MOCK_REDEEM_CODES = [
  { id: "rc1", code: "VIP888", amount: 1000, isUsed: false, expiresAt: "2025-01-01" },
  { id: "rc2", code: "WELCOME", amount: 500, isUsed: true, usedBy: "u1", expiresAt: "2024-12-31" },
];

// Helper to deduct credits
const deductCredits = (userId: string, inputTokens: number, outputTokens: number, modelId: string) => {
  const model = MOCK_MODELS.find(m => m.id === modelId) || MOCK_MODELS[0];
  // Formula: (In * InMult + Out * OutMult) / 100000
  const cost = (inputTokens * model.inputMultiplier + outputTokens * model.outputMultiplier) / 100000;
  // Round to 2 decimals for display, but keep precision internally usually
  const finalCost = parseFloat(cost.toFixed(4));
  
  if (userId === MOCK_USER.id) {
    MOCK_USER.credits -= finalCost;
    MOCK_CREDIT_LOGS.unshift({
      id: Date.now().toString(),
      userId,
      amount: -finalCost,
      reason: "generation",
      details: `Model: ${model.displayName}`,
      createdAt: new Date().toISOString()
    });
  }
  
  return finalCost;
};

// API Methods
export const api = {
  auth: {
    login: async (username: string, password: string): Promise<{ token: string; user: User }> => {
      await delay(800);
      if (username === "admin" && password === "admin") {
        return { token: "mock-admin-token", user: MOCK_ADMIN };
      }
      if (username === "user" && password === "user") {
        return { token: "mock-user-token", user: MOCK_USER };
      }
      // Default fallback for demo
      return { token: "mock-jwt-token-" + Date.now(), user: { ...MOCK_USER, username } };
    },
    // V2: Send verification code
    sendCode: async (email: string, captchaToken: string) => {
      await delay(1000);
      if (!captchaToken) throw new Error("人机验证失败");
      console.log(`[Mock] Code sent to ${email}: 123456`);
      return { success: true, message: "验证码已发送" };
    },
    // V2: Register with code
    registerV2: async (data: any) => {
      await delay(1000);
      if (data.code !== "123456") throw new Error("验证码错误");
      return { success: true };
    },
    validate: async (token: string) => {
      await delay(400);
      if (token.includes("admin")) return MOCK_ADMIN;
      return MOCK_USER;
    }
  },
  user: {
    getProfile: async () => {
      await delay(300);
      return MOCK_USER;
    },
    checkIn: async () => {
      await delay(600);
      const points = Math.floor(Math.random() * (MOCK_SETTINGS.checkInMaxPoints - MOCK_SETTINGS.checkInMinPoints + 1)) + MOCK_SETTINGS.checkInMinPoints;
      MOCK_USER = { ...MOCK_USER, credits: MOCK_USER.credits + points, lastCheckIn: new Date().toISOString() };
      MOCK_CREDIT_LOGS.unshift({
        id: Date.now().toString(),
        userId: MOCK_USER.id,
        amount: points,
        reason: "check_in",
        details: "每日签到",
        createdAt: new Date().toISOString()
      });
      return { success: true, points, newTotal: MOCK_USER.credits };
    },
    redeem: async (code: string) => {
      await delay(500);
      const redeemCode = MOCK_REDEEM_CODES.find(c => c.code === code && !c.isUsed);
      if (redeemCode) {
        MOCK_USER = { ...MOCK_USER, credits: MOCK_USER.credits + redeemCode.amount };
        redeemCode.isUsed = true;
        redeemCode.usedBy = MOCK_USER.id;
        MOCK_CREDIT_LOGS.unshift({
          id: Date.now().toString(),
          userId: MOCK_USER.id,
          amount: redeemCode.amount,
          reason: "redeem",
          details: `兑换码: ${code}`,
          createdAt: new Date().toISOString()
        });
        return { success: true, points: redeemCode.amount, newTotal: MOCK_USER.credits };
      }
      throw new Error("无效或已使用的兑换码");
    },
    updatePassword: async (oldPass: string, newPass: string) => {
      await delay(800);
      return { success: true };
    }
  },
  ai: {
    // V2: Chat with context
    chat: async (messages: any[], modelId: string, context: any) => {
      await delay(1500); // Simulate generation time
      
      // Mock token usage
      const inputTokens = JSON.stringify(context).length / 4 + 100;
      const outputTokens = 200; // Mock output length
      
      const cost = deductCredits(MOCK_USER.id, inputTokens, outputTokens, modelId);
      
      return {
        role: 'assistant',
        content: `[AI 回复] 这是一个基于上下文的模拟回复。\n\n使用了模型: ${modelId}\n消耗积分: ${cost}\n\n针对你的问题，建议...`,
        usage: { inputTokens, outputTokens, cost },
        remainingCredits: MOCK_USER.credits
      };
    },
    // V2: Refine text
    refine: async (text: string, instruction: string, modelId: string) => {
      await delay(2000);
      
      const inputTokens = text.length / 4 + instruction.length / 4 + 50;
      const outputTokens = text.length / 4 + 100;
      
      const cost = deductCredits(MOCK_USER.id, inputTokens, outputTokens, modelId);
      
      return {
        result: `[AI 润色结果] ${text} (已根据指令 "${instruction}" 优化)`,
        usage: { inputTokens, outputTokens, cost },
        remainingCredits: MOCK_USER.credits
      };
    },
    getModels: async () => {
      await delay(200);
      return MOCK_MODELS;
    }
  },
  admin: {
    getDashboardStats: async (): Promise<AdminDashboardStats> => {
      await delay(500);
      return {
        totalUsers: 1205,
        todayNewUsers: 45,
        totalCreditsConsumed: 5400000,
        todayCreditsConsumed: 120000,
        apiErrorRate: 0.02,
        pendingReviews: 12
      };
    },
    getModels: async () => {
      await delay(300);
      return MOCK_MODELS;
    },
    updateModel: async (model: ModelConfig) => {
      await delay(400);
      return true;
    },
    getUsers: async () => {
      await delay(400);
      return [MOCK_USER, MOCK_ADMIN]; // Mock list
    },
    grantCredits: async (userId: string, amount: number) => {
      await delay(300);
      if (userId === MOCK_USER.id) {
        MOCK_USER.credits += amount;
        MOCK_CREDIT_LOGS.unshift({
          id: Date.now().toString(),
          userId,
          amount,
          reason: "admin_grant",
          details: "管理员手动调整",
          createdAt: new Date().toISOString()
        });
      }
      return true;
    },
    getLogs: async () => {
      await delay(300);
      return MOCK_CREDIT_LOGS;
    },
    getCodes: async () => {
      await delay(300);
      return MOCK_REDEEM_CODES;
    },
    createCode: async (data: any) => {
      await delay(300);
      MOCK_REDEEM_CODES.unshift({
        id: `rc_${Date.now()}`,
        code: data.code,
        amount: data.amount,
        isUsed: false,
        expiresAt: "2025-12-31"
      });
      return true;
    }
  },
  // ... Existing APIs (stories, worlds, etc.) kept as is for brevity, assuming they exist ...
  stories: {
    list: async () => { await delay(500); return MOCK_STORIES; },
    create: async (data: any) => { await delay(600); return { ...data, id: "s_new" }; },
    get: async (id: string) => MOCK_STORIES[0],
  },
  outlines: {
    listByStory: async (id: string) => MOCK_OUTLINES,
  },
  worlds: {
    list: async () => MOCK_WORLDS,
    getDefinitions: async () => MOCK_WORLD_DEFINITIONS,
    getDetail: async (id: string) => MOCK_WORLD_DETAILS[id],
    create: async (data: any) => ({ ...data, id: "w_new" } as any),
    update: async () => true,
  },
  materials: {
    list: async () => MOCK_MATERIALS,
    search: async () => MOCK_MATERIALS,
    create: async () => ({} as any),
    upload: async () => ({} as any),
    getUploadStatus: async () => ({} as any),
    getPending: async () => [],
    review: async () => true,
  },
  settings: {
    get: async () => MOCK_SETTINGS,
    update: async (data: any) => { MOCK_SETTINGS = { ...MOCK_SETTINGS, ...data }; return MOCK_SETTINGS; },
    testConnection: async () => true,
  },
  prompts: {
    getWorkspace: async () => MOCK_PROMPTS,
    updateWorkspace: async () => MOCK_PROMPTS,
    getWorld: async () => MOCK_WORLD_PROMPTS,
    updateWorld: async () => MOCK_WORLD_PROMPTS,
  }
};
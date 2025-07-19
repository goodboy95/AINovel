import { Story, User, World, Material, Outline, Manuscript, WorldDetail, WorldModuleDefinition, FileImportJob, SystemSettings, PromptTemplates, WorldPromptTemplates } from "@/types";

// 模拟延迟
const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

// Mock Data
const MOCK_USER: User = {
  id: "u1",
  username: "WriterOne",
  email: "writer@example.com",
  avatar: "https://github.com/shadcn.png",
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
  {
    id: "w2",
    name: "艾尔利亚大陆",
    tagline: "魔法与蒸汽共存的世界",
    status: "draft",
    version: "0.5.0",
    updatedAt: "2024-03-12",
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
  {
    key: "magic_tech",
    label: "魔法/科技",
    description: "定义超自然力量或高科技水平。",
    fields: [
      { key: "system_name", label: "体系名称", type: "text", placeholder: "例如：以太魔法、赛博义体" },
      { key: "rules", label: "核心规则/原理", type: "textarea", placeholder: "力量的来源与限制" },
      { key: "limitations", label: "代价与副作用", type: "textarea", placeholder: "使用力量需要付出什么？" }
    ]
  }
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
    modules: [
      {
        key: "geography",
        fields: {
          terrain: "填海造陆形成的巨型都市群，底层终年不见阳光。",
          climate: "酸雨频繁，空气污染严重，霓虹灯穿透雾霾。",
          locations: "荒坂塔、歌舞伎町地下街、填埋区贫民窟"
        }
      },
      {
        key: "society",
        fields: {
          politics: "名义上的市政府，实则由巨型企业联合会控制。",
          economy: "义体制造、生物科技、非法数据交易。",
          culture: "享乐主义盛行，由于生活压力大，人们沉迷于超梦体验。"
        }
      }
    ]
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
      },
      {
        id: "c2",
        title: "第二章：黑诊所",
        summary: "主角寻找医生修理手臂。",
        scenes: [
          { id: "sc3", title: "进入地下街", summary: "繁华与肮脏并存的地下世界。" }
        ]
      }
    ]
  }
];

const MOCK_MATERIALS: Material[] = [
  { id: "m1", title: "赛博义肢型号大全", type: "text", content: "...", tags: ["设定", "科技"], status: "approved", createdAt: "2024-03-01" },
  { id: "m2", title: "2077年大事记", type: "text", content: "...", tags: ["历史", "背景"], status: "approved", createdAt: "2024-03-02" },
  { id: "m3", title: "待审：神秘的信号", type: "text", content: "收到一段来自深空的加密信号...", tags: ["线索"], status: "pending", createdAt: "2024-03-15" },
];

let MOCK_SETTINGS: SystemSettings = {
  baseUrl: "https://api.openai.com/v1",
  modelName: "gpt-4-turbo",
  apiKeyIsSet: true,
};

let MOCK_PROMPTS: PromptTemplates = {
  storyCreation: "你是一个专业的小说家。请根据以下灵感创作一个故事大纲...",
  outlineChapter: "请根据故事大纲，为第 {chapterNumber} 章生成详细的场景列表...",
  manuscriptSection: "请根据场景描述，撰写正文。风格要求：{tone}...",
  refineWithInstruction: "请根据以下指示修改这段文本：{instruction}...",
  refineWithoutInstruction: "请润色以下文本，使其更加通顺优美...",
};

let MOCK_WORLD_PROMPTS: WorldPromptTemplates = {
  modules: {
    geography: "请设计一个奇幻世界的地理环境，包含地形、气候和重要地点...",
    society: "请设计该世界的社会结构，包含政治、经济和文化...",
  },
  finalTemplates: {},
  fieldRefine: "请优化以下设定字段的描述...",
};

// API Methods
export const api = {
  auth: {
    login: async (username: string, password: string): Promise<{ token: string; user: User }> => {
      await delay(800);
      if (username === "admin" && password === "password") {
        return { token: "mock-jwt-token", user: MOCK_USER };
      }
      return { token: "mock-jwt-token-" + Date.now(), user: { ...MOCK_USER, username } };
    },
    register: async (data: any) => {
      await delay(800);
      return { success: true };
    },
    validate: async (token: string) => {
      await delay(400);
      return MOCK_USER;
    }
  },
  stories: {
    list: async () => {
      await delay(500);
      return MOCK_STORIES;
    },
    create: async (data: Partial<Story>) => {
      await delay(600);
      const newStory = { ...data, id: `s${Date.now()}`, updatedAt: new Date().toISOString() } as Story;
      MOCK_STORIES.push(newStory);
      return newStory;
    },
    get: async (id: string) => {
      await delay(300);
      return MOCK_STORIES.find(s => s.id === id);
    }
  },
  outlines: {
    listByStory: async (storyId: string) => {
      await delay(400);
      return MOCK_OUTLINES.filter(o => o.storyId === storyId);
    }
  },
  worlds: {
    list: async () => {
      await delay(500);
      return MOCK_WORLDS;
    },
    getDefinitions: async () => {
      await delay(300);
      return MOCK_WORLD_DEFINITIONS;
    },
    getDetail: async (id: string) => {
      await delay(400);
      return MOCK_WORLD_DETAILS[id] || {
        ...MOCK_WORLDS.find(w => w.id === id),
        themes: [],
        creativeIntent: "",
        notes: "",
        modules: []
      } as WorldDetail;
    },
    create: async (data: Partial<World>) => {
      await delay(500);
      const newWorld = { 
        ...data, 
        id: `w${Date.now()}`, 
        status: 'draft', 
        version: '0.1.0',
        updatedAt: new Date().toISOString() 
      } as World;
      MOCK_WORLDS.push(newWorld);
      return newWorld;
    },
    update: async (id: string, data: Partial<WorldDetail>) => {
      await delay(400);
      return true;
    }
  },
  materials: {
    list: async () => {
      await delay(500);
      return MOCK_MATERIALS;
    },
    search: async (query: string) => {
      await delay(300);
      return MOCK_MATERIALS.filter(m => m.title.includes(query) || m.tags.some(t => t.includes(query)));
    },
    create: async (data: Partial<Material>) => {
      await delay(600);
      const newMaterial = { 
        ...data, 
        id: `m${Date.now()}`, 
        status: 'approved', // Default to approved for manual creation
        createdAt: new Date().toISOString() 
      } as Material;
      MOCK_MATERIALS.push(newMaterial);
      return newMaterial;
    },
    upload: async (file: File) => {
      await delay(1000);
      return { id: `job-${Date.now()}`, fileName: file.name, status: 'processing', progress: 0 } as FileImportJob;
    },
    getUploadStatus: async (jobId: string) => {
      await delay(300);
      // Mock completion
      return { id: jobId, fileName: "example.txt", status: 'completed', progress: 100 } as FileImportJob;
    },
    getPending: async () => {
      await delay(400);
      return MOCK_MATERIALS.filter(m => m.status === 'pending');
    },
    review: async (id: string, action: 'approve' | 'reject') => {
      await delay(500);
      const idx = MOCK_MATERIALS.findIndex(m => m.id === id);
      if (idx >= 0) {
        MOCK_MATERIALS[idx].status = action === 'approve' ? 'approved' : 'rejected';
      }
      return true;
    }
  },
  settings: {
    get: async () => {
      await delay(300);
      return MOCK_SETTINGS;
    },
    update: async (data: Partial<SystemSettings>) => {
      await delay(500);
      MOCK_SETTINGS = { ...MOCK_SETTINGS, ...data };
      return MOCK_SETTINGS;
    },
    testConnection: async () => {
      await delay(1000);
      return true;
    }
  },
  prompts: {
    getWorkspace: async () => {
      await delay(300);
      return MOCK_PROMPTS;
    },
    updateWorkspace: async (data: Partial<PromptTemplates>) => {
      await delay(400);
      MOCK_PROMPTS = { ...MOCK_PROMPTS, ...data };
      return MOCK_PROMPTS;
    },
    getWorld: async () => {
      await delay(300);
      return MOCK_WORLD_PROMPTS;
    },
    updateWorld: async (data: Partial<WorldPromptTemplates>) => {
      await delay(400);
      MOCK_WORLD_PROMPTS = { ...MOCK_WORLD_PROMPTS, ...data };
      return MOCK_WORLD_PROMPTS;
    }
  }
};
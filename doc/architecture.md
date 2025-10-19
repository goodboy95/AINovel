# 项目架构总览

AINovel 是一个前后端分离的 AI 小说创作工作台，采用单仓库（monorepo）管理 Spring Boot 后端与 React/Vite 前端。整体架构分为以下层级：

- **后端（`backend/`）**：基于 Spring Boot 3.3 与 Spring Security 构建的 REST API，负责用户认证、故事/大纲/稿件/世界观数据管理以及与大语言模型（LLM）的交互。
- **前端（`frontend/`）**：使用 React、TypeScript 与 Ant Design 构建的单页应用，通过自定义 hooks 与服务层封装访问后端接口。
- **文档与设计（`doc/`）**：保存产品体验、交互设计与系统说明，新增此文件聚焦整体架构。

## 后端架构

后端遵循典型的 Spring 分层结构：

| 层级 | 关键目录 | 职责 |
| --- | --- | --- |
| Web 层 | `controller/` | 暴露 REST API，负责认证上下文解析与响应封装，例如 `StoryController` 负责故事卡 CRUD（`/api/v1/stories`）【F:backend/src/main/java/com/example/ainovel/controller/StoryController.java†L24-L69】|
| 服务层 | `service/` | 实现业务逻辑，编排 Repository、AI 服务与辅助类。例如 `WorldGenerationWorkflowService` 维护世界模块生成任务与状态机。【F:backend/src/main/java/com/example/ainovel/service/world/WorldGenerationWorkflowService.java†L22-L205】|
| 数据访问层 | `repository/` | Spring Data JPA 仓库接口，对接 MySQL 模型对象如 `WorldRepository`、`StoryCardRepository`。|
| 数据模型 | `model/` | 定义实体与枚举，覆盖故事卡、角色卡、稿件、大纲与世界模块等核心概念。【F:backend/src/main/java/com/example/ainovel/model/world/WorldModule.java†L1-L120】|
| DTO 层 | `dto/` | 在 API 与实体间传输结构化数据，例如 `WorldGenerationStatusResponse` 用于世界生成进度轮询。【F:backend/src/main/java/com/example/ainovel/dto/world/WorldGenerationStatusResponse.java†L1-L75】|
| 配置层 | `config/` | 封装安全、CORS、JSON 序列化、RestTemplate 代理等配置。【F:backend/src/main/java/com/example/ainovel/config/SecurityConfig.java†L33-L101】【F:backend/src/main/java/com/example/ainovel/config/RestTemplateConfig.java†L15-L44】|
| 资源层 | `src/main/resources/` | 包含默认 Prompt、世界模块定义、`application*.properties` 等配置文件，用于引导 AI 生成内容。【F:backend/src/main/resources/prompts/default-prompts.yaml†L1-L120】【F:backend/src/main/resources/worldbuilding/modules.yml†L1-L90】|

### 业务子域

- **故事构思与角色卡**：`ConceptionController` 与 `ConceptionService` 提供故事+角色生成与精修、角色增删改等功能，依赖 Prompt 模板服务统一格式。【F:backend/src/main/java/com/example/ainovel/controller/ConceptionController.java†L18-L119】
- **大纲管理**：`OutlineController` 协调章节、场景生成、PATCH 更新与文本精修，服务层负责调用 AI 生成章节细纲。【F:backend/src/main/java/com/example/ainovel/controller/OutlineController.java†L56-L165】
- **稿件创作**：`ManuscriptController` 支持场景生成、章节正文写作、角色记忆分析等，`CharacterDialogueService` 根据角色记忆生成对白。【F:backend/src/main/java/com/example/ainovel/controller/ManuscriptController.java†L18-L125】【F:backend/src/main/java/com/example/ainovel/service/CharacterDialogueService.java†L18-L119】
- **世界观工作台**：`WorldController` 管理世界元数据、模块内容、生成流水线与发布流程，配合 `WorldModuleDefinitionRegistry` 等组件驱动 AI 生成与复用。【F:backend/src/main/java/com/example/ainovel/controller/WorldController.java†L18-L199】
- **Prompt 模板管理**：`PromptTemplateController` 与 `WorldPromptTemplateController` 提供模板读取、更新与重置的 API。【F:backend/src/main/java/com/example/ainovel/controller/PromptTemplateController.java†L18-L53】
- **认证与安全**：`AuthController`、`SecurityConfig`、`JwtRequestFilter` 完成 JWT 登录、鉴权与 CORS 管理，`EncryptionService` 用 AES 对 API Key 加密存储。【F:backend/src/main/java/com/example/ainovel/controller/AuthController.java†L21-L85】【F:backend/src/main/java/com/example/ainovel/service/EncryptionService.java†L1-L42】

后端依赖 MySQL（默认 `ainovel` 数据库）并通过 `spring.jpa.hibernate.ddl-auto=update` 自动迁移模式维护表结构。【F:backend/src/main/resources/application.properties†L3-L25】

## 前端架构

前端基于 React + TypeScript，入口 `App.tsx` 定义路由与受保护页面，统一在 `AuthProvider` 中校验 JWT。【F:frontend/src/App.tsx†L1-L55】【F:frontend/src/contexts/AuthContext.tsx†L1-L60】

目录结构：

- `components/`：工作台、角色/故事列表、设置页、弹窗等 UI 模块，`Workbench.tsx` 统筹故事构思、大纲、稿件标签页。【F:frontend/src/components/Workbench.tsx†L1-L120】
- `pages/WorldBuilder/`：世界观工作台复杂组件，封装世界列表、元数据编辑、模块 Tab、自动保存与生成进度等流程。【F:frontend/src/pages/WorldBuilder/WorldBuilderPage.tsx†L1-L120】
- `hooks/`：封装数据加载逻辑，如 `useStoryData`、`useOutlineData`，负责调用 `services/api.ts` 并处理状态。【F:frontend/src/hooks/useStoryData.ts†L1-L120】
- `services/api.ts`：集中声明所有 REST API 调用与错误处理策略，复用授权头构造、响应兜底逻辑。【F:frontend/src/services/api.ts†L1-L120】
- `types.ts`：统一后端 DTO 映射，避免魔法字符串并方便 IDE 推断。【F:frontend/src/types.ts†L1-L120】

Vite 配置提供开发时的代理至后端 8080 端口，生产构建输出到 `dist/` 与后端解耦。【F:frontend/vite.config.ts†L1-L24】

## 前后端交互

- 前端通过 `fetch` 调用 `/api/v1/**`、`/api/auth/**` 接口，`services/api.ts` 自动添加本地存储中的 JWT 并在 204/205 响应时返回 `undefined` 防止 JSON 解析错误。【F:frontend/src/services/api.ts†L21-L68】
- 世界观模块的生成、发布在前端世界工作台触发，对应后端的 `/worlds/{id}/generation`、`/publish` 系列端点；生成过程中需轮询任务状态并处理失败重试。【F:frontend/src/pages/WorldBuilder/WorldBuilderPage.tsx†L120-L210】
- Prompt 模板、世界定义、角色对话等高级功能都基于后端的模板渲染与 AI 服务实现，需要用户在设置页配置 API Key、Base URL 与模型名称。【F:frontend/src/services/api.ts†L290-L360】【F:backend/src/main/java/com/example/ainovel/service/SettingsService.java†L20-L115】

## 关键技术要点与潜在陷阱

1. **AI 凭证存储**：API Key 通过 AES/ECB/PKCS5Padding 加密后写入数据库，密钥长度必须为 16/24/32 字节，否则 `EncryptionService` 无法正常工作。部署时需替换 `app.encryption.key`，避免使用示例值。【F:backend/src/main/java/com/example/ainovel/service/EncryptionService.java†L9-L41】
2. **世界生成作业状态**：`WorldGenerationWorkflowService` 会在生成前清空旧任务、逐个执行模块，失败的任务需通过 `/generation/{moduleKey}/retry` 重试，状态机依赖数据库事务保持一致性。【F:backend/src/main/java/com/example/ainovel/service/world/WorldGenerationWorkflowService.java†L50-L205】
3. **SPA 路由兼容**：后端 `WebConfig` 将非 API 请求重写到 `index.html`，若自定义静态资源路径需同步更新该配置以避免刷新 404。【F:backend/src/main/java/com/example/ainovel/config/WebConfig.java†L16-L43】
4. **开发代理**：前端开发环境默认代理 `/api` 与 `/ws` 到 `localhost:8080`，修改后端端口时记得同步更新 Vite 配置及 `cors.allowed-origins` 列表。【F:frontend/vite.config.ts†L6-L24】【F:backend/src/main/java/com/example/ainovel/config/SecurityConfig.java†L37-L69】

## 数据流示意（从故事构思到稿件写作）

1. 用户在前端 `StoryConception` 表单提交构思 → `useStoryData.generateStory` 调用 `/api/v1/conception` → `ConceptionService` 使用 Prompt 模板调用 `OpenAiService`，生成故事卡与角色卡后入库。【F:frontend/src/hooks/useStoryData.ts†L49-L92】【F:backend/src/main/java/com/example/ainovel/controller/ConceptionController.java†L38-L63】
2. 用户在大纲工作台请求章节 → `/api/v1/outlines/{id}/chapters` → `OutlineService` 结合故事信息与世界设定渲染模板，调用 LLM 获得场景列表并持久化。【F:backend/src/main/java/com/example/ainovel/controller/OutlineController.java†L69-L97】
3. 稿件写作阶段前端调用 `/api/v1/manuscript/scenes/{sceneId}/generate` → `ManuscriptService` 拉取场景上下文、角色状态、世界设定后生成正文并记录 `ManuscriptSection` 版本；`CharacterChangeLog` 用于跟踪角色发展。【F:backend/src/main/java/com/example/ainovel/controller/ManuscriptController.java†L36-L76】【F:backend/src/main/java/com/example/ainovel/model/ManuscriptSection.java†L1-L120】
4. 角色对白与记忆在 `CharacterDialogueService` 中聚合角色档案、近期变更与场景描述，调用 LLM 输出角色语气一致的对白。【F:backend/src/main/java/com/example/ainovel/service/CharacterDialogueService.java†L32-L118】

通过上述层次与流程，开发者可以迅速定位模块、理解跨域调用方式并在不破坏整体架构的情况下扩展功能。

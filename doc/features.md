# 功能概览与重点说明

本文件梳理 AINovel 的核心功能模块、关键接口以及实现细节，帮助新成员快速理解系统能力与限制。

## 用户与安全

- **注册/登录/JWT 校验**：`AuthController` 提供 `/api/v1/auth/register`、`/api/v1/auth/login`、`/api/auth/validate`，成功登录返回 JWT，后续请求需携带 `Authorization: Bearer` 头。【F:backend/src/main/java/com/example/ainovel/controller/AuthController.java†L37-L85】
- **安全过滤器**：`SecurityConfig` 配置 `/api/v1/**` 需认证，并允许 Vite 构建产物与静态资源匿名访问；`JwtRequestFilter` 在请求链路中解析 Token。【F:backend/src/main/java/com/example/ainovel/config/SecurityConfig.java†L33-L77】
- **设置中心**：`SettingsController` 负责存取 API Key、Base URL、模型名称，同时支持 `/api/v1/settings/test` 验证连接；`SettingsService` 自动回退到用户已保存的密钥并调用 `OpenAiService.validateApiKey`。【F:backend/src/main/java/com/example/ainovel/controller/SettingsController.java†L17-L51】【F:backend/src/main/java/com/example/ainovel/service/SettingsService.java†L58-L119】

## 故事创作流程

1. **故事构思**：`ConceptionController` 的 `/api/v1/conception` 接收 `ConceptionRequest`，通过 `PromptTemplateService` 渲染 `default-prompts.yaml` 后调用 `OpenAiService.generate`，生成故事卡与 3-5 个角色卡并落库。【F:backend/src/main/java/com/example/ainovel/controller/ConceptionController.java†L38-L77】【F:backend/src/main/resources/prompts/default-prompts.yaml†L1-L33】
2. **故事管理**：`StoryController` 提供 `/api/v1/stories` CRUD，前端 `StoryList`、`StoryDetail` 与 `useStoryData` 共同维护选中故事与角色列表，支持手动新增故事与角色精修。【F:backend/src/main/java/com/example/ainovel/controller/StoryController.java†L31-L69】【F:frontend/src/hooks/useStoryData.ts†L13-L116】
3. **角色精修**：`ConceptionController` 下的 `/story-cards/{id}/refine`、`/character-cards/{id}/refine` 调用 `ConceptionService.refine*`，基于 refine 模板向 AI 请求润色。【F:backend/src/main/java/com/example/ainovel/controller/ConceptionController.java†L97-L118】【F:backend/src/main/resources/prompts/default-prompts.yaml†L120-L150】
4. **大纲生成与维护**：`OutlineController` 的 `/outlines/{outlineId}/chapters` 按章节生成场景结构；`PATCH /chapters/{id}`、`/scenes/{id}` 支持局部更新；`/outlines/scenes/{id}/refine` 提供场景梗概润色。【F:backend/src/main/java/com/example/ainovel/controller/OutlineController.java†L69-L165】
5. **稿件创作**：`ManuscriptController` 支持 `/manuscript/scenes/{sceneId}/generate` 生成正文、`/manuscripts/{id}` 获取章节+分节文本、`/outlines/{outlineId}/manuscripts` 管理多稿件；`CharacterChangeLog` 记录角色状态变化并提供 `/sections/analyze-character-changes` 分析接口。【F:backend/src/main/java/com/example/ainovel/controller/ManuscriptController.java†L29-L125】【F:backend/src/main/java/com/example/ainovel/model/CharacterChangeLog.java†L1-L120】
6. **角色对白**：`/api/v1/ai/generate-dialogue` 聚合角色档案、近期记忆、场景描述后生成对白，前端可在稿件编辑器中调用。【F:backend/src/main/java/com/example/ainovel/controller/AiController.java†L19-L30】【F:backend/src/main/java/com/example/ainovel/service/CharacterDialogueService.java†L36-L118】

## 世界观工作台

- **世界定义加载**：`WorldBuildingDefinitionController` 返回 `worldbuilding/modules.yml`、字段校验规则、提示模板等元数据，前端在进入世界页时加载一次。【F:backend/src/main/java/com/example/ainovel/controller/WorldBuildingDefinitionController.java†L15-L24】【F:frontend/src/pages/WorldBuilder/WorldBuilderPage.tsx†L1-L60】
- **世界 CRUD 与模块编辑**：`WorldController` 提供 `/api/v1/worlds` 列表、`POST/PUT/DELETE` 世界、批量更新模块 `/modules`，前端 `ModuleTabs` 负责渲染各模块表单并聚合保存状态。【F:backend/src/main/java/com/example/ainovel/controller/WorldController.java†L46-L130】【F:frontend/src/pages/WorldBuilder/WorldBuilderPage.tsx†L60-L150】
- **AI 生成流水线**：前端触发 `previewWorldPublish` → `/publish/preview` 校验缺失字段，再调用 `/publish` 初始化生成任务。`WorldGenerationWorkflowService` 建立 job 队列，`runWorldGenerationModule` 逐个执行，失败时可用 `/generation/{moduleKey}/retry` 重试，前端 `GenerationProgressModal` 轮询显示进度并在成功后刷新详情。【F:backend/src/main/java/com/example/ainovel/controller/WorldController.java†L130-L199】【F:backend/src/main/java/com/example/ainovel/service/world/WorldGenerationWorkflowService.java†L52-L205】【F:frontend/src/pages/WorldBuilder/WorldBuilderPage.tsx†L150-L240】
- **Prompt 模板管理**：世界模板接口 `/api/v1/world-prompts` 支持自定义模块草稿、最终模板与字段精修模板，配合 `world-defaults.yaml` 渲染 AI 提示词。【F:backend/src/main/java/com/example/ainovel/controller/WorldPromptTemplateController.java†L20-L46】【F:backend/src/main/resources/prompts/world-defaults.yaml†L1-L120】
- **世界引用**：故事/大纲/稿件 API 支持 `worldId` 关联，Prompt 模板会从世界模块中提取摘要或全文，为生成内容提供设定上下文。【F:frontend/src/services/api.ts†L90-L170】【F:backend/src/main/resources/prompts/default-prompts.yaml†L3-L45】

## 前端交互亮点

- **工作台 Tab 管理**：`Workbench.tsx` 通过 React Router 的 `useParams` 同步 URL 与活动标签，结合 `useStoryData`、`useOutlineData` 与各类弹窗实现全流程创作体验。【F:frontend/src/components/Workbench.tsx†L21-L120】
- **自动保存与草稿状态**：世界工作台使用 `AUTO_SAVE_DELAY` 与本地 `dirty` 标记延迟保存；页面切换或关闭前会触发保存提示，避免草稿丢失。【F:frontend/src/pages/WorldBuilder/WorldBuilderPage.tsx†L30-L120】
- **统一 API 错误处理**：`services/api.ts` 在 `handleResponse` 中尝试解析 JSON 错误、回退到纯文本，并为 204/205 返回 `undefined`，减少边缘情况处理成本。【F:frontend/src/services/api.ts†L25-L68】
- **权限控制**：`ProtectedRoute` 组件依赖 `AuthContext` 的 `loading/isAuthenticated` 状态决定是否重定向至登录页，防止未验证时渲染敏感内容。【F:frontend/src/App.tsx†L15-L55】【F:frontend/src/contexts/AuthContext.tsx†L1-L60】

## 已知限制与注意事项

1. **配置安全**：示例 `application.properties` 包含默认数据库凭据与加密密钥，部署时必须覆盖并启用环境变量或外部配置，避免泄露风险。【F:backend/src/main/resources/application.properties†L3-L25】
2. **MySQL 依赖**：`spring.jpa.hibernate.ddl-auto=update` 会在模型变更时自动调整表结构，生产环境建议改为 `validate` 并使用 Flyway/Liquibase 控制迁移，防止误删字段。
3. **AI 服务超时与失败**：`OpenAiService` 通过 Spring Retry 自动重试三次，仍失败会抛出 RuntimeException；前端需在按钮上提供 loading/重试提示以提升体验。【F:backend/src/main/java/com/example/ainovel/service/OpenAiService.java†L41-L132】
4. **世界生成并发**：`WorldGenerationWorkflowService` 使用数据库锁控制单世界的任务队列，但目前为同步执行；如需后台异步生成需扩展队列系统并注意幂等性。
5. **代理设置**：后端 `RestTemplateConfig` 支持 SOCKS 代理，生产环境若无需代理应将 `proxy.enabled=false`，否则外部调用会失败。【F:backend/src/main/java/com/example/ainovel/config/RestTemplateConfig.java†L15-L44】

## 后续扩展建议

- 引入统一的审计日志记录 AI 请求输入/输出，以便排查质量问题。
- 为世界生成任务加入异步执行（例如 Spring Batch + 消息队列），避免 HTTP 请求阻塞。
- 在前端增加统一的错误上报与提示组件，减少各页面重复 `message.error` 代码。

掌握上述功能点后，可针对具体需求迅速定位到对应 Controller/Service/前端模块，并识别潜在风险与依赖。

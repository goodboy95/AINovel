# AINovel 后端服务

基于 Spring Boot 3.3 的 RESTful 服务，为前端 AI 小说创作工作台提供认证、故事管理、世界观建模与 AI 调用能力。

## 技术栈

- Spring Boot / Spring Security / Spring Data JPA
- MySQL 8 (默认) + Hibernate 自动迁移
- JSON Web Token (JWT) 认证，AES 加密存储 LLM API Key
- Spring Retry + RestTemplate 调用 OpenAI 兼容接口

## 模块划分

```
backend/src/main/java/com/example/ainovel
├── config              # 安全、CORS、RestTemplate 等全局配置
├── controller          # REST 控制器（认证、故事、大纲、稿件、世界观、Prompt 等）
├── dto                 # API 输入输出 DTO
├── exception           # 自定义异常
├── model               # JPA 实体（故事卡、角色卡、稿件、世界模块等）
├── prompt              # Prompt 模板渲染与上下文工厂
├── repository          # Spring Data JPA 仓库接口
├── service             # 业务服务层与 AI 集成
├── utils               # JWT 工具
└── worldbuilding       # 世界模块定义与加载器
```

- 控制器示例：`StoryController` 处理 `/api/v1/stories` CRUD。【F:backend/src/main/java/com/example/ainovel/controller/StoryController.java†L24-L69】
- 世界观相关的服务集中在 `service/world/`，例如 `WorldGenerationWorkflowService` 负责模块生成队列与重试。【F:backend/src/main/java/com/example/ainovel/service/world/WorldGenerationWorkflowService.java†L52-L205】
- Prompt 模板存储在 `src/main/resources/prompts`，默认模板覆盖故事生成、大纲章节、稿件正文与世界模块提示词。【F:backend/src/main/resources/prompts/default-prompts.yaml†L1-L150】【F:backend/src/main/resources/prompts/world-defaults.yaml†L1-L120】

## 运行与开发

1. 创建数据库并配置 `spring.datasource.*`。
2. 设置关键密钥：`app.encryption.key`、`app.jwt.secret`、`openai.model.default` 等。默认值仅用于开发。【F:backend/src/main/resources/application.properties†L3-L32】
3. 启动服务：
   ```bash
   ./mvnw spring-boot:run
   ```
4. 运行测试：
   ```bash
   ./mvnw test
   ```
5. 构建可执行包：`./mvnw clean package`。

开发时推荐激活 `dev` profile（允许 Vite 地址）：
```bash
./mvnw spring-boot:run -Dspring.profiles.active=dev
```
或在 `application.properties` 中设置 `spring.profiles.active=dev`。【F:backend/src/main/resources/application.properties†L23-L24】

## 核心功能一览

| 功能 | 主要接口 | 说明 |
| --- | --- | --- |
| 用户注册/登录 | `/api/v1/auth/register`, `/api/v1/auth/login`, `/api/auth/validate` | 返回 JWT，供前端存储并通过 `Authorization` 访问其他接口。【F:backend/src/main/java/com/example/ainovel/controller/AuthController.java†L37-L85】|
| 故事与角色 | `/api/v1/conception`, `/api/v1/story-cards/**`, `/api/v1/stories` | 生成故事+角色卡，支持 CRUD 与字段润色。【F:backend/src/main/java/com/example/ainovel/controller/ConceptionController.java†L38-L118】【F:backend/src/main/java/com/example/ainovel/controller/StoryController.java†L31-L69】|
| 大纲 | `/api/v1/outlines/**` | 章节生成、PATCH 更新、场景润色。【F:backend/src/main/java/com/example/ainovel/controller/OutlineController.java†L69-L165】|
| 稿件 | `/api/v1/manuscript/**`, `/api/v1/outlines/{id}/manuscripts` | 场景生成正文、稿件列表、角色变化分析。【F:backend/src/main/java/com/example/ainovel/controller/ManuscriptController.java†L29-L125】|
| 世界观 | `/api/v1/worlds/**`, `/api/v1/world-building/definitions` | 世界 CRUD、模块编辑、生成流水线、发布校验。【F:backend/src/main/java/com/example/ainovel/controller/WorldController.java†L46-L199】【F:backend/src/main/java/com/example/ainovel/controller/WorldBuildingDefinitionController.java†L15-L24】|
| Prompt 模板 | `/api/v1/prompt-templates`, `/api/v1/world-prompts` | 用户可自定义故事/世界 Prompt，支持更新与重置。【F:backend/src/main/java/com/example/ainovel/controller/PromptTemplateController.java†L23-L52】【F:backend/src/main/java/com/example/ainovel/controller/WorldPromptTemplateController.java†L20-L45】|
| AI 对话 | `/api/v1/ai/generate-dialogue` | 根据角色档案与记忆生成对白。【F:backend/src/main/java/com/example/ainovel/controller/AiController.java†L21-L29】|

## 安全与配置注意事项

- **JWT 密钥与 AES 密钥**：必须在生产环境中替换默认配置，建议通过环境变量注入。
- **CORS**：默认仅允许 `http://localhost:8080`，开发 profile 已添加 `http://localhost:5173`，部署时需更新。【F:backend/src/main/resources/application-dev.properties†L4-L10】
- **代理**：如需经代理访问外部 LLM，设置 `proxy.enabled=true` 与主机/端口；否则保持禁用以避免连接失败。【F:backend/src/main/java/com/example/ainovel/config/RestTemplateConfig.java†L15-L44】
- **数据库迁移**：`spring.jpa.hibernate.ddl-auto=update` 便于开发但可能在生产造成结构漂移，建议结合 Flyway 管控。

## 扩展指引

- 新增业务模块时，建议按照 Controller → Service → Repository → DTO 层次实现，并在 `doc/architecture.md` 更新说明。
- 增加 AI 能力时，可实现 `AiService` 接口或扩展 `OpenAiService`，并在 `SettingsService` 中增加 provider 选择逻辑。【F:backend/src/main/java/com/example/ainovel/service/AiService.java†L1-L33】【F:backend/src/main/java/com/example/ainovel/service/SettingsService.java†L20-L55】
- 为世界生成任务引入异步执行或消息队列时，注意 `WorldGenerationWorkflowService` 的事务边界与状态机更新。

## 目录资源

- `src/main/resources/prompts/`：默认 Prompt 与世界模板。
- `src/main/resources/worldbuilding/modules.yml`：世界模块字段与校验规则。
- `src/main/resources/application*.properties`：环境配置模板。

如需更多上下文，请参考 `doc/architecture.md` 与 `doc/features.md`，并保持文档同步更新。

# AINovel 项目开发指南（development_guide.md）

本文档面向新加入的研发成员，帮助你快速完成本地环境搭建、理解项目结构，并掌握端到端功能开发流程。请先阅读项目的系统概述与详细设计文档：[design_document.md](design_document.md) 与 [design_doc.md](design_doc.md)，以及根目录说明 [README.md](README.md)。

---

## 1. 环境搭建

本项目包含前后端两部分，建议在本地以“前后端分离”的方式进行开发与调试：
- 前端开发服务器（Vite）：默认端口 5173
- 后端 Spring Boot 应用：默认端口 8080
- 数据库：MySQL 8.x

为便于环境隔离，建议使用 dev Profile（已提供 [backend/src/main/resources/application-dev.properties](backend/src/main/resources/application-dev.properties)）。

### 1.1 必备软件版本（建议）
- Node.js ≥ 18.x（推荐 20.x）
- npm ≥ 9.x（随 Node.js 安装）
- JDK ≥ 17（Spring Boot 3 通常需要 JDK 17+）
- Maven ≥ 3.8.x
- MySQL ≥ 8.0.x

你可以在以下位置确认或调整依赖与脚本：
- 前端依赖与脚本：[frontend/package.json](frontend/package.json)
- 前端 ESLint 配置：[frontend/eslint.config.js](frontend/eslint.config.js)
- 后端依赖与插件：[backend/pom.xml](backend/pom.xml)

### 1.2 数据库初始化（MySQL）
1) 启动 MySQL 并创建数据库（例如：ainovel）
- 通过命令行：
```
mysql -u root -p
CREATE DATABASE ainovel CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2) 导入初始化 SQL
- 使用命令行：
```
mysql -u root -p ainovel < backend/src/database.sql
```
- 或通过 MySQL Workbench/客户端导入 [backend/src/database.sql](backend/src/database.sql)

3) 在后端开发配置中设置数据库连接
- 打开 [backend/src/main/resources/application-dev.properties](backend/src/main/resources/application-dev.properties)，按需配置（示例）：
```
spring.datasource.url=jdbc:mysql://localhost:3306/ainovel?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=你的密码

# 开发环境可使用自动建表/更新（谨慎用于生产）
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# 如需 JWT/第三方 API Key，请在此或系统环境变量中配置（按需命名）
# jwt.secret=your-jwt-secret
# openai.api.key=...
```

### 1.3 启动后端（Spring Boot）
在项目根目录打开终端或 VSCode 内置终端，进入后端目录：
```
cd backend
mvn -v
mvn clean install
# 使用 dev Profile 启动
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
- 启动成功后，后端服务监听 http://localhost:8080
- 主要配置类与横切配置位于：
  - 安全与 CORS：[backend/src/main/java/com/example/ainovel/config/SecurityConfig.java](backend/src/main/java/com/example/ainovel/config/SecurityConfig.java)、[backend/src/main/java/com/example/ainovel/config/WebConfig.java](backend/src/main/java/com/example/ainovel/config/WebConfig.java)
  - HTTP 客户端配置：[backend/src/main/java/com/example/ainovel/config/RestTemplateConfig.java](backend/src/main/java/com/example/ainovel/config/RestTemplateConfig.java)

如需生产模式整合前端构建产物，本项目已支持在后端的静态资源目录托管（参考 [backend/src/main/resources/static/](backend/src/main/resources/static/) 与 [backend/src/main/java/com/example/ainovel/controller/SpaController.java](backend/src/main/java/com/example/ainovel/controller/SpaController.java)）。

### 1.4 启动前端（Vite + React + TypeScript）
在项目根目录打开终端，进入前端目录：
```
cd frontend
npm ci
# 若网络环境不佳或未锁定包，则使用
# npm install
npm run dev
```
- 启动成功后，访问 http://localhost:5173
- Vite 代理与 API 基础路径可在 [frontend/vite.config.ts](frontend/vite.config.ts) 与 [frontend/src/services/api.ts](frontend/src/services/api.ts) 中确认与调整。
- 典型情况下，前端通过相对路径（如 /api 前缀）访问后端，或在 Vite 代理中将 /api 转发到 http://localhost:8080。

---

## 2. 项目结构

项目采用前后端分离的 Monorepo 布局（根目录仅存放文档与通用配置）。

```
AINovel/
├─ backend/    # Spring Boot 后端
├─ frontend/   # Vite + React + TS 前端
├─ doc/        # 文档（部署/使用/二开等，可在此放置最终交付文档）
├─ design_document.md, design_doc.md, README.md, ...
```

### 2.1 后端结构（Spring Boot）
- 应用入口：
  - [backend/src/main/java/com/example/ainovel/AinovelApplication.java](backend/src/main/java/com/example/ainovel/AinovelApplication.java)

- 配置层（Config）：
  - [backend/src/main/java/com/example/ainovel/config/](backend/src/main/java/com/example/ainovel/config/)
  - 安全策略、安全拦截与 CORS、跨域、静态资源等全局配置（例如 SecurityConfig、WebConfig）
  - HTTP 客户端配置（RestTemplateConfig）等

- 控制器（Controller，暴露 REST API）：
  - [backend/src/main/java/com/example/ainovel/controller/](backend/src/main/java/com/example/ainovel/controller/)
  - 例如：AuthController、ConceptionController、ManuscriptController、OutlineController、SettingsController、StoryController、SpaController

- 传输对象（DTO，Request/Response）：
  - [backend/src/main/java/com/example/ainovel/dto/](backend/src/main/java/com/example/ainovel/dto/)
  - 封装接口输入输出的对象，避免直接暴露实体结构

- 领域模型（Model，JPA 实体）：
  - [backend/src/main/java/com/example/ainovel/model/](backend/src/main/java/com/example/ainovel/model/)
  - 例如：User、UserSetting、StoryCard、OutlineCard、OutlineChapter、OutlineScene、CharacterCard、TemporaryCharacter、ManuscriptSection

- 数据访问（Repository，Spring Data JPA）：
  - [backend/src/main/java/com/example/ainovel/repository/](backend/src/main/java/com/example/ainovel/repository/)
  - 例如：UserRepository、StoryCardRepository、OutlineCardRepository、OutlineChapterRepository、OutlineSceneRepository 等

- 业务服务（Service）：
  - [backend/src/main/java/com/example/ainovel/service/](backend/src/main/java/com/example/ainovel/service/)
  - 核心业务逻辑与第三方 AI 服务对接：
    - AI 服务：`OpenAiService` 直接实现 `AiService` 接口，不再有多种提供商实现。依赖 `AiService` 的地方改为直接注入 `OpenAiService`。
    - 业务域服务：ConceptionService、OutlineService、ManuscriptService、SettingsService、AuthService、StoryService

- 工具与异常：
  - 工具：[backend/src/main/java/com/example/ainovel/utils/JwtUtil.java](backend/src/main/java/com/example/ainovel/utils/JwtUtil.java)
  - 自定义异常：[backend/src/main/java/com/example/ainovel/exception/ResourceNotFoundException.java](backend/src/main/java/com/example/ainovel/exception/ResourceNotFoundException.java)

- 资源与配置：
  - [backend/src/main/resources/application.properties](backend/src/main/resources/application.properties)
  - [backend/src/main/resources/application-dev.properties](backend/src/main/resources/application-dev.properties)
  - 静态资源（生产构建前端产物托管）：[backend/src/main/resources/static/](backend/src/main/resources/static/)

- 测试：
  - [backend/src/test/java/](backend/src/test/java/)
  - 示例：AuthServiceTest、ConceptionServiceTest、OutlineServiceTest、SettingsServiceTest

### 2.2 前端结构（Vite + React + TypeScript + Tailwind）
- 入口与全局：
  - [frontend/index.html](frontend/index.html)
  - [frontend/src/main.tsx](frontend/src/main.tsx)
  - [frontend/src/App.tsx](frontend/src/App.tsx)
  - 样式：[frontend/src/index.css](frontend/src/index.css)、[frontend/src/App.css](frontend/src/App.css)
  - 构建配置：[frontend/vite.config.ts](frontend/vite.config.ts)、[frontend/tailwind.config.js](frontend/tailwind.config.js)、[frontend/tsconfig.json](frontend/tsconfig.json)

- 类型定义与服务：
  - 共享类型定义：[frontend/src/types.ts](frontend/src/types.ts)
  - API 调用服务（封装接口请求）：[frontend/src/services/api.ts](frontend/src/services/api.ts)

- 组件（UI）：
  - 页面/工作台等复合组件：[frontend/src/components/](frontend/src/components/)
  - 示例：Workbench、StoryList、StoryDetail、OutlinePage、OutlineTreeView、ManuscriptWriter、Settings、StoryConception、Login、Register
  - 模态框与表单：
    - [frontend/src/components/modals/](frontend/src/components/modals/)
    - 示例：EditStoryCardModal、EditOutlineModal、AddCharacterModal、EditCharacterCardModal、TemporaryCharacterEditModal、RefineModal
  - 业务表单组件：
    - 例如：ChapterEditForm、SceneEditForm

- Hooks（数据与状态）：
  - [frontend/src/hooks/useOutlineData.ts](frontend/src/hooks/useOutlineData.ts)
  - [frontend/src/hooks/useStoryData.ts](frontend/src/hooks/useStoryData.ts)
  - [frontend/src/hooks/useRefineModal.ts](frontend/src/hooks/useRefineModal.ts)

- 上下文（Context）：
  - [frontend/src/contexts/AuthContext.tsx](frontend/src/contexts/AuthContext.tsx)
  - 用于提供全局认证状态。

### 2.3 前端核心概念

*   **`AuthContext`**:
    *   **作用**: 提供了一个全局的认证状态容器。通过 `AuthProvider` 包裹整个应用，它利用 React Context API 来存储和分发用户认证信息（如用户对象、Token）和加载状态。
    *   **使用**: 组件可以通过 `useAuth` 自定义 Hook 方便地访问认证状态（`user`, `loading`）和操作方法（`login`, `logout`），从而避免了通过 props 逐层传递状态。

*   **`ProtectedRoute`**:
    *   **作用**: 这是一个路由保护组件，用于限制只有登录用户才能访问的页面。
    *   **原理**: 它包裹在需要保护的路由上（例如 `/workbench`）。在渲染子组件之前，它会检查 `AuthContext` 中的 `user` 状态。如果用户未登录（`user` 为 `null`），它会使用 React Router 的 `Navigate` 组件将用户重定向到首页 (`/`)。

---

## 3. 核心 API 变更参考

在进行开发前，请注意以下根据最新功能调整的核心 API：

*   **新增故事创建接口**
    *   **Endpoint**: `POST /api/v1/stories`
    *   **说明**: 用于用户在工作台直接创建新的故事卡。
    *   **请求体**: `StoryCardDto`
    *   **成功响应**: `201 Created`，返回新创建的故事卡对象。

*   **更新用户设置接口**
    *   **Endpoint**: `PUT /api/v1/settings`
    *   **说明**: 更新用户的 AI 配置。`getSettings` 接口不再返回完整的 API 密钥，而是返回一个布尔值 `apiKeyIsSet` 来指示密钥是否已设置。
    *   **请求体**: `SettingsDto` 中不再包含 `llmProvider` 字段，新增了 `baseUrl` 字段。

*   **新增资源管理与查询接口**
    *   **删除故事**:
        *   **Endpoint**: `DELETE /api/v1/stories/{storyId}`
        *   **说明**: 删除一个完整的故事及其所有相关数据（大纲、稿件等）。
        *   **成功响应**: `204 No Content`
    *   **删除稿件**:
        *   **Endpoint**: `DELETE /api/v1/manuscripts/{manuscriptId}`
        *   **说明**: 删除指定的一篇稿件。
        *   **成功响应**: `204 No Content`
    *   **获取故事下的大纲列表**:
        *   **Endpoint**: `GET /api/v1/stories/{storyId}/outlines`
        *   **说明**: 获取指定故事下的所有大纲。
        *   **成功响应**: `200 OK`，返回 `OutlineDto` 列表。
    *   **获取大纲下的稿件列表**:
        *   **Endpoint**: `GET /api/v1/outlines/{outlineId}/manuscripts`
        *   **说明**: 获取指定大纲下的所有稿件。
        *   **成功响应**: `200 OK`，返回 `ManuscriptDto` 列表。
    *   **创建新稿件**:
        *   **Endpoint**: `POST /api/v1/outlines/{outlineId}/manuscripts`
        *   **说明**: 为指定的大纲创建一篇新的稿件。
        *   **成功响应**: `201 Created`，返回新创建的 `ManuscriptDto` 对象。
    *   **获取稿件详情**:
        *   **Endpoint**: `GET /api/v1/manuscripts/{manuscriptId}`
        *   **说明**: 获取一篇稿件及其所有章节的详细内容。
        *   **成功响应**: `200 OK`，返回 `ManuscriptWithSectionsDto` 对象。

*   **更新场景接口**
    *   **Endpoint**: `PATCH /api/v1/scenes/{id}`
    *   **说明**: 更新大纲中的某个场景。
    *   **请求体**: `SceneDto` 中新增了 `expectedWords` 字段，并将 `presentCharacters` 修改为 `presentCharacterIds` (一个 `Long` 型数组)。

*   **新增 Token 验证接口**
    *   **Endpoint**: `GET /api/auth/validate`
    *   **说明**: 用于验证客户端 `localStorage` 中存储的 JWT Token 是否有效，以实现自动登录。
    *   **请求头**: `Authorization: Bearer <token>`
    *   **成功响应 (200 OK)**: Token 有效，返回包含用户基本信息的 JSON 对象，例如：`{ "username": "current_user" }`。
    *   **失败响应 (401 Unauthorized)**: Token 无效、过期或不存在。

## 4. 开发流程指南（示例：为“故事卡”增加“标签 tags”字段）

本示例演示端到端改动：后端模型/DTO → 持久化/业务 → 控制器 API → 前端类型定义 → 组件显示与编辑 → 服务调用。你可以将 “标签” 存储为后端字符串（逗号分隔）或建立关联表。本示例采用“字符串（逗号分隔）”以降低复杂度。

### 3.1 后端改动

1) 修改实体（Model）
- 打开并编辑故事卡实体：[backend/src/main/java/com/example/ainovel/model/StoryCard.java](backend/src/main/java/com/example/ainovel/model/StoryCard.java)
- 新增字段（示例设计）：
  - 字段名：tags
  - 数据类型：String（内容为逗号分隔的标签，如 "科幻,AI,冒险"）
  - 数据库列：VARCHAR(255) 或 TEXT（按标签长度需求）
- 注意：为保持兼容，可以为 tags 设置默认空字符串，并在读取时将空值转为 ""。

2) 更新数据库表结构
- 若使用开发模式自动建表/更新（application-dev.properties 中 spring.jpa.hibernate.ddl-auto=update），启动后将自动添加列。
- 若生产或关闭自动更新，需执行手工 SQL（根据实际表名调整，下例仅示意）：
```
ALTER TABLE story_card ADD COLUMN tags VARCHAR(255) NOT NULL DEFAULT '';
```
- 确认导入/迁移策略，避免覆盖现有数据。

3) DTO（数据传输对象）
- 若已有面向前端的 StoryCard 相关 DTO，请在对应文件中新增 tags 字段。
- 若尚无 StoryCard 的专用 DTO，可在此目录新建并使用：
  - [backend/src/main/java/com/example/ainovel/dto/](backend/src/main/java/com/example/ainovel/dto/)
- 在 Service 或 Controller 中进行实体与 DTO 的相互转换（包含 tags）。

4) Repository（数据访问）
- 位置：[backend/src/main/java/com/example/ainovel/repository/StoryCardRepository.java](backend/src/main/java/com/example/ainovel/repository/StoryCardRepository.java)
- 通常新增字段无需修改仓储接口。
- 如需按标签搜索，可新增派生查询方法（示例：按包含关键字查询），并在 Service 中使用。

5) Service（业务逻辑）
- 位置（根据实际模块归属选择相应服务，一般是处理大纲/故事卡的服务）：[backend/src/main/java/com/example/ainovel/service/OutlineService.java](backend/src/main/java/com/example/ainovel/service/OutlineService.java)
- 增加读写 tags 的逻辑，例如在保存/更新故事卡时处理：
  - 入参 DTO 的 tags 字段 → 实体属性
  - 校验与清洗（去重、去空格、限制长度等）

6) Controller（API 暴露）
- 位置：可在与大纲/故事卡相关的控制器中添加或扩展，常见在 [backend/src/main/java/com/example/ainovel/controller/OutlineController.java](backend/src/main/java/com/example/ainovel/controller/OutlineController.java)
- 示例接口（路由仅示意，按项目既有风格调整）：
  - GET /api/story-cards/{id} → 返回含 tags 的故事卡详情
  - PUT /api/story-cards/{id}/tags → 更新指定故事卡的 tags 字段
- 请求/响应示例：
```
PUT /api/story-cards/123/tags
Content-Type: application/json

{
  "tags": "科幻,AI,冒险"
}
```
返回：
```
{
  "id": 123,
  "title": "示例故事卡",
  "summary": "...",
  "tags": "科幻,AI,冒险"
}
```

7) 单元测试
- 在测试目录扩展或新增用例，覆盖映射、读写与接口行为：
  - [backend/src/test/java/com/example/ainovel/service/OutlineServiceTest.java](backend/src/test/java/com/example/ainovel/service/OutlineServiceTest.java)

### 3.2 前端改动

1) 更新前端类型定义
- 打开类型定义文件：[frontend/src/types.ts](frontend/src/types.ts)
- 在故事卡相关类型上新增可选字段：
  - 示例：tags?: string
- 若使用逗号分隔的字符串，在组件中按需 split/join 与输入序列化。

2) 更新 API 服务
- 打开接口封装文件：[frontend/src/services/api.ts](frontend/src/services/api.ts)
- 新增或扩展请求方法：
  - getStoryCard(id)
  - updateStoryCardTags(id, { tags })
- 确认基础路径与代理（/api → 后端 8080），避免跨域问题。

3) 更新组件（显示与编辑）
- 编辑模态框示例位置：[frontend/src/components/modals/EditStoryCardModal.tsx](frontend/src/components/modals/EditStoryCardModal.tsx)
  - 新增输入控件（如 Input/TextArea）用于编辑 tags（用中文逗号或英文逗号分隔）
  - 表单提交时，将 UI 值序列化为字符串传给 update 接口
- 列表/树视图显示：
  - 可以在大纲/故事卡展示处（例如 [frontend/src/components/OutlineTreeView.tsx](frontend/src/components/OutlineTreeView.tsx)、[frontend/src/components/OutlinePage.tsx](frontend/src/components/OutlinePage.tsx)）追加 tags 的只读显示，便于检索与识别。

4) 自测流程
- 启动后端（dev Profile）与前端（vite dev）
- 新建/编辑故事卡，填写 tags 并提交
- 刷新页面或通过接口查看，确认 tags 正常持久化与显示

5) 可选增强
- 前端将字符串 tags 解析为数组，提供标签选择器/可删除的标签 UI
- 后端若希望结构化存储，可将 tags 拆为关联表并提供标签维度检索接口

---

## 4. 编码规范与工具

### 4.1 前端规范
- 使用 TypeScript，优先明确类型，避免 any
- ESLint：脚本与配置见 [frontend/eslint.config.js](frontend/eslint.config.js)
  - 常用命令：
  ```
  cd frontend
  npm run lint
  ```
- 风格化：若启用 Prettier 或 ESLint Stylistic 规则，统一缩进、引号、分号与导入顺序
- 组件与文件命名：PascalCase（组件）、kebab-case（资源）、camelCase（变量/函数）
- 请求封装在 [frontend/src/services/api.ts](frontend/src/services/api.ts)，避免在组件中散落 fetch/axios 逻辑
- UI 样式：Tailwind CSS，尽量复用原子类与组合

### 4.2 后端规范
- 分层清晰：Controller（仅编排与校验）→ Service（业务逻辑）→ Repository（数据访问）→ Model（实体）
- DTO 与实体分离：Controller 输入输出使用 DTO（位于 [backend/src/main/java/com/example/ainovel/dto/](backend/src/main/java/com/example/ainovel/dto/)）
- 异常处理：使用自定义异常（如 [backend/src/main/java/com/example/ainovel/exception/ResourceNotFoundException.java](backend/src/main/java/com/example/ainovel/exception/ResourceNotFoundException.java)）并在控制器层或全局异常处理处统一返回
- 安全与认证：基于 Spring Security + JWT（参考 [backend/src/main/java/com/example/ainovel/config/SecurityConfig.java](backend/src/main/java/com/example/ainovel/config/SecurityConfig.java)、[backend/src/main/java/com/example/ainovel/utils/JwtUtil.java](backend/src/main/java/com/example/ainovel/utils/JwtUtil.java)）
- 第三方 AI 服务：抽象在服务层，具体实现位于：
  - [backend/src/main/java/com/example/ainovel/service/OpenAiService.java](backend/src/main/java/com/example/ainovel/service/OpenAiService.java) (当前唯一的 AI 服务实现)
- 配置与环境：
  - 公共配置：[backend/src/main/resources/application.properties](backend/src/main/resources/application.properties)
  - 开发配置：[backend/src/main/resources/application-dev.properties](backend/src/main/resources/application-dev.properties)
  - 不将敏感信息提交至仓库，使用环境变量或私密配置

### 4.3 常用脚本与命令

- 前端（在 [frontend/](frontend/) 下）：
  - 安装依赖：
  ```
  npm ci
  # 或
  npm install
  ```
  - 开发启动：
  ```
  npm run dev
  ```
  - 产物构建与本地预览：
  ```
  npm run build
  npm run preview
  ```
  - 代码检查：
  ```
  npm run lint
  ```

- 后端（在 [backend/](backend/) 下）：
  - 构建与测试：
  ```
  mvn clean install
  mvn test
  ```
  - 开发启动（dev Profile）：
  ```
  mvn spring-boot:run -Dspring-boot.run.profiles=dev
  ```
  - 打包：
  ```
  mvn package
  ```

---

## 5. 运行与联调注意事项

- 端口与代理
  - 后端默认 8080，前端 Vite 默认 5173
  - 前端调用 API 时，推荐通过相对路径（如 /api 前缀）+ Vite 代理，避免 CORS
  - 如需改动，检查 [frontend/vite.config.ts](frontend/vite.config.ts) 与 [frontend/src/services/api.ts](frontend/src/services/api.ts)

- CORS 设置
  - 若出现跨域问题，请确认后端安全与 CORS 配置是否放行前端地址（参考 [backend/src/main/java/com/example/ainovel/config/SecurityConfig.java](backend/src/main/java/com/example/ainovel/config/SecurityConfig.java)、[backend/src/main/java/com/example/ainovel/config/WebConfig.java](backend/src/main/java/com/example/ainovel/config/WebConfig.java)）

- 数据库迁移
  - 开发环境可使用 ddl-auto=update 自动更新
  - 生产环境建议采用手工 SQL 或引入迁移工具（如 Flyway/Liquibase），避免隐式结构变更

- 静态资源托管（生产）
  - 前端构建产物可拷贝/输出到后端静态目录 [backend/src/main/resources/static/](backend/src/main/resources/static/)，由 Spring Boot 统一对外服务
  - 或采用独立的前端静态资源服务器 + 后端 API 服务分离部署

---

## 6. 参考清单（快速入口）

- 文档
  - 设计文档与系统概述：[design_document.md](design_document.md)、[design_doc.md](design_doc.md)
  - 根目录说明：[README.md](README.md)

- 后端核心目录
  - 入口类：[backend/src/main/java/com/example/ainovel/AinovelApplication.java](backend/src/main/java/com/example/ainovel/AinovelApplication.java)
  - 控制器：[backend/src/main/java/com/example/ainovel/controller/](backend/src/main/java/com/example/ainovel/controller/)
  - 服务层：[backend/src/main/java/com/example/ainovel/service/](backend/src/main/java/com/example/ainovel/service/)
  - 仓储层：[backend/src/main/java/com/example/ainovel/repository/](backend/src/main/java/com/example/ainovel/repository/)
  - 实体模型：[backend/src/main/java/com/example/ainovel/model/](backend/src/main/java/com/example/ainovel/model/)
  - DTO：[backend/src/main/java/com/example/ainovel/dto/](backend/src/main/java/com/example/ainovel/dto/)
  - 配置与资源：[backend/src/main/java/com/example/ainovel/config/](backend/src/main/java/com/example/ainovel/config/)、[backend/src/main/resources/](backend/src/main/resources/)

- 前端核心目录
  - 类型：[frontend/src/types.ts](frontend/src/types.ts)
  - 服务：[frontend/src/services/api.ts](frontend/src/services/api.ts)
  - 组件与模态框：[frontend/src/components/](frontend/src/components/)、[frontend/src/components/modals/](frontend/src/components/modals/)
  - Hooks：[frontend/src/hooks/](frontend/src/hooks/)
  - 构建与配置：[frontend/vite.config.ts](frontend/vite.config.ts)、[frontend/tailwind.config.js](frontend/tailwind.config.js)、[frontend/tsconfig.json](frontend/tsconfig.json)

---

以上为 AINovel 项目的开发入门指南。请在开始编码前阅读并遵循本文档与设计文档，确保前后端接口契约、模型定义与业务流程保持一致。祝开发顺利。
# AI 小说创作平台 - 开发指导文档

## 1. 项目概述与核心目标

**项目名称：** AI Novelist (暂定)

**核心目标：** 打造一个集故事构思、大纲设计、正文创作于一体的智能化小说创作平台。平台深度集成大型语言模型（LLM），旨在赋能写作者，将他们的创意快速转化为结构完整、内容丰富的故事文本，同时提供强大的管理和自定义功能，优化整个创作流程。

## 2. 技术选型

*   **后端：** Spring Boot + Spring Security + JPA (Hibernate) + MySQL 8.0
    *   **目录：** `backend/`
    *   **理由：** Spring Boot 提供了快速开发、易于配置的特性。Spring Security 负责健壮的用户认证和授权。JPA 简化了数据库操作。MySQL 是成熟可靠的关系型数据库。
*   **前端：** React (使用 Vite) + TypeScript + Zustand + Tailwind CSS
    *   **目录：** `frontend/`
    *   **理由：** React 拥有庞大的生态和社区，组件化思想非常适合构建复杂的单页应用 (SPA)。Vite 提供极速的开发体验。TypeScript 提供类型安全。Zustand 是一个轻量级的状态管理库。Tailwind CSS 可以高效地构建自定义UI。
*   **AI 集成：** 后端统一封装 AI 服务调用接口，前端通过 API 与后端交互。

## 3. 系统架构设计

### 3.1. 整体架构 (分层架构)

1.  **前端 (Presentation Layer):** 用户交互界面，负责数据展示和用户输入。
2.  **后端 (Business Logic Layer):**
    *   **Controller:** 接收前端请求，验证输入，调用 Service 层。
    *   **Service:** 核心业务逻辑，包括用户管理、卡片管理、AI 交互等。
    *   **Repository:** 数据访问层，通过 JPA 与数据库交互。
    *   **AI Service (Adapter):** 封装对不同 LLM（OpenAI, Claude, Gemini）的调用逻辑，对上层 Service 提供统一接口。这是一个典型的**适配器模式**应用场景。
3.  **数据层 (Data Persistence Layer):** MySQL 数据库，存储所有持久化数据。

### 3.2. API 设计理念

采用 RESTful API 设计风格。API 应具备良好的自描述性、无状态性，并使用标准的 HTTP 方法 (GET, POST, PUT, DELETE)。所有 API 都应以 `/api/v1/` 为前缀。

## 4. 数据库表结构设计 (核心表)

**`users`**
| 列名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | 用户唯一标识 |
| `username` | VARCHAR(50) | UNIQUE, NOT NULL | 用户名 |
| `password` | VARCHAR(255) | NOT NULL | 加密后的密码 |
| `email` | VARCHAR(100) | UNIQUE, NOT NULL | 邮箱 |
| `created_at` | TIMESTAMP | | 创建时间 |

**`user_settings`**
| 列名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | 设置唯一标识 |
| `user_id` | BIGINT | FK -> users.id | 关联用户 |
| `llm_provider` | VARCHAR(20) | | AI提供商 (openai, claude, gemini) |
| `model_name` | VARCHAR(100) | | 模型名称 |
| `api_key` | VARCHAR(512) | | **加密存储**的 API Key |
| `custom_prompt`| TEXT | | **(扩充)** 全局自定义指令/系统提示 |

**`story_cards`**
| 列名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | 故事卡唯一标识 |
| `user_id` | BIGINT | FK -> users.id | 关联用户 |
| `title` | VARCHAR(255) | NOT NULL | 故事标题 |
| `genre` | VARCHAR(50) | | **(扩充)** 故事类型 (如科幻, 奇幻) |
| `tone` | VARCHAR(50) | | **(扩充)** 故事基调 (如黑暗, 幽默) |
| `synopsis` | TEXT | | 故事简介 |
| `story_arc` | TEXT | | 故事走向/核心情节 |
| `created_at` | TIMESTAMP | | 创建时间 |
| `updated_at` | TIMESTAMP | | 更新时间 |

**`character_cards`**
| 列名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | 角色卡唯一标识 |
| `user_id` | BIGINT | FK -> users.id | 关联用户 |
| `story_card_id`| BIGINT | FK -> story_cards.id | 关联的故事卡 |
| `name` | VARCHAR(100) | NOT NULL | 角色姓名 |
| `synopsis` | TEXT | | 角色简介 (性别, 年龄, 外貌, 性格等) |
| `details` | TEXT | | 角色详情 (过往经历, 详细设定等) |
| `relationships`| TEXT | | 人际关系 (建议用JSON格式存储) |
| `avatar_url` | VARCHAR(255) | | **(扩充)** 角色头像URL |
| `created_at` | TIMESTAMP | | 创建时间 |
| `updated_at` | TIMESTAMP | | 更新时间 |

**`outline_cards`**
| 列名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | 大纲卡唯一标识 |
| `user_id` | BIGINT | FK -> users.id | 关联用户 |
| `story_card_id`| BIGINT | FK -> story_cards.id | 关联的故事卡 |
| `title` | VARCHAR(255) | | **(扩充)** 大纲标题 |
| `point_of_view`| VARCHAR(20) | | **(扩充)** 叙事视角 (第一人称, 第三人称) |
| `created_at` | TIMESTAMP | | 创建时间 |
| `updated_at` | TIMESTAMP | | 更新时间 |

**`outline_chapters`**
| 列名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | 章节唯一标识 |
| `outline_card_id`| BIGINT | FK -> outline_cards.id | 关联的大纲卡 |
| `chapter_number`| INT | NOT NULL | 章节序号 |
| `title` | VARCHAR(255) | | 章节标题 |
| `synopsis` | TEXT | | 本章梗概 |

**`outline_scenes`**
| 列名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | 场景/小节唯一标识 |
| `chapter_id` | BIGINT | FK -> outline_chapters.id | 关联的章节 |
| `scene_number` | INT | NOT NULL | 场景/小节序号 |
| `synopsis` | TEXT | | 本节梗概 |
| `expected_words`| INT | | 预期字数 |

**`manuscript_sections`**
| 列名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | 正文小节唯一标识 |
| `scene_id` | BIGINT | FK -> outline_scenes.id | 关联的大纲小节 |
| `content` | LONGTEXT | | 生成的正文内容 |
| `version` | INT | | **(扩充)** 版本号，用于支持多版本生成 |
| `is_active` | BOOLEAN | | **(扩充)** 是否为当前选定的版本 |
| `created_at` | TIMESTAMP | | 创建时间 |

## 5. 核心功能模块详解

### 5.1. 用户认证与设置模块
*   **UI/UX:**
    *   标准的注册/登录表单。
    *   设置页面提供清晰的表单，用于填写/修改 LLM Provider, Model Name, API Key。
    *   API Key 输入框应为密码类型，并提供一个“测试连接”按钮。
*   **API Endpoints:**
    *   `POST /api/v1/auth/register`
    *   `POST /api/v1/auth/login`
    *   `GET /api/v1/settings`
    *   `PUT /api/v1/settings`
    *   `POST /api/v1/settings/test`
*   **AI 集成逻辑 (测试连接):**
    *   后端接收到测试请求后，使用用户提供的凭据，向对应的 LLM 发送一个简单的、低成本的请求（如 "hello"），验证凭据是否有效。
*   **安全:**
    *   密码必须使用 BCrypt 等强哈希算法进行加密。
    *   API Key 在存入数据库前必须进行对称加密（如 AES），密钥存储在安全的环境变量中。
    *   所有需要登录的 API 都需要通过 Spring Security 进行 Token（如 JWT）验证。

### 5.2. 故事构思模块
*   **UI/UX:**
    *   一个简洁的输入框，引导用户输入“一句话想法”。
    *   提供“故事类型”和“故事基调”的下拉选择框。
    *   生成后，故事卡和角色卡以卡片形式并排展示。
    *   卡片内容应为可编辑的文本区域。每个区域旁边都有一个“AI 优化”按钮。
    *   管理区域应提供列表视图，支持按标题搜索和按类型筛选。
*   **API Endpoints:**
    *   `POST /api/v1/conception` (生成故事和角色)
    *   `PUT /api/v1/story-cards/{id}` (手动更新故事卡)
    *   `POST /api/v1/story-cards/{id}/refine` (AI 优化故事卡)
    *   `PUT /api/v1/character-cards/{id}` (手动更新角色卡)
    *   `POST /api/v1/character-cards/{id}/refine` (AI 优化角色卡)
    *   `GET /api/v1/story-cards` (获取所有故事卡)
*   **AI 集成逻辑 (Prompt 示例):**
    *   **初次生成:**
        ```
        你是一个富有想象力的故事作家。请根据以下信息，为我构思一个故事。
        用户想法: "${userInput}"
        故事类型: "${genre}"
        故事基调: "${tone}"

        请以JSON格式返回，包含两个键: "storyCard" 和 "characterCards"。
        "storyCard" 的值应包含 "title", "synopsis", "storyArc"。
        "characterCards" 的值应为一个数组，包含至少2个主要角色。每个角色对象应包含 "name", "synopsis" (性别、年龄、外貌、性格), "details" (背景故事), "relationships"。
        ```
    *   **AI 优化:**
        ```
        你是一个编辑。请根据我的修改意见，优化以下文本。
        原始文本: "${originalText}"
        我的意见: "${userFeedback}"
        请只返回优化后的文本内容。
        ```

### 5.3. 大纲设计模块
*   **UI/UX:**
    *   引导用户选择一个故事卡和多个关联的角色卡。
    *   输入框用于设置章节数。
    *   生成后，大纲以可折叠的树状结构展示 (Chapter -> Scene)。
    *   每一节梗概都是可编辑的，并有“AI 优化”按钮。
    *   **(扩充)** 支持拖拽调整章节和场景的顺序。
*   **API Endpoints:**
    *   `POST /api/v1/outlines` (生成大纲)
    *   `GET /api/v1/outlines/{id}` (获取大纲详情)
    *   `PUT /api/v1/outlines/scenes/{id}` (更新某一节梗概)
    *   `POST /api/v1/outlines/scenes/{id}/refine` (AI 优化梗概)
*   **AI 集成逻辑 (Prompt 示例):**
    ```
    你是一个专业的小说大纲设计师。请根据以下信息，设计一个详细的故事大纲。
    故事信息:
    - 标题: ${story.title}
    - 走向: ${story.storyArc}
    主要角色:
    - ${character1.name}: ${character1.synopsis}
    - ${character2.name}: ${character2.synopsis}
    要求:
    - 总章节数: ${numberOfChapters}
    - 叙事视角: ${pointOfView}

    请以JSON格式返回。根对象包含一个 "chapters" 键，其值为一个数组。
    每个章节对象应包含 "chapterNumber", "title", "synopsis" 和一个 "scenes" 数组。
    每个场景对象应包含 "sceneNumber", "synopsis", "expectedWords"。
    请确保章节和场景的梗概连贯且符合故事走向和角色设定。
    ```

### 5.4. 故事创作模块
*   **UI/UX:**
    *   左侧为大纲的树形导航器，中间为正文编辑区，右侧为关联信息（故事/角色卡片）。
    *   编辑区下方有一个醒目的“生成本节内容”按钮。
    *   **(扩充)** 点击生成后，可以提供2-3个不同风格的版本供用户选择。
    *   **(扩充)** 提供“导出”功能，可导出为 .txt, .md, .docx 格式。
    *   实现**自动保存**功能，防止用户丢失内容。
*   **API Endpoints:**
    *   `POST /api/v1/manuscript/scenes/{sceneId}/generate` (生成正文)
    *   `PUT /api/v1/manuscript/sections/{sectionId}` (手动更新正文)
    *   `GET /api/v1/manuscript/outlines/{outlineId}` (获取某大纲的所有已生成正文)
*   **AI 集成逻辑 (Prompt 示例):**
    ```
    你是一位才华横溢的小说家。现在请你接续创作故事。
    **全局信息:**
    - 故事简介: ${story.synopsis}
    - 叙事视角: ${outline.pointOfView}
    **主要角色设定:**
    ${characterProfiles}
    **本章梗概:**
    ${chapter.synopsis}
    **本节梗概:**
    ${scene.synopsis}
    **上下文:**
    - 上一节内容: "${previousSectionContent}"
    - 本章前面内容: "${currentChapterPreviousContent}"

    请根据以上所有信息，创作本节的详细内容，字数在 ${scene.expectedWords} 字左右。文笔要生动，符合故事基调和人物性格。请直接开始写正文，不要包含任何解释性文字。
    ```

## 6. 开发路线图 (建议)

*   **第一阶段 (MVP - 核心功能):**
    1.  **[已完成]** 搭建前后端项目框架 (`backend` 和 `frontend` 目录)。
    2.  **[已完成]** 实现用户注册、登录、认证。
    3.  **[已完成]** 实现设置模块，完成至少一种 LLM (如 OpenAI) 的调用封装。
    4.  **[已完成]** 完成故事构思模块的完整闭环（生成、查看、编辑、管理）。
*   **第二阶段 (创作核心):**
    1.  **[已完成]** 完成大纲设计模块的完整闭环。
    2.  完成故事创作模块的核心功能（生成和保存）。
    3.  实现基本的树状导航和内容展示。
*   **第三阶段 (体验优化与扩展):**
    1.  实现“AI 优化/修改”功能。
    2.  实现多版本生成与选择功能。
    3.  完善 UI/UX，增加拖拽、自动保存等高级功能。
    4.  实现内容导出功能。
    5.  集成 Claude 和 Gemini 的 API 调用。

## 7. 构建与部署

### 7.1. 前端构建

要构建前端应用程序，请在 `frontend` 目录中运行以下命令：

```bash
npm run build
```

这将生成一个 `dist` 目录，其中包含用于生产的静态文件。

### 7.2. 后端部署

构建前端后，需要将静态文件复制到后端的 `src/main/resources/static` 目录中。可以使用以下命令（在项目根目录中运行）：

```bash
xcopy frontend\dist backend\src\main\resources\static /E /I /Y
```

然后，可以使用以下命令运行后端服务器：

```bash
cd backend
./mvnw spring-boot:run
```

该应用程序将在 `http://localhost:8080` 上可用。

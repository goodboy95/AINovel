# AINovel 系统概述

## 1. 系统用途

AINovel 是一个利用人工智能辅助小说创作的 Web 应用。它旨在为写作者提供一个从故事构思、大纲设计、角色创建到稿件写作的全流程创作平台，并通过集成多种大型语言模型（LLM）来提升创作效率和激发灵感。

## 2. 主要功能模块

系统围绕小说创作的核心流程，构建了以下几个主要功能模块：

*   **用户认证**：提供用户注册和登录功能，保障用户数据的安全性。
*   **故事构思**：用户可以输入简单的想法或关键词，系统通过 AI 生成完整的故事设定，包括故事背景、核心情节、主要角色等，并以“故事卡”的形式进行管理。
*   **角色管理**：支持创建和管理故事中的角色，每个角色以“角色卡”的形式存在，包含姓名、简介、背景故事、人际关系等详细信息。
*   **大纲编辑**：提供结构化的大纲编辑功能，用户可以创建和管理故事的章节和场景。系统支持 AI 辅助生成章节内容和场景细节。
*   **稿件写作**：在确定大纲后，用户可以进入稿件写作阶段。系统能够根据场景描述，通过 AI 自动生成具体的章节内容，并支持用户对生成的内容进行修改和版本管理。
*   **AI 润色**：在创作的各个环节（如故事构思、角色设定、大纲编写、稿件撰写），用户都可以调用 AI 对指定的文本进行润色、改写或扩写。
*   **个性化设置**：用户可以配置自己的 OpenAI API Key、模型名称以及自定义的 `baseUrl`，以实现更灵活的 AI 服务接入。

## 3. 整体架构

AINovel 采用前后端分离的架构。

### 3.1. 前端

*   **框架**: 使用 React 作为核心 UI 框架，并采用 TypeScript 进行开发，以增强代码的健壮性。
*   **UI 库**: 结合使用 Ant Design 组件库和 Tailwind CSS，快速构建美观且一致的用户界面。
*   **路由**: 使用 React Router 管理应用的页面导航。
*   **核心页面**: 系统的核心操作界面，如工作台、大纲编辑和故事创作，均采用了双栏布局设计，将列表/导航与内容编辑区整合在同一页面，以简化操作流程。
*   **构建工具**: 采用 Vite 作为构建工具，提供快速的开发服务器和高效的打包性能。

### 3.2. 后端

*   **框架**: 基于 Spring Boot 构建，提供稳定、高效的 RESTful API 服务。
*   **语言**: 使用 Java 21。
*   **数据库**: 使用 MySQL 作为主数据库，通过 Spring Data JPA 和 Hibernate 进行数据持久化操作。
*   **认证与安全**: 采用 Spring Security 进行安全管理，并使用 JSON Web Tokens (JWT) 实现无状态的用户认证。
*   **AI 服务集成**: 系统当前统一使用 OpenAI 作为后端 AI 服务。通过 `OpenAiService` 直接提供服务。
*   **控制器**: 新增了 `StoryController`，专门用于处理故事（StoryCard）相关的 CRUD 操作，实现了更清晰的职责分离。

## 4. 数据模型关系图 (Mermaid)

```mermaid
erDiagram
    USER ||--o{ STORY_CARD : "has"
    USER ||--o{ USER_SETTING : "has"
    STORY_CARD ||--o{ CHARACTER_CARD : "has"
    STORY_CARD ||--o{ OUTLINE_CARD : "has"
    OUTLINE_CARD ||--o{ OUTLINE_CHAPTER : "has"
    OUTLINE_CHAPTER ||--o{ OUTLINE_SCENE : "has"
    OUTLINE_SCENE ||--o{ MANUSCRIPT_SECTION : "corresponds to"
    OUTLINE_SCENE ||--o{ TEMPORARY_CHARACTER : "has"

    USER {
        Long id PK
        String username
        String password
        String email
    }
    USER_SETTING {
        Long id PK
        Long user_id FK
        String base_url
        String model_name
        String api_key
    }
    STORY_CARD {
        Long id PK
        Long user_id FK
        String title
        String genre
        String tone
        String synopsis
    }
    CHARACTER_CARD {
        Long id PK
        Long story_card_id FK
        String name
        String synopsis
        String details
    }
    OUTLINE_CARD {
        Long id PK
        Long story_card_id FK
        String title
        String point_of_view
    }
    OUTLINE_CHAPTER {
        Long id PK
        Long outline_card_id FK
        Integer chapter_number
        String title
        String synopsis
    }
    OUTLINE_SCENE {
        Long id PK
        Long chapter_id FK
        Integer scene_number
        String synopsis
        Integer expected_words
        String present_characters "存储角色ID的JSON数组字符串"
    }
    MANUSCRIPT_SECTION {
        Long id PK
        Long scene_id FK
        String content
        Integer version
    }
    TEMPORARY_CHARACTER {
        Long id PK
        Long scene_id FK
        String name
        String summary
    }
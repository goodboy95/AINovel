# 项目结构概述

本文档旨在详细解析项目的前后端代码结构，帮助开发者快速理解项目全貌。

## Backend (Java - Spring Boot)

后端采用 Spring Boot 框架，遵循经典的分层架构模式，主要包括 Controller、Service、Repository、Model 和 DTO 等。

### 核心目录: `src/main/java/com/example/ainovel/`

| 路径 | 文件名 | 主要作用 |
| :--- | :--- | :--- |
| **`controller`** | | **API 接口层**：负责接收前端请求，并调用相应的 Service 进行业务处理。 |
| | `AiController.java` | 处理与 AI 模型交互的请求，如文本生成、润色等。 |
| | `AuthController.java` | 负责用户认证，包括登录和注册。 |
| | `ConceptionController.java` | 处理故事构思（灵感）相关的请求。 |
| | `ManuscriptController.java` | 稿件管理，包括章节生成、内容更新等。 |
| | `OutlineController.java` | 故事大纲管理，处理大纲的创建、查询和编辑。 |
| | `PromptTemplateController.java` | 提示词模板管理。 |
| | `SettingsController.java` | 用户设置管理。 |
| | `StoryController.java` | 故事管理，处理故事卡片的增删改查。 |
| | `MaterialController.java` | 素材库接口，支持创建、上传及检索。 |
| | `WorldBuildingDefinitionController.java` | 世界观模块定义相关的接口。 |
| | `WorldController.java` | 世界观设定管理。 |
| | `WorldPromptTemplateController.java` | 世界观提示词模板管理。 |
| **`dto`** | | **数据传输对象 (Data Transfer Object)**：用于在不同层之间传输数据。 |
| | `ChapterDto.java` | 章节数据传输对象。 |
| | `CharacterChangeLogDto.java` | 角色变化日志数据。 |
| | `CharacterDialogueResponse.java` | 角色对话生成响应。 |
| | `ConceptionRequest.java` / `ConceptionResponse.java` | 故事构思的请求和响应数据。 |
| | `GenerateChapterRequest.java` | 生成章节的请求数据。 |
| | `GenerateManuscriptSectionRequest.java` | 生成稿件片段的请求数据。 |
| | `LoginRequest.java` / `RegisterRequest.java` | 登录和注册的请求数据。 |
| | `ManuscriptDto.java` / `ManuscriptWithSectionsDto.java` | 稿件及其片段的数据。 |
| | `OutlineDto.java` / `OutlineRequest.java` | 大纲的请求和响应数据。 |
| | `RefineRequest.java` / `RefineResponse.java` | 文本润色的请求和响应数据。 |
| | `SettingsDto.java` | 用户设置数据。 |
| | `StoryCardDto.java` | 故事卡片数据。 |
| | `material/MaterialCreateRequest.java` 等 | 素材库相关 DTO（创建、检索、导入任务响应）。 |
| **`model`** | | **数据模型 (Entity)**：与数据库表对应的实体类。 |
| | `CharacterCard.java` | 角色卡片实体。 |
| | `CharacterChangeLog.java` | 角色变化日志实体。 |
| | `Manuscript.java` / `ManuscriptSection.java` | 稿件及稿件片段实体。 |
| | `OutlineCard.java` | 大纲卡片实体。 |
| | `UserSetting.java` | 用户设置实体。 |
| | `material/Material.java` / `MaterialChunk.java` / `FileImportJob.java` | 素材库实体，包含文本切片与文件导入作业。 |
| **`repository`** | | **数据访问层 (Repository)**：负责与数据库进行交互。 |
| | `CharacterCardRepository.java` | 角色卡片数据仓库。 |
| | `ManuscriptRepository.java` | 稿件数据仓库。 |
| | `OutlineCardRepository.java` | 大纲卡片数据仓库。 |
| | `UserRepository.java` | 用户数据仓库。 |
| | `material/MaterialRepository.java` 等 | 素材主数据、文本块与导入任务的仓库。 |
| **`service`** | | **业务逻辑层 (Service)**：处理核心业务逻辑。 |
| | `AiService.java` / `OpenAiService.java` | AI 服务，封装与 OpenAI 等模型的交互。 |
| | `AuthService.java` | 用户认证服务。 |
| | `ConceptionService.java` | 故事构思服务。 |
| | `ManuscriptService.java` | 稿件管理服务。 |
| | `OutlineService.java` | 大纲管理服务。 |
| | `StoryService.java` | 故事管理服务。 |
| | `world/` | 世界观相关服务，如模块管理、生成等。 |
| | `material/MaterialService.java` / `material/FileImportService.java` | 素材创建、文本切片与文件上传解析流程。 |
| **`prompt`** | | **提示词工程**：管理和构建与 AI 交互的提示词。 |
| | `TemplateEngine.java` | 提示词模板引擎。 |
| | `context/` | 构建不同场景下（如故事、大纲）的提示词上下文。 |
| **`config`** | | **配置类** |
| | `SecurityConfig.java` | Spring Security 安全配置。 |
| | `WebConfig.java` | Web 相关配置，如 CORS。 |
| **`AinovelApplication.java`** | | Spring Boot 应用主入口。 |

---

## Frontend (TypeScript - React)

前端使用 React 和 TypeScript 构建，采用组件化开发模式。

### 核心目录: `src/`

| 路径 | 文件名/目录 | 主要作用 |
| :--- | :--- | :--- |
| **`components`** | | **通用组件**：可在多个页面复用的 UI 组件。 |
| | `HomePage.tsx` | 应用首页。 |
| | `Login.tsx` / `Register.tsx` | 登录和注册页面。 |
| | `ManuscriptWriter.tsx` | 稿件写作界面核心组件。 |
| | `OutlinePage.tsx` | 大纲管理页面。 |
| | `StoryConception.tsx` | 故事构思页面。 |
| | `StoryList.tsx` | 故事列表页面。 |
| | `Workbench.tsx` | 主工作台界面，整合写作、大纲等功能。 |
| | `modals/` | 存放各类模态框（弹窗）组件，如编辑角色、编辑大纲等。 |
| | `MaterialCreateForm.tsx` / `MaterialUpload.tsx` / `MaterialSearchPanel.tsx` | 素材库组件：手动录入、文件上传与检索面板。 |
| **`pages`** | | **页面级组件**：通常代表一个完整的页面。 |
| | `Material/MaterialPage.tsx` | 素材库管理页面（MVP：创建+上传）。 |
| | `WorldBuilder/WorldBuilderPage.tsx` | 世界观构建器主页面。 |
| **`contexts`** | | **React Context**：用于全局状态管理。 |
| | `AuthContext.tsx` | 存储和管理用户认证状态。 |
| **`hooks`** | | **自定义 Hooks**：封装可复用的逻辑。 |
| | `useOutlineData.ts` | 获取和管理大纲数据的 Hook。 |
| | `useStoryData.ts` | 获取和管理故事数据的 Hook。 |
| **`services`** | | **API 服务**：封装与后端 API 的交互。 |
| | `api.ts` | 定义了所有与后端通信的 API 请求函数。 |
| **`App.tsx`** | | 应用根组件，负责路由和整体布局。 |
| **`main.tsx`** | | 应用入口文件，负责渲染 React 应用。 |
| **`types.ts`** | | 定义了项目中使用的 TypeScript 类型。 |

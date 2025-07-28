# AI 小说创作平台 - 第二期开发方案 (修订版)

## 1. 项目概述与核心目标 (第二期)

**核心目标：** 在第一期 MVP 的基础上，对平台的核心创作流程进行深度优化和改造。重点围绕**故事构思、大纲生成、正文创作**三大环节，引入更精细化的控制、更强大的上下文关联能力和更优的用户体验，同时提升系统的健壮性。

## 2. 核心需求分析与设计变更

| 需求点 | 分析与设计对策 |
| :--- | :--- |
| **1. 故事构思优化** | - **后端:** 修改 `ConceptionService` 的 Prompt，要求生成更长的梗概和基于角色关系的角色卡。 <br> - **后端:** 在 Prompt 中强制要求 `genre` 和 `tone` 字段使用简体中文。 |
| **2. 大纲生成优化** | - **前端:** 在 `OutlineDesign` 组件中增加“每章节数”和“每节大概字数”的输入控件。 <br> - **后端:** 修改 `OutlineController` 的 `/api/v1/outlines/chapters` 接口，接收这两个新参数。 |
| **3. 大纲内容增强 (已更新)** | - **数据库:** 扩展 `outline_scenes` 表，增加 `character_states` 和 `present_characters` 字段。 <br> - **后端:** 修改 `OutlineService` 的 Prompt，要求为每节生成**更加详细、充实的故事梗概**，并为核心出场人物提供**非常详细的**状态、想法和行动描述。 |
| **4. 支持临时角色 (新增)** | - **数据库:** 新增 `temporary_characters` 表，用于存储非核心的临时角色，并与 `outline_scenes` 表关联。 <br> - **后端:** `OutlineService` 的 Prompt 需引导模型在必要时创造临时角色，并在服务层逻辑中解析和持久化这些角色。 <br> - **后端:** `ManuscriptService` 在生成正文时，需将临时角色信息一并纳入上下文。 |
| **5. 大纲生成流程改造** | - **后端:** 废弃原有的 `POST /api/v1/outlines` 接口。新增 `POST /api/v1/outlines/{outlineId}/chapters` 接口，用于按章生成大纲。 <br> - **后端:** `OutlineService` 需调整逻辑，接收并处理上一章大纲作为上下文。 |
| **6. 故事创作流程改造**| - **后端:** `ManuscriptService` 在调用 LLM 前，增加一个预处理步骤：调用 LLM 对“上一章”和“本章上一节”内容进行概括。 <br> - **后端:** 改造 `ManuscriptService` 的主 Prompt，将新生成的上下文摘要、增强后的大纲信息（含人物状态、临时角色等）一同传入。 |
| **7. 健壮性提升** | - **后端:** 引入 Spring Retry 依赖。使用 `@Retryable` 注解为 `AiService` 的核心调用方法和 `*Repository` 的数据库操作方法增加重试逻辑。 |
| **8. UI/UX优化** | - **前端:** 在完成功能后，重新设计 `OutlineTreeView` 组件，优化其视觉表现和交互体验，需能展示临时角色。 |

## 3. 数据库模型变更

### 3.1. `outline_chapters` 表 (新增字段)
(无变更)
| 列名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | 章节唯一标识 |
| `outline_card_id`| BIGINT | FK -> outline_cards.id | 关联的大纲卡 |
| `chapter_number`| INT | NOT NULL | 章节序号 |
| `title` | VARCHAR(255) | | 章节标题 |
| `synopsis` | TEXT | | 本章梗概 |
| `settings` | JSON | **(新增)** | 存储用户设定的参数，如 `{"sectionsPerChapter": 5, "wordsPerSection": 800}` |

### 3.2. `outline_scenes` 表 (描述更新)

| 列名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | 场景/小节唯一标识 |
| `chapter_id` | BIGINT | FK -> outline_chapters.id | 关联的章节 |
| `scene_number` | INT | NOT NULL | 场景/小节序号 |
| `synopsis` | TEXT | | **(更新)** 本节的**详细、充实**的故事梗概，避免过于简短的概括。 |
| `expected_words`| INT | | 预期字数 |
| `present_characters`| TEXT | | 出场的核心人物列表 (以逗号分隔的姓名) |
| `character_states`| TEXT | | **(更新)** 对核心出场人物在本节中**非常详细的**状态、内心想法和关键行动的描述。 |

### 3.3. `temporary_characters` 表 (新增)

为了支持临时角色的创建和管理，新增此表。

| 列名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | 临时角色唯一标识 |
| `scene_id` | BIGINT | FK -> outline_scenes.id | 关联的场景/小节，表明该角色在此节出现 |
| `name` | VARCHAR(255)| NOT NULL | 临时角色的姓名 |
| `description` | TEXT | | 对该临时角色的简要描述（如身份、外貌、作用等） |

## 4. 后端设计变更

### 4.1. 实体类 (Model) 和 DTO 变更

*   **`com.example.ainovel.model.OutlineChapter`**:
    *   增加 `private String settings;` 字段。
*   **`com.example.ainovel.model.OutlineScene`**:
    *   增加 `private String presentCharacters;`
    *   增加 `private String characterStates;`
    *   增加 `private List<TemporaryCharacter> temporaryCharacters;` 并使用 `@OneToMany` 关联。
*   **`com.example.ainovel.model.TemporaryCharacter` (新增)**:
    *   对应 `temporary_characters` 表的实体。
*   **DTOs**:
    *   `ChapterDto`, `SceneDto` 同步实体类变更。
    *   新增 `TemporaryCharacterDto`。
    *   `SceneDto` 中应包含 `List<TemporaryCharacterDto>`。
    *   `GenerateChapterRequest.java` 保持不变。

### 4.2. API 接口 (Controller) 变更
(无结构性变更，但 `ChapterDto` 响应内容会更丰富)

*   **`OutlineController`**:
    *   **[新增]** `POST /api/v1/outlines/{outlineId}/chapters`
        *   **Response Body:** `ChapterDto` (现在其 `scenes` 列表中会包含临时角色信息)。

### 4.3. 核心业务逻辑 (Service) 实现思路

*   **`OutlineService`**:
    *   **`generateChapterOutline` (重构)**:
        1.  ... (获取故事、角色、上下文等步骤不变) ...
        2.  **构建 Prompt (更新)**:
            ```
            你是一个专业的小说大纲设计师。请根据以下信息，为故事的第 ${chapterNumber} 章设计详细大纲。
            ... (全局信息、主要角色、上下文不变) ...

            **本章要求:**
            - 章节序号: ${chapterNumber}
            - 包含节数: ${sectionsPerChapter}
            - 每节字数: 约 ${wordsPerSection} 字

            **输出要求:**
            请以JSON格式返回。根对象应包含 "title", "synopsis" 和一个 "scenes" 数组。
            每个 scene 对象必须包含:
            - "sceneNumber": 序号
            - "synopsis": (要求) 必须是详细、充实、引人入胜的故事梗概，长度不应少于150字。
            - "presentCharacters": 核心出场人物列表 (字符串数组)。
            - "characterStates": (要求) 一个对象，键为核心人物姓名，值为该人物在本节中非常详细的状态、内心想法和关键行动的描述。
            - "temporaryCharacters": (要求) 一个对象数组，用于描写本节新出现的临时人物。如果不需要，则返回空数组。每个对象包含 "name" 和 "description" 字段。
            ```
        3.  调用 `AiService`，解析返回的 JSON，持久化 `outline_chapters`, `outline_scenes` 以及新增的 `temporary_characters`。

*   **`ManuscriptService`**:
    *   **`generateSectionContent` (重构)**:
        1.  **获取基础信息:** `storyCard`, `characterCards`, `outlineScene` (包含增强后的大纲信息)，以及**本节的临时角色列表** (`temporaryCharacters`)。
        2.  ... (上下文概括步骤不变) ...
        3.  **构建主 Prompt (更新)**:
            ```
            ... (全局信息、主要角色、上下文摘要不变) ...

            **本节大纲:**
            - 梗概: ${scene.synopsis}
            - 核心出场人物: ${scene.presentCharacters}
            - 核心人物状态与行动: ${scene.characterStates}
            - 临时出场人物: ${temporaryCharacters.description} // 将临时角色信息整合进来

            请根据以上所有信息，创作本节的详细内容...
            ```
        4.  调用 `AiService` 获取生成内容并持久化。

## 5. 前端设计变更

### 5.1. 类型定义 (`types.ts`) 变更

```typescript
// src/types.ts

export interface TemporaryCharacter { // 新增
  id: number;
  name: string;
  description: string;
}

export interface Scene {
  id: number;
  sceneNumber: number;
  synopsis: string; // 描述更新：更详细的梗概
  expectedWords: number;
  presentCharacters?: string; // 核心人物
  characterStates?: string;   // 核心人物状态
  temporaryCharacters?: TemporaryCharacter[]; // 新增：临时人物
  content?: string;
}

// ... Chapter 等其他类型相应调整
```

### 5.2. 组件 (`components`) 变更

*   **`OutlineTreeView.tsx`**:
    *   在展示每个 Scene (小节) 节点时，除了显示梗概、核心人物及其状态外，还需**新增一个区域用于展示本节出现的“临时人物”及其简介**。
    *   **Mermaid 示图 (更新):**
        ```mermaid
        graph TD
            subgraph Chapter 1: The Awakening
                Scene1["<b>Scene 1: 意外的发现</b><br/><i>(约800字)</i><br/>---<br/><b>梗概:</b> 艾丽莎在阁楼中找到一个古老的罗盘...<br/><b>核心人物:</b> [艾丽莎]<br/><b>临时人物:</b> [老管家约翰 (白发苍苍，忠心耿耿)]<br/><b>状态:</b> (点击展开)"]
                Scene2["<b>Scene 2: 罗盘的秘密</b><br/><i>(约900字)</i><br/>---<br/><b>梗概:</b> 雷恩来访，认出了罗盘的来历...<br/><b>核心人物:</b> [艾丽莎] [雷恩]<br/><b>状态:</b> (点击展开)"]
            end
            Scene1 --> Scene2
        ```

## 6. 实施计划 (第二期 - 修订)

*   **阶段一: 后端核心改造 (约 6-8 天)**
    1.  **[后端]** 添加 Spring Retry 依赖并完成配置。
    2.  **[后端]** **(更新)** 修改数据库表结构（`outline_scenes`），新增 `temporary_characters` 表，并更新所有相关的 `model` 和 `dto`。
    3.  **[后端]** 实现 `ConceptionService` 的 Prompt 优化。
    4.  **[后端]** **(更新)** 实现按章生成大纲的核心逻辑 (`OutlineService`) 和新 API，确保能正确处理和持久化**详细梗概、详细状态和临时角色**。
    5.  **[单元测试]** 为 `OutlineService` 的新逻辑编写单元测试。

*   **阶段二: 前后端联调与创作流程改造 (约 4-6 天)**
    1.  **[前端]** 修改 `OutlineDesign` 组件以适应新的按章生成流程。
    2.  **[联调]** 联调前后端，确保按章生成大纲功能闭环，并能在前端正确接收到所有增强信息。
    3.  **[后端]** **(更新)** 重构 `ManuscriptService`，加入上下文概括和能利用**临时角色**的增强版 Prompt 逻辑。
    4.  **[联调]** 联调正文生成功能，验证上下文关联效果。
    5.  **[后端]** 为所有关键服务添加 `@Retryable` 注解。

*   **阶段三: UI/UX 优化与收尾 (约 3-4 天)**
    1.  **[前端]** **(更新)** 根据新设计，重构并美化 `OutlineTreeView` 组件，确保能清晰展示所有大纲信息，包括临时角色。
    2.  **[前端]** 调整 `ManuscriptWriter` 页面布局，确保新大纲树的兼容性。
    3.  **[测试]** 进行完整的端到端功能测试和回归测试。
    4.  **[文档]** 更新项目 README 和相关开发文档。
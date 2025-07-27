# AI 小说创作平台 - 第二期开发方案

## 1. 项目概述与核心目标 (第二期)

**核心目标：** 在第一期 MVP 的基础上，对平台的核心创作流程进行深度优化和改造。重点围绕**故事构思、大纲生成、正文创作**三大环节，引入更精细化的控制、更强大的上下文关联能力和更优的用户体验，同时提升系统的健壮性。

## 2. 核心需求分析与设计变更

| 需求点 | 分析与设计对策 |
| :--- | :--- |
| **1. 故事构思优化** | - **后端:** 修改 `ConceptionService` 的 Prompt，要求生成更长的梗概和基于角色关系的角色卡。 <br> - **后端:** 在 Prompt 中强制要求 `genre` 和 `tone` 字段使用简体中文。 |
| **2. 大纲生成优化** | - **前端:** 在 `OutlineDesign` 组件中增加“每章节数”和“每节大概字数”的输入控件。 <br> - **后端:** 修改 `OutlineController` 的 `/api/v1/outlines/chapters` 接口，接收这两个新参数。 |
| **3. 大纲内容增强** | - **数据库:** 扩展 `outline_scenes` 表，增加 `character_states` (TEXT) 和 `present_characters` (TEXT) 字段。 <br> - **后端:** 修改 `OutlineService` 的 Prompt，要求为每节生成更详细的梗概、出场人物列表及人物状态。 |
| **4. 大纲生成流程改造** | - **后端:** 废弃原有的 `POST /api/v1/outlines` 接口。新增 `POST /api/v1/outlines/{outlineId}/chapters` 接口，用于按章生成大纲。 <br> - **后端:** `OutlineService` 需调整逻辑，接收并处理上一章大纲作为上下文。 |
| **5. 故事创作流程改造**| - **后端:** `ManuscriptService` 在调用 LLM 前，增加一个预处理步骤：调用 LLM 对“上一章”和“本章上一节”内容进行概括。 <br> - **后端:** 改造 `ManuscriptService` 的主 Prompt，将新生成的上下文摘要、增强后的大纲信息（含人物状态等）一同传入。 |
| **6. 健壮性提升** | - **后端:** 引入 Spring Retry 依赖。使用 `@Retryable` 注解为 `AiService` 的核心调用方法和 `*Repository` 的数据库操作方法增加重试逻辑。 |
| **7. UI/UX优化** | - **前端:** 在完成功能后，重新设计 `OutlineTreeView` 组件，优化其视觉表现和交互体验。 |

## 3. 数据库模型变更

### 3.1. `outline_chapters` 表 (新增字段)

为了记录用户对每章的设定，增加 `settings` 字段。

| 列名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | 章节唯一标识 |
| `outline_card_id`| BIGINT | FK -> outline_cards.id | 关联的大纲卡 |
| `chapter_number`| INT | NOT NULL | 章节序号 |
| `title` | VARCHAR(255) | | 章节标题 |
| `synopsis` | TEXT | | 本章梗概 |
| `settings` | JSON | **(新增)** | 存储用户设定的参数，如 `{"sectionsPerChapter": 5, "wordsPerSection": 800}` |

### 3.2. `outline_scenes` 表 (新增字段)

为了存储更丰富的大纲信息，增加以下字段。

| 列名 | 类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `id` | BIGINT | PK, AI | 场景/小节唯一标识 |
| `chapter_id` | BIGINT | FK -> outline_chapters.id | 关联的章节 |
| `scene_number` | INT | NOT NULL | 场景/小节序号 |
| `synopsis` | TEXT | | 本节梗概 (增强后) |
| `expected_words`| INT | | 预期字数 |
| `present_characters`| TEXT | **(新增)** | 出场人物列表 (以逗号分隔的姓名) |
| `character_states`| TEXT | **(新增)** | 人物状态、想法和行动的详细描述 |

## 4. 后端设计变更

### 4.1. 实体类 (Model) 和 DTO 变更

*   **`com.example.ainovel.model.OutlineChapter`**:
    *   增加 `private String settings;` 字段，并使用 `@Type(JsonType.class)` (需要引入 `hypersistence-utils` 依赖) 或自定义转换器处理 JSON。
*   **`com.example.ainovel.model.OutlineScene`**:
    *   增加 `private String presentCharacters;`
    *   增加 `private String characterStates;`
*   **`com.example.ainovel.dto.ChapterDto`**:
    *   同步 `OutlineChapter` 的字段变更。
*   **`com.example.ainovel.dto.SceneDto`**:
    *   同步 `OutlineScene` 的字段变更。
*   **新增 DTO:**
    *   **`GenerateChapterRequest.java`**:
        ```java
        public class GenerateChapterRequest {
            private int chapterNumber;
            private int sectionsPerChapter; // 每章节数
            private int wordsPerSection;    // 每节字数
        }
        ```
    *   **`GenerateManuscriptRequest.java`**: (可能已存在，需确认或修改)
        ```java
        public class GenerateManuscriptRequest {
            // 保留原有字段
        }
        ```

### 4.2. API 接口 (Controller) 变更

*   **`ConceptionController`**: 无需变更接口，仅修改内部 Prompt。
*   **`OutlineController`**:
    *   **[废弃]** `POST /api/v1/outlines`
    *   **[新增]** `POST /api/v1/outlines/{outlineId}/chapters`
        *   **功能:** 按章生成大纲。
        *   **Request Body:** `GenerateChapterRequest`
        *   **Response Body:** `ChapterDto` (包含新生成的章节和所有小节的详细信息)
*   **`ManuscriptController`**:
    *   **[修改]** `POST /api/v1/manuscript/scenes/{sceneId}/generate`
        *   **功能:** 接口定义不变，但内部逻辑将进行重大改造，以包含上下文概括和更丰富的 Prompt。
        *   **Request Body:** `GenerateManuscriptRequest` (或直接从路径和安全上下文中获取所需信息)
        *   **Response Body:** `ManuscriptSection`

### 4.3. 核心业务逻辑 (Service) 实现思路

*   **`ConceptionService`**:
    *   修改 `generateConception` 方法中的 Prompt，明确要求“更长的故事梗概”、“基于角色关系生成更多角色卡”，并指定“类型和基调必须使用简体中文”。

*   **`OutlineService`**:
    *   **`generateChapterOutline` (新方法)**:
        1.  接收 `outlineId`, `chapterNumber`, 以及用户设定的参数。
        2.  从数据库中获取 `storyCard`, `characterCards`。
        3.  **获取上下文:** 如果 `chapterNumber > 1`，则查询 `outline_chapters` 表获取 `chapterNumber - 1` 的章节梗概和所有小节梗概。
        4.  **构建 Prompt:**
            ```
            你是一个专业的小说大纲设计师。请根据以下信息，为故事的第 ${chapterNumber} 章设计详细大纲。

            **全局信息:**
            - 故事简介: ${story.synopsis}
            - 故事走向: ${story.storyArc}

            **主要角色:**
            ${characterProfiles}

            **上下文 (上一章内容):**
            ${previousChapterSynopsis} // 如果是第一章，则此部分为空

            **本章要求:**
            - 章节序号: ${chapterNumber}
            - 包含节数: ${sectionsPerChapter}
            - 每节字数: 约 ${wordsPerSection} 字

            请以JSON格式返回。根对象应包含 "title", "synopsis" 和一个 "scenes" 数组。
            每个 scene 对象必须包含:
            - "sceneNumber": 序号
            - "synopsis": 更充分的故事梗概
            - "presentCharacters": 出场人物列表 (字符串)
            - "characterStates": 详细描述每个人物在本节的状态、想法和行动
            ```
        5.  调用 `AiService`，解析返回的 JSON，并将结果持久化到 `outline_chapters` 和 `outline_scenes` 表。

*   **`ManuscriptService`**:
    *   **`generateSectionContent` (重构)**:
        1.  **获取基础信息:** `storyCard`, `characterCards`, `outlineScene` (包含增强后的大纲信息)。
        2.  **上下文概括 (新步骤):**
            a.  获取 `previousChapterContent` (上一章所有已生成的正文) 和 `currentChapterPreviousSectionsContent` (本章之前所有已生成的正文)。
            b.  如果上下文内容不为空，调用 `AiService` 进行概括。**Prompt 示例:**
               ```
               请用一两句话概括以下小说内容，抓住核心冲突和情节进展。
               内容: "${rawContextText}"
               ```
            c.  得到 `contextSummary`。
        3.  **构建主 Prompt:**
            ```
            你是一位才华横溢的小说家。现在请你接续创作故事。

            **全局信息:**
            - 故事类型/基调: ${story.genre} / ${story.tone}
            - 故事简介: ${story.synopsis}

            **主要角色设定:**
            ${characterProfiles}

            **上下文摘要:**
            ${contextSummary} // 上一步生成的摘要

            **本节大纲:**
            - 梗概: ${scene.synopsis}
            - 出场人物: ${scene.presentCharacters}
            - 人物状态与行动: ${scene.characterStates}

            请根据以上所有信息，创作本节的详细内容，字数在 ${scene.expectedWords} 字左右。文笔要生动，符合故事基调和人物性格。请直接开始写正文。
            ```
        4.  调用 `AiService` 获取生成内容并持久化。

*   **重试机制 (`AiService`, Repositories)**:
    1.  在 `pom.xml` 中添加 `spring-boot-starter-aop` 和 `spring-retry` 依赖。
    2.  在主应用类 `@SpringBootApplication` 上添加 `@EnableRetry` 注解。
    3.  在 `AiService` 的调用 LLM 的方法上添加注解:
        ```java
        @Retryable(value = {RestClientException.class, IOException.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000))
        public String getCompletion(String prompt) { ... }
        ```
    4.  在各个 Repository 接口的方法上添加注解 (如果需要对数据库操作重试):
        ```java
        @Retryable(value = {DataAccessException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
        void save(T entity);
        ```

## 5. 前端设计变更

### 5.1. 类型定义 (`types.ts`) 变更

```typescript
// src/types.ts

export interface Chapter {
  id: number;
  chapterNumber: number;
  title: string;
  synopsis: string;
  scenes: Scene[];
  settings?: { // 新增
    sectionsPerChapter?: number;
    wordsPerSection?: number;
  };
}

export interface Scene {
  id: number;
  sceneNumber: number;
  synopsis: string;
  expectedWords: number;
  presentCharacters?: string; // 新增
  characterStates?: string;   // 新增
  content?: string; // 正文内容
}

// ... 其他类型保持不变
```

### 5.2. 组件 (`components`) 变更

*   **`OutlineDesign.tsx`**:
    *   移除原有的“总章节数”输入框。
    *   新增一个表单区域，用于输入“本章计划节数”和“每节大概字数”。
    *   修改“生成大纲”按钮的逻辑，使其调用新的 `POST /api/v1/outlines/{outlineId}/chapters` 接口。
    *   生成逻辑需要改为循环/逐次调用，或由用户手动触发每一章的生成。

*   **`OutlineTreeView.tsx`**:
    *   **功能完成后进行美化。**
    *   在展示每个 Scene (小节) 节点时，除了显示梗概，还应以更结构化的方式展示“出场人物”和“人物状态”。

*   **`ManuscriptWriter.tsx`**:
    *   左侧的大纲树 (`OutlineTreeView`) 需要能正确展示增强后的大纲信息。
    *   “生成本节内容”按钮的点击事件处理函数 `handleGenerate` 无需修改调用接口，但需要确保状态管理能正确处理后端返回的数据。

### 5.3. UI/UX 优化方案

*   **大纲生成界面 (`OutlineDesign.tsx`)**:
    *   提供一个清晰的区域让用户设置即将生成的章节的参数。
    *   界面需要明确告知用户当前正在生成第几章，并显示生成进度或状态。

*   **大纲浏览区 (`OutlineTreeView.tsx` - 美化草案)**:
    *   **目标:** 从纯文本列表升级为信息卡片式布局，提高可读性。
    *   **设计:**
        *   每个 Scene (小节) 作为一个独立的、带边框和阴影的卡片。
        *   卡片头部显示“第 X 节”和预期字数。
        *   卡片主体分为三部分：
            1.  **梗概:** 占用主要空间。
            2.  **出场人物:** 以标签 (Tags) 的形式展示，如 `[艾丽莎]` `[雷恩]`。
            3.  **人物状态:** 可折叠区域，点击展开后显示详细的人物状态和行动描述。
    *   **Mermaid 示图:**
        ```mermaid
        graph TD
            subgraph Chapter 1: The Awakening
                Scene1["<b>Scene 1: 意外的发现</b><br/><i>(约800字)</i><br/>---<br/><b>梗概:</b> 艾丽莎在阁楼中找到一个古老的罗盘...<br/><b>人物:</b> [艾丽莎] [管家]<br/><b>状态:</b> (点击展开)"]
                Scene2["<b>Scene 2: 罗盘的秘密</b><br/><i>(约900字)</i><br/>---<br/><b>梗概:</b> 雷恩来访，认出了罗盘的来历...<br/><b>人物:</b> [艾丽莎] [雷恩]<br/><b>状态:</b> (点击展开)"]
            end
            Scene1 --> Scene2
        ```

## 6. 实施计划 (第二期)

建议将第二期开发分为三个阶段进行，以便于迭代和测试。

*   **阶段一: 后端核心改造 (约 5-7 天)**
    1.  **[后端]** 添加 Spring Retry 依赖并完成配置。
    2.  **[后端]** 修改数据库表结构，并更新 `model` 和 `dto`。
    3.  **[后端]** 实现 `ConceptionService` 的 Prompt 优化。
    4.  **[后端]** 实现按章生成大纲的核心逻辑 (`OutlineService`) 和新 API (`OutlineController`)。
    5.  **[单元测试]** 为 `OutlineService` 的新逻辑编写单元测试。

*   **阶段二: 前后端联调与创作流程改造 (约 4-6 天)**
    1.  **[前端]** 修改 `OutlineDesign` 组件以适应新的按章生成流程。
    2.  **[联调]** 联调前后端，确保按章生成大纲功能闭环。
    3.  **[后端]** 重构 `ManuscriptService`，加入上下文概括和增强版 Prompt 的逻辑。
    4.  **[联调]** 联调正文生成功能，验证上下文关联效果。
    5.  **[后端]** 为所有关键服务添加 `@Retryable` 注解。

*   **阶段三: UI/UX 优化与收尾 (约 3-4 天)**
    1.  **[前端]** 根据设计草案，重构并美化 `OutlineTreeView` 组件。
    2.  **[前端]** 调整 `ManuscriptWriter` 页面布局，确保新大纲树的兼容性。
    3.  **[测试]** 进行完整的端到端功能测试和回归测试。
    4.  **[文档]** 更新项目 README 和相关开发文档。
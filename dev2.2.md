# AI小说创作助手 v2.2 开发方案

## 1. 概述

本文档旨在为 **AI小说创作助手 v2.2** 版本的功能迭代提供一份全面的技术实现方案和提示词工程指南。本次迭代的核心目标是深化大纲编辑的细节，引入临时人物系统，增强AI辅助创作能力，并全面优化与AI交互的提示词，旨在提升AI生成内容的人性化和创造性，减少模板化痕迹。

**核心需求点：**

1.  **临时人物系统：** 允许在特定“节”（Scene）中创建和管理临时角色。
2.  **大纲编辑增强：** 提供对大纲所有细节（包括临时人物）的完整编辑能力。
3.  **通用“AI优化”功能：** 为所有核心文本输入框增加一键式AI润色功能。
4.  **故事创作上下文增强：** 在生成正文时，为AI提供更明确的章节/小节位置信息。
5.  **提示词（Prompt）全面优化：** 重新设计大纲和正文生成的提示词，激发AI的创造力。

本方案将从后端、前端和提示词工程三个维度，对上述需求进行详细拆解和设计。

---

## 2. 后端实现方案

后端的核心改动在于扩展数据模型以支持更丰富的临时人物信息，并提供相应的API接口来处理大纲的完整编辑和AI优化请求。

### 2.1. 数据模型 (Entities & DTOs)

#### 2.1.1. `TemporaryCharacter` Entity (`model/TemporaryCharacter.java`)

需要对现有的 `TemporaryCharacter` 实体进行扩展，以存储更丰富的信息。

**建议修改：**

```java
// ... imports
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "temporary_characters")
@Data
@NoArgsConstructor
public class TemporaryCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_id", nullable = false)
    @JsonBackReference
    private OutlineScene scene;

    @Column(nullable = false)
    private String name; // 姓名 (已有)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summary; // 概要 (新字段，替代原description)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String details; // 详情 (新字段)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String relationships; // 与核心人物的关系 (新字段)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String statusInScene; // 在本节中的状态 (新字段)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String moodInScene; // 在本节中的心情 (新字段)

    @Lob
    @Column(columnDefinition = "TEXT")
    private String actionsInScene; // 在本节中的核心行动 (新字段)
}
```

**数据库迁移：** 需要为 `temporary_characters` 表添加 `summary`, `details`, `relationships`, `statusInScene`, `moodInScene`, `actionsInScene` 字段，并将原有 `description` 字段的数据迁移至 `summary` 或直接废弃。

#### 2.1.2. `TemporaryCharacterDto` (`dto/TemporaryCharacterDto.java`)

同步更新DTO以匹配Entity的变化。

**建议修改：**

```java
// ... imports
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TemporaryCharacterDto {
    private Long id;
    private String name;
    private String summary;
    private String details;
    private String relationships;
    private String statusInScene;
    private String moodInScene;
    private String actionsInScene;
}
```

#### 2.1.3. `OutlineDto` (`dto/OutlineDto.java`)

无需修改。`OutlineDto` 通过层级关系已包含所有需要的数据。前端将通过 `OutlineDto` 获得完整的、可编辑的大纲信息。

#### 2.1.4. `RefineRequest` (`dto/RefineRequest.java`)

现有的 `RefineRequest` 已经可以满足“AI优化”功能的需求，但为了语义更清晰，建议增加一个字段。

**建议修改：**

```java
// ... imports
import lombok.Data;

@Data
public class RefineRequest {
    private String text; // 待优化的原文
    private String instruction; // 优化方向
    private String contextType; // (新字段) 文本类型，如 "角色介绍", "大纲梗概"
}
```

### 2.2. API 接口 (Controllers)

#### 2.2.1. `OutlineController` (`controller/OutlineController.java`)

需要增强大纲的更新功能，并为“AI优化”提供一个通用接口。

*   **`PUT /api/v1/outlines/{id}`**
    *   **当前实现：** 已存在，但 `updateOutline` 服务的逻辑可能需要调整以支持深度更新（包括对 `TemporaryCharacter` 的增删改）。
    *   **后续任务：** 审查并增强 `OutlineService.updateOutline` 方法，确保其能够正确处理包含完整临时人物信息的 `OutlineDto`。

*   **`POST /api/v1/ai/refine-text` (新接口)**
    *   **功能：** 提供一个通用的文本优化API，以替代特定于场景的 `refineSceneSynopsis`。
    *   **Request Body:** `RefineRequest`
    *   **Response Body:** `RefineResponse`
    *   **实现：**
        *   在 `OutlineController` 或创建一个新的 `AiController` 中添加此端点。
        *   该接口将调用一个新的服务方法 `AiService.refineGenericText(RefineRequest request, User user)`。

### 2.3. 服务层 (Services)

#### 2.3.1. `OutlineService` (`service/OutlineService.java`)

*   **`updateOutline(Long outlineId, OutlineDto outlineDto, Long userId)`**
    *   **核心改动：** 需要重写此方法的映射逻辑，使其能够处理对 `TemporaryCharacter` 的完整CRUD操作。
    *   **建议策略：**
        1.  加载现有的 `OutlineCard` 实体。
        2.  使用 `Map<Long, TemporaryCharacter>` 来跟踪数据库中已存在的临时人物。
        3.  遍历传入的 `outlineDto` 中的章节、场景和临时人物。
        4.  **对于临时人物：**
            *   如果传入的临时人物有ID且在Map中存在，则更新其所有字段。
            *   如果传入的临时人物没有ID，则视为新增，创建一个新的 `TemporaryCharacter` 实例并关联到对应的 `OutlineScene`。
            *   在遍历结束后，Map中仍然存在的临时人物，意味着它们在传入的DTO中已被删除，应从数据库中移除（`orphanRemoval=true` 会自动处理）。
        5.  保存 `OutlineCard` 根实体。

#### 2.3.2. `AiService` & `ManuscriptService`

*   **`AiService.refineText`**
    *   需要修改此方法的实现，以接收新的 `contextType` 字段，并将其整合到发送给大模型的提示词中。

*   **`ManuscriptService.buildGenerationPrompt`**
    *   **核心改动：** 需要在此方法中加入新的上下文信息。
    *   **建议修改：**
        1.  在方法签名中增加参数 `int chapterNumber`, `int totalChapters`, `int sceneNumber`, `int totalScenesInChapter`。
        2.  在调用 `buildGenerationPrompt` 之前，从 `OutlineScene` 和 `OutlineChapter` 的关系中计算出这些数字。
        3.  将这些信息添加到提示词模板中，例如：`"当前位置: 第 ${chapterNumber}/${totalChapters} 章, 第 ${sceneNumber}/${totalScenesInChapter} 节"`。

---

## 3. 前端实现方案

前端的核心工作是创建一个新的、功能强大的大纲编辑界面，并实现通用的“AI优化”组件。

### 3.1. 状态管理 (Hooks & `App.tsx`)

*   **`useOutlineData` hook:** 现有的 `useOutlineData` hook (或类似的逻辑) 需要增强，以支持对整个 `outline` 对象进行本地状态的修改，包括对临时人物的增、删、改。当用户在新的编辑界面中修改任何内容时，都应该先更新本地的 `outline` 状态。
*   **保存逻辑:** 当用户点击“保存”按钮时，将整个本地的 `outline` 对象通过 `PUT /api/v1/outlines/{id}` 接口提交给后端。

### 3.2. 新增/修改组件

#### 3.2.1. **`OutlineManagementPage.tsx` (新页面/组件)**

取代现有的 `OutlineDesign.tsx` 中大纲展示和生成的部分，或者在其基础上进行大规模改造，成为一个专门的大纲“管理”和“编辑”中心。

*   **功能职责:**
    1.  **展示大纲树:** 使用 `OutlineTreeView` 组件清晰地展示完整的大纲结构。
    2.  **编辑区域:** 当用户在树中选择一个节点（章、节、临时人物）时，在页面右侧或弹窗中显示一个对应的编辑表单。
    3.  **保存与加载:** 提供“保存大纲”按钮，将当前状态的整个大纲提交给后端。

#### 3.2.2. **`ChapterEditForm.tsx` (新组件)**

*   **功能:** 用于编辑一个 `OutlineChapter` 的表单。
*   **包含字段:**
    *   章节标题 (`title`)
    *   章节梗概 (`synopsis`) - 使用 `TextArea` 并集成 `AiRefineButton`。

#### 3.2.3. **`SceneEditForm.tsx` (新组件)**

*   **功能:** 用于编辑一个 `OutlineScene` 的表单。
*   **包含字段:**
    *   场景梗概 (`synopsis`) - 使用 `TextArea` 并集成 `AiRefineButton`。
    *   核心出场人物 (`presentCharacters`)
    *   核心人物状态 (`characterStates`) - 使用 `TextArea` 并集成 `AiRefineButton`。
    *   **临时人物列表:**
        *   显示该场景下所有 `temporaryCharacters` 的列表。
        *   提供“新增临时人物”按钮。
        *   列表中的每一项都有“编辑”和“删除”按钮。点击“编辑”会弹出 `TemporaryCharacterEditModal`。

#### 3.2.4. **`TemporaryCharacterEditModal.tsx` (新组件)**

*   **功能:** 一个模态框（Modal），用于创建或编辑一个 `TemporaryCharacter`。
*   **包含字段:**
    *   姓名 (`name`)
    *   概要 (`summary`) - 使用 `TextArea` 并集成 `AiRefineButton`。
    *   详情 (`details`) - 使用 `TextArea` 并集成 `AiRefineButton`。
    *   关系 (`relationships`) - 使用 `TextArea` 并集成 `AiRefineButton`。
    *   在本节中的状态 (`statusInScene`) - 使用 `TextArea` 并集成 `AiRefineButton`。
    *   在本节中的心情 (`moodInScene`) - 使用 `TextArea` 并集成 `AiRefineButton`。
    *   在本节中的核心行动 (`actionsInScene`) - 使用 `TextArea` 并集成 `AiRefineButton`。
*   **交互:** 点击“确定”后，更新父组件 (`SceneEditForm`) 中的本地 `outline` 状态，关闭模态框。

#### 3.2.5. **`AiRefineButton.tsx` & `RefineModal.tsx` (新/修改组件)**

*   **`AiRefineButton.tsx`:**
    *   **外观:** 一个小图标按钮，可放置在 `TextArea` 或 `Input` 的右下角。
    *   **功能:** 点击后，打开 `RefineModal`。
    *   **Props:**
        *   `originalText: string` - 待优化的文本。
        *   `contextType: string` - 文本类型，如 "角色介绍"。
        *   `onRefined: (newText: string) => void` - 优化完成后的回调函数。

*   **`RefineModal.tsx`:**
    *   **功能:**
        1.  弹出一个模态框。
        2.  内部有一个 `TextArea` 让用户输入“优化方向”。
        3.  一个“开始优化”按钮，点击后调用 `POST /api/v1/ai/refine-text` 接口。
        4.  显示优化后的结果，并提供“应用修改”和“撤销”按钮。
        5.  “应用修改”会调用 `onRefined` 回调，将新文本传回父组件。
        6.  “撤销”功能可以通过在 `RefineModal` 内部用一个 state 保存 `originalText` 来实现。

### 3.3. `types.ts`

需要更新 `TemporaryCharacter` 和 `Chapter`, `Scene` 的类型定义，以匹配后端的DTO。

---

## 4. 提示词（Prompt）优化方案

这是本次迭代的灵魂。目标是引导AI从一个“任务执行者”转变为一个“创意合作者”。核心思想是：给予AI更多的创作自由度、更强的角色代入感和更明确的创作目标。

### 4.1. 大纲设计 (`OutlineService.buildChapterPrompt`)

**旧的Prompt问题：** 过于死板，像一个填空题，限制了AI的发挥。

**新的Prompt设计指南：**

1.  **角色扮演：** 让AI扮演一个经验丰富、富有创意的“网文大神”或“金牌编剧”，而不仅仅是“大纲设计师”。
2.  **强调“钩子”和“爆点”：** 明确要求AI在设计情节时，思考如何埋下伏笔、设置悬念（钩子），并在适当时机引爆情绪（爆点）。
3.  **给予“建议权”：** 允许AI在遵循核心要求的前提下，对情节走向提出自己的“建议”或“变体”，甚至可以对临时人物的设定进行微调，以服务于剧情。
4.  **情感与逻辑并重：** 要求AI不仅要设计“发生了什么”，更要关注“人物为什么这么做”（动机）和“这会带来什么感受”（情绪）。
5.  **避免套路：** 明确指示AI“请避免使用常见的、可预测的套路化情节。如果必须使用，请尝试从新的角度诠释它。”

**新Prompt草案：**

```
你是一位洞悉读者心理、擅长制造“爽点”与“泪点”的顶尖网络小说家。现在，请你以合作者的身份，为我的故事设计接下来的一章。我希望这一章不仅是情节的推进，更是情感的积累和爆发。

# 故事核心信息
- **故事简介:** {story.synopsis}
- **核心主题与基调:** {story.genre} / {story.tone}
- **故事长期走向:** {story.storyArc}

# 主要角色设定
{characterProfiles}

# 上下文回顾
- **上一章梗概:** {previousChapterSynopsis}
- **已完成的章节:** {completedChaptersSummary} // (可选) 提供更宏观的已完成情节摘要

# 本章创作任务 (第 {chapterNumber} 章)
- **核心要求:** {request.userRequirements} // (可选) 用户可输入本章必须发生的事件
- **预设节数:** {request.sectionsPerChapter}
- **预估每节字数:** {request.wordsPerSection}

# 你的创作目标与自由度
1.  **情节设计:** 请构思一章充满“钩子”的情节。思考：这一章的结尾，最能让读者好奇地想读下一章的悬念是什么？中间是否可以安排一个小的“情绪爆点”或“情节反转”？
2.  **人物弧光:** 思考核心人物在本章的经历，他们的内心会产生怎样的变化？他们的信念是会更坚定，还是会受到挑战？
3.  **伏笔与回收:** 如果有机会，可以埋下一些与长线剧情相关的伏笔。如果前文有伏笔，思考本章是否是回收它的好时机。
4.  **创作建议 (重要):** 在满足核心要求的前提下，你完全可以提出更有创意的想法。例如，你认为某个临时人物的设定稍微调整一下会更有戏剧性，或者某个情节有更好的表现方式，请大胆地在你的设计中体现出来，并用 `[创作建议]` 标签标注。
5.  **拒绝平庸:** 请极力避免机械地推进剧情。每一节都应该有其独特的作用，或是塑造人物，或是铺垫情绪，或是揭示信息。

# 输出格式
请严格以JSON格式返回。根对象应包含 "title", "synopsis" 和一个 "scenes" 数组。
每个 scene 对象必须包含:
- "sceneNumber": (number) 序号。
- "synopsis": (string) 详细、生动、充满画面感的故事梗概，字数不少于200字。
- "presentCharacters": (string[]) 核心出场人物姓名列表。
- "characterStates": (object) 一个对象，键为核心人物姓名，值为该人物在本节中非常详细的状态、内心想法和关键行动的描述。
- "temporaryCharacters": (object[]) 一个对象数组，用于描写本节新出现的或需要详细刻画的临时人物。如果不需要，则返回空数组[]。每个对象必须包含所有字段: "name", "summary", "details", "relationships", "statusInScene", "moodInScene", "actionsInScene"。
```

### 4.2. 故事创作 (`ManuscriptService.buildGenerationPrompt`)

**旧的Prompt问题：** 像一个翻译器，把大纲直译成正文，缺乏文学性和情感深度。

**新的Prompt设计指南：**

1.  **赋予“作者”身份：** 让AI扮演一个文笔细腻、情感充沛的“小说家”，而不是一个没有感情的写作机器。
2.  **强调“沉浸式体验”：** 要求AI在写作时，不仅仅是描述事件，更要通过细节、感官描写（视觉、听觉、触觉等）和心理活动，让读者“身临其境”。
3.  **正能量的融入方式：** 明确指示“不要在结尾进行说教或强行升华。请将积极的价值观、人物的闪光点，自然地融入到他们的行动和对话中。”
4.  **鼓励“临场发挥”：** 给予AI在不违背大纲核心的前提下，进行“合理演绎”的权力。例如，可以增加一些生动的对话、细节动作或环境描写，来让场景更丰满。
5.  **上下文感知：** 明确告知AI当前在故事中的具体位置，帮助其掌握节奏。

**新Prompt草案：**

```
你是一位才华横溢、情感细腻的小说家。你的文字拥有直击人心的力量。现在，请你将灵魂注入以下场景，创作出能让读者沉浸其中的精彩故事。

# 故事背景
- **故事类型/基调:** {story.genre} / {story.tone}
- **故事简介:** {story.synopsis}

# 主要角色设定
{characterProfiles}

# 上下文回顾
- **前情提要 (AI总结):** {contextSummary}
- **当前位置:** 这是故事的 **第 {chapterNumber}/{totalChapters} 章** 的 **第 {sceneNumber}/{totalScenesInChapter} 节**。请根据这个位置把握好创作的节奏和情绪的烈度。

# 本节创作蓝图 (大纲)
- **梗概:** {scene.synopsis}
- **核心出场人物:** {scene.presentCharacters}
- **核心人物状态与行动:** {scene.characterStates}
- **临时出场人物详情:**
{temporaryCharactersInfo}

# 你的创作要求
1.  **沉浸式写作:** 请勿平铺直叙。运用感官描写、心理活动和精妙的比喻，让读者完全代入。
2.  **对话与行动:** 对话要符合人物性格，行动要体现人物动机。允许你在大纲基础上，丰富对话和细节，让人物“活”起来。
3.  **自然地融入主题:** 如果需要传递正向价值，请通过人物的选择和成长来体现，避免任何形式的说教。
4.  **忠于大纲，但高于大纲:** 你必须遵循大纲的核心情节和人物状态，但你有权进行合理的艺术加工，让故事更精彩。
5.  **直接输出正文:** 请直接开始创作本节的故事正文，字数在 {scene.expectedWords} 字左右。不要包含任何前言、标题或总结。

现在，请开始你的创作。
```

## 5. 总结

本次v2.2版本的迭代，是一次从“功能实现”到“体验优化”的深刻转变。通过对数据模型的扩展、前后端架构的调整，以及对AI提示词的精雕细琢，我们期望能为用户提供一个更强大、更智能、也更具人性化的创作伙伴。

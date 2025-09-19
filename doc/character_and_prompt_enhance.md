# 设计文档：大纲角色卡与提示词优化

## 1. 概述

本文档旨在详细阐述对AINovel系统进行的功能增强和修改，核心目标是优化大纲创作体验、提升AI生成内容的质量与一致性。主要改进点包括：

1.  **大纲角色卡化**：将大纲中“人物状态与行动”的自由文本形式，重构为结构化的、可独立编辑的“人物卡”形式，支持核心人物与临时人物。
2.  **提示词（Prompt）全面优化**：对大纲生成和小说正文生成的AI提示词进行系统性重构，以满足新的内容风格要求，并确保角色状态能够在新旧模块间正确流转。

## 2. 大纲角色卡化改造 (Req. I & II)

### 2.1. 问题分析

当前 `OutlineScene` 实体中的 `characterStates` 字段是一个 `TEXT` 类型的字段，用于存储一个大的JSON字符串，其中包含了场景中所有核心人物的状态描述。这种方式存在以下弊端：

-   **不易编辑**：前端难以提供针对单个角色的精细化编辑界面。
-   **结构不清晰**：后端难以对非结构化数据进行校验和利用。
-   **扩展性差**：无法方便地为角色状态添加新的属性（如情绪、目标等）。

### 2.2. 改造方案

我们将引入一个新的数据模型 `SceneCharacter` 来结构化地存储核心角色在特定场景中的状态。

#### 2.2.1. 数据库设计

新增 `scene_characters` 表：

| 字段名 (Column) | 类型 (Type) | 约束 (Constraints) | 描述 (Description) |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PK`, `AUTO_INCREMENT` | 主键ID |
| `scene_id` | `BIGINT` | `FK (outline_scenes.id)` | 关联的场景ID |
| `character_card_id` | `BIGINT` | `FK (character_cards.id)` | 关联的核心角色ID |
| `character_name` | `VARCHAR(255)` | `NOT NULL` | 角色姓名（冗余字段，便于查询） |
| `status` | `TEXT` | `NULL` | 角色在本节的状态（如生理、心理状态） |
| `thought` | `TEXT` | `NULL` | 角色在本节的核心想法或心理活动 |
| `action` | `TEXT` | `NULL` | 角色在本节的关键行为 |

同时，废弃 `OutlineScene` 表中的 `characterStates` 字段。

#### 2.2.2. 后端模型修改

1.  **创建 `SceneCharacter.java` 实体**:
    -   包含上述所有字段。
    -   与 `OutlineScene` 建立 `ManyToOne` 关系。
    -   与 `CharacterCard` 建立 `ManyToOne` 关系。

2.  **修改 `OutlineScene.java` 实体**:
    -   移除 `characterStates` 字段。
    -   添加 `private List<SceneCharacter> sceneCharacters;`，并使用 `@OneToMany` 注解进行关联。

3.  **修改 `TemporaryCharacter.java` 实体**:
    -   为保持统一，将字段 `statusInScene`, `moodInScene`, `actionsInScene` 分别重命名为 `status`, `thought`, `action`。

#### 2.2.3. 前端界面修改

1.  **`SceneEditForm.tsx`**:
    -   移除原有的“核心人物状态” `TextArea`。
    -   新增一个“核心人物状态卡”区域。
    -   当用户在“核心出场人物” `Select` 组件中选择一个或多个核心人物后，下方会自动为每个人物生成一张卡片。
    -   每张卡片包含“状态”、“想法”、“行为”三个可编辑的 `TextArea`。
    -   临时人物的管理方式保持不变，但其编辑模态框内的字段应与核心人物卡保持一致。

## 3. AI提示词优化 (Req. III, IV, V, VI)

### 3.1. 大纲生成提示词优化 (`OutlineService.buildChapterPrompt`)

#### 3.1.1. 结构化角色卡生成 (Req. III)

修改Prompt中的输出格式要求，将原有的 `characterStates` 对象，变更为 `sceneCharacters` 数组，每个对象对应一个核心出场人物的状态卡。

**修改前 (示例):**
```json
{
  "characterStates": {
    "李寻欢": "他内心痛苦，但表面冷漠...",
    "阿飞": "他感到困惑和背叛..."
  }
}
```

**修改后 (示例):**
```json
{
  "sceneCharacters": [
    {
      "characterName": "李寻欢",
      "status": "内心痛苦，表面冷漠",
      "thought": "必须尽快找到梅花盗，洗清自己的嫌疑。",
      "action": "喝完杯中酒，起身离开酒馆。"
    },
    {
      "characterName": "阿飞",
      "status": "感到困惑和背叛",
      "thought": "他为什么要这么做？我不相信他是凶手。",
      "action": "紧随李寻欢身后，试图寻求一个解释。"
    }
  ]
}
```后端 `parseAndSaveChapter` 方法也需要同步修改，以正确解析新的JSON结构，并创建/保存 `SceneCharacter` 实体。

#### 3.1.2. 内容风格优化 (Req. VI.1)

在Prompt中加入更明确的风格指令：

-   **强调关键节点**：明确要求“对于每节大纲，不应当写一篇长简介，而是应该写出本节的多个关键故事节点（按照每节的预定长度来决定关键故事节点个数，可以考虑平均每200-400字一个关键节点），每个关键故事节点只有两三句话。”
-   **减少修辞**：加入指令“应当减少比喻、排比等修辞手法的使用，仅在认为确实有必要的情况下才少量使用。”
-   **避免套路**：加入指令“减少套路化的写作格式和剧情走向，允许在一定程度上自由发挥。”

### 3.2. 小说生成提示词优化 (`ManuscriptService.buildGenerationPrompt`)

#### 3.2.1. 整合最新角色信息 (Req. IV)

在构建小说生成Prompt时，需要将大纲中最新的、结构化的角色状态信息（来自 `SceneCharacter` 和 `TemporaryCharacter`）以及角色成长信息（来自 `CharacterChangeLog`）整合进去。

**修改 `buildGenerationPrompt` 方法**:

1.  **输入参数**: 增加 `List<SceneCharacter> sceneCharacters`。
2.  **Prompt内容**:
    -   将原有的 `核心人物状态与行动: %s` 部分，修改为一个更详细的、循环生成的文本块，清晰地列出每个核心人物的姓名、状态、想法和行为。
    -   在“全部角色档案”部分，为每个核心角色补充“最近的成长轨迹”和“最新的关系图谱”，这些信息可以从 `CharacterChangeLogService` (如果存在) 或直接查询 `character_change_log` 表获得。

#### 3.2.2. 内容风格优化 (Req. VI.2)

在Prompt的“写作规则”部分，加入新的指令：

-   **减少修辞**：“应当减少比喻、排比等修辞手法的使用，仅在认为确实有必要的情况下才少量使用。”
-   **避免套路**：“减少套路化的写作格式和剧情走向，允许在一定程度上自由发挥。”
-   **自然化描述**：“减少机械式的描述，应当让文章更符合人们日常说话和描述事情的感觉。”
-   **段落饱满**：“让每个自然段多写一些东西，避免‘一句话拆出一个自然段’的细碎式写作。”

### 3.3. 角色演化分析提示词检查 (Req. V)

检查 `ManuscriptService.buildCharacterChangePrompt` 方法。该Prompt的目标是**分析已生成的正文**，并输出角色变化。

**结论**：当前设计已能满足需求。该Prompt的输入是“正文内容”和“角色变更前的信息”，输出是结构化的 `CharacterChangeLog`。它本身就是一个独立的分析模块，只要小说生成Prompt能利用最新的角色状态生成更精确的正文，这个分析模块就能更准确地捕捉到变化。因此，**此部分无需修改**。

## 4. 实施计划

### 4.1. 待办事项清单 (TODO List)

1.  **后端**:
    -   [ ] 在 `pom.xml` 中移除对 `characterStates` 的依赖（如果存在）。
    -   [ ] 创建 `SceneCharacter.java` 实体类。
    -   [ ] 修改 `OutlineScene.java`，用 `List<SceneCharacter>` 替换 `characterStates`。
    -   [ ] 修改 `TemporaryCharacter.java` 的字段名以保持一致。
    -   [ ] 更新 `OutlineService` 中的 `buildChapterPrompt` 方法，以符合新的输出格式和风格要求。
    -   [ ] 更新 `OutlineService` 中的 `parseAndSaveChapter` 方法，以解析新的JSON结构并保存 `SceneCharacter` 实体。
    -   [ ] 更新 `ManuscriptService` 中的 `buildGenerationPrompt` 方法，以整合新的角色卡信息和风格要求。
    -   [ ] 更新数据库结构，执行相应的数据库迁移脚本。

2.  **前端**:
    -   [ ] 修改 `SceneEditForm.tsx`，实现核心人物的状态卡编辑界面。
    -   [ ] 修改 `TemporaryCharacterEditModal.tsx` 中的字段以匹配后端模型。
    -   [ ] 确保 `onUpdate` 等回调函数能正确处理新的 `sceneCharacters` 数据结构。

### 4.2. 流程图 (Mermaid)

```mermaid
graph TD
    subgraph "Phase 1: 大纲生成"
        A[用户点击'生成章节'] --> B{OutlineService.generateChapterOutline};
        B --> C{buildChapterPrompt (新)};
        C -- Prompt --> D[AI Service];
        D -- JSON (含sceneCharacters) --> B;
        B --> E{parseAndSaveChapter (新)};
        E -- 保存 --> F[数据库: outline_scenes, scene_characters];
    end

    subgraph "Phase 2: 小说生成"
        G[用户点击'生成内容'] --> H{ManuscriptService.generateSceneContent};
        H -- 读取 --> F;
        H --> I{buildGenerationPrompt (新)};
        I -- 整合角色状态/成长轨迹 --> J[Prompt];
        J --> K[AI Service];
        K -- 正文 --> H;
        H -- 保存 --> L[数据库: manuscript_sections];
    end

    subgraph "Phase 3: 角色演化分析"
        M[生成/保存正文后] --> N{ManuscriptService.analyzeCharacterChanges};
        N -- 读取 --> L;
        N --> O{buildCharacterChangePrompt (不变)};
        O -- Prompt --> P[AI Service];
        P -- JSON (含角色变化) --> N;
        N -- 保存 --> Q[数据库: character_change_log];
    end

    F --> I;
    Q --> I;
```

## 5. 风险与缓解措施

-   **风险**: AI可能无法严格按照新的JSON格式返回数据，导致解析失败。
-   **缓解措施**: 在后端的 `parseAndSaveChapter` 方法中增加更强的健壮性处理，对于不符合格式的部分进行日志记录和优雅降级（例如，即使 `sceneCharacters` 解析失败，也尝试保存场景的其他信息）。

-   **风险**: 新的写作风格指令可能不被AI很好地遵守。
-   **缓解措施**: 进行多轮测试和微调，如果效果不佳，可以考虑在Prompt中提供“反例”（e.g., "不要这样写..."），以强化AI的理解。
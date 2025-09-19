# “角色动态演化系统”功能设计文档

## 1. 系统概述 (System Overview)

本系统旨在构建一个能让小说角色随着故事发展而动态演化的完整生态。其核心目标是打破静态的角色设定，通过追踪、记录并应用角色的状态、关系、记忆等变化，使角色真正“活起来”。

为了便于迭代开发和功能解耦，整个系统将分阶段实施。开发者可以按照顺序，独立完成每个阶段的功能开发、测试和上线。

---

## **阶段一：核心功能 - 角色状态追踪 (Character Status Tracking)**

**目标**：实现对角色“认知”和“状态”变化的基础记录与展示。

### 1.1. 数据库设计 (Database Design)

#### 新增数据表：`character_change_log` (角色变动记录)

| 字段名 (Column) | 类型 (Type) | 约束 (Constraints) | 描述 (Description) |
| :--- | :--- | :--- | :--- |
| `id` | `BIGINT` | `PRIMARY KEY`, `AUTO_INCREMENT` | 主键ID |
| `character_id` | `BIGINT` | `NOT NULL`, `FOREIGN KEY` | 关联的角色ID (`character_card.id`) |
| `manuscript_id` | `BIGINT` | `NOT NULL`, `FOREIGN KEY` | 关联的稿件ID (`manuscript.id`) |
| `outline_id` | `BIGINT` | `NULL` | 关联的大纲ID (`outline_card.id`) |
| `chapter_number` | `INT` | `NOT NULL` | 章节编号 |
| `section_number` | `INT` | `NOT NULL` | 小节编号 |
| `newly_known_info` | `TEXT` | `NULL` | **[P1]** 角色在本节新知道的事情 |
| `character_changes` | `TEXT` | `NULL` | **[P1]** 角色在本节发生的变化（生理、心理、外貌等） |
| `character_details_after` | `TEXT` | `NOT NULL` | **[P1]** 本节完毕后的角色详情 |
| `is_auto_copied` | `BOOLEAN` | `NOT NULL`, `DEFAULT FALSE` | **[P1]** 标记`character_details_after`是否为程序自动复制 |
| `created_at` | `DATETIME` | `NOT NULL` | 创建时间 |
| `updated_at` | `DATETIME` | `NOT NULL` | 更新时间 |
| `deleted_at` | `DATETIME` | `NULL` | 软删除时间 |

### 1.2. 后端设计 (Backend Design)

#### 新增 API Endpoint: `POST /api/manuscripts/{manuscriptId}/sections/analyze-character-changes`

- **功能**: 分析并记录指定章节内容中，出场角色的状态变化。
- **Request Body**:
  ```json
  {
    "chapter_number": 1,
    "section_number": 3,
    "section_content": "...",
    "character_ids":
  }
  ```
- **核心逻辑 (`ManuscriptService`)**:
    1.  接收请求，遍历 `character_ids`。
    2.  对每个角色，查询其最近的 `character_details_after` 作为AI分析的上下文。
    3.  调用 `AiService`，传入“本节内容”和“变更前角色详情”。
    4.  根据AI返回结果（有变化/无变化），创建新的 `character_change_log` 记录。
    5.  返回新创建的记录列表。

### 1.3. AI 服务交互 (AI Service Interaction)

- **Prompt 设计**:
    - **任务**: 分析角色在给定情节中的认知和状态变化。
    - **输入**: `previous_character_details`, `section_content`。
    - **输出**:
        - **有变化时**: `{"newly_known_info": "...", "character_changes": "...", "character_details_after": "..."}`
        - **无变化时**: `{"no_change": true}`

### 1.4. 前端设计 (Frontend Design)

- **`ManuscriptWriter.tsx`**:
    - 在保存或生成小节后，调用上述新API。
    - 将返回结果存入state，并传递给侧边栏组件。
- **`CharacterStatusSidebar.tsx` (新增)**:
    - 实时展示本节出场角色的“新知信息”、“角色变化”和“最新详情”。

---

## **阶段二：关系演化 - 动态关系图谱 (Dynamic Relationship Mapping)**

**目标**：在角色状态追踪的基础上，增加对角色之间“关系”变化的记录与可视化。

### 2.1. 数据库设计 (Database Design)

#### 修改数据表：`character_change_log`

在 `is_auto_copied` 字段后，**新增**一个字段：

| 字段名 (Column) | 类型 (Type) | 约束 (Constraints) | 描述 (Description) |
| :--- | :--- | :--- | :--- |
| `relationship_changes` | `JSON` | `NULL` | **[P2]** 记录本节中当前角色与其他角色关系的变化 |

- **JSON 结构示例**:
  ```json
  [
    {
      "target_character_id": 102,
      "previous_relationship": "盟友",
      "current_relationship": "敌人",
      "change_reason": "因梅花盗事件产生误解"
    }
  ]
  ```

### 2.2. 后端设计 (Backend Design)

- **API Endpoint**: `POST /api/manuscripts/{manuscriptId}/sections/analyze-character-changes` (扩展)
    - **Response Body** 中增加 `relationship_changes` 字段。
- **核心逻辑 (`ManuscriptService`)**:
    - 在调用 `AiService` 后，额外处理返回的 `relationship_changes` 数据，并存入数据库。

### 2.3. AI 服务交互 (AI Service Interaction)

- **Prompt 设计 (扩展)**:
    - **任务**: 在原有分析基础上，增加“分析角色间关系变化”的任务。
    - **输入**: 增加 `all_characters_in_section` (本节所有出场角色的列表及其当前关系)。
    - **输出 (有变化时)**: 扩展JSON，增加 `relationship_changes` 字段。
      `{"newly_known_info": "...", "character_changes": "...", "character_details_after": "...", "relationship_changes": [...]}`

### 2.4. 前端设计 (Frontend Design)

- **`CharacterStatusSidebar.tsx` (扩展)**:
    - 在角色卡片中，增加一个区域显示本节发生的关系变化。
- **`RelationshipGraphModal.tsx` (新增)**:
    - 新增一个按钮，点击可打开关系图谱弹窗。
    - 使用可视化库（如 D3.js, ECharts）展示稿件中所有角色的关系网络。
    - 提供时间轴或章节选择器，可回溯关系图谱的历史演变。

---

## **阶段三：深化角色声音 - 记忆驱动的对话生成 (Memory-Driven Dialogue Generation)**

**目标**：利用已记录的角色“记忆”，让AI在生成对话时更符合角色的个人经历。

### 3.1. 数据库设计 (Database Design)

- 无需修改。本阶段复用 `character_change_log` 表中的 `newly_known_info` 字段作为角色的“记忆”数据源。

### 3.2. 后端设计 (Backend Design)

#### 新增 API Endpoint: `POST /api/ai/generate-dialogue` (示例)

- **功能**: 为指定角色，在特定情境下生成对话。
- **Request Body**:
  ```json
  {
    "character_id": 101,
    "manuscript_id": 1,
    "current_scene_description": "李寻欢与阿飞在酒馆对峙...",
    "dialogue_topic": "关于信任"
  }
  ```
- **核心逻辑 (`AiDialogueService`)**:
    1.  接收请求，根据 `character_id` 和 `manuscript_id` 从 `character_change_log` 表中查询该角色的“记忆”列表（即所有 `newly_known_info` 不为空的记录）。
    2.  对记忆进行摘要处理，提取最近或最重要的几条作为“关键记忆”。
    3.  调用 `AiService`，将“角色原始设定”、“当前情境”以及“关键记忆”一同传入。

### 3.3. AI 服务交互 (AI Service Interaction)

- **Prompt 设计**:
    - **任务**: 基于角色的性格、经历和当前情境，生成一段自然且符合逻辑的对话。
    - **输入**:
        - `character_profile`: 角色的核心设定。
        - `scene_description`: 当前的场景描述。
        - `key_memories`: 角色的关键记忆摘要列表 (e.g., `["得知了梅花盗的真实身份", "失去了好友阿飞的信任"]`)。
    - **输出**: `{"dialogue": "..."}`

### 3.4. 前端设计 (Frontend Design)

- **`ManuscriptWriter.tsx` (扩展)**:
    - 在创作工具栏中增加一个“AI生成对话”的按钮。
    - 点击后，允许用户选择角色、输入情境，然后调用 `generate-dialogue` API，并将结果插入到编辑器中。

---

## **阶段四：高亮成长 - 关键转折点标记 (Automatic Turning Point Flagging)**

**目标**：自动识别并标记对角色有重大影响的事件，便于作者回顾和聚焦。

### 4.1. 数据库设计 (Database Design)

#### 修改数据表：`character_change_log`

在 `relationship_changes` 字段后，**新增**一个字段：

| 字段名 (Column) | 类型 (Type) | 约束 (Constraints) | 描述 (Description) |
| :--- | :--- | :--- | :--- |
| `is_turning_point` | `BOOLEAN` | `NOT NULL`, `DEFAULT FALSE` | **[P4]** 标记本条记录是否为角色的关键转折点 |

### 4.2. 后端设计 (Backend Design)

- **API Endpoint**: `POST /api/manuscripts/{manuscriptId}/sections/analyze-character-changes` (扩展)
    - **Response Body** 中增加 `is_turning_point` 字段。
- **核心逻辑 (`ManuscriptService`)**:
    - 在调用 `AiService` 后，额外处理返回的 `is_turning_point` 数据，并存入数据库。

### 4.3. AI 服务交互 (AI Service Interaction)

- **Prompt 设计 (扩展)**:
    - **任务**: 在原有分析基础上，增加“评估本次变化的重要性”的任务。
    - **输出 (有变化时)**: 扩展JSON，增加 `is_turning_point` 字段。
      `{..., "is_turning_point": true}`

### 4.4. 前端设计 (Frontend Design)

- **`CharacterStatusSidebar.tsx` (扩展)**:
    - 对于被标记为 `is_turning_point` 的更新，在UI上使用特殊图标或样式（如加星、高亮）进行突出显示。
- **`CharacterGrowthPath.tsx` (新增)**:
    - 创建一个独立的角色详情页面或弹窗，其中有一个“成长路径”或“人生轨迹”的视图。
    - 在这个视图中，以时间轴的形式，只筛选并展示该角色的所有“关键转折点”记录，方便作者快速回顾角色的核心成长经历。---

## 实现进度记录（2025-09）
- `character_change_log` 表及对应的 JPA 实体已落地，并通过 `ManuscriptService` 在生成/保存场景时自动写入。
- 新增接口：
  - `POST /api/manuscripts/{manuscriptId}/sections/analyze-character-changes`
  - `GET /api/manuscripts/{manuscriptId}/character-change-logs`
  - `GET /api/manuscripts/{manuscriptId}/character-change-logs/{characterId}`
  - `POST /api/ai/generate-dialogue`
- 前端在 `ManuscriptWriter` 中提供了“角色状态侧边栏”“关系图谱”“成长轨迹”和“AI 生成对话”等功能，直接消费上述接口。

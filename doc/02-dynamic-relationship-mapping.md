# 阶段二子任务设计：关系演化图谱与可视化

## 1. 背景与目标
- **业务目标**：在角色状态追踪的基础上，记录并可视化角色之间的关系变化，帮助作者掌握人物之间的张力演化。
- **问题现状**：阶段一虽已积累角色状态，但缺少角色关系的结构化数据，作者无法快速了解人物关系的波动或冲突点。
- **阶段定位**：在延用阶段一数据模型的同时，扩展数据结构、AI Prompt、前端展示与分析能力。

## 2. 涉及系统与依赖
| 层级 | 模块 | 本阶段新增/修改点 |
| --- | --- | --- |
| 数据层 | `character_change_logs` | 新增 `relationship_changes` JSON 字段及索引 |
| 后端 | `CharacterChangeLog`、`CharacterChangeLogService` | 扩展实体、解析关系变化、增加图谱数据聚合服务 |
| 后端 API | `ManuscriptController` | 扩展原分析接口返回关系变化，新增关系图谱查询接口 |
| AI 服务 | `OpenAiService` | Prompt 增加关系分析需求，输出 JSON 包含 `relationship_changes` |
| 前端 | `types.ts`、`ManuscriptWriter`、`CharacterStatusSidebar`、新增 `RelationshipGraphModal` | 展示关系变化、调用新接口、可视化关系网络 |
| 依赖 | 前端新增 `echarts` + `echarts-for-react`（或同级可视化库） | 支撑关系图绘制 |

## 3. 数据库与实体扩展
### 3.1 数据库字段
- 在 `backend/src/database.sql` 中为 `character_change_logs` 增加字段：
  ```sql
  ALTER TABLE `character_change_logs`
    ADD COLUMN `relationship_changes` JSON NULL AFTER `is_auto_copied`;
  ```
- 索引建议：
  - `IDX_ccl_relationship_source` (`manuscript_id`, `character_id`, `deleted_at`)
  - 可选：在 JSON 中存储 `target_character_id`，使用虚拟列+索引（MySQL 8.0 支持），后续若查询性能不足再引入。

### 3.2 JPA 实体调整
- `CharacterChangeLog` 新增字段：
  ```java
  @Type(JsonType.class)
  @Column(name = "relationship_changes", columnDefinition = "JSON")
  private List<RelationshipChange> relationshipChanges;
  ```
- `RelationshipChange` 值对象（可创建为静态内部类或 `backend/src/main/java/com/example/ainovel/model/value` 下独立类）：
  ```java
  @Data
  public class RelationshipChange {
      private Long targetCharacterId;
      private String previousRelationship;
      private String currentRelationship;
      private String changeReason;
  }
  ```
- `JsonType` 依赖来自 `hypersistence-utils`（`pom.xml` 已引入）。
- 在实体的 `copyFrom` 方法中同时复制上一条的 `relationshipChanges`，便于 `no_change` 分支维持关系状态。

### 3.3 DTO 更新
- `CharacterChangeLogResponse` 增加 `List<RelationshipChangeDto> relationshipChanges`。
- 新增 DTO `RelationshipChangeDto`（与实体值对象字段一致）用于序列化。

## 4. 后端服务与 API 扩展
### 4.1 AI 请求上下文扩展
- 在 `CharacterChangeLogService` 中构建关系上下文：
  - 对于当前 `characterId`，查询上一条日志的 `relationshipChanges`，并折算成 “当前已知关系快照”。
  - 组装成 `Map<Long, RelationshipSnapshot>`：`targetId -> {currentRelationship, lastChangeReason, lastUpdateChapter}`。
  - 在 Prompt 中追加：
    ```
    【角色现有关系】
    {{#each relationshipSnapshot}}
    - 与 {{targetName}}：当前关系={{currentRelationship}}；最近更新章节={{chapter}}；备注={{reason}}
    {{/each}}
    【本节其他角色】
    {{list of other characters with synopsis}}
    额外任务：判断与每位出场角色的关系是否发生变化，若变化请写入 relationship_changes 数组。
    relationship_changes 为数组，每个元素包含 target_character_id, previous_relationship, current_relationship, change_reason。
    若无变化返回 []。
    ```
- 需要向 AI 提供目标角色姓名，故在服务中将 `characterId -> CharacterCard` 映射缓存，避免重复查询。

### 4.2 解析与持久化
- 在解析 JSON 时新增对 `relationship_changes` 字段的处理：
  - 若字段缺失，默认 `Collections.emptyList()`。
  - 校验 `target_character_id` 是否在本节出场角色名单内；若不在则忽略或记录警告日志。
  - 对每个 `RelationshipChange` 设置 `targetCharacterId` 等字段并保存。

### 4.3 API 调整
- `POST /api/manuscripts/{manuscriptId}/sections/analyze-character-changes`
  - Response JSON 增加 `relationshipChanges`。
- 新增 `GET /api/manuscripts/{manuscriptId}/relationships/graph`
  - 可选查询参数：`?outlineId=xxx`（默认为稿件关联的大纲）、`?chapterStart=...&chapterEnd=...` 用于筛选范围。
  - 返回结构：
    ```json
    {
      "nodes": [
        { "id": 101, "name": "李寻欢" },
        ...
      ],
      "edges": [
        {
          "sourceId": 101,
          "targetId": 102,
          "latestRelationship": "盟友",
          "latestChangeLogId": 555,
          "history": [
            {
              "logId": 555,
              "chapterNumber": 1,
              "sectionNumber": 3,
              "previousRelationship": "朋友",
              "currentRelationship": "盟友",
              "changeReason": "共同破案",
              "timestamp": "2025-07-20T12:00:00"
            },
            ...
          ]
        }
      ]
    }
    ```
  - Graph 数据由 `CharacterChangeLogService` 新增方法 `buildRelationshipGraph(Long manuscriptId, Long userId, RelationshipGraphQuery query)` 提供。

### 4.4 服务实现要点
- `buildRelationshipGraph` 步骤：
  1. 加载稿件下的所有角色卡（`CharacterCardRepository.findByStoryCardId`）。
  2. 查询 `character_change_logs` 中未删除的记录，按 `createdAt` 及章节顺序排序。
  3. 遍历日志，将 `relationshipChanges` 展开到 `Map<Pair<Long, Long>, List<HistoryEntry>>`，`Pair` 需规范化（小 ID 在前）以避免重复。
  4. 生成节点集合（所有涉及角色）。
  5. 为每条边计算最新关系（history 最后一条 `currentRelationship`）。

## 5. AI Prompt 扩展
- 在阶段一 Prompt 基础上追加：
  ```
  4. 判断该角色与本节出场的其他角色关系是否发生变化。
  5. 输出 relationship_changes 数组，每个对象包含：
     - target_character_id: number（请根据提供的角色列表返回 ID）
     - previous_relationship: string（若无记录请写 "未知"）
     - current_relationship: string
     - change_reason: string（描述导致关系变化的关键事件）
  输出示例：
  {
    "newly_known_info": "...",
    "character_changes": "...",
    "character_details_after": "...",
    "relationship_changes": [
      {
        "target_character_id": 102,
        "previous_relationship": "盟友",
        "current_relationship": "敌人",
        "change_reason": "因误解而反目"
      }
    ],
    "no_change": false
  }
  ```
- 追加输入：`allCharactersInScene`，包含 `{id, name, synopsis}`。
- 对于 AI 无法识别的角色（ID 未匹配），后端记录 warning 并跳过，以免破坏数据质量。

## 6. 前端设计与可视化
### 6.1 类型与 API 扩展
- `types.ts`：
  ```ts
  export interface RelationshipChange {
    targetCharacterId: number;
    previousRelationship: string;
    currentRelationship: string;
    changeReason: string;
  }

  export interface CharacterChangeLog {
    // 阶段一字段...
    relationshipChanges?: RelationshipChange[];
  }

  export interface RelationshipGraphNode {
    id: number;
    name: string;
  }

  export interface RelationshipGraphEdge {
    sourceId: number;
    targetId: number;
    latestRelationship: string;
    latestChangeLogId: number;
    history: Array<{
      logId: number;
      chapterNumber: number;
      sectionNumber: number;
      previousRelationship: string;
      currentRelationship: string;
      changeReason: string;
      timestamp: string;
    }>;
  }

  export interface RelationshipGraphResponse {
    nodes: RelationshipGraphNode[];
    edges: RelationshipGraphEdge[];
  }
  ```
- `services/api.ts` 新增：
  - `fetchRelationshipGraph(manuscriptId: number, params?: GraphQueryParams): Promise<RelationshipGraphResponse>`。
  - `GraphQueryParams` 包含章节范围、是否仅显示最新关系等选项。

### 6.2 `CharacterStatusSidebar` 更新
- 在角色卡片中增加“关系变化”区域：
  - 若 `relationshipChanges` 非空，使用列表展示 `targetName -> 变化描述`。
  - 可使用 Tag 表示新增/恶化/改善（根据 `previousRelationship` 与 `currentRelationship` 对比）。
- 增加“查看关系图谱”按钮，触发 `RelationshipGraphModal`。

### 6.3 新组件 `RelationshipGraphModal.tsx`
- 结构：
  - `Modal` + `Tabs`，一个 Tab 显示图谱，另一个 Tab 显示时间轴列表。
  - 图谱：使用 `echarts` Force Graph，节点显示角色名，边显示最新关系，可根据历史染色。
  - 时间轴：`Timeline` 或 `Table` 展示所有关系变化记录，允许按角色筛选。
- 交互：
  - Modal 打开时调用 `fetchRelationshipGraph`。
  - 提供章节范围筛选（`Slider` 或 `Select`）。
  - 点击图谱边可在右侧显示该关系的历史详情。
- 依赖：在 `package.json` 增加
  ```json
  "echarts": "^5.x",
  "echarts-for-react": "^3.x"
  ```
  并在 `vite.config.ts` 确保按需引入。

### 6.4 `ManuscriptWriter` 交互
- 维持阶段一逻辑，同时：
  - 将 `characterChangeLogs[sceneId]` 传入侧边栏，侧边栏负责展示关系变化。
  - 将 `selectedManuscript.id` 作为 prop 传给侧边栏，便于其触发图谱弹窗时调用接口。

## 7. 权限与性能考量
- 所有新数据均基于 `manuscriptId` + `userId` 权限校验，与阶段一一致。
- 图谱接口需做分页/限量策略：
  - 默认返回前 500 条关系变化；超过时要求前端带筛选条件。
  - 可在查询参数中提供 `limit`、`order`，后端使用 `PageRequest` 控制。
- JSON 列存储注意 MySQL 版本兼容；若测试环境为 5.7 需降级为 `LONGTEXT` 存储字符串 JSON 并手动序列化。

## 8. 测试计划
- **后端单元测试**：
  - `CharacterChangeLogServiceRelationshipTest`：覆盖 AI 返回关系变化、关系为空、目标角色不匹配等场景。
  - `RelationshipGraphServiceTest`：验证图谱聚合逻辑、章节筛选功能。
- **后端集成测试**：
  - `ManuscriptControllerIT` 中新增关系图谱接口测试，验证鉴权与返回结构。
- **前端测试**：
  - `CharacterStatusSidebar` 对关系变化的渲染测试。
  - `RelationshipGraphModal` 使用 mocked API 验证加载、筛选、渲染（可通过 `@testing-library/react` + `jest`）。
- **性能测试**（可选）：
  - 构造 1k+ 条关系变动数据，验证接口响应时间与前端渲染表现。

## 9. 实施步骤
1. **数据库升级**：新增 JSON 字段，更新实体与仓储，编译验证。
2. **服务层改造**：扩展 `CharacterChangeLogService`，实现关系上下文收集、AI Prompt 扩展、JSON 解析。
3. **图谱聚合能力**：实现 `buildRelationshipGraph` 方法及 DTO。
4. **API 与 Controller**：扩展原分析接口返回字段、新增图谱查询接口。
5. **前端类型与 API**：同步更新类型定义、封装新接口。
6. **UI 开发**：更新 `CharacterStatusSidebar`，实现 `RelationshipGraphModal`，引入可视化库。
7. **联调测试**：补充单元/集成测试，运行 `npm test` / `npm run lint`（如有），人工验证交互。
8. **文档与回归**：更新用户手册、在 README 中说明关系图谱功能，并回归阶段一核心流程。

完成后，用户即可在写作过程中查看每节角色间关系的即时变化，并通过关系图谱回顾故事中人物关系的动态演进。

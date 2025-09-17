# 阶段四子任务设计：角色关键转折点标记

## 1. 背景与目标
- **目标**：自动识别对角色成长影响重大的情节，并在 UI 中突出展示，帮助作者快速回顾角色弧光。
- **依赖**：基于阶段一、二已存储的角色状态与关系数据，阶段三产生的记忆摘要也将引用这些标记。
- **成果**：
  - 数据层新增 `is_turning_point` 标记。
  - 后端扩展分析逻辑和查询接口。
  - 前端突出显示转折点并提供“成长路径”时间轴视图。

## 2. 数据库与实体变更
### 2.1 数据库结构
- 在 `backend/src/database.sql` 中为 `character_change_logs` 增加字段：
  ```sql
  ALTER TABLE `character_change_logs`
    ADD COLUMN `is_turning_point` BOOLEAN NOT NULL DEFAULT FALSE AFTER `relationship_changes`;
  ```
- 索引：
  - `IDX_ccl_turning_points` (`manuscript_id`, `character_id`, `is_turning_point`, `deleted_at`) 用于高效查询某角色的关键节点。

### 2.2 实体调整
- `CharacterChangeLog` 新增属性：
  ```java
  @Column(name = "is_turning_point", nullable = false)
  private boolean isTurningPoint = false;
  ```
- `copyFrom` 方法在复制上一条记录时默认 `isTurningPoint = false`（无变化时不自动标记）。
- DTO `CharacterChangeLogResponse` 增加 `isTurningPoint` 字段。

## 3. 后端逻辑扩展
### 3.1 AI 结果解析
- 在 `CharacterChangeLogService.analyzeAndPersist` 中解析 AI JSON：
  - 若存在布尔字段 `is_turning_point`，写入实体。
  - 若缺失则默认 `false` 并记录 warning 日志。
- 当 `no_change = true` 时，强制 `isTurningPoint = false`。

### 3.2 Prompt 扩展
- 在阶段一/二 Prompt 基础上追加指令：
  ```
  6. 请判断本节对角色是否构成“关键转折点”，标准包括但不限于：
     - 角色信念或价值观发生重大转变；
     - 与重要角色的关系出现不可逆的改变；
     - 影响角色命运的重大事件（濒死、觉醒、背叛等）；
     - 触发后续剧情核心冲突的决定。
     如果符合，请将 is_turning_point 设为 true，并在 character_changes 中明确说明转折原因。
  输出示例：
  {
    ...,
    "is_turning_point": true
  }
  ```
- 在 AI 输入部分补充提醒：若判断为转折点需在 `character_changes` 中给出理由，方便前端展示。

### 3.3 查询接口
- 新增服务方法：
  ```java
  public List<CharacterChangeLog> getTurningPoints(Long manuscriptId, Long characterId, Long userId);
  ```
- 新增 API：
  - `GET /api/manuscripts/{manuscriptId}/characters/{characterId}/turning-points`
    - 返回排序后的转折点列表（按章节、节、创建时间）。
    - 响应字段：`chapterNumber`、`sectionNumber`、`characterChanges`、`relationshipChanges`、`createdAt`、`logId`。
- 在 `CharacterChangeLogService` 中实现查询逻辑，复用仓储方法 `findByCharacter_IdAndManuscript_IdAndDeletedAtIsNullOrderByChapterNumberAscSectionNumberAsc` 并过滤 `isTurningPoint=true`。

### 3.4 与其他模块协作
- 阶段三对话生成：在选择记忆时优先包含转折点记录（若存在）。可在 `getMemoriesForDialogue` 中增加 `boolean prioritizeTurningPoints` 参数。
- 关系图谱：当转折点涉及关系变化时，可在前端图谱中高亮对应边（扩展功能，非必须）。

## 4. 前端改动
### 4.1 `CharacterStatusSidebar`
- 对 `isTurningPoint=true` 的记录：
  - 在卡片标题旁显示星标或醒目标签（如 `Tag` + “转折点”）。
  - `characterChanges` 文案高亮显示。
- 新增“查看成长路径”按钮（在角色面板顶部），点击后打开新组件。

### 4.2 新组件 `CharacterGrowthPath.tsx`
- 位置：`frontend/src/components/CharacterGrowthPath.tsx`。
- Props：
  ```ts
  interface CharacterGrowthPathProps {
    visible: boolean;
    onClose: () => void;
    manuscriptId: number;
    character: CharacterCard;
  }
  ```
- 功能：
  - 弹窗（`Drawer` 或 `Modal`），展示该角色的所有转折点，按时间轴排列。
  - 每个节点显示：章节/小节、发生的变化、关系变化摘要、创建时间。
  - 支持筛选：按章节范围、按关键词搜索。
  - “跳转到章节”按钮：调用父级回调，在 `ManuscriptWriter` 中定位相应场景。
- 数据来源：在打开时调用 `GET /api/manuscripts/{id}/characters/{characterId}/turning-points`。
- UI 建议：使用 Ant Design `Timeline` 或自定义竖向步骤条。

### 4.3 `ManuscriptWriter` 集成
- 将 `selectedCharacterForSidebar` 状态下沉至 `CharacterStatusSidebar`，当用户点击成长路径时传递角色对象。
- 通过回调 `onJumpToScene(sceneId)` 使时间轴与左侧大纲联动。

### 4.4 类型与 API
- `types.ts` 扩展：
  ```ts
  export interface CharacterChangeLog {
    ...
    isTurningPoint: boolean;
  }

  export interface TurningPointEntry {
    id: number;
    chapterNumber: number;
    sectionNumber: number;
    characterChanges: string;
    relationshipChanges?: RelationshipChange[];
    createdAt: string;
    sceneId: number;
  }
  ```
- `services/api.ts` 新增：
  ```ts
  export const fetchTurningPoints = (manuscriptId: number, characterId: number): Promise<TurningPointEntry[]> =>
    fetch(`/api/v1/manuscripts/${manuscriptId}/characters/${characterId}/turning-points`, { headers: getAuthHeaders() })
      .then(res => handleResponse<TurningPointEntry[]>(res));
  ```

## 5. 安全与一致性
- 权限检查与阶段一一致，所有查询需校验 `manuscriptId` 与 `userId`。
- AI 返回转折点时的可信度：
  - 可在日志中记录 AI 原始响应，便于人工审查。
  - 预留手动调整能力（后续扩展）：允许用户在 UI 中手动设/取消转折点（本阶段不实现，但实体设计已支持手动更新 `is_turning_point`）。

## 6. 测试计划
- **后端单元测试**：
  - `CharacterChangeLogServiceTurningPointTest`：验证 `is_turning_point` 解析、`no_change` 分支强制为 false、查询接口返回顺序正确。
- **后端集成测试**：
  - `ManuscriptControllerIT`：新增 GET 转折点接口鉴权、返回字段校验。
- **前端测试**：
  - `CharacterStatusSidebar` 渲染星标测试。
  - `CharacterGrowthPath` 组件交互测试（加载、筛选、跳转）。
- **人工验证**：
  - 构造包含多个转折点的场景，确认时间轴排序正确且与章节联动。

## 7. 实施步骤
1. **数据库与实体**：添加 `is_turning_point` 字段，更新实体、仓储与 DTO。
2. **AI 逻辑**：扩展 Prompt 与解析逻辑，单元测试覆盖布尔标记。
3. **查询接口**：实现 `getTurningPoints` 服务及 REST API。
4. **前端改造**：更新类型、API 封装、侧边栏高亮、成长路径组件。
5. **联调与测试**：运行 `mvn test`、`npm test`，手动验证 UI 与数据一致。
6. **文档更新**：在用户手册中新增转折点功能说明，示意如何使用成长路径视图。

完成后，作者可以快速识别角色的关键成长节点，并通过时间轴回顾人物弧光，实现更高效的故事打磨。

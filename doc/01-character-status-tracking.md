# 阶段一子任务设计：角色状态追踪核心能力

## 1. 背景与目标
- **业务目标**：让系统能够随章节追踪每位角色的认知和状态变化，为后续关系演化、记忆驱动生成等高级能力打下数据基础。
- **交付边界**：本阶段仅覆盖角色状态变动的采集、存储、展示，不含关系图谱、对话生成、转折点等扩展能力。
- **关键痛点**：目前稿件系统只保存正文，缺乏结构化的角色动态信息，作者无法快速回溯角色认知，AI 也无法感知角色演化。

## 2. 涉及系统与依赖
| 层级 | 受影响的模块 | 说明 |
| --- | --- | --- |
| 数据层 | MySQL (`backend/src/database.sql`) | 新增角色变动记录表、索引与软删除字段 |
| 后端 | Spring Boot (`backend/src/main/java/com/example/ainovel`) | 新增实体、仓储、服务、控制器接口，扩展 `ManuscriptService` 逻辑 |
| AI 调用 | `OpenAiService` | 需要新增 JSON 输出的分析 Prompt 以及解析逻辑 |
| 前端 | React + Ant Design (`frontend/src`) | 扩展 `ManuscriptWriter`、新增 `CharacterStatusSidebar`、新增 API 封装与类型 |
| 认证授权 | Spring Security | 复用现有权限校验逻辑，确保用户只能分析自己稿件 |

## 3. 数据库与实体设计
### 3.1 新表 `character_change_logs`
- 新建表文件位置：`backend/src/database.sql` 添加建表语句；若后续引入 Flyway/Liquibase 可再拆分脚本。
- 字段定义：
  | 字段 | 类型 | 约束 | 说明 |
  | --- | --- | --- | --- |
  | `id` | BIGINT | PK, AUTO_INCREMENT | 主键 |
  | `character_id` | BIGINT | NOT NULL | 外键 -> `character_cards.id` |
  | `manuscript_id` | BIGINT | NOT NULL | 外键 -> `manuscripts.id` |
  | `scene_id` | BIGINT | NOT NULL | 对应大纲场景 ID，便于按场景检索 |
  | `outline_id` | BIGINT | NOT NULL | 外键 -> `outline_cards.id`，与稿件保持一致 |
  | `chapter_number` | INT | NOT NULL | 数字章节（`OutlineChapter.chapterNumber`） |
  | `section_number` | INT | NOT NULL | 数字小节（`OutlineScene.sceneNumber`） |
  | `newly_known_info` | TEXT | NULL | 本节角色获知的新信息 |
  | `character_changes` | TEXT | NULL | 角色在本节发生的变化 |
  | `character_details_after` | LONGTEXT | NOT NULL | 本节结束后角色的完整画像 |
  | `is_auto_copied` | BOOLEAN | NOT NULL DEFAULT FALSE | 当 AI 判定无变化时复制上一条详情并置为 true |
  | `created_at` | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP | 创建时间 |
  | `updated_at` | DATETIME | NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
  | `deleted_at` | DATETIME | NULL | 软删除时间 |
- 索引建议：
  - `IDX_ccl_character` (`character_id`, `manuscript_id`)
  - `IDX_ccl_scene` (`manuscript_id`, `scene_id`, `chapter_number`, `section_number`)
  - `IDX_ccl_turning` (`manuscript_id`, `character_id`, `deleted_at`) 便于过滤。
- 软删除：
  - 在实体上使用 `@SQLDelete` 和 `@Where(deleted_at IS NULL)` 控制。
  - 删除操作默认打标 `deleted_at`，保留历史数据。

### 3.2 JPA 实体 `CharacterChangeLog`
- 位置：`backend/src/main/java/com/example/ainovel/model/CharacterChangeLog.java`
- 关键设计：
  - `@ManyToOne` 关联 `CharacterCard`、`Manuscript`、`OutlineCard`；`sceneId`、`chapterNumber`、`sectionNumber` 使用普通列。
  - `characterDetailsAfter` 使用 `@Lob(columnDefinition = "LONGTEXT")` 以容纳大文本。
  - 审计字段使用 `@CreationTimestamp` / `@UpdateTimestamp`。
  - 提供便捷方法：
    - `copyFrom(CharacterChangeLog previous)`：复制上一条详情并将 `isAutoCopied` 设为 true。
    - `buildTimelineKey()`：返回 `chapterNumber-sectionNumber`，用于排序。

### 3.3 仓储接口 `CharacterChangeLogRepository`
- 位置：`backend/src/main/java/com/example/ainovel/repository`
- 方法清单：
  - `Optional<CharacterChangeLog> findFirstByCharacter_IdAndManuscript_IdAndDeletedAtIsNullOrderByChapterNumberDescSectionNumberDescCreatedAtDesc(Long characterId, Long manuscriptId)`：获取角色上一条有效记录。
  - `List<CharacterChangeLog> findByManuscript_IdAndSceneIdAndDeletedAtIsNullOrderByCharacter_Id(Long manuscriptId, Long sceneId)`：用于当前场景展示。
  - `List<CharacterChangeLog> findByCharacter_IdAndManuscript_IdAndDeletedAtIsNullOrderByChapterNumberAscSectionNumberAsc(Long characterId, Long manuscriptId)`：后续阶段重用。

## 4. 后端服务与 API 设计
### 4.1 DTO 层
- `AnalyzeCharacterChangesRequest` (`backend/src/main/java/com/example/ainovel/dto`)
  - 字段：`sceneId`、`chapterNumber`、`sectionNumber`、`sectionContent`、`characterIds: List<Long>`、`outlineId`（冗余校验用）。
  - 前端传入章节编号；后端会通过 sceneId 反查并校验，若不一致则以后端数据为准。
- `CharacterChangeLogResponse`
  - 字段：`characterId`、`newlyKnownInfo`、`characterChanges`、`characterDetailsAfter`、`isAutoCopied`、`chapterNumber`、`sectionNumber`、`createdAt`、`logId`。
- 返回体：`List<CharacterChangeLogResponse>`。

### 4.2 新增服务 `CharacterChangeLogService`
- 位置：`backend/src/main/java/com/example/ainovel/service`
- 职责：
  1. 校验稿件与场景归属（复用 `ManuscriptService` 中的 `validateManuscriptAccess`）。
  2. 根据 `sceneId` 解析 `chapterNumber`、`sectionNumber`、`outlineId`，防止前端篡改。
  3. 汇总角色上下文：
     - `previousDetails`：优先取上一条日志的 `characterDetailsAfter`；如无则使用 `CharacterCard.details`。
     - `baselineSummary`：角色卡 Synopsis，用于 Prompt。
  4. 调用 AI（详见 5 节），解析结果。
  5. 将结果持久化为 `CharacterChangeLog`。
  6. 返回最新记录（保存后刷新实体）。
- 主要方法：
  - `List<CharacterChangeLog> analyzeAndPersist(Long manuscriptId, AnalyzeCharacterChangesRequest req, Long userId)`。
  - `List<CharacterChangeLog> getLogsForScene(Long manuscriptId, Long sceneId, Long userId)`。

### 4.3 控制器扩展
- 在 `ManuscriptController` 下新增接口：
  ```java
  @PostMapping("/manuscripts/{manuscriptId}/sections/analyze-character-changes")
  public ResponseEntity<List<CharacterChangeLogResponse>> analyzeCharacterChanges(
      @PathVariable Long manuscriptId,
      @RequestBody AnalyzeCharacterChangesRequest request,
      @AuthenticationPrincipal User user)
  ```
- 校验：
  - `manuscriptId` 必须属于当前登录用户。
  - `sceneId` 必须属于稿件关联的大纲；若缺失或不匹配返回 400。
  - `characterIds` 为空则返回 400。
- 新增 `@GetMapping("/manuscripts/{manuscriptId}/sections/{sceneId}/character-change-logs")` 用于加载已有记录（页面初始化或切换场景时调用）。

### 4.4 与 `ManuscriptService` 的协作
- `ManuscriptService` 保持生成/保存正文的逻辑不变。
- 对外提供便捷方法：
  - `public List<CharacterChangeLog> analyzeCharacterChanges(Long manuscriptId, AnalyzeCharacterChangesRequest req, Long userId)` 代理至 `CharacterChangeLogService`，以减少控制器对多个服务的依赖。
- 生成正文后由前端再次调用分析接口；后端不自动触发，避免阻塞原写作流程。

## 5. AI 交互设计
### 5.1 Prompt 结构
- 新增工具类 `CharacterChangePromptBuilder`（或放入 `CharacterChangeLogService` 内部）：
  ```
  你是小说编辑，负责追踪角色状态。请根据提供的信息判断角色是否在本小节发生变化。
  【角色既有设定】
  - 基础档案: {{characterSynopsis}}
  - 上一次记录的角色详情: {{previousDetails}}
  【本节正文】
  {{sectionContent}}
  任务：
  1. 分析角色在本节新增认知或信息，使用第一人称/第三人称均可。
  2. 总结角色状态（心理、生理、外貌、人际态度等）发生的变化。
  3. 输出本节结束后的最新角色详情，需完整覆盖角色核心设定，允许保留未变化的描述。
  输出格式必须是 JSON：
  {
    "newly_known_info": "string，可为空字符串",
    "character_changes": "string，可为空字符串",
    "character_details_after": "string",
    "no_change": true/false
  }
  若确无变化，将 newly_known_info 和 character_changes 置为 ""，并将 no_change 设为 true。
  ```
- 采用 `OpenAiService.callOpenAi(..., jsonMode=true)` 获得 JSON。
- 出错处理：
  - 若 AI 响应缺失 `character_details_after`，抛出 502 并记录日志。
  - 若 `no_change=true`，则复制上一条详情并设置 `is_auto_copied=true`。

### 5.2 Token 控制与重试
- 依赖现有 `SettingsService` 获取用户 API Key / Base URL / Model。
- 使用 `@Retryable`（已在 `OpenAiService` 中实现）。
- 对于多角色请求：串行调用保证上下文清晰；若后期性能受限再考虑批量 Prompt。

## 6. 前端设计
### 6.1 类型与 API
- `frontend/src/types.ts` 新增：
  ```ts
  export interface CharacterChangeLog {
    id: number;
    characterId: number;
    chapterNumber: number;
    sectionNumber: number;
    newlyKnownInfo?: string;
    characterChanges?: string;
    characterDetailsAfter: string;
    isAutoCopied: boolean;
    createdAt: string;
  }
  ```
- `frontend/src/services/api.ts` 新增函数：
  - `analyzeCharacterChanges(manuscriptId: number, payload: AnalyzeCharacterChangesPayload): Promise<CharacterChangeLog[]>`
  - `fetchCharacterChangeLogs(manuscriptId: number, sceneId: number): Promise<CharacterChangeLog[]>`
- `AnalyzeCharacterChangesPayload` 定义在同文件或 `types.ts` 中，包含 `sceneId`, `chapterNumber`, `sectionNumber`, `sectionContent`, `characterIds`。

### 6.2 `ManuscriptWriter.tsx` 调整
- 额外加载角色列表：当 `selectedStoryId` 变化时调用 `fetchStoryDetails` 获取 `characterCards`，缓存为 `characterMap`（`id -> CharacterCard`）。
- 本地状态：
  - `const [characterChangeLogs, setCharacterChangeLogs] = useState<Record<number, CharacterChangeLog[]>>({}); // key = sceneId`
  - `const [isAnalyzing, setIsAnalyzing] = useState(false);`
- 场景切换时：调用 `fetchCharacterChangeLogs`，更新 `characterChangeLogs[sceneId]`。
- 在 `handleGenerateContent` 与 `handleSaveContent` 成功后追加：
  ```ts
  await runCharacterAnalysis(sectionContent, selectedSceneId);
  ```
  - `runCharacterAnalysis` 将 `sectionContent`、`selectedScene?.sceneNumber`、`chapterNumber`（需从 `selectedOutlineDetail` 中找到对应章节）以及 `presentCharacterIds` 传入。
  - 若无角色 ID，提示用户补齐大纲数据。
- UI 调整：
  - 将原布局改为三列：大纲树（span=6）、正文编辑（span=12）、角色侧栏（span=6）。
  - 在右侧渲染 `<CharacterStatusSidebar>`，传入 `characters`、`logs`、`chapterNumber` 等。
  - 在底部按钮区添加“分析角色变化”独立按钮，允许手动触发。

### 6.3 新组件 `CharacterStatusSidebar.tsx`
- 位置：`frontend/src/components/CharacterStatusSidebar.tsx`
- 功能：
  - 按角色分组展示最近一次分析结果。
  - 显示字段：角色名、`newlyKnownInfo`（若为空显示“无”）、`characterChanges`、`characterDetailsAfter`（可折叠展开）。
  - 使用 Ant Design `Collapse` 或 `Card`。对于 `isAutoCopied=true` 的记录在标题处显示“无变化”标记。
  - 提供“历史记录”链接，点击后弹出 Modal（本阶段仅展示当前场景的所有记录，后续阶段可拓展）。
- 属性：
  ```ts
  interface CharacterStatusSidebarProps {
    characterMap: Record<number, CharacterCard>;
    logs: CharacterChangeLog[];
    chapterNumber?: number;
    sectionNumber?: number;
    isAnalyzing: boolean;
    onAnalyze?: () => void;
  }
  ```

### 6.4 交互反馈
- 分析中显示 `Spin` 或按钮 loading。
- API 错误通过 `message.error` 呈现。
- 当场景缺少 `presentCharacterIds` 时，提示用户回到大纲补充角色信息。

## 7. 安全与权限
- 所有新接口均要求登录（沿用 `@AuthenticationPrincipal`）。
- `CharacterChangeLogService` 在写入前调用：
  - `validateManuscriptAccess(manuscript, userId)`。
  - 校验 `sceneId` 是否属于稿件关联的大纲（通过 `OutlineScene` -> `OutlineChapter` -> `OutlineCard`）。
  - 校验 `characterIds` 是否属于同一个故事卡 `storyCardId`，避免越权访问其他故事的角色档案。

## 8. 测试计划
- **后端单元测试**（`backend/src/test/java`）：
  - `CharacterChangeLogServiceTest`
    - 场景：AI 返回有变化 / 无变化 / JSON 缺失字段 / 场景与稿件不匹配抛异常。
    - 使用 `@MockBean` 模拟 `OpenAiService` 回传 JSON。
  - `CharacterChangeLogRepositoryTest`
    - 验证排序与软删除过滤逻辑。
- **后端集成测试**：
  - `ManuscriptControllerIT` 新增接口测试，覆盖鉴权、输入校验、返回结构。
- **前端测试**：
  - 使用 React Testing Library 对 `CharacterStatusSidebar` 编写快照与交互测试。
  - 为 `runCharacterAnalysis` 添加 mock API 调用测试，确保 state 更新正确。
- **手动验证**：
  - 启动前后端，完成“生成章节 -> 分析 -> 查看侧栏”流程。

## 9. 实施步骤拆解
1. **数据库与实体**：创建表结构、编写实体/仓储、跑一次 `mvn test` 保障编译通过。
2. **服务层开发**：实现 `CharacterChangeLogService`、AI Prompt 构建与 JSON 解析。
3. **控制器与 DTO**：新增请求/响应对象与 REST 接口，补充权限校验与异常处理。
4. **前端基础能力**：扩展类型与 API 方法，调整 `ManuscriptWriter` 状态管理。
5. **UI 组件**：实现 `CharacterStatusSidebar`、修改布局、打通分析流程。
6. **联调与测试**：补充单元/集成测试、跑端到端验证，更新文档/README 中的使用说明（如必要）。
7. **验收与回归**：确认旧流程（生成/保存稿件）保持可用，检查软删除与历史查询准确性。

完成以上步骤后，系统即可在每个章节生成/保存后自动得到角色状态追踪数据，为后续阶段提供可靠的数据积累。

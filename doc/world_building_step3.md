# 世界构建 Step 3 —— AI 能力与生成队列

## 1. 目标

在完成数据模型之后，本阶段接入 AI 服务，实装以下能力：

1. 模块级“一键生成”接口：调用 LLM 产出结构化字段内容，并写回模块。
2. 字段级“AI 优化”接口：沿用现有润色服务但注入世界构建上下文。
3. 正式创建流程：生成队列、进度查询、失败重试、断点续传。
4. 队列执行器：确保同一世界的模块顺序生成，避免并发调用。
5. 状态同步：与 Step 2 的世界/模块状态、版本号联动。

## 2. 数据结构扩展

### 2.1 `world_generation_jobs`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT PK |
| `world_id` | BIGINT | 关联世界。 |
| `module_key` | VARCHAR(32) | 对应模块。 |
| `job_type` | ENUM | `MODULE_FINAL`（正式创建生成完整信息）。后续可扩展 `MODULE_DRAFT` 等。 |
| `status` | ENUM | `WAITING` / `RUNNING` / `SUCCEEDED` / `FAILED` / `CANCELLED`。 |
| `sequence` | INT | 同一世界内的执行顺序，从 1 开始。 |
| `attempts` | INT | 已重试次数，默认 0。 |
| `last_error` | TEXT | 最近一次失败信息。 |
| `started_at` | DATETIME | 实际开始时间。 |
| `finished_at` | DATETIME | 结束时间。 |
| `created_at` | DATETIME | 创建时间。 |
| `updated_at` | DATETIME | 更新时间。 |

索引：
- `idx_generation_jobs_world_status` (`world_id`, `status`, `sequence`).
- `idx_generation_jobs_status_seq` (`status`, `sequence`).

### 2.2 `world_generation_runs`（可选）

用于记录一次“正式创建”流程的整体运行：
- `id`, `world_id`, `status`, `created_at`, `completed_at`, `triggered_by`, `version_before`, `version_after`。
- 在 `/publish` 时创建，成功后更新 `status=SUCCEEDED` 并递增 `world.version`。

> 若暂不实现，可在 `WorldEntity` 上直接记录 `generationStartedAt`、`generationRunId`。

## 3. 模块级自动生成

### 3.1 服务接口

新增 `WorldModuleGenerationService`：

```java
public interface WorldModuleGenerationService {
    ModuleGenerationResult generateModule(Long worldId, String moduleKey, Long userId);
}
```

- 读取世界、模块内容与 `WorldModuleDefinition`。
- 构造 `WorldModulePromptContext`（对应 Step 1 的 JSON 结构）。
- 调用 `WorldPromptTemplateService.renderDraftTemplate(moduleKey, context)` 获取提示词。
- 调用 `AiService` 发送请求（支持模型配置、温度等参数）。
- 解析 LLM 返回的 JSON，将字段写入 `WorldModuleEntity.fields`，更新 `status`、`contentHash`。
- 记录 `world_module_change_log`（`AUTO_GENERATE`）。

### 3.2 接口设计

`POST /api/v1/worlds/{id}/modules/{moduleKey}/generate`

- 请求：无附加参数（后续可扩展“覆盖/合并”选项）。
- 响应：更新后的模块对象。
- 限制：
  - `world.status` 不为 `GENERATING`（避免与正式创建冲突）。
  - 根据用户设置的调用频率限制（可复用现有 AI 调用速率限制）。

错误处理：
- JSON 解析失败时返回 502，并保留原字段（不覆盖）。
- 若 AI 返回空内容，提示用户手动编辑。

## 4. 字段级 AI 优化

### 4.1 上下文构建

复用现有 `/api/v1/refine` 思路，但添加世界信息：

```java
public class WorldFieldRefineRequest {
    Long worldId;
    String moduleKey;
    String fieldKey;
    String text;
    String instruction; // 可空
}
```

服务层：
1. 校验模块与字段。
2. 取出 `focusNote`（Step 1 提供），拼接到提示词骨架中。
3. 调用现有 `AiService` 使用世界构建专用模板 `world.field.refine`（默认文本同 Step 1）。
4. 返回润色结果，前端可选择覆盖原值。

### 4.2 API

`POST /api/v1/worlds/{id}/modules/{moduleKey}/fields/{fieldKey}/refine`

- 请求体： `{ "text": "……", "instruction": "（可选）" }`
- 响应： `{ "result": "……" }`
- 记录 change log：`AUTO_GENERATE` 类型，标记为“refine”。

## 5. 正式创建流程

### 5.1 `/publish` 执行流程

1. 前端调用 `POST /worlds/{id}/publish`。
2. `WorldPublicationService.preparePublish`：
   - 校验所有模块 `status`，收集 `modulesToGenerate` 与 `modulesToReuse`。
   - 若存在 `missingFields`，抛出 `400` 并附带 `{ "missingFields": [{"module":"cosmos","fields":[...]}] }`。
   - 将世界状态置为 `GENERATING`，将需生成模块 `status` 设置为 `AWAITING_GENERATION`。
3. 创建 `WorldGenerationRun`（若有）。
4. 依序生成 `world_generation_jobs`：

```java
int sequence = 1;
for (module : modulesToGenerate) {
    saveJob(worldId, module.getModuleKey(), sequence++, jobType = MODULE_FINAL);
}
```

5. 返回响应（同 Step 2）。前端根据响应弹出进度窗口并开始轮询。

> 若 `modulesToGenerate` 为空（例如仅调整备注），直接将世界状态改回 `ACTIVE` 并返回成功。

### 5.2 队列执行器

实现 `WorldGenerationJobProcessor`：

- 运行在 `@Scheduled(fixedDelay = 3000ms)` 或基于 Spring Batch/TaskExecutor。
- 每次从数据库取一个 `WAITING` 任务，按 `sequence` 升序：

```sql
SELECT * FROM world_generation_jobs
WHERE status = 'WAITING'
ORDER BY sequence ASC
LIMIT 1
FOR UPDATE SKIP LOCKED;
```

- 将任务状态置为 `RUNNING`，记录 `started_at`，并执行：
  1. 读取对应 `WorldModuleEntity` 与 `fields`。
  2. 构造 `WorldModulePromptContext`，调用 `world.module.${moduleKey}.final` 模板。
  3. 调用 AI 生成完整信息，写入 `world_modules.full_content`、`full_content_updated_at`。
  4. 将模块 `status` 更新为 `COMPLETED`，`world.contentHash` 保持不变。
  5. 任务标记 `SUCCEEDED`，记录 `finished_at`。
- 若调用失败：
  - 捕获异常，`status=FAILED`，写入 `last_error` 与 `finished_at`。
  - 模块状态保持 `FAILED`，供前端显示重试按钮。

### 5.3 顺序与并发控制

- 同一世界的任务按 `sequence` 串行执行；`FOR UPDATE SKIP LOCKED` + `ORDER BY sequence` 可避免并发。
- 若多个世界同时生成，可允许在同一调度器中交错执行（只要 `LIMIT 1` 逐个处理即可），或根据系统负载调整并发度。
- 可配置最大并发数 `generation.maxConcurrentJobs`，通过 `ThreadPoolTaskExecutor` 控制；但为满足“排队生成，不要并发调用 AI”，默认设置为 1。

### 5.4 断点续传

- 服务器重启后，启动时执行：

```sql
UPDATE world_generation_jobs
SET status = 'WAITING', last_error = CONCAT('[recovered]', last_error)
WHERE status = 'RUNNING';
```

- 将对应世界的状态仍保持 `GENERATING`，模块状态保持 `AWAITING_GENERATION`。
- 轮询任务会自动继续生成。

## 6. 进度查询与重试

### 6.1 进度 API

`GET /api/v1/worlds/{id}/generation`

响应示例：

```json
{
  "worldId": 12,
  "status": "GENERATING",
  "version": 0,
  "queue": [
    {
      "moduleKey": "cosmos",
      "label": "宇宙观与法则",
      "status": "RUNNING",
      "attempts": 1,
      "startedAt": "2025-01-20T10:35:00",
      "finishedAt": null,
      "error": null
    },
    {
      "moduleKey": "geography",
      "label": "地理与生态",
      "status": "WAITING",
      "attempts": 0,
      "startedAt": null,
      "finishedAt": null,
      "error": null
    }
  ]
}
```

- `status` 取自 `world.status`；若全部 `SUCCEEDED`，则后端会在 Step 3.7 中将状态切换为 `ACTIVE` 并返回空队列。

### 6.2 重试 API

`POST /api/v1/worlds/{id}/generation/{moduleKey}/retry`

- 检查对应任务是否为 `FAILED`。
- 将 `status` 更新为 `WAITING`，`attempts++`，清空 `error`。
- 若世界当前状态不是 `GENERATING`（例如用户先停止后重新触发），需要重新创建 `world_generation_jobs`。

### 6.3 取消（可选）

- 可提供 `POST /api/v1/worlds/{id}/generation/cancel`，将所有 `WAITING`/`RUNNING` 任务标记为 `CANCELLED`，世界状态回到 `DRAFT`。本次范围内可暂不实现。

## 7. 完成与版本更新

- 当任务处理器检测到 `world_generation_jobs` 中的任务全部 `SUCCEEDED`：
  1. 更新世界 `status = ACTIVE`。
  2. `version++`，`published_at = now()`。
  3. 对每个模块 `status = COMPLETED`，并将 `contentHash` 与 `full_content` 对齐（可额外存储 `generatedHash = contentHash`）。
  4. 若存在 `WorldGenerationRun`，标记 `SUCCEEDED`。
- 若存在失败任务但用户未重试，世界维持 `GENERATING`，前端继续提示。

## 8. 与 AI 服务的集成细节

- 新建 `WorldPromptTemplateService`（Step 5 会扩展配置功能）：
  - `renderDraftTemplate(moduleKey, context)` → `world.module.${moduleKey}.draft`。
  - `renderFinalTemplate(moduleKey, context)` → `world.module.${moduleKey}.final`。
  - `renderFieldRefineTemplate(moduleKey, fieldKey, context)` → `world.field.refine` + `focusNote`。
- 使用现有 `AiService`（OpenAI/兼容接口），设置：
  - 温度：`0.6`（可配置）。
  - 输出最大 token：根据模块长度设定（建议 800-1200 token）。
  - JSON 解析：优先使用模型的 `response_format=json_schema`（若支持），否则使用正则提取 + Jackson 验证。
- 错误分类：`AiServiceException`、`PromptTemplateException`、`JsonParseException`，分别记录到 `last_error`。

## 9. 日志与监控

- 记录每次 AI 调用耗时、token 消耗、错误类型，写入应用日志或 `world_generation_jobs`。
- 为调度器添加指标：当前排队任务数、平均处理时长、失败率。
- 可在后续迭代结合 Prometheus/Grafana 监控。

## 10. 测试用例

1. **模块自动生成**：模拟 OpenAI 返回 JSON，验证字段写入、状态更新、hash 更新。
2. **字段优化**：校验 `focusNote` 注入、返回结果写入。
3. **发布流程**：
   - 全部模块 READY，创建任务 → 队列执行 → 世界状态切换。
   - 部分模块缺失 → 返回 `missingFields`。
   - 修改已发布世界一个字段 → 仅对应模块入队。
4. **失败重试**：模拟 AI 超时，任务标记 `FAILED`，调用 `/retry` 后重新执行成功。
5. **断点续传**：在任务 RUNNING 时模拟服务重启，确保任务回到 WAITING 并重新执行。

完成本阶段后，世界构建模块具备完整的 AI 功能与可靠的生成流程，可进入 Step 4 的前端界面实现。

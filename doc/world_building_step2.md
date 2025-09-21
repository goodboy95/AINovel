# 世界构建 Step 2 —— 数据模型与后端基础能力

## 1. 目标

在完成 Step 1 的信息架构后，本阶段负责将“世界构建”抽象为可持久化、可访问的后端能力。核心目标：

1. 建立数据库表结构与领域模型，支持世界、模块、字段内容的存储与状态管理。
2. 实现世界草稿/正式世界的 CRUD、模块内容读写、基础校验与状态流转。
3. 提供 REST API（含 DTO）供前端调用，覆盖世界列表、详情、保存草稿、模块更新、删除草稿等场景。
4. 搭建模块定义注册器，保障前后端字段一致、校验安全，并为后续 AI 生成提供元数据。
5. 预留正式创建流程入口，返回待生成的模块清单（实际生成在 Step 3 完成）。

## 2. 数据模型设计

### 2.1 表结构

#### 2.1.1 `worlds`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT PK | 世界唯一标识，自增。 |
| `user_id` | BIGINT | 所属用户，外键 `users(id)`。 |
| `name` | VARCHAR(80) | 世界名称。 |
| `tagline` | VARCHAR(180) | 一句话概述。 |
| `themes` | JSON | 主题标签数组（字符串）。 |
| `creative_intent` | TEXT | 创作意图。 |
| `notes` | TEXT | 开发者备注，可空。 |
| `status` | ENUM | `DRAFT` / `GENERATING` / `ACTIVE` / `ARCHIVED`。 |
| `version` | INT | 每次正式创建完成后自增，用于缓存刷新。初始为 0。 |
| `published_at` | DATETIME | 最近一次正式创建完成时间，可空。 |
| `created_at` | DATETIME | 创建时间。 |
| `updated_at` | DATETIME | 更新时间。 |
| `deleted_at` | DATETIME | 软删除时间（保留历史，前端默认过滤）。 |

索引：
- `idx_worlds_user_status` (`user_id`, `status`)：快速筛选草稿/已发布世界。
- `idx_worlds_status_updated` (`status`, `updated_at`)：世界选择器按状态 + 最近更新时间排序。

#### 2.1.2 `world_modules`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT PK | 模块记录主键。 |
| `world_id` | BIGINT | 关联世界。 |
| `module_key` | VARCHAR(32) | 模块标识（如 `cosmos`、`geography`）。唯一约束 `(world_id, module_key)`。 |
| `fields` | JSON | 字段内容，key 为 Step 1 定义的字段 key，值为字符串。 |
| `status` | ENUM | `EMPTY` / `IN_PROGRESS` / `READY` / `AWAITING_GENERATION` / `GENERATING` / `COMPLETED` / `FAILED`。初始 `EMPTY`。 |
| `content_hash` | CHAR(64) | 将 `fields` 序列化后的 SHA-256，用于检测内容变更。 |
| `full_content` | LONGTEXT | 最近一次正式创建生成的完整信息（Step 3 填充）。 |
| `full_content_updated_at` | DATETIME | 完整信息更新时间。 |
| `created_at` | DATETIME | 创建时间。 |
| `updated_at` | DATETIME | 更新时间。 |

索引：
- `idx_world_modules_world` (`world_id`).
- `idx_world_modules_status` (`status`).

#### 2.1.3 `world_module_change_log`（可选，建议保留以便审计）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT PK |
| `world_module_id` | BIGINT |
| `changed_fields` | JSON | 本次修改的字段列表与旧值摘要。 |
| `change_type` | ENUM | `USER_EDIT` / `AUTO_GENERATE` / `RESET` / `IMPORT`。 |
| `changed_by` | BIGINT | 用户 ID。 |
| `created_at` | DATETIME | |

> 若当前迭代希望节省时间，可仅预留表结构或推迟实现；至少在代码中记录 `lastEditedBy`, `lastEditedAt` 以便前端提示。

### 2.2 枚举与常量

- `WorldStatus`: `DRAFT`, `GENERATING`, `ACTIVE`, `ARCHIVED`。
- `WorldModuleStatus`: `EMPTY`, `IN_PROGRESS`, `READY`, `AWAITING_GENERATION`, `GENERATING`, `COMPLETED`, `FAILED`。
- `WorldModuleKey`: `cosmos`, `geography`, `society`, `history`, `culture`, `economy`, `factions`。

### 2.3 模块定义注册器

新增 `WorldModuleDefinition` 类，用于描述模块的字段元数据：

```java
public record WorldModuleDefinition(
    String key,
    String label,
    List<FieldDefinition> fields,
    int order
) {
    public record FieldDefinition(
        String key,
        String label,
        boolean required,
        int minLength,
        int maxLength
    ) {}
}
```

实现 `WorldModuleDefinitionRegistry`（单例或 Spring Bean），从 YAML/JSON 资源文件加载 Step 1 的结构，提供：
- `List<WorldModuleDefinition> getAll()` 按 `order` 排序。
- `Optional<WorldModuleDefinition> find(String key)`。
- `FieldDefinition requireField(String moduleKey, String fieldKey)` —— 用于校验请求。

> 字段 Tooltip、focusNote、默认提示词文本仍保存在资源文件中，供 Step 5 使用；本阶段仅需字段结构与校验参数。

## 3. 领域模型与仓储

### 3.1 JPA/实体类

- `WorldEntity` —— 对应 `worlds` 表，包含字段及 `List<WorldModuleEntity> modules`（`@OneToMany`）。
- `WorldModuleEntity` —— 对应 `world_modules`，包含 `fields`（映射为 `Map<String, String>` 或 JSON 字符串）、`status`、`contentHash` 等。

若使用 Spring Data JPA，字段 JSON 可通过 `@Convert`+Jackson 处理；或使用 MyBatis/自定义 DAO 直接映射。

### 3.2 仓储接口

- `WorldRepository`
  - `Optional<WorldEntity> findByIdAndUserId(Long id, Long userId)`
  - `List<WorldSummaryProjection> findAllByUserIdAndStatus(Long userId, WorldStatus status)`
  - `void save(WorldEntity entity)`
- `WorldModuleRepository`
  - `List<WorldModuleEntity> findByWorldId(Long worldId)`
  - `Optional<WorldModuleEntity> findByWorldIdAndModuleKey(Long worldId, String key)`
  - `void saveAll(Collection<WorldModuleEntity> modules)`

> 在 `WorldEntity` 上添加乐观锁（`@Version`）可防止并发写冲突。

## 4. 服务层设计

### 4.1 `WorldService`

职责：
- 新建世界草稿：创建 `WorldEntity` + 7 个 `WorldModuleEntity`（status=EMPTY，fields=空 map）。
- 更新顶栏基础信息：校验长度、敏感词（如需），更新世界状态为 `DRAFT`，刷新 `updatedAt`。
- 删除草稿：仅允许 `status == DRAFT` 的世界软删除。
- 获取世界详情：返回世界 + 模块字段，包含状态、contentHash、fullContent（如存在）。
- 检查是否拥有权：`userId` 与 `WorldEntity.userId` 一致。

### 4.2 `WorldModuleService`

职责：
- 更新模块字段（草稿保存）：
  1. 读取模块定义，校验字段 key、必填项（草稿阶段允许为空但记录状态）。
  2. 对于有值的字段执行长度校验、去除前后空白。
  3. 计算新 `contentHash`，若与旧值不同则将 `status` 更新为：
     - 草稿场景：`IN_PROGRESS`（有任意字段填写但未全部完成）。
     - 所有字段均非空：`READY`。
     - 若世界先前为 `ACTIVE`，且内容被修改，则额外将状态标记为 `AWAITING_GENERATION` 并设置 `world.status = DRAFT_DIRTY`（即 `DRAFT` 但保留 `version`，可直接用 `status = DRAFT`+`version > 0` 表示）。
  4. 更新 `WorldEntity.updatedAt`。
- 批量保存多个模块（草稿保存按钮一次提交多个模块）。

### 4.3 `WorldPublicationService`（预留）

- `PublicationPreparation preparePublish(Long worldId, Long userId)`：
  - 校验所有模块 `status == READY` 或 `COMPLETED`。
  - 返回需要生成完整信息的模块列表（初次创建为全部模块；修改后的世界只返回 `AWAITING_GENERATION` 模块）。
  - 将世界状态更新为 `GENERATING`，并将待生成模块的 `status` 更新为 `AWAITING_GENERATION`。
  - Step 3 将基于 `PublicationPreparation` 创建生成任务。

`PublicationPreparation` 示例：

```java
public record PublicationPreparation(
    WorldEntity world,
    List<WorldModuleEntity> modulesToGenerate,
    List<WorldModuleEntity> modulesToReuse
) {}
```

## 5. REST API 设计

### 5.1 路由概览

| 方法 | 路径 | 描述 |
| --- | --- | --- |
| `POST` | `/api/v1/worlds` | 新建世界草稿，返回世界 ID + 初始状态。 |
| `GET` | `/api/v1/worlds` | 按状态查询世界列表（`status=draft/active/generating/all`）。 |
| `GET` | `/api/v1/worlds/{id}` | 获取世界详情（含模块字段与状态）。 |
| `PUT` | `/api/v1/worlds/{id}` | 更新世界基础信息。 |
| `PUT` | `/api/v1/worlds/{id}/modules/{moduleKey}` | 更新单个模块字段。 |
| `PUT` | `/api/v1/worlds/{id}/modules` | 批量更新多个模块字段（前端“保存草稿”提交）。 |
| `DELETE` | `/api/v1/worlds/{id}` | 删除草稿世界。 |
| `POST` | `/api/v1/worlds/{id}/publish` | 触发正式创建（Step 3 继续实现生成）。返回待生成模块列表。 |
| `GET` | `/api/v1/worlds/{id}/publish/preview` | 返回缺失字段清单、模块完成度，供前端在弹窗中提示。 |

> `publish/preview` 端点可复用 `WorldPublicationService` 的校验逻辑，在正式创建前先校验一次，返回 `missingFields` 列表。

### 5.2 DTO 结构

#### 5.2.1 世界列表（Summary）

```json
{
  "id": 12,
  "name": "永昼群星",
  "tagline": "白昼永不落的多层天空都市",
  "themes": ["高幻想", "阴谋"],
  "status": "DRAFT",
  "version": 0,
  "updatedAt": "2025-01-20T10:35:00",
  "moduleProgress": {
    "cosmos": "READY",
    "geography": "IN_PROGRESS"
  }
}
```

#### 5.2.2 世界详情

```json
{
  "world": {
    "id": 12,
    "name": "永昼群星",
    "tagline": "白昼永不落的多层天空都市",
    "themes": ["高幻想", "阴谋"],
    "creativeIntent": "……",
    "notes": "……",
    "status": "DRAFT",
    "version": 0,
    "publishedAt": null
  },
  "modules": [
    {
      "key": "cosmos",
      "label": "宇宙观与法则",
      "status": "READY",
      "fields": {
        "cosmos_structure": "……",
        "space_time": "……",
        "energy_system": "……",
        "technology_magic_balance": "……",
        "constraints": "……"
      },
      "contentHash": "a6f...",
      "fullContent": null,
      "fullContentUpdatedAt": null
    },
    {
      "key": "geography",
      "label": "地理与生态",
      "status": "IN_PROGRESS",
      "fields": { ... }
    }
  ]
}
```

#### 5.2.3 模块更新请求

```json
{
  "fields": {
    "cosmos_structure": "……",
    "space_time": "……"
  }
}
```

> 服务端根据模块定义过滤未知字段，忽略 `null` 值（保持原值），但允许显式传 `""` 清空内容。

#### 5.2.4 正式创建请求与响应

- 请求体：暂不需要额外参数，默认使用最新草稿内容。
- 响应：

```json
{
  "worldId": 12,
  "modulesToGenerate": [
    {"key": "cosmos", "label": "宇宙观与法则"},
    {"key": "geography", "label": "地理与生态"}
  ],
  "modulesToReuse": [
    {"key": "history", "label": "历史与传说"}
  ]
}
```

Step 3 将使用该响应创建生成队列。

### 5.3 错误码建议

| 状态码 | 场景 |
| --- | --- |
| 400 | 未知模块/字段、字段长度不合法、草稿未填写完整就触发发布。 |
| 403 | 用户访问非本人世界。 |
| 404 | 世界或模块不存在。 |
| 409 | 世界状态不允许当前操作（例如 `GENERATING` 时不允许更新字段）。 |

## 6. 业务规则与校验

1. **权限**：所有世界 API 需校验 `userId`，避免跨用户访问。
2. **状态转换**：
   - `NEW` -> `DRAFT`（创建世界后立即为 DRAFT）。
   - `DRAFT` -> `GENERATING`（调用 `/publish` 时）。
   - `GENERATING` -> `ACTIVE`（Step 3 完成全部任务时）。
   - `ACTIVE` -> `DRAFT`（当用户再次编辑任一模块，模块状态变更为 `AWAITING_GENERATION`）。
   - `ACTIVE` -> `ARCHIVED`（预留，未来实现）。
3. **模块状态**：
   - 当字段全部为空时为 `EMPTY`。
   - 部分字段有值但未全部完成为 `IN_PROGRESS`。
   - 全部字段非空时为 `READY`。
   - 如果世界 `version > 0` 且用户修改字段，状态转为 `AWAITING_GENERATION`，以提醒需要重新生成完整信息。
4. **内容哈希**：保存模块时计算 `contentHash`，用于 Step 3 判断是否需要重新生成。
5. **删除草稿**：仅允许 `status == DRAFT` 且 `version == 0` 的世界删除；若世界已发布（`version > 0`），建议提供“归档”功能（后续迭代）。
6. **并发**：为避免并发覆盖，模块更新接口接受 `If-Match`（ETag）或 `lastModified` 参数；或依赖 `@Version` 乐观锁，如果更新失败返回 409。

## 7. 与后续步骤的接口

- Step 3 将基于 `WorldModuleDefinitionRegistry` 与 `/publish` 响应创建生成任务，需要 `fields` 原文与 `contentHash`。
- Step 4 需要 `/worlds`, `/worlds/{id}`, `/worlds/{id}/modules` 接口提供的数据；前端将根据 `status` 与 `contentHash` 控制 UI 提示。
- Step 5 的提示词配置将读取模块定义（key、label）生成配置表单。
- Step 6 的工作台集成将依赖 `status == ACTIVE` 世界列表与 `fullContent` 字段。

## 8. 开发与测试建议

1. **迁移脚本**：使用 Flyway/Liquibase 添加上述表结构及索引；若暂不创建 `world_module_change_log`，至少保留 SQL 草案。
2. **单元测试**：
   - `WorldModuleService` 字段校验、状态变更、contentHash 更新。
   - `WorldPublicationService` 校验缺失字段、识别需生成模块。
3. **集成测试**：模拟用户创建世界、填写模块、保存草稿、再次加载、触发发布的完整流程。
4. **数据初始化**：创建模块定义资源文件（YAML/JSON），在应用启动时加载，确保字段 key 与 Step 1 对齐。

完成本阶段后，系统已具备可靠的数据支撑，可在 Step 3 中接入 AI 生成与队列处理。

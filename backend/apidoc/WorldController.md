# WorldController 接口文档

基础路径：`/api/v1/worlds`

## POST /api/v1/worlds
- **认证**：需要登录。
- **功能**：创建世界及模块初始数据。
- **请求体**：`WorldUpsertRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `name` | String | 世界名称。 |
  | `tagline` | String | 宣传语。 |
  | `themes` | List<String> | 主题标签。 |
  | `creativeIntent` | String | 创作意图。 |
  | `notes` | String | 备注。 |
- **响应**：`201 Created`，`WorldDetailResponse`（包含世界基础信息与模块列表）。
- **主要逻辑**：`WorldService.createWorld(user, request)` 并通过 `WorldDtoMapper` 组装响应。

## GET /api/v1/worlds
- **认证**：需要登录。
- **功能**：按状态筛选世界列表。
- **查询参数**：`status` 可选，支持 `draft/published` 等，`all` 或为空表示不过滤。
- **响应**：`200 OK`，`List<WorldSummaryResponse>`（含世界状态、版本、模块进度）。
- **主要逻辑**：`WorldService.listWorlds(userId, filter)`。

## GET /api/v1/worlds/{worldId}
- **认证**：需要登录。
- **功能**：获取世界详情及模块概要。
- **响应**：`200 OK`，`WorldDetailResponse`。
- **主要逻辑**：`WorldService.getWorld(worldId, userId)`。

## GET /api/v1/worlds/{worldId}/full
- **认证**：需要登录。
- **功能**：获取已发布版本的完整模块文本。
- **响应**：`200 OK`，`WorldFullResponse`（含 `world` 信息与模块 `fullContent`/`excerpt`）。
- **主要逻辑**：`WorldService.getPublishedWorldWithModules`。

## PUT /api/v1/worlds/{worldId}
- **认证**：需要登录。
- **功能**：更新世界基础信息。
- **请求体**：`WorldUpsertRequest`。
- **响应**：`200 OK`，`WorldDetailResponse`。
- **主要逻辑**：`WorldService.updateWorld(worldId, userId, request)`。

## DELETE /api/v1/worlds/{worldId}
- **认证**：需要登录。
- **功能**：删除世界。
- **响应**：`204 No Content`。
- **主要逻辑**：`WorldService.deleteWorld(worldId, userId)`。

## PUT /api/v1/worlds/{worldId}/modules/{moduleKey}
- **认证**：需要登录。
- **功能**：更新单个模块字段。
- **请求体**：`WorldModuleUpdateRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `fields` | Map<String, String> | 字段键值对。 |
- **响应**：`200 OK`，`WorldModuleResponse`（包含模块状态、字段内容、全文等）。
- **主要逻辑**：`WorldModuleService.updateModule(worldId, userId, moduleKey, request)`。

## PUT /api/v1/worlds/{worldId}/modules
- **认证**：需要登录。
- **功能**：批量更新多个模块。
- **请求体**：`WorldModulesBatchUpdateRequest`
  - `modules`：数组，每项 `ModuleUpdate` 包含 `key` 与 `fields`。
- **响应**：`200 OK`，`List<WorldModuleResponse>`。
- **主要逻辑**：`WorldModuleService.updateModules(worldId, userId, request)`。

## POST /api/v1/worlds/{worldId}/modules/{moduleKey}/generate
- **认证**：需要登录。
- **功能**：调用 AI 生成指定模块内容。
- **响应**：`200 OK`，`WorldModuleResponse`。
- **主要逻辑**：`WorldModuleGenerationService.generateModule(worldId, moduleKey, userId)`。

## POST /api/v1/worlds/{worldId}/modules/{moduleKey}/fields/{fieldKey}/refine
- **认证**：需要登录。
- **功能**：对模块内单个字段进行润色。
- **请求体**：`WorldFieldRefineRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `text` | String | 原文。 |
  | `instruction` | String | 优化指令。 |
- **响应**：`200 OK`，`WorldFieldRefineResponse`（`result` 为优化结果）。
- **主要逻辑**：`WorldFieldRefineService.refineField(worldId, moduleKey, fieldKey, userId, request)`。

## GET /api/v1/worlds/{worldId}/publish/preview
- **认证**：需要登录。
- **功能**：检查发布所需字段是否齐全。
- **响应**：`200 OK`，`WorldPublishPreviewResponse`
  - `ready`：是否可以发布。
  - `moduleStatuses`：各模块状态。
  - `missingFields`：缺失字段列表。
  - `modulesToGenerate` / `modulesToReuse`：发布时需处理的模块概览。
- **主要逻辑**：`WorldPublicationService.preview(worldId, userId)`。

## POST /api/v1/worlds/{worldId}/publish
- **认证**：需要登录。
- **功能**：准备发布，返回需要生成/复用的模块计划。
- **响应**：`200 OK`，`WorldPublishResponse`。
- **主要逻辑**：`WorldPublicationService.preparePublish(worldId, userId)`。

## GET /api/v1/worlds/{worldId}/generation
- **认证**：需要登录。
- **功能**：查询世界模块生成任务进度。
- **响应**：`200 OK`，`WorldGenerationStatusResponse`（包含队列中各模块的状态、尝试次数、时间戳等）。
- **主要逻辑**：`WorldGenerationWorkflowService.getStatus(worldId, userId)`。

## POST /api/v1/worlds/{worldId}/generation/{moduleKey}
- **认证**：需要登录。
- **功能**：触发生成队列中的指定模块。
- **响应**：`200 OK`，更新后的 `WorldGenerationStatusResponse`。
- **主要逻辑**：`WorldGenerationWorkflowService.generateModule(worldId, moduleKey, userId)`。

## POST /api/v1/worlds/{worldId}/generation/{moduleKey}/retry
- **认证**：需要登录。
- **功能**：对失败模块重新排队生成。
- **响应**：`204 No Content`。
- **主要逻辑**：`WorldGenerationWorkflowService.retryModule(worldId, moduleKey, userId)`。

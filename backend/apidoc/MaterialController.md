# MaterialController 接口文档

基础路径：`/api/v1/materials`

## POST /api/v1/materials
- **认证**：需要登录。
- **功能**：手动创建素材。
- **请求体**：`MaterialCreateRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `title` | String | 素材标题。 |
  | `type` | String | 素材类型（如角色、地点等）。 |
  | `summary` | String | 摘要。 |
  | `content` | String | 具体内容。 |
  | `tags` | String | 标签，通常为逗号分隔字符串。 |
- **响应**：
  - 成功：`201 Created`，返回 `MaterialResponse`（含 `id/workspaceId/title/type/summary/tags/status/entitiesJson/reviewNotes/createdAt/updatedAt`）。
  - 参数错误：`400 Bad Request`，包含 `message`。
- **主要逻辑**：使用用户 ID 同时作为 workspace ID 创建素材，捕获非法参数。

## GET /api/v1/materials
- **认证**：需要登录。
- **功能**：列出当前工作空间的全部素材。
- **响应**：`200 OK`，`List<MaterialResponse>`。
- **主要逻辑**：`MaterialService.listMaterials(workspaceId)`。

## GET /api/v1/materials/{materialId}
- **认证**：需要登录。
- **功能**：查看素材详情（含正文）。
- **路径参数**：`materialId` 素材 ID。
- **响应**：
  - 成功：`200 OK`，`MaterialDetailResponse`（在 `MaterialResponse` 基础上新增 `content`）。
  - 不存在：`404 Not Found`，返回 `message`。
- **主要逻辑**：`MaterialService.getMaterialDetail(materialId, workspaceId)`。

## PUT /api/v1/materials/{materialId}
- **认证**：需要登录。
- **功能**：更新素材属性与内容。
- **路径参数**：`materialId`。
- **请求体**：`MaterialUpdateRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `title` | String | 标题。 |
  | `type` | String | 类型。 |
  | `summary` | String | 摘要。 |
  | `tags` | String | 标签。 |
  | `content` | String | 正文。 |
  | `status` | String | 状态枚举，例如 `draft`/`published`。 |
  | `entitiesJson` | String | 结构化实体 JSON。 |
- **响应**：
  - 成功：`200 OK`，返回更新后的 `MaterialDetailResponse`。
  - 业务异常：`400 Bad Request`，包含 `message`。
- **主要逻辑**：`MaterialService.updateMaterial(materialId, workspaceId, userId, request)`。

## DELETE /api/v1/materials/{materialId}
- **认证**：需要登录。
- **功能**：删除素材。
- **响应**：
  - 成功：`204 No Content`。
  - 未找到：`404 Not Found`，包含 `message`。
- **主要逻辑**：`MaterialService.deleteMaterial(materialId, workspaceId, userId)`。

## POST /api/v1/materials/search
- **认证**：需要登录。
- **功能**：关键词检索素材。
- **请求体**：`MaterialSearchRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `query` | String | 搜索关键字。 |
  | `limit` | Integer | 期望条数，后端会限制在 1-20。 |
- **响应**：`200 OK`，返回搜索结果列表（`MaterialResponse` 的精简视图）。
- **主要逻辑**：根据 limit 取最小值，调用 `MaterialService.searchMaterials`。

## POST /api/v1/materials/find-duplicates
- **认证**：需要登录。
- **功能**：检测可能重复的素材。
- **响应**：`200 OK`，返回 `List<MaterialDuplicateCandidate>`（含相似度、重叠片段信息）。
- **主要逻辑**：`DeduplicationService.findDuplicates(workspaceId)`。

## POST /api/v1/materials/merge
- **认证**：需要登录。
- **功能**：合并两个素材。
- **请求体**：`MaterialMergeRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `sourceMaterialId` | Long | 来源素材 ID。 |
  | `targetMaterialId` | Long | 目标素材 ID。 |
  | `mergeTags` | boolean | 是否合并标签。 |
  | `mergeSummaryWhenEmpty` | boolean | 目标摘要为空时是否合并。 |
  | `note` | String | 备注。 |
- **响应**：
  - 成功：`200 OK`，返回合并后的 `MaterialResponse`。
  - 业务异常：`400 Bad Request`，包含 `message`。
- **主要逻辑**：`DeduplicationService.mergeMaterials(workspaceId, userId, request)`。

## POST /api/v1/materials/upload
- **认证**：需要登录。
- **功能**：上传文件触发素材导入作业。
- **请求参数**：`file` (MultipartFile)。
- **响应**：
  - 成功：`202 Accepted`，返回 `FileImportJobResponse`（含作业 ID、状态、错误信息等）。
  - 参数错误：`400 Bad Request`，包含 `message`。
  - IO 异常：`500 Internal Server Error`，包含 `message` 和 `detail`。
- **主要逻辑**：`FileImportService.startFileImportJob`。

## GET /api/v1/materials/upload/{jobId}
- **认证**：需要登录。
- **功能**：查询文件导入作业状态。
- **路径参数**：`jobId`。
- **响应**：
  - 成功：`200 OK`，`FileImportJobResponse`。
  - 未找到：`404 Not Found`，`message`。
- **主要逻辑**：`FileImportService.getJobStatus(jobId, workspaceId)`。

## GET /api/v1/materials/review/pending
- **认证**：需要登录。
- **功能**：列出待审核素材。
- **响应**：`200 OK`，返回 `List<MaterialReviewItem>`。
- **主要逻辑**：`MaterialService.listPendingReview(workspaceId)`。

## POST /api/v1/materials/{materialId}/review/approve
- **认证**：需要登录。
- **功能**：审核通过素材，可选更新信息。
- **路径参数**：`materialId`。
- **请求体**（可选）：`MaterialReviewDecisionRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `title` | String | 更新后的标题。 |
  | `summary` | String | 更新后的摘要。 |
  | `tags` | String | 标签。 |
  | `entitiesJson` | String | 实体 JSON。 |
  | `type` | String | 类型。 |
  | `reviewNotes` | String | 审核备注。 |
- **响应**：
  - 成功：`200 OK`，返回 `MaterialReviewItem`。
  - 失败：`400 Bad Request`，`message`。
- **主要逻辑**：`MaterialService.approveMaterial(materialId, workspaceId, userId, request)`。

## POST /api/v1/materials/{materialId}/review/reject
- **认证**：需要登录。
- **功能**：驳回素材，同步记录备注。
- **请求体**：可选 `MaterialReviewDecisionRequest`（字段同上）。
- **响应**：同通过接口。
- **主要逻辑**：`MaterialService.rejectMaterial(materialId, workspaceId, userId, request)`。

## POST /api/v1/materials/editor/auto-hints
- **认证**：需要登录。
- **功能**：根据编辑器文本自动推荐素材引用。
- **请求体**：`EditorContextDto`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `text` | String | 当前编辑器文本片段。 |
  | `workspaceId` | Long | 可选，指定工作空间，缺省使用用户 ID。 |
  | `limit` | Integer | 可选，结果条数上限（1-15，默认 6）。 |
- **响应**：`200 OK`，返回匹配的素材列表；若文本为空返回空数组。
- **主要逻辑**：预处理文本后调用 `MaterialService.searchMaterials`。

## GET /api/v1/materials/{materialId}/citations
- **认证**：需要登录。
- **功能**：查看素材被引用情况。
- **路径参数**：`materialId`。
- **响应**：
  - 成功：`200 OK`，`List<MaterialCitationDto>`（含引用文档信息、使用场景、时间）。
  - 失败：`400 Bad Request`，`message`。
- **主要逻辑**：`MaterialAuditQueryService.listCitations(workspaceId, materialId)`。

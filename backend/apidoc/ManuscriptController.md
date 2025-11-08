# ManuscriptController 接口文档

基础路径：`/api/v1`

## GET /api/v1/manuscript/outlines/{outlineId}
- **认证**：需要登录。
- **功能**：按旧版兼容接口获取某大纲下各场景的激活手稿段落。
- **路径参数**：`outlineId` 大纲 ID。
- **响应**：`200 OK`，返回 `Map<Long, ManuscriptSection>`，键为场景 ID，值为段落实体（含 `id/sceneId/content/version/isActive/createdAt`）。
- **主要逻辑**：`ManuscriptService.getManuscriptForOutline(outlineId, userId)`。

## POST /api/v1/manuscript/scenes/{sceneId}/generate
- **认证**：需要登录。
- **功能**：为指定场景生成手稿内容并写入最新手稿。
- **路径参数**：`sceneId` 场景 ID。
- **请求体**：可选 `GenerateManuscriptSectionRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `worldId` | Long | 可选，指定生成时使用的世界设定。 |
- **响应**：`200 OK`，返回生成的 `ManuscriptSection`。
- **主要逻辑**：`ManuscriptService.generateSceneContent(sceneId, userId, worldId)`。

## PUT /api/v1/manuscript/sections/{sectionId}
- **认证**：需要登录。
- **功能**：更新手稿段落正文。
- **路径参数**：`sectionId` 手稿段落 ID。
- **请求体**：`UpdateSectionRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `content` | String | 新的段落内容。 |
- **响应**：`200 OK`，返回更新后的 `ManuscriptSection`。
- **主要逻辑**：`ManuscriptService.updateSectionContent(sectionId, content, userId)`。

## POST /api/v1/manuscripts/{manuscriptId}/sections/analyze-character-changes
- **认证**：需要登录。
- **功能**：分析并保存某段落涉及的角色变化。
- **路径参数**：`manuscriptId` 手稿 ID。
- **请求体**：`AnalyzeCharacterChangesRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `chapterNumber` | Integer | 所在章编号。 |
  | `sectionNumber` | Integer | 段落编号。 |
  | `sectionContent` | String | 待分析内容。 |
  | `characterIds` | List<Long> | 涉及的角色 ID 列表。 |
- **响应**：`200 OK`，`List<CharacterChangeLogDto>`，包含角色变化、关系变化、自动复制标识等。
- **主要逻辑**：`ManuscriptService.analyzeCharacterChanges(manuscriptId, request, userId)`。

## GET /api/v1/manuscripts/{manuscriptId}/character-change-logs
- **认证**：需要登录。
- **功能**：列出某手稿下全部角色变更日志。
- **路径参数**：`manuscriptId` 手稿 ID。
- **响应**：`200 OK`，`List<CharacterChangeLogDto>`。
- **主要逻辑**：`ManuscriptService.getCharacterChangeLogs(manuscriptId, userId)`。

## GET /api/v1/manuscripts/{manuscriptId}/character-change-logs/{characterId}
- **认证**：需要登录。
- **功能**：查看指定角色的变更日志。
- **路径参数**：`manuscriptId` 手稿 ID；`characterId` 角色 ID。
- **响应**：`200 OK`，`List<CharacterChangeLogDto>`。
- **主要逻辑**：`ManuscriptService.getCharacterChangeLogsForCharacter(manuscriptId, characterId, userId)`。

## GET /api/v1/outlines/{outlineId}/manuscripts
- **认证**：需要登录。
- **功能**：列出某大纲下的手稿概览。
- **路径参数**：`outlineId` 大纲 ID。
- **响应**：`200 OK`，返回 `List<ManuscriptDto>`（含 `id/title/outlineId/worldId/createdAt/updatedAt`）。
- **主要逻辑**：`ManuscriptService.getManuscriptsForOutline(outlineId, userId)`。

## POST /api/v1/outlines/{outlineId}/manuscripts
- **认证**：需要登录。
- **功能**：在大纲下创建新手稿。
- **路径参数**：`outlineId` 大纲 ID。
- **请求体**：`ManuscriptDto`（至少包含 `title`，可带 `worldId`）。
- **响应**：`200 OK`，返回创建后的 `ManuscriptDto`。
- **主要逻辑**：`ManuscriptService.createManuscript(outlineId, dto, userId)`。

## GET /api/v1/manuscripts/{manuscriptId}
- **认证**：需要登录。
- **功能**：获取手稿详情及激活段落映射。
- **路径参数**：`manuscriptId` 手稿 ID。
- **响应**：`200 OK`，`ManuscriptWithSectionsDto`
  - `manuscript`：`ManuscriptDto`
  - `sections`：`Map<Long, ManuscriptSection>`。
- **主要逻辑**：`ManuscriptService.getManuscriptWithSections(manuscriptId, userId)`。

## DELETE /api/v1/manuscripts/{manuscriptId}
- **认证**：需要登录。
- **功能**：删除手稿及其段落。
- **路径参数**：`manuscriptId` 手稿 ID。
- **响应**：`204 No Content`。
- **主要逻辑**：`ManuscriptService.deleteManuscript(manuscriptId, userId)`。

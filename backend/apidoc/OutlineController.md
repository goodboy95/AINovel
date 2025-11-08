# OutlineController 接口文档

基础路径：`/api/v1`

## POST /api/v1/outlines *(已废弃)*
- **认证**：需要登录。
- **功能**：旧版生成整套大纲接口，现已返回 410。
- **响应**：`410 Gone`。

## POST /api/v1/outlines/{outlineId}/chapters
- **认证**：需要登录。
- **功能**：为现有大纲生成单章结构。
- **路径参数**：`outlineId` 大纲 ID。
- **请求体**：`GenerateChapterRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `chapterNumber` | int | 章节序号。 |
  | `sectionsPerChapter` | int | 期望场景数。 |
  | `wordsPerSection` | int | 每场景目标字数。 |
  | `worldId` | Long | 可选，覆盖默认世界设定。 |
- **响应**：`200 OK`，`ChapterDto`（含 `id/chapterNumber/title/synopsis/scenes/settings`）。
- **主要逻辑**：`OutlineService.generateChapterOutline`。

## GET /api/v1/outlines/{id}
- **认证**：需要登录。
- **功能**：获取指定大纲详情。
- **路径参数**：`id` 大纲 ID。
- **响应**：`200 OK`，`OutlineDto`（含 `id/title/pointOfView/chapters/worldId/createdAt`）。
- **主要逻辑**：`OutlineService.getOutlineById(id, userId)`。

## GET /api/v1/story-cards/{storyCardId}/outlines
- **认证**：需要登录。
- **功能**：根据故事卡列出关联大纲。
- **路径参数**：`storyCardId`。
- **响应**：`200 OK`，`List<OutlineDto>`。
- **主要逻辑**：`OutlineService.getOutlinesByStoryCardId(storyCardId, userId)`。

## GET /api/v1/stories/{storyId}/outlines
- **认证**：需要登录。
- **功能**：同上，提供 `/stories` 别名。
- **响应**：`200 OK`，`List<OutlineDto>`。

## POST /api/v1/story-cards/{storyCardId}/outlines
- **认证**：需要登录。
- **功能**：为故事创建空白大纲。
- **路径参数**：`storyCardId`。
- **响应**：`200 OK`，`OutlineDto`。
- **主要逻辑**：`OutlineService.createEmptyOutline(storyCardId, userId)`。

## PUT /api/v1/outlines/{id}
- **认证**：需要登录。
- **功能**：完整更新大纲。
- **路径参数**：`id` 大纲 ID。
- **请求体**：`OutlineDto`（包括章节/场景树）。
- **响应**：`200 OK`，更新后的 `OutlineDto`。
- **主要逻辑**：`OutlineService.updateOutline(id, dto, userId)`。

## DELETE /api/v1/outlines/{id}
- **认证**：需要登录。
- **功能**：删除大纲。
- **响应**：`204 No Content`。
- **主要逻辑**：`OutlineService.deleteOutline(id, userId)`。

## POST /api/v1/outlines/scenes/{id}/refine
- **认证**：需要登录。
- **功能**：对场景梗概进行 AI 优化。
- **路径参数**：`id` 场景 ID。
- **请求体**：`RefineRequest`（`text/instruction/contextType`）。
- **响应**：`200 OK`，`RefineResponse`（`refinedText`）。
- **主要逻辑**：`OutlineService.refineScene(id, request, user)`。

## POST /api/v1/ai/refine-text
- **认证**：需要登录。
- **功能**：通用文本润色。
- **请求体**：`RefineRequest`。
- **响应**：`200 OK`，`RefineResponse`。
- **主要逻辑**：`OutlineService.refineGenericText(request, user)`。

## PATCH /api/v1/chapters/{id}
- **认证**：需要登录。
- **功能**：局部更新章节信息。
- **路径参数**：`id` 章节 ID。
- **请求体**：`ChapterDto`（允许部分字段）。
- **响应**：`200 OK`，更新后的 `ChapterDto`。
- **主要逻辑**：`OutlineService.updateChapter(id, dto, userId)`。

## PATCH /api/v1/scenes/{id}
- **认证**：需要登录。
- **功能**：局部更新场景信息。
- **路径参数**：`id` 场景 ID。
- **请求体**：`SceneDto`（可更新 `synopsis/expectedWords/presentCharacterIds/presentCharacters/sceneCharacters/temporaryCharacters` 等）。
- **响应**：`200 OK`，更新后的 `SceneDto`。
- **主要逻辑**：`OutlineService.updateScene(id, dto, userId)`。

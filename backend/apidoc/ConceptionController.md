# ConceptionController 接口文档

基础路径：`/api/v1`

## POST /api/v1/conception
- **认证**：需要登录。
- **功能**：根据设定生成故事卡及角色卡并保存。
- **请求体**：`ConceptionRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `idea` | String | 故事核心创意或提示。 |
  | `genre` | String | 故事类型。 |
  | `tone` | String | 故事风格。 |
  | `tags` | List<String> | 额外标签。 |
  | `worldId` | Long | 可选，引用发布世界以增强生成。 |
- **响应**：`200 OK`
  - `ConceptionResponse`
    | 字段 | 类型 | 说明 |
    | --- | --- | --- |
    | `storyCard` | StoryCard | 生成并保存的故事卡，含 `id/title/genre/tone/synopsis/storyArc/worldId` 等字段。 |
    | `characterCards` | List<CharacterCard> | 关联角色卡列表，含 `id/name/synopsis/details/relationships/avatarUrl` 等。 |
- **主要逻辑**：调用 `ConceptionService.generateAndSaveStory`，使用登录用户名保存生成结果。

## GET /api/v1/story-cards
- **认证**：需要登录。
- **功能**：获取当前用户的全部故事卡。
- **响应**：`200 OK`，返回 `List<StoryCard>`。
- **主要逻辑**：`ConceptionService.getAllStoryCards(user.getUsername())`。

## GET /api/v1/story-cards/{id}
- **认证**：需要登录。
- **功能**：按 ID 获取故事卡详情。
- **路径参数**：`id` 故事卡 ID。
- **响应**：`200 OK`，返回 `StoryCard`。
- **主要逻辑**：校验归属后调用 `ConceptionService.getStoryCardById(id, userId)`。

## GET /api/v1/story-cards/{storyId}/character-cards
- **认证**：需要登录。
- **功能**：列出指定故事下的全部角色卡。
- **路径参数**：`storyId` 故事卡 ID。
- **响应**：`200 OK`，返回 `List<CharacterCard>`。
- **主要逻辑**：`ConceptionService.getCharacterCardsByStoryId(storyId, userId)`。

## PUT /api/v1/story-cards/{id}
- **认证**：需要登录。
- **功能**：更新故事卡内容。
- **路径参数**：`id` 故事卡 ID。
- **请求体**：`StoryCard`（支持更新 `title/genre/tone/synopsis/storyArc/worldId` 等字段）。
- **响应**：`200 OK`，返回更新后的 `StoryCard`。
- **主要逻辑**：`ConceptionService.updateStoryCard(id, body, userId)`。

## PUT /api/v1/character-cards/{id}
- **认证**：需要登录。
- **功能**：更新角色卡。
- **路径参数**：`id` 角色卡 ID。
- **请求体**：`CharacterCard`（支持更新 `name/synopsis/details/relationships/avatarUrl` 等字段）。
- **响应**：`200 OK`，返回更新后的 `CharacterCard`。
- **主要逻辑**：`ConceptionService.updateCharacterCard(id, body, userId)`。

## POST /api/v1/story-cards/{storyCardId}/characters
- **认证**：需要登录。
- **功能**：为指定故事新增角色卡。
- **路径参数**：`storyCardId` 故事卡 ID。
- **请求体**：`CharacterCard`（至少需要 `name`，可附带简介/关系等）。
- **响应**：`200 OK`，返回新建的 `CharacterCard`。
- **主要逻辑**：`ConceptionService.addCharacterToStory(storyCardId, body, user)`。

## DELETE /api/v1/character-cards/{id}
- **认证**：需要登录。
- **功能**：删除角色卡。
- **路径参数**：`id` 角色卡 ID。
- **响应**：`204 No Content`。
- **主要逻辑**：`ConceptionService.deleteCharacterCard(id, userId)`。

## POST /api/v1/story-cards/{id}/refine
- **认证**：需要登录。
- **功能**：对故事卡的指定字段进行 AI 优化。
- **路径参数**：`id` 故事卡 ID。
- **请求体**：`RefineRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `text` | String | 原文本。 |
  | `instruction` | String | 优化指令。 |
  | `contextType` | String | 文本类型标识。 |
- **响应**：`200 OK`，`RefineResponse`，字段 `refinedText` 为优化结果。
- **主要逻辑**：`ConceptionService.refineStoryCardField(id, request, user)`。

## POST /api/v1/character-cards/{id}/refine
- **认证**：需要登录。
- **功能**：对角色卡字段进行 AI 优化。
- **路径参数**：`id` 角色卡 ID。
- **请求体**：`RefineRequest`（字段同上）。
- **响应**：`200 OK`，`RefineResponse`。
- **主要逻辑**：`ConceptionService.refineCharacterCardField(id, request, user)`。

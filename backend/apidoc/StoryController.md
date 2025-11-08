# StoryController 接口文档

基础路径：`/api/v1/stories`

## POST /api/v1/stories
- **认证**：需要登录。
- **功能**：创建新的故事卡。
- **请求体**：`StoryCardDto`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `title` | String | 故事标题。 |
  | `synopsis` | String | 故事简介。 |
  | `genre` | String | 类型。 |
  | `tone` | String | 风格。 |
  | `worldId` | Long | 可选，关联世界。 |
- **响应**：`201 Created`，返回持久化后的 `StoryCard`（含生成的 `id` 及时间戳等完整字段）。
- **主要逻辑**：`StoryService.createStory(user, dto)`。

## GET /api/v1/stories
- **认证**：需要登录。
- **功能**：列出当前用户的故事卡。
- **响应**：`200 OK`，`List<StoryCardDto>`。
- **主要逻辑**：`StoryService.getUserStories(userId)`。

## DELETE /api/v1/stories/{storyId}
- **认证**：需要登录。
- **功能**：删除指定故事及级联资源。
- **路径参数**：`storyId`。
- **响应**：`204 No Content`。
- **主要逻辑**：`StoryService.deleteStory(storyId, userId)`。

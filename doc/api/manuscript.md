# Manuscript API
- `GET /api/v1/outlines/{outlineId}/manuscripts`：按大纲获取稿件列表，返回 `ManuscriptDto[]`。
- `POST /api/v1/outlines/{outlineId}/manuscripts`：创建稿件，Body `{title, worldId?}`，返回 `ManuscriptDto`。
- `GET /api/v1/manuscripts/{id}`：稿件详情，返回 `ManuscriptDto`。
- `DELETE /api/v1/manuscripts/{id}`：删除稿件，返回 204。
- `POST /api/v1/manuscript/scenes/{sceneId}/generate`：生成指定场景稿件内容，返回 `ManuscriptDto`。
- `PUT /api/v1/manuscript/sections/{sectionId}`：保存章节内容，Body `{content}`，返回 `ManuscriptDto`。
- `POST /api/v1/manuscripts/{id}/sections/analyze-character-changes`：分析角色变化，Body `{chapterNumber?, sectionNumber?, sectionContent, characterIds?}`，返回 `CharacterChangeLogDto[]`。
- `GET /api/v1/manuscripts/{id}/character-change-logs`：角色变化日志列表。
- `GET /api/v1/manuscripts/{id}/character-change-logs/{characterId}`：指定角色的变化日志列表。
- `POST /api/v1/ai/generate-dialogue`：对话生成，Body `{text, instruction?, contextType?}`，返回文本。

## 数据结构
- `ManuscriptDto`：`{id, outlineId, title, worldId, sections, updatedAt}`，其中 `sections` 为 `sceneId -> content` 的映射。
- `CharacterChangeLogDto`：`{id, characterId, summary, createdAt}`。

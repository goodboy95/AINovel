# Settings & Prompts API
- `GET /api/v1/settings`：获取模型接入设置。
- `PUT /api/v1/settings`：更新 `{baseUrl?,modelName?,apiKey?}`。
- `POST /api/v1/settings/test`：测试连接（返回布尔）。

## 工作区提示词
- `GET /api/v1/prompt-templates`：获取故事/大纲/稿件/润色模板。
- `PUT /api/v1/prompt-templates`：更新对应字段。
- `POST /api/v1/prompt-templates/reset`：恢复默认模板。
- `GET /api/v1/prompt-templates/metadata`：帮助页元数据（变量/函数/示例）。

## 世界观提示词
- `GET /api/v1/world-prompts`：获取世界模板（modules/finalTemplates/fieldRefine）。
- `PUT /api/v1/world-prompts`：更新部分字段。
- `POST /api/v1/world-prompts/reset`：恢复默认。
- `GET /api/v1/world-prompts/metadata`：世界提示词帮助元数据。

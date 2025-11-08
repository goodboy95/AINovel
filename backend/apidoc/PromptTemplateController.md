# PromptTemplateController 接口文档

基础路径：`/api/v1/prompt-templates`

## GET /api/v1/prompt-templates
- **认证**：需要登录（若未登录会拿不到个性化模板）。
- **功能**：获取生效的故事/大纲/手稿/润色提示模板。
- **响应**：`200 OK`，`PromptTemplatesResponse`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `storyCreation` | PromptTemplateItemDto | 创建故事模板（含默认值、自定义内容、更新时间等）。 |
  | `outlineChapter` | PromptTemplateItemDto | 生成章节模板。 |
  | `manuscriptSection` | PromptTemplateItemDto | 生成手稿段落模板。 |
  | `refine` | RefinePromptTemplateDto | 润色模板（含带指令与无指令版本）。 |
- **主要逻辑**：`PromptTemplateService.getEffectiveTemplates(userId)`。

## PUT /api/v1/prompt-templates
- **认证**：需要登录。
- **功能**：保存用户自定义模板。
- **请求体**：`PromptTemplatesUpdateRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `storyCreation` | String | 自定义故事模板。 |
  | `outlineChapter` | String | 自定义章节模板。 |
  | `manuscriptSection` | String | 自定义手稿模板。 |
  | `refine` | RefinePromptTemplatesUpdateRequest | 润色模板更新（`withInstruction/withoutInstruction`）。 |
- **响应**：`200 OK`，无 body。
- **主要逻辑**：`PromptTemplateService.saveTemplates(userId, request)`。

## POST /api/v1/prompt-templates/reset
- **认证**：需要登录。
- **功能**：按 key 恢复默认模板。
- **请求体**：`PromptTemplatesResetRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `keys` | List<String> | 需要重置的模板标识，如 `storyCreation`。 |
- **响应**：`200 OK`，无 body。
- **主要逻辑**：`PromptTemplateService.resetTemplates(userId, request)`。

## GET /api/v1/prompt-templates/metadata
- **认证**：无需登录。
- **功能**：获取模板语法、变量、函数说明。
- **响应**：`200 OK`，`PromptTemplateMetadataResponse`
  - `templates`：模板类型元数据。
  - `functions`：可用函数说明。
  - `syntaxTips`：语法提示。
  - `examples`：示例。
- **主要逻辑**：`PromptTemplateService.getMetadata()`。

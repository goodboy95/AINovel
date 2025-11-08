# WorldPromptTemplateController 接口文档

基础路径：`/api/v1/world-prompts`

## GET /api/v1/world-prompts
- **认证**：需要登录（未登录返回公共默认）。
- **功能**：获取世界生成相关的模块模板、终稿模板及字段润色模板。
- **响应**：`200 OK`，`WorldPromptTemplatesResponse`
  - `modules`：Map，键为模块 key，值为 `WorldPromptTemplateItemDto`。
  - `finalTemplates`：Map，终稿模板集合。
  - `fieldRefine`：`WorldPromptTemplateItemDto`，字段润色模板。
- **主要逻辑**：`WorldPromptTemplateService.getEffectiveTemplates(userId)`。

## PUT /api/v1/world-prompts
- **认证**：需要登录。
- **功能**：保存自定义世界模板。
- **请求体**：`WorldPromptTemplatesUpdateRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `modules` | Map<String, String> | 每个模块的自定义模板文本。 |
  | `finalTemplates` | Map<String, String> | 终稿模板文本。 |
  | `fieldRefine` | String | 字段润色模板。 |
- **响应**：`200 OK`，无 body。
- **主要逻辑**：`WorldPromptTemplateService.saveTemplates(userId, request)`。

## POST /api/v1/world-prompts/reset
- **认证**：需要登录。
- **功能**：按 key 重置世界模板。
- **请求体**：`WorldPromptTemplatesResetRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `keys` | List<String> | 待重置的模板 key。 |
- **响应**：`200 OK`，无 body。
- **主要逻辑**：`WorldPromptTemplateService.resetTemplates(userId, request)`。

## GET /api/v1/world-prompts/metadata
- **认证**：无需登录。
- **功能**：查看世界模板支持的模块结构、变量、函数与示例。
- **响应**：`200 OK`，`WorldPromptTemplateMetadataResponse`
  - `modules`：模块元数据列表。
  - `variables`：变量说明。 
  - `functions`：函数说明。
  - `examples`：模板示例。 
- **主要逻辑**：`WorldPromptTemplateService.getMetadata()`。

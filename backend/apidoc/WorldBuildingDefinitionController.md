# WorldBuildingDefinitionController 接口文档

基础路径：`/api/v1/world-building/definitions`

## GET /api/v1/world-building/definitions
- **认证**：无需登录。
- **功能**：提供世界观编辑器使用的模块/字段定义、字段润色模板与提示上下文。
- **响应**：`200 OK`，`WorldBuildingDefinitionResponse`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `basicInfo` | WorldBasicInfoDefinitionDto | 世界基础信息配置。 |
  | `modules` | List<WorldModuleDefinitionDto> | 各模块的字段与描述。 |
  | `fieldRefineTemplate` | WorldFieldRefineTemplateDto | 字段润色模板。 |
  | `promptContext` | WorldPromptContextDefinitionDto | Prompt 上下文定义。 |
- **主要逻辑**：`WorldBuildingDefinitionService.getDefinitions()` 直接返回静态配置。

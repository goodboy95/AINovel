# 提示词帮助页（PromptHelpPage）

- **路由/文件**：`/settings/prompt-guide`
- **对应设计稿文件**：`src/pages/Settings/PromptHelpPage.tsx`
- **布局**：居中卡片，顶部标题+“返回设置”按钮；内容依次为插值语法列表、函数表、各模板变量表、示例列表。
- **功能**：
  - 加载提示词元数据并呈现为表格/列表。
  - 仅展示，不修改数据。
- **接口**：`GET /api/v1/prompt-templates/metadata`，返回：
  - `functions[]`（name/description/usage）、
  - `templates[]` 每个含 `variables[] { name, valueType, description }`，
  - `syntaxTips[]`、`examples[]`。
- **待完善**：
  - 增加搜索/过滤；
  - 支持复制到剪贴板；
  - 与模板编辑联动高亮变量。

## 开发对接指南 (Mock vs Real)

### 1. 动态元数据
- **当前 Mock**：变量列表（`variables`）是硬编码在组件文件中的。
- **真实对接**：
  - 应调用 `GET /api/v1/prompt-templates/metadata`。
  - 这样当后端增加新的插值变量（例如新增 `{characterList}`）时，前端帮助文档能自动更新，无需改代码。
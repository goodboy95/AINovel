# 世界提示词帮助页（WorldPromptHelpPage）

- **路由/文件**：`/settings/world-prompts/help`
- **对应设计稿文件**：`src/pages/Settings/WorldPromptHelpPage.tsx`
- **布局**：卡片 + Anchor 目录；段落包含编写要点、可用变量表、函数表、模块字段表、示例、FAQ；右上“返回设置”。
- **接口**：`GET /api/v1/world-prompts/metadata`，返回：
  - `variables[] { name, valueType, description }`
  - `functions[] { name, description, usage }`
  - `modules[] { key, label, fields[] { key, label, recommendedLength } }`
  - `examples[]` 文本片段。
- **用途**：指导用户编写世界观的模块草稿模板、正式模板、字段精修模板。
- **待完善**：
  - 示例与当前世界的实时对照；
  - FAQ 可链接到实际模板编辑位置；
  - 支持一键复制推荐片段。

## 开发对接指南 (Mock vs Real)

### 1. 动态元数据
- **当前 Mock**：变量列表是硬编码的。
- **真实对接**：
  - 同上，应从后端获取元数据。
  - 特别是 `modules` 列表，应与世界观定义的模块保持一致。
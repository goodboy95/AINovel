# 页面地图与导航关系

| 页面 | 路由 | 前端文件 (设计稿对应) | 说明 | 关联/入口 |
| --- | --- | --- | --- | --- |
| 首页 | `/` | `src/pages/Index.tsx` | 登陆/注册/进入工作台、素材库、世界观的网关页；根据登录态显示按钮。 | 登录后跳转工作台；顶部按钮直达 `/materials`、`/worlds`。 |
| 登录 | `/login` | `src/pages/auth/Login.tsx` | 表单提交用户名/密码获取 JWT。 | 成功后跳 `/workbench`；页脚链接注册。 |
| 注册 | `/register` | `src/pages/auth/Register.tsx` | 创建账户的表单页。 | 成功后跳 `/login`；页脚链接登录。 |
| 工作台 | `/workbench/:tab?` | `src/pages/Workbench/Workbench.tsx` | 多标签创作中枢：故事构思、故事管理、大纲工作台、小说创作、素材检索。 | 受 `ProtectedRoute` 保护；故事/大纲/素材/世界观等核心流程均在此串联。 |
| 设置 | `/settings` | `src/pages/Settings/Settings.tsx` | LLM 接入参数、提示词模板管理。 | 头部入口；子页 `/settings/prompt-guide`、`/settings/world-prompts/help`。 |
| 提示词帮助 | `/settings/prompt-guide` | `src/pages/Settings/PromptHelpPage.tsx` | 展示故事/大纲/正文/润色模板的变量、函数、示例。 | 从设置页按钮返回。 |
| 世界提示词帮助 | `/settings/world-prompts/help` | `src/pages/Settings/WorldPromptHelpPage.tsx` | 展示世界构建模板的变量、函数、字段说明与 FAQ。 | 从设置页按钮返回。 |
| 素材库 | `/materials` | `src/pages/Material/MaterialPage.tsx` | 素材创建、上传、审核、列表/查重/引用。 | 首页/工作台按钮进入；内部 Tab 切换功能子区。 |
| 世界构建 | `/worlds` | `src/pages/WorldBuilder/WorldBuilderPage.tsx` | 世界观草稿/发布、模块编辑、自动生成流水线、发布。 | 首页/头部入口；世界可在故事/大纲/稿件、素材引用组件中被选择。 |

关系摘要：`AuthProvider` 提供登录态；`ProtectedRoute` 将未认证用户重定向到 `/login`；工作台内部 Tab 与 URL 同步；素材库与世界观为独立受保护页面；设置页链接两份帮助文档；首页提供所有入口的外显导航。
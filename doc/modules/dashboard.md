# 模块说明：Dashboard 与项目入口

## 入口与权限
- 路由：`/dashboard`，需登录。
- 用途：作为“模块选择器”，引导用户进入小说管理（`/novels`）与世界管理（`/worlds`）。

## 主要页面
- **Dashboard**：展示小说数、世界数、累计字数、累计设定条目数（来自 `/api/v1/user/summary`）。
- **小说管理**：`/novels` 列出故事卡，入口 `/novels/create` 新建小说，点击卡片进入 `/workbench?id=...`。
- **世界管理**：`/worlds` 列出世界卡，入口 `/worlds/create` 新建世界，点击卡片进入 `/world-editor?id=...`。

## 关键交互
- 登录后默认跳转到 `/dashboard`。
- 小说/世界卡片支持删除（故事删除、草稿世界删除）。


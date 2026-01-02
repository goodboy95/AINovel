# 首页（HomePage）

- **路由/文件**：`/`
- **对应设计稿文件**：`src/pages/Index.tsx`
- **布局**：全屏渐变背景，居中标题+副标题；下方按登录态切换按钮组。未登录显示“登录/注册”；已登录显示“进入工作台 / 素材库 / 世界构建”三按钮。
- **功能点**：
  - 读取 `AuthContext.isAuthenticated` 决定导航入口。
  - 作为所有受保护页的落地/回退入口。
- **后端接口**：无直接调用。登录态由 `AuthProvider` 通过 `/api/auth/validate` 预先校验。
- **待完善/备注**：可补充产品亮点、最近更新、快速引导或 Demo 入口；可在按钮上增加权限提示（例如无素材写权限时灰显）。

## 开发对接指南 (Mock vs Real)

### 1. 登录态校验
- **当前 Mock**：`AuthContext` 初始化时仅检查 `localStorage` 中是否存在 `token` 字符串。
- **真实对接**：
  - `AuthProvider` 初始化时，应携带 Token 调用 `GET /api/auth/validate`。
  - 如果后端返回 401 或 Token 无效，应自动清除本地存储并重置状态为未登录。

### 2. 导航权限
- **当前 Mock**：只要有 Token 就显示所有入口（工作台、素材库、世界构建）。
- **真实对接**：
  - 建议在 `/api/auth/validate` 的响应中包含用户权限列表（如 `permissions: ['material:write', 'world:publish']`）。
  - 首页按钮应根据权限决定是否显示或置灰（例如：普通用户可能无法进入“世界构建”发布页）。
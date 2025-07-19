# 登录页（Login）

- **路由/文件**：`/login`
- **对应设计稿文件**：`src/pages/auth/Login.tsx`
- **布局**：居中卡片；表单含用户名、密码；底部跳转注册链接。
- **交互流程**：
  - 提交后 `POST /api/v1/auth/login`，Body: `{ username, password }`。
  - 成功：`data.token` 写入 `localStorage`，`AuthContext.login` 更新状态并跳转 `/workbench`。
  - 失败：展示 `message.error`（使用后端的 `message` 字段或通用提示）。
  - 首次加载若已认证则自动重定向 `/workbench`（useEffect）。
- **依赖/权限**：无需 Token；其他页面通过 `AuthContext` 共享登录态。
- **待完善**：
  - 增加忘记密码/验证码；
  - 明确密码规则与错误文案；
  - 支持 SSO / 第三方登录（目前仅用户名密码）。

## 开发对接指南 (Mock vs Real)

### 1. 认证接口
- **当前 Mock**：`src/lib/mock-api.ts` 中硬编码了 `admin/password` 的判断，并返回一个假的 JWT 字符串。
- **真实对接**：
  - 替换为真实的 `POST /api/v1/auth/login` 调用。
  - **安全注意**：生产环境建议使用 HttpOnly Cookie 存储 Token，或者在前端使用更安全的 Token 存储策略。如果使用 Header 传输，需确保 `api.ts` 的拦截器中正确注入 `Authorization: Bearer <token>`。

### 2. 错误处理
- **当前 Mock**：简单的 `try/catch` 弹窗提示“登录失败”。
- **真实对接**：
  - 需细分错误码：账号不存在、密码错误、账户被锁、验证码错误等，并在 UI 上给予具体反馈。
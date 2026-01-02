# V2 认证与安全设计

## 1. 注册流程改造
为了防止恶意注册和接口滥用，V2 引入了“邮箱验证码 + PoW 人机验证”的双重机制。

### 流程图
1. **用户输入邮箱** -> 点击“发送验证码”。
2. **前端拦截** -> 唤起 `CapJS` 组件。
3. **CapJS 计算** -> 浏览器进行 Hash 碰撞计算 (PoW)，耗时约 1-2 秒，生成 `captchaToken`。
4. **请求发送** -> `POST /api/auth/send-code` Body: `{ email, captchaToken }`。
5. **后端校验** -> 验证 Token 有效性 -> SMTP 发送 6 位验证码。
6. **用户填码** -> `POST /api/auth/register` Body: `{ email, code, username, password }`。

### 后端实现指南
- **CapJS 验证**：后端需校验提交的 Token 是否满足难度要求（例如前几位为 0），且 Token 未被使用过（防重放）。
- **邮件限流**：对同一邮箱、同一 IP 进行发送频率限制（如 60s 一次，1 小时 5 次）。

## 2. 角色权限
- **User**: 普通用户，访问 `/workbench`, `/profile`。
- **Admin**: 管理员，额外访问 `/admin/*`。
- **前端控制**：`AdminRoute` 组件检查 `user.role === 'admin'`。
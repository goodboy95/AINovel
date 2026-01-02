# V2 开发文档指南

欢迎查阅 Novel Studio V2 版本开发文档。V2 版本在 V1 的基础创作功能之上，增加了**商业化运营能力**（积分系统）和**后台管理能力**。

本文档旨在指引后端与前端开发人员如何利用 `doc/v2/` 下的文档进行实际开发与落地。

## 1. 文档地图

请按照以下顺序阅读文档：

1.  **全局概览**:
    *   `changelog.md`: 快速了解 V2 改了什么。
    *   `structure.md`: 了解 V2 的页面结构和路由规划。

2.  **功能模块详解 (Implementation Guides)**:
    *   `page-auth-v2.md`: **[高优先级]** 注册、登录、PoW 验证、邮箱服务。
    *   `page-profile-v2.md`: 个人中心、签到、兑换码、积分数据库事务。
    *   `page-workbench-ai.md`: **[核心]** AI 计费逻辑、Token 计算、流式响应 (SSE)、Copilot 组件。
    *   `page-admin-system.md`: 后台仪表盘、系统全局配置、模型倍率管理。
    *   `page-admin-users.md`: 用户列表、封禁逻辑、积分审计日志。

## 2. 开发人员落地指南

### 后端开发人员 (Backend)
V2 的核心在于**数据一致性**与**安全**。请重点关注各文档中的 **"后端实现规范"** 章节。

*   **数据库变更**: 需新增 `credit_logs`, `redeem_codes`, `model_configs`, `system_settings` 表。用户表需增加积分与角色字段。
*   **事务处理**: 积分变动（签到、兑换、AI 消耗）必须在数据库事务中完成，严禁在应用层计算后直接 Update。
*   **Redis 使用**: 验证码存储、高频配置（如模型倍率）缓存。
*   **Token 计算**: 务必引入 `tiktoken` (Python) 或 `JTokkit` (Java) 等库，Mock 中的 `length/4` 算法仅供演示。

### 前端开发人员 (Frontend)
V2 的 UI 已经通过 Mock API 实现。您的工作是将 Mock 替换为真实接口调用。请关注文档中的 **"Mock vs Real 替换指南"**。

*   **流式响应**: AI 对话目前是 Mock 的一次性返回。对接真实后端时，需改造 `api.ai.chat` 以支持 `EventSource` 或 `fetch` 流式读取，实现打字机效果。
*   **状态管理**: 积分变动后，记得调用 `refreshProfile()` 更新全局 Context 中的用户余额。
*   **错误处理**: 完善 HTTP 402 (Payment Required / 积分不足)、429 (Too Many Requests) 的 UI 反馈。

## 3. 关键技术栈建议

*   **PoW 验证**: 前端推荐 `@mcaptcha/pow` 或类似库；后端需对应校验逻辑。
*   **邮件服务**: SendGrid, Aliyun DM, 或 AWS SES。
*   **支付/充值**: V2 目前仅实现了积分消耗与兑换码。如需接入 Stripe/微信支付，请参考 `page-profile-v2.md` 中的积分逻辑进行扩展。

---

> **提示**: 所有页面代码位于 `src/pages/`，Mock 逻辑位于 `src/lib/mock-api.ts`。在开发过程中，建议先保留 Mock 模式用于 UI 调试，待后端接口 Ready 后再逐个替换。
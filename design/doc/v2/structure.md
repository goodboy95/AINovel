# V2 网站地图与结构

## 1. 认证体系 (Auth V2)
- `/register`: 注册页 (Updated)
  - 流程：邮箱输入 -> CapJS 人机验证 -> 发送验证码 -> 填写信息 -> 完成。
  - 组件：`src/components/auth/CapJS.tsx`
- `/login`: 登录页 (保持不变，逻辑对接 V2 API)

## 2. 用户中心 (User Center)
- `/profile`: 个人主页 (New)
  - 功能：展示积分、角色；每日签到；积分兑换；修改密码。
  - 文件：`src/pages/Profile/ProfilePage.tsx`

## 3. 创作工作台 (Workbench)
- `/workbench`: 核心创作区
  - **AI Copilot**: 右侧边栏助手 (待实现)
  - **Inline AI**: 选中文本润色 (待实现)
  - **积分消耗**: 实时显示与扣除逻辑。

## 4. 后台管理系统 (Admin Panel)
- 路由前缀: `/admin` (需 Admin 角色)
- 布局: `src/components/layout/AdminLayout.tsx` (深色主题)
- 页面:
  - `/admin/dashboard`: 仪表盘 (流量、积分、健康度)
  - `/admin/models`: 模型与 API 池管理 (配置倍率)
  - `/admin/users`: 用户管理 (封禁、发分)
  - `/admin/logs`: 积分日志
  - `/admin/codes`: 兑换码管理
  - `/admin/email`: 邮件服务监控
  - `/admin/settings`: 系统全局设置

## 5. 核心数据流变更
- **User**: 增加 `credits`, `role`, `lastCheckIn`.
- **Economy**: 引入 `ModelConfig` (倍率) 和 `CreditLog`.
- **Security**: 引入 CapJS PoW 验证。
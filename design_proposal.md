# AI 小说家功能优化设计文档

## 1. 引言

本文档旨在详细阐述 AI 小说家应用的功能优化方案，核心目标是简化用户界面、改进用户认证流程，并提升整体用户体验。通过本次调整，我们将实现更直观的首页、更便捷的用户管理功能以及无缝的自动登录体验。

## 2. 前端架构设计

### 2.1. 组件变更

为了实现新的功能需求，前端将进行以下组件的修改或新建：

*   **`HomePage.tsx` (修改)**
    *   **职责**: 应用的入口页面。
    *   **修改内容**: 移除现有的多个导航链接，仅保留“注册”和“登录”两个按钮，使其界面更简洁、目标更明确。

*   **`WorkbenchHeader.tsx` (新建)**
    *   **职责**: 工作台顶部的导航栏/头部组件。
    *   **功能**:
        *   在右上角显示当前登录的用户名。
        *   集成一个下拉菜单，包含“设置”和“退出”选项。

*   **`UserSettingsModal.tsx` (新建)**
    *   **职责**: AI 模型设置对话框。
    *   **功能**:
        *   完整复现并替代现有的 `/settings` 页面功能。
        *   用户可以在此对话框内配置 Base URL、模型名称和 API 密钥。
        *   提供“保存设置”和“测试连接”功能。
        *   此对话框将由 `WorkbenchHeader.tsx` 中的“设置”按钮触发。

*   **`App.tsx` (修改)**
    *   **职责**: 应用的根组件，负责路由管理。
    *   **修改内容**:
        *   引入新的路由逻辑，实现自动登录和访问控制。
        *   集成 `AuthContext` 来管理全局用户状态。

*   **`AuthContext.tsx` (新建)**
    *   **职责**: 全局状态管理。
    *   **功能**:
        *   使用 React Context API 创建一个 `AuthContext`，用于在整个应用中存储和共享用户信息（如用户名、Token）和认证状态。
        *   提供 `login`, `logout` 等方法来更新认证状态。

*   **`ProtectedRoute.tsx` (新建)**
    *   **职责**: 受保护的路由组件。
    *   **功能**:
        *   这是一个高阶组件（HOC），用于包装需要用户登录才能访问的路由（如 `/workbench`）。
        *   在渲染子组件之前，它会检查 `AuthContext` 中的认证状态。如果用户未登录，则自动重定向到首页 (`/`)。

### 2.2. 路由逻辑调整

我们将重构 `App.tsx` 中的路由配置，以实现更强大和安全的访问控制。

```tsx
// App.tsx (伪代码)
import { AuthProvider, useAuth } from './AuthContext';
import ProtectedRoute from './ProtectedRoute';

function AppRoutes() {
  const { user, loading } = useAuth();

  if (loading) {
    return <Spin />; // 显示加载指示器，直到认证状态确认
  }

  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route 
        path="/workbench/*" 
        element={
          <ProtectedRoute>
            <Workbench />
          </ProtectedRoute>
        } 
      />
    </Routes>
  );
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </Router>
  );
}
```

### 2.3. 状态管理策略

*   **用户信息存储**:
    *   JWT Token 将继续存储在 `localStorage` 中，以便在浏览器会话之间保持登录状态。
    *   用户信息（如用户名）和认证状态将存储在 `AuthContext` 中，作为全局的内存状态。
*   **状态验证**:
    *   应用加载时，`AuthProvider` 会检查 `localStorage` 中是否存在 Token。
    *   如果存在 Token，会调用后端的 `/api/auth/validate` 接口进行验证。
    *   验证成功后，从 Token 中解析出用户信息，更新 `AuthContext` 的状态。
    *   验证失败或 Token 不存在，则保持未登录状态。
*   **状态清除**:
    *   用户点击“退出”时，调用 `logout` 方法。
    *   该方法将清除 `localStorage` 中的 Token，并重置 `AuthContext` 的状态。
    *   最后，将用户重定向到首页。

## 3. 后端架构设计

### 3.1. 新增 API 接口

为了支持前端的自动登录和 Token 验证需求，需要新增一个 API 接口。

*   **接口路径**: `GET /api/auth/validate`
*   **职责**: 验证请求头中提供的 JWT Token 的有效性。
*   **请求**:
    *   Header: `Authorization: Bearer <token>`
*   **成功响应 (HTTP 200 OK)**:
    *   返回一个包含用户信息的 JSON 对象，例如：
      ```json
      {
        "username": "current_user"
      }
      ```
*   **失败响应 (HTTP 401 Unauthorized)**:
    *   如果 Token 无效、过期或不存在，返回错误信息。

### 3.2. 现有接口评估

*   `/api/v1/auth/login`: 无需修改。
*   `/api/v1/auth/register`: 无需修改。
*   `/api/v1/settings`: 无需修改。前端将通过新的 `UserSettingsModal.tsx` 组件调用此接口。

后端现有的安全配置已能满足需求，`JwtRequestFilter` 会拦截所有 `/api/v1/**` 的请求并验证 Token，这同样适用于新的 `/api/auth/validate` 接口。

## 4. 认证流程

以下是用户从打开网站到成功进入工作台的完整认证流程。

```mermaid
sequenceDiagram
    participant User as 用户
    participant Browser as 浏览器 (前端)
    participant Server as 服务器 (后端)

    User->>Browser: 访问网站
    Browser->>Browser: App 加载, AuthProvider 初始化
    Browser->>Browser: 检查 localStorage 中是否存在 Token
    
    alt Token 存在
        Browser->>Server: 发送请求 GET /api/auth/validate (携带 Token)
        Server->>Server: 验证 Token 有效性
        alt Token 有效
            Server-->>Browser: 返回用户信息 (HTTP 200 OK)
            Browser->>Browser: 更新 AuthContext (标记为已登录)
            Browser->>Browser: 自动跳转到 /workbench
        else Token 无效
            Server-->>Browser: 返回错误 (HTTP 401 Unauthorized)
            Browser->>Browser: 清除无效 Token, 保持未登录状态
            Browser->>Browser: 显示首页 /
        end
    else Token 不存在
        Browser->>Browser: 保持未登录状态
        Browser->>Browser: 显示首页 /
    end

    User->>Browser: 点击“登录”
    Browser->>Browser: 导航到 /login 页面
    User->>Browser: 输入用户名和密码并提交
    Browser->>Server: 发送请求 POST /api/v1/auth/login
    Server->>Server: 验证凭证
    Server-->>Browser: 返回 JWT Token
    Browser->>Browser: 存储 Token 到 localStorage
    Browser->>Browser: 更新 AuthContext (标记为已登录)
    Browser->>Browser: 跳转到 /workbench
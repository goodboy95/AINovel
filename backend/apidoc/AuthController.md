# AuthController 接口文档

## POST /api/v1/auth/register
- **认证**：匿名。
- **功能**：注册新用户。
- **请求体**：`RegisterRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `username` | String | 用户名。 |
  | `password` | String | 密码。 |
  | `email` | String | 邮箱。 |
- **响应**：
  - 成功：`201 Created`，`{"message": "User registered successfully"}`。
  - 失败：`400 Bad Request`，返回 `message` 说明失败原因。
- **主要逻辑**：调用 `AuthService.register` 执行注册，捕获 `IllegalArgumentException` 返回 400。

## POST /api/v1/auth/login
- **认证**：匿名。
- **功能**：登录并获取 JWT。
- **请求体**：`LoginRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `username` | String | 用户名。 |
  | `password` | String | 密码。 |
- **响应**：
  - 成功：`200 OK`，`{"token": "<JWT>"}`。
  - 失败：`401 Unauthorized`，`{"message": "Invalid username or password"}`。
- **主要逻辑**：调用 `AuthService.login` 校验凭证并生成 JWT。

## GET /api/v1/auth/validate
- **认证**：请求头 `Authorization: Bearer <token>`。
- **功能**：校验 JWT 是否有效，并返回基础权限信息。
- **请求参数**：无额外路径参数。
- **响应**：
  - 成功：`200 OK`，返回字段：
    | 字段 | 类型 | 说明 |
    | --- | --- | --- |
    | `username` | String | JWT 内的用户名。 |
    | `userId` | Long | 当前用户 ID。 |
    | `workspaceId` | Long | 当前工作空间 ID（等于用户 ID）。 |
    | `permissions` | Map<String, List<String>> | 每个资源对应的权限级别枚举名称列表。 |
  - 失败：`401 Unauthorized`，无 body。
- **主要逻辑**：解析 `Authorization` 头获取 token，使用 `JwtUtil.validateToken` 校验后查询 `PermissionService.findPermissionsForWorkspace`。异常时返回 401。

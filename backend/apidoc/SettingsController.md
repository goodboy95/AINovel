# SettingsController 接口文档

基础路径：`/api/v1/settings`

## GET /api/v1/settings
- **认证**：需要登录。
- **功能**：获取当前用户的 AI 设置。
- **响应**：`200 OK`，`SettingsDto`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `baseUrl` | String | 模型调用基础地址。 |
  | `modelName` | String | 使用的模型名称。 |
  | `apiKey` | String | 仅在更新请求中使用；响应中不会返回明文。 |
  | `customPrompt` | String | 自定义系统提示。 |
  | `apiKeyIsSet` | boolean | 是否已配置 API Key。 |
- **主要逻辑**：`SettingsService.getSettings(username)`。

## PUT /api/v1/settings
- **认证**：需要登录。
- **功能**：更新 AI 设置。
- **请求体**：`SettingsDto`（填写需更新的字段；`apiKey` 若不为空会覆盖原值）。
- **响应**：`200 OK`，无 body。
- **主要逻辑**：`SettingsService.updateSettings(username, dto)`。

## POST /api/v1/settings/test
- **认证**：需要登录。
- **功能**：验证当前设置能否成功连接到模型服务。
- **请求体**：`SettingsDto`（提供待测 provider/密钥等）。
- **响应**：
  - 成功：`200 OK`，`{"message": "Connection successful!"}`。
  - 失败：`400 Bad Request`，`{"message": "Connection test failed. Please check your API key and provider."}`。
- **主要逻辑**：`SettingsService.testConnectionForUser(userId, dto)`，返回布尔结果决定状态码。

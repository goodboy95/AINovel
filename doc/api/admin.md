# Admin API（需管理员权限）

## 仪表盘
- `GET /api/v1/admin/dashboard`：返回 `{totalUsers,todayNewUsers,totalCreditsConsumed,todayCreditsConsumed,apiErrorRate,pendingReviews}`。

## 模型配置
- `GET /api/v1/admin/models`：模型列表。
- `PUT /api/v1/admin/models/{id}`：更新模型倍率/启用状态等，返回 `true`。

## 用户管理
- `GET /api/v1/admin/users`：用户列表（含积分/封禁/签到时间）。
- `POST /api/v1/admin/users/{id}/grant-credits`：发放积分，请求 `{amount}`，返回 `true`。
- `POST /api/v1/admin/users/{id}/ban`：封禁用户，返回 `true`。
- `POST /api/v1/admin/users/{id}/unban`：解封用户，返回 `true`。

## 积分日志
- `GET /api/v1/admin/logs?limit=200`：最近积分流水，返回数组。

## 兑换码
- `GET /api/v1/admin/redeem-codes`：兑换码列表。
- `POST /api/v1/admin/redeem-codes`：创建兑换码，请求 `{code,amount}`，返回 `true`。

## 邮件验证/SMTP
- `GET /api/v1/admin/email/smtp`：SMTP 状态 `{host,port,username,passwordIsSet}`。
- `POST /api/v1/admin/email/test`：发送测试邮件，请求 `{email}`，返回 `true`。
- `GET /api/v1/admin/email/verification-codes?limit=50`：最近验证码记录（用于测试定位验证码）。


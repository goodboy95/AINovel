# AuthController
- `POST /api/v1/auth/login`：请求 `{username,password}`，返回 `{token,id,username,email,roles}`。
- `POST /api/v1/auth/register`：请求 `{username,email,password}`，返回同登录。
- `POST /api/v1/auth/send-code`：发送注册验证码，请求 `{email,captchaToken}`，返回 `{success,message}`。
  - `captchaToken` 为前端 PoW（CapJS）生成的 Base64(JSON)。
  - 触发频控（默认：同邮箱 1 小时内最多 5 次）会返回 HTTP 429。
- `POST /api/v1/auth/register-v2`：验证码注册，请求 `{email,code,username,password}`，返回 `{success,message}`。
- `GET /api/v1/auth/validate`：需要 Bearer Token，返回 `{id,username,email,permissions}`。

# AuthController
- `POST /api/v1/auth/login`：请求 `{username,password}`，返回 `{token,id,username,email,roles}`。
- `POST /api/v1/auth/register`：请求 `{username,email,password}`，返回同登录。
- `POST /api/v1/auth/send-code`：发送注册验证码，请求 `{email,captchaToken}`，返回 `{success,message}`。
- `POST /api/v1/auth/register-v2`：验证码注册，请求 `{email,code,username,password}`，返回 `{success,message}`。
- `GET /api/v1/auth/validate`：需要 Bearer Token，返回 `{id,username,email,permissions}`。

# AuthController
- `POST /api/v1/auth/login`：请求 `{username,password}`，返回 `{token,id,username,email,roles}`。
- `POST /api/v1/auth/register`：请求 `{username,email,password}`，返回同登录。
- `GET /api/v1/auth/validate`：需要 Bearer Token，返回 `{id,username,email,permissions}`。

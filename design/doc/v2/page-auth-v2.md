# V2 注册与安全验证开发指南

## 1. 页面设计 (`src/pages/auth/Register.tsx`)

### UI 交互逻辑
- **分步表单**：
  - **Step 1**: 输入邮箱 -> 完成 CapJS 人机验证 -> 点击“发送验证码”。
  - **Step 2**: 填写验证码、用户名、密码、确认密码 -> 点击“完成注册”。
- **CapJS 组件** (`src/components/auth/CapJS.tsx`)：
  - 初始状态显示“点击验证”。
  - 点击后进入 `computing` 状态（前端模拟 PoW 计算）。
  - 计算完成后回调 `onVerify(token)`，按钮变更为“验证通过”。

### 前端开发注意
- **防抖与倒计时**：发送验证码按钮在点击后应进入 60s 倒计时禁用状态（Mock 中未实现，需补充）。
- **状态重置**：如果用户修改了邮箱，应重置 CapJS 状态和 Step 步骤。
- **错误处理**：需优雅处理 429 (Too Many Requests) 和 409 (Conflict) 错误。

---

## 2. 后端实现规范

### 接口 A: 发送验证码
- **Endpoint**: `POST /api/v1/auth/send-code`
- **Payload**: `{ email: string, captchaToken: string }`
- **逻辑流程**:
  1.  **PoW 校验**: 验证 `captchaToken` 是否有效（检查 Hash 难度、时间戳、是否已使用）。
  2.  **限流检查**: 检查该 Email/IP 在过去 1 小时内的发送次数（建议阈值：5次/小时）。
  3.  **生成验证码**: 生成 6 位数字随机码。
  4.  **存储**: 存入 Redis，Key=`verify:email:{email}`, Value=`code`, TTL=15分钟。
  5.  **发送**: 调用 SMTP 服务（如 SendGrid/Aliyun DM）发送邮件。

### 接口 B: 完成注册
- **Endpoint**: `POST /api/v1/auth/register` (V2)
- **Payload**: `{ email, code, username, password }`
- **逻辑流程**:
  1.  **校验验证码**: 从 Redis 读取并比对，若不匹配或过期返回 400。
  2.  **查重**: 检查 Email 或 Username 是否已存在。
  3.  **入库**: 创建用户记录，密码需 BCrypt 加密。
  4.  **清理**: 注册成功后删除 Redis 中的验证码。

---

## 3. Mock vs Real 替换指南

| 数据/逻辑 | Mock 实现 | 真实开发替换方案 |
| :--- | :--- | :--- |
| **CapJS Token** | `setTimeout` 生成假字符串 | **前端**: 引入真实的 PoW 库 (如 `proof-of-work` 包) 计算 Hash。<br>**后端**: 校验 Hash 前缀零的个数是否达标。 |
| **发送邮件** | `console.log` 打印验证码 | 集成 Spring Boot Starter Mail 或第三方邮件 SDK。 |
| **验证码存储** | 内存变量 (不可靠) | **Redis**。必须设置过期时间。 |
| **密码存储** | 明文存储 | **BCrypt** 或 **Argon2** 哈希存储。 |
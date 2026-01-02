# V2 个人中心与积分经济开发指南

## 1. 页面设计 (`src/pages/Profile/ProfilePage.tsx`)

### UI 交互逻辑
- **资产卡片**：展示当前积分余额（需千分位格式化）。
- **签到按钮**：
  - 根据 `user.lastCheckIn` 判断今日是否已签到。
  - 若已签到，按钮禁用并显示“今日已签到”。
  - 点击签到后，触发全局 `refreshProfile()` 更新余额。
- **兑换输入框**：输入 Code -> 提交 -> 显示获得的积分数 -> 清空输入框 -> 刷新余额。
- **安全设置**：修改密码表单。

### 前端开发注意
- **乐观更新**：签到成功后，可以先在前端手动 `user.credits += points` 提升响应速度，再等待 `refreshProfile` 确认。
- **并发防止**：兑换和签到请求期间必须禁用按钮，防止双重提交。

---

## 2. 后端实现规范

### 接口 A: 每日签到
- **Endpoint**: `POST /api/v1/user/check-in`
- **逻辑流程**:
  1.  **获取配置**: 读取系统设置中的 `checkInMinPoints` 和 `checkInMaxPoints`。
  2.  **日期校验**: 检查数据库中该用户 `lastCheckIn` 字段。注意时区问题（建议统一使用 UTC 判断是否跨天）。
  3.  **事务处理 (Transaction)**:
      - 计算随机积分 `points`。
      - 更新用户表: `UPDATE users SET credits = credits + ?, last_check_in = NOW() WHERE id = ?`。
      - 插入日志: `INSERT INTO credit_logs (user_id, amount, reason, ...) VALUES ...`。
  4.  **返回**: `{ success: true, points, newTotal }`。

### 接口 B: 积分兑换
- **Endpoint**: `POST /api/v1/user/redeem`
- **Payload**: `{ code: string }`
- **逻辑流程**:
  1.  **查询 Code**: `SELECT * FROM redeem_codes WHERE code = ?`。
  2.  **有效性校验**: 检查是否存在、`is_used` 是否为 true、`expires_at` 是否过期。
  3.  **锁机制**: 推荐使用 `SELECT ... FOR UPDATE` 悲观锁或版本号乐观锁，防止多人同时兑换同一个一次性码。
  4.  **事务处理**:
      - 标记已用: `UPDATE redeem_codes SET is_used = 1, used_by = ? WHERE id = ?`。
      - 加分: `UPDATE users SET credits = credits + ? WHERE id = ?`。
      - 日志: 写入 `credit_logs`。

---

## 3. Mock vs Real 替换指南

| 数据/逻辑 | Mock 实现 | 真实开发替换方案 |
| :--- | :--- | :--- |
| **签到判断** | 简单的字符串日期比较 | **数据库层**: 使用 SQL 日期函数或应用层 `LocalDate` 比较。 |
| **兑换码并发** | 无锁，仅内存判断 | **数据库锁**: 必须处理并发，否则一个码可能被兑换多次。 |
| **积分日志** | 内存数组 `unshift` | **独立表**: `credit_logs` 表，建议建立 `user_id` 和 `created_at` 索引。 |
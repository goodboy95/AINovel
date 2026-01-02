# 个人中心与积分系统

## 1. 积分经济模型
- **单位**：1 积分 = 100,000 系统 Token。
- **消耗计算**：
  - `Cost = (InputTokens * Model.InputMult + OutputTokens * Model.OutputMult) / 100,000`
  - 允许扣至负数，但负数时禁止发起新请求。

## 2. 签到机制
- **规则**：每日 0 点重置。
- **奖励**：随机积分，范围由后台 `SystemSettings` 配置 (`checkInMinPoints` ~ `checkInMaxPoints`)。
- **实现**：
  - 前端：检查 `user.lastCheckIn` 日期是否为今天。
  - 后端：`POST /api/user/check-in`，校验日期，更新数据库，写入 `CreditLog`。

## 3. 兑换码
- **用途**：活动赠送、补偿。
- **逻辑**：
  - 用户输入 Code -> 后端查找 `RedeemCode` 表。
  - 校验：是否存在、是否过期、是否已使用（对于一次性码）。
  - 成功：用户积分增加，Code 标记为已用，写入 `CreditLog`。
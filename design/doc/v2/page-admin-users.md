# V2 后台用户与审计开发指南

## 1. 页面设计

### 用户管理 (`src/pages/Admin/UserManager.tsx`)
- **列表**：分页展示用户（Mock 未分页）。
- **操作**：
  - **调整积分**：弹窗输入正负数。
  - **封禁**：修改 `isBanned` 状态。

### 积分日志 (`src/pages/Admin/CreditLogs.tsx`)
- **列表**：展示全站流水。
- **筛选**：Mock 未实现，真实需支持按 UserID、类型、时间筛选。

### 兑换码管理 (`src/pages/Admin/RedeemCodes.tsx`)
- **列表**：展示所有码及使用状态。
- **生成**：输入面额和数量（或自定义码）。

---

## 2. 后端实现规范

### 接口 A: 用户列表
- **Endpoint**: `GET /api/v1/admin/users?page=1&size=20&q=username`
- **分页**: 必须实现分页，否则数据量大时会拖垮数据库。

### 接口 B: 管理员调整积分
- **Endpoint**: `POST /api/v1/admin/users/{id}/credits`
- **Payload**: `{ amount: number, reason: string }`
- **逻辑**:
  - 必须记录日志，`reason` 标记为 `admin_grant`。
  - 记录操作管理员的 ID (`operator_id`)，以便审计。

### 接口 C: 兑换码生成
- **Endpoint**: `POST /api/v1/admin/codes`
- **逻辑**:
  - 批量插入。
  - 保证 Code 唯一性（数据库唯一索引）。

---

## 3. Mock vs Real 替换指南

| 数据/逻辑 | Mock 实现 | 真实开发替换方案 |
| :--- | :--- | :--- |
| **分页** | 返回全部数据 | **Pageable**: 后端接收 `page/size` 参数，返回 `{ content, totalElements }`。 |
| **搜索** | 内存 Filter | **SQL LIKE** 或全文检索。 |
| **操作审计** | 仅记录变动 | **Audit Log**: 记录“谁”在“什么时间”修改了“谁”的积分。 |
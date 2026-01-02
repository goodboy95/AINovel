# V2 后台系统管理开发指南

## 1. 页面设计

### 仪表盘 (`src/pages/Admin/Dashboard.tsx`)
- **指标卡片**：总用户、今日积分消耗、API 错误率、待审素材。
- **图表**：Mock 中为占位符。真实开发需引入 `recharts` 或 `chart.js`。

### 模型管理 (`src/pages/Admin/ModelManager.tsx`)
- **列表**：展示所有模型配置。
- **开关**：快速启用/禁用模型。
- **倍率**：显示输入/输出倍率。

### 系统设置 (`src/pages/Admin/SystemSettings.tsx`)
- **配置项**：签到积分范围、注册开关、维护模式。

---

## 2. 后端实现规范

### 接口 A: 仪表盘统计
- **Endpoint**: `GET /api/v1/admin/dashboard-stats`
- **性能注意**:
  - `totalUsers`: `SELECT COUNT(*) FROM users` (可缓存)。
  - `todayCreditsConsumed`: `SELECT SUM(ABS(amount)) FROM credit_logs WHERE amount < 0 AND created_at > TODAY`。
  - **优化**: 对于大数据量，不要实时聚合。建议使用定时任务每 5-10 分钟计算一次存入 Redis，接口直接读 Redis。

### 接口 B: 模型配置
- **Endpoint**: `GET / POST /api/v1/admin/models`
- **存储**: `model_configs` 表。
- **热更新**: 修改配置后，应用层需刷新缓存，确保计费逻辑立即生效。

### 接口 C: 系统设置
- **Endpoint**: `GET / PUT /api/v1/admin/settings`
- **存储**: `system_settings` 表 (Key-Value 结构) 或 单行记录表。
- **生效**: 前端某些开关（如注册）需在对应接口（注册接口）中校验该配置。

---

## 3. Mock vs Real 替换指南

| 数据/逻辑 | Mock 实现 | 真实开发替换方案 |
| :--- | :--- | :--- |
| **统计数据** | 静态数字 | **SQL 聚合查询**。注意索引优化。 |
| **图表数据** | 无 | **时间序列数据**: 后端需提供按天/小时分组的统计接口。 |
| **配置持久化** | 内存变量 | **数据库 + Redis 缓存**。配置读取频率高，必须缓存。 |
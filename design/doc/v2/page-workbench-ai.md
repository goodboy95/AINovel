# V2 AI 助手与计费逻辑开发指南

## 1. 页面设计

### 模块 A: Copilot Sidebar (`src/components/ai/CopilotSidebar.tsx`)
- **功能**：对话式交互，支持选择模型。
- **上下文**：前端需将当前编辑器内容、大纲信息打包在 `context` 参数中传给后端。
- **积分展示**：每条 AI 回复下方显示 `-{cost} 积分`。

### 模块 B: Tiptap AI Refine (`src/components/editor/TiptapEditor.tsx`)
- **触发**：选中文字 -> 气泡菜单 -> 点击“润色”。
- **交互**：弹出 `AiRefineDialog` 对比原文本与新文本。
- **确认**：点击确认后替换编辑器选区内容。

### 前端开发注意
- **流式响应 (Streaming)**：Mock 中使用的是一次性返回。真实开发中，为了体验，**强烈建议**使用 Server-Sent Events (SSE) 或 WebSocket 实现打字机效果。
- **积分不足拦截**：在发起请求前，检查 `user.credits`。虽然允许透支，但若已透支（<0）应禁止请求。

---

## 2. 后端实现规范

### 核心逻辑：计费引擎
- **公式**: `Cost = (InputTokens * Model.InputMult + OutputTokens * Model.OutputMult) / 100,000`
- **Token 计算**:
  - 不要简单使用 `length / 4`。
  - 必须使用 `tiktoken` (Python/JS) 或 `JTokkit` (Java) 等库进行准确计算。
- **模型配置**: 从数据库 `model_configs` 表读取倍率，支持动态调整。

### 接口 A: AI 对话 (Chat)
- **Endpoint**: `POST /api/v1/ai/chat`
- **Payload**: `{ messages: [], modelId: string, context: object }`
- **逻辑流程**:
  1.  **构建 Prompt**: 将 `context` 转化为 System Prompt 注入消息列表。
  2.  **计算 Input Token**: 此时计算输入成本。
  3.  **调用 LLM**: 请求 OpenAI/Claude 接口。
  4.  **计算 Output Token**: 获取响应后计算输出成本。
  5.  **扣费事务**:
      - `UPDATE users SET credits = credits - ? WHERE id = ?` (允许扣为负数)。
      - `INSERT INTO credit_logs ...`。
  6.  **返回**: 包含内容和 `usage` 信息。

### 接口 B: 文本润色 (Refine)
- **Endpoint**: `POST /api/v1/ai/refine`
- **逻辑**: 类似 Chat，但 Prompt 是固定的润色模板（如“请优化以下文本...”）。

---

## 3. Mock vs Real 替换指南

| 数据/逻辑 | Mock 实现 | 真实开发替换方案 |
| :--- | :--- | :--- |
| **LLM 调用** | `setTimeout` + 字符串拼接 | **LangChain** 或官方 SDK 调用真实模型 API。 |
| **Token 计算** | `length / 4` (极不准) | **Tokenizer**: 使用 BPE 算法库进行精确计算。 |
| **流式输出** | 不支持 | **SSE**: 接口返回 `text/event-stream`，前端使用 `EventSource` 或 `fetch` reader 处理。 |
| **上下文注入** | 简单的 JSON stringify | **Prompt Engineering**: 精心设计的 System Prompt，包含任务定义、风格约束等。 |
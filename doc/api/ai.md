# AI API（需登录）

## 模型列表
- `GET /api/v1/ai/models`：返回已启用模型列表（给 Copilot 下拉选择）。

## Copilot 对话
- `POST /api/v1/ai/chat`：请求 `{modelId,context?,messages:[{role,content}]}`，返回 `{role,content,usage:{inputTokens,outputTokens,cost},remainingCredits}`。

## 文本润色
- `POST /api/v1/ai/refine`：请求 `{text,instruction?,modelId}`，返回 `{result,usage,remainingCredits}`。


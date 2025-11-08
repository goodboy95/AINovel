# AiController 接口文档

## POST /api/v1/ai/generate-dialogue
- **认证**：需要登录，使用 `@AuthenticationPrincipal` 注入的用户。
- **功能**：根据传入的角色信息与场景上下文生成对话内容。
- **请求体**：`CharacterDialogueRequest`
  | 字段 | 类型 | 说明 |
  | --- | --- | --- |
  | `characterId` | Long | 目标角色卡 ID，用于定位角色记忆。 |
  | `manuscriptId` | Long | 所属手稿 ID，便于加载相关记忆。 |
  | `currentSceneDescription` | String | 当前场景描述，提供上下文。 |
  | `dialogueTopic` | String | 本次对话主题。 |
- **响应**：`200 OK`
  - `CharacterDialogueResponse`
    | 字段 | 类型 | 说明 |
    | --- | --- | --- |
    | `dialogue` | String | 模型生成的对话正文。 |
- **主要逻辑**：调用 `CharacterDialogueService.generateDialogue(request, userId)` 根据用户 ID 与请求内容生成结果，再以 200 返回。

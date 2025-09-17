# 阶段三子任务设计：记忆驱动的对话生成

## 1. 背景与目标
- **目标**：利用阶段一、二积累的角色“记忆”（`newly_known_info`、`character_changes`、关系演化等），让 AI 生成更贴合角色经历与个性的对话。
- **价值**：提升 AI 对话的一致性，避免角色性格和信息前后矛盾，帮助作者快速产出符合人物弧光的对白。
- **范围**：新增后端对话生成服务、API 与前端交互。对现有日志数据结构不做破坏性变更。

## 2. 现状分析
- 角色记忆来源：`character_change_logs` 中 `newly_known_info` 与 `character_changes` 字段。
- 角色基础设定：`character_cards`（`synopsis`、`details`、`relationships`）。
- 场景上下文：`outline_scenes`（梗概）与 `manuscript_sections`（正文）。
- 用户 API Key 管理：`SettingsService` 已支持 per-user 模型/endpoint 配置，可复用。

## 3. 后端设计
### 3.1 新服务 `AiDialogueService`
- 位置：`backend/src/main/java/com/example/ainovel/service/AiDialogueService.java`。
- 依赖：`OpenAiService`、`CharacterChangeLogRepository`、`CharacterCardRepository`、`ManuscriptRepository`、`ManuscriptSectionRepository`、`OutlineSceneRepository`、`SettingsService`。
- 核心方法：
  ```java
  public DialogueResponse generateDialogue(Long manuscriptId, DialogueRequest request, Long userId);
  ```
- 主要步骤：
  1. **校验权限**：确认 `manuscriptId` 属于当前用户；确认 `characterId` 属于稿件对应故事卡。
  2. **收集角色档案**：读取 `CharacterCard` 的 `name`、`synopsis`、`details`、`relationships`。
  3. **提取关键记忆**：
     - 调用 `CharacterChangeLogRepository.findByCharacter_IdAndManuscript_IdAndDeletedAtIsNullOrderByChapterNumberDescSectionNumberDesc`。
     - 过滤掉 `newly_known_info`、`character_changes` 均为空的记录。
     - 取最近 N 条（默认 5 条，可配置），并生成摘要：
       ```
       第1章第3节：获知“梅花盗真实身份”。状态变化：怀疑转为信任。
       ```
     - 如数据量大，可引入简易加权：优先包含被标记为 `isTurningPoint`（阶段四字段，预留）。
  4. **构建场景上下文**：
     - 若 `request` 提供 `sceneId`，读取对应 `OutlineScene` 梗概及 `ManuscriptSection` 最新正文；否则使用 `currentSceneDescription`。
     - 将用户输入的 `dialogueTopic` 作为额外指令。
  5. **组装 Prompt**（详见 4.1）。
  6. **调用 AI**：使用 `openAiService.generate(prompt, apiKey, baseUrl, model)`，默认期望纯文本输出。
  7. **返回结果**：封装为 `DialogueResponse`，包含 `dialogue` 文本、`usedMemories` 列表、`characterName` 等辅助信息。

### 3.2 DTO 定义
- `DialogueRequest`（放置在 `backend/src/main/java/com/example/ainovel/dto`）：
  ```java
  public class DialogueRequest {
      private Long manuscriptId;
      private Long characterId;
      private Long sceneId; // 可选，用于自动填充场景上下文
      private String currentSceneDescription; // sceneId 缺失时必填
      private String dialogueTopic; // 可选
      private Integer memoryLimit; // 可选，默认 5
      private boolean includeRelationships; // 默认 true
  }
  ```
- `DialogueResponse`：
  ```java
  public class DialogueResponse {
      private String dialogue;
      private String characterName;
      private List<String> usedMemories; // 输出给前端展示
  }
  ```

### 3.3 控制器
- 新增 `AiController` 或在现有 `ManuscriptController` 中添加端点：
  ```java
  @PostMapping("/ai/dialogue")
  public ResponseEntity<DialogueResponse> generateDialogue(@RequestBody DialogueRequest request,
                                                            @AuthenticationPrincipal User user) {
      return ResponseEntity.ok(aiDialogueService.generateDialogue(request.getManuscriptId(), request, user.getId()));
  }
  ```
- `DialogueRequest` 中需包含 `manuscriptId`，以便校验。
- 响应错误：
  - 400：缺少必要字段（如 `characterId`、`manuscriptId`、`sceneId` 与 `currentSceneDescription` 二者皆空）。
  - 403：角色或稿件不属于当前用户。
  - 502：AI 返回异常或解析失败。

### 3.4 与现有服务的协作
- 可在 `CharacterChangeLogService` 中新增方法 `List<CharacterChangeLog> getMemoriesForDialogue(...)`，由 `AiDialogueService` 调用，避免重复查询逻辑。
- `SettingsService` 提供 `getBaseUrlByUserId`、`getModelNameByUserId`、`getDecryptedApiKeyByUserId`，复用阶段一逻辑。

## 4. AI Prompt 设计
### 4.1 Prompt 模板
```
你是一位了解{{characterName}}经历的小说家。请根据角色设定、记忆与当前情境，写一段符合其语气和立场的对话。

【角色核心设定】
- 人物简介：{{synopsis}}
- 详细背景：{{details}}
- 固有关系：{{relationships}}

【角色近期记忆（按时间倒序）】
{{#each memories}}
- 第{{chapter}}章第{{section}}节：{{memory}}
{{/each}}
（若无记忆则写“暂无新增记忆”）

【当前情境】
{{sceneDescription}}

【对话主题】
{{dialogueTopic 或 “自由发挥”}}

要求：
1. 对话中必须体现角色对上述记忆的态度或影响。
2. 保持角色口吻、价值观与既有设定一致。
3. 若场景涉及其他角色，请结合其与{{characterName}}的最新关系回应（见记忆或关系描述）。
4. 输出为纯文本对白，可包含对话双方简短动作描写，但避免解释性文字。
```
- 若 `includeRelationships=true`，在“角色近期记忆”后追加一节“【重要关系现状】”，列出上一条 `relationshipChanges` 的最新状态。
- 若 `dialogueTopic` 为空，则替换为“自由发挥”。

### 4.2 Token 控制策略
- 限制记忆条数（默认 5），可通过 `memoryLimit` 调整。
- 对每条记忆做简化：`String.format("%s；状态变化：%s", newlyKnownInfo, characterChanges)`，超过 200 字截断。
- 场景正文若过长（>1200 字），截取最近 600 字，保持 AI 输入简洁。

## 5. 前端设计
### 5.1 API 封装与类型
- `types.ts` 新增：
  ```ts
  export interface DialogueRequest {
    manuscriptId: number;
    characterId: number;
    sceneId?: number;
    currentSceneDescription?: string;
    dialogueTopic?: string;
    memoryLimit?: number;
    includeRelationships?: boolean;
  }

  export interface DialogueResponse {
    dialogue: string;
    characterName: string;
    usedMemories: string[];
  }
  ```
- `services/api.ts` 新增：
  ```ts
  export const generateDialogue = (payload: DialogueRequest): Promise<DialogueResponse> =>
    fetch('/api/v1/ai/dialogue', { method: 'POST', headers: getAuthHeaders(), body: JSON.stringify(payload) })
      .then(res => handleResponse<DialogueResponse>(res));
  ```

### 5.2 UI/UX：`GenerateDialogueModal`
- 新建组件 `frontend/src/components/modals/GenerateDialogueModal.tsx`。
- Props：
  ```ts
  interface GenerateDialogueModalProps {
    open: boolean;
    onClose: () => void;
    manuscriptId: number;
    characters: CharacterCard[];
    defaultScene?: Scene;
    onInsertToEditor?: (dialogue: string) => void;
  }
  ```
- Modal 内容：
  - 角色选择（`Select`），默认选中当前侧边栏正在查看的角色。
  - 场景描述：
    - 若有 `defaultScene`，展示梗概并允许编辑补充。
    - 提供 `Include latest manuscript content` 复选框，自动填充 `currentSceneDescription` 为梗概 + 正文片段。
  - 对话主题输入框（可选）。
  - 记忆条数滑条（1-10，默认 5）。
  - “生成对话”按钮，点击后调用 `generateDialogue`，显示 loading。
- 结果展示：
  - 显示生成文本（可复制）。
  - “插入到正文光标处”按钮，调用 `onInsertToEditor`。
  - 展示 `usedMemories` 列表，帮助作者理解 AI 依据。

### 5.3 `ManuscriptWriter` 集成
- 在正文编辑区域上方工具栏加入按钮：“🗣️ AI 生成对话”。
- 维护状态 `isDialogueModalOpen`。
- 打开 Modal 时传入：
  - `manuscriptId = selectedManuscript.id`
  - `characters = characterCards`（阶段一加载的角色数据）
  - `defaultScene = selectedScene`
  - `onInsertToEditor` 实现为在当前光标处插入文本（可借助 `TextArea` 的 `setRangeText`）。

### 5.4 错误处理
- 若 API 返回 400/403，弹出错误提示并保留 Modal。
- 若生成内容为空，提示用户调整主题或补充场景描述。

## 6. 安全与权限
- `AiDialogueService` 必须复用 `validateManuscriptAccess`。
- 角色校验：
  - 通过 `CharacterCard.getStoryCard().getId()` 与 `Manuscript.getOutlineCard().getStoryCard().getId()` 对比。
- 记录审计日志（可选）：生成请求参数、响应长度，以便后续排查。

## 7. 测试计划
- **后端单元测试**：
  - `AiDialogueServiceTest`
    - 无记忆时生成 Prompt、记忆存在时排序、`memoryLimit` 生效。
    - 角色不属于稿件、场景不匹配等异常。
    - 模拟 `OpenAiService` 返回字符串，验证 Response 封装。
- **后端集成测试**：
  - `AiControllerIT`：登录用户请求生成对话，校验鉴权与返回体。
- **前端测试**：
  - `GenerateDialogueModal`：
    - 表单校验（角色必选、场景描述或 sceneId 至少一个）。
    - Mock API 成功/失败流程。
    - `onInsertToEditor` 被调用。
- **手动测试**：
  - 选择带有多条记忆的角色，验证生成文本包含记忆点。
  - 切换记忆条数、主题，观察输出差异。

## 8. 实施步骤
1. **后端基础**：创建 DTO、服务、控制器；完善权限校验与 Prompt 构建；编写单元测试。
2. **前端 API 与类型**：更新 `types.ts`、`services/api.ts`，确认 TS 类型编译通过。
3. **Modal 组件开发**：实现 UI、表单校验、调用逻辑，与 `ManuscriptWriter` 打通。
4. **联调**：前后端集成测试，确保生成结果可插入正文。
5. **文档更新**：在 `doc/user_manual.md` 或 README 中新增使用说明。
6. **验收与回归**：回归阶段一/二功能（状态追踪、关系图谱）确保无回归。

完成后，作者可在写作时一键生成符合角色记忆的对白，大幅提升创作效率与角色一致性。

# 世界构建 Step 6 —— 工作台集成与上下文注入

## 1. 目标

1. 在故事构思、大纲工作台、小说创作三个页面提供“选择世界”能力，支持不选择（默认）。
2. 后端在生成故事、大纲、正文时，将选定世界的完整信息注入提示词上下文。
3. 默认提示词更新，确保当选择世界后自动引用世界信息；未选择世界时保持原行为。
4. 支持在 AI 提示词模板中通过插值表达式访问世界各模块的完整信息。

## 2. 数据与模型调整

### 2.1 数据库字段

在以下表中新增可空外键 `world_id`：
- `story_cards`：表示故事关联的世界。
- `outline_cards`：保留与故事的关联即可，可通过 `story_card.world_id` 获取，是否冗余由性能考虑决定（可选）。
- `manuscripts`：标记正文创作所引用的世界（便于后续统计）。

若不希望修改历史数据，可仅在业务层维护 `worldId`，不写入数据库；但推荐持久化，便于后续统计与同步。

### 2.2 DTO 与请求体

- `ConceptionRequest` / `CreateStoryRequest` 新增 `worldId?: number | null`。
- `GenerateChapterRequest` / `GenerateManuscriptSectionRequest` 新增 `worldId?: number | null`。
- 响应中返回故事/大纲/正文关联的 `worldId`，供前端回显。

## 3. 世界信息读取接口

新增后端接口：
- `GET /api/v1/worlds/{id}/full`：仅返回已发布世界的完整信息。

响应示例：

```json
{
  "world": {
    "id": 12,
    "name": "永昼群星",
    "tagline": "白昼永不落的多层天空都市",
    "themes": ["高幻想", "阴谋"],
    "creativeIntent": "……",
    "version": 3,
    "publishedAt": "2025-01-20T11:02:00"
  },
  "modules": [
    {
      "key": "cosmos",
      "label": "宇宙观与法则",
      "fullContent": "……",
      "excerpt": "概述：……",
      "updatedAt": "2025-01-20T11:01:30"
    },
    ...
  ]
}
```

- `excerpt`：取完整信息前 200 字，用于快速预览或提示词中做概要。
- 前端在工作台页面加载时调用，并缓存于 React Query。

## 4. 提示词上下文扩展

### 4.1 Prompt Builder 调整

在 `StoryPromptContextBuilder` / `OutlinePromptContextBuilder` / `ManuscriptPromptContextBuilder` 中：
1. 判断请求是否携带 `worldId`。
2. 若有，调用 `WorldService.getPublishedWorldWithModules(worldId)` 获取完整信息。
3. 构造 `workspaceWorldContext`：

```java
record WorkspaceWorldContext(
    Long id,
    String name,
    String tagline,
    List<String> themes,
    String creativeIntent,
    Map<String, WorkspaceWorldModule> modules
) {}

record WorkspaceWorldModule(
    String key,
    String label,
    String fullContent,
    String excerpt,
    Instant updatedAt
) {}
```

4. 在模板渲染上下文中新增 `workspace.world`：

```java
context.put("workspace", Map.of("world", workspaceWorldContext));
```

若未选择世界，则 `workspace.world` 置为 `null` 或完全不提供，模板可通过 `default` 函数处理。

### 4.2 提示词变量元数据

更新 `PromptTemplateMetadataProvider`：
- 在 `storyCreation`、`outlineChapter`、`manuscriptSection` 模板的变量列表中新增：
  - `${workspace.world.name}`
  - `${workspace.world.tagline}`
  - `${workspace.world.themes}` / `${workspace.world.themes[*]}`
  - `${workspace.world.creativeIntent}`
  - `${workspace.world.modules.KEY.fullContent}`
  - `${workspace.world.modules.KEY.excerpt}`
  - `${workspace.world.modules[*].label}`
  - `${workspace.world.modules[*].fullContent}`
- 说明：仅当选择世界时有值；可使用 `default("无")` 处理空值。

### 4.3 默认提示词更新

#### 故事构思 (`storyCreation`)
在现有提示词开头添加可选世界段落：

```
【世界参考】
${workspace.world|default(null) ? `- 世界名称：${workspace.world.name}
- 世界概述：${workspace.world.tagline}
- 创作意图：${workspace.world.creativeIntent|default("未提供")}
- 关键设定摘要：
${workspace.world.modules[*]|map(m -> "  · " + m.label + "：" + m.excerpt)|join("\n")}` : "- 未选择世界，可自由发挥。"}
```

> 采用模板引擎的条件/函数实现（示例为伪代码，实际模板需使用 `${workspace.world|default(null)}` + `|json()` 等函数组合）。

#### 大纲章节 (`outlineChapter`)
- 在“故事核心信息”部分追加世界信息列表。
- 若 `workspace.world` 存在，将 `workspace.world.modules[*].fullContent` 附加在提示末尾：“【世界详细设定】……”。

#### 正文创作 (`manuscriptSection`)
- 在“【故事核心设定】”之后插入世界信息摘要。
- 在提示末尾提供“【世界参考（完整）】”段落，拼接所有模块的 `fullContent`，供模型长期记忆；必要时可限制长度（如截断至 1000 字）。

### 4.4 长度控制

- 若将全部 `fullContent` 直接注入导致 prompt 超长，可设计：
  - 仅在默认模板中拼接 `excerpt`；在需要时（例如正文创作）再追加完整内容并在调用前做长度截断（例如 `StringUtils.abbreviate(fullContent, 1200)`）。
  - 提供配置选项（后续迭代），允许用户勾选需要注入的模块。

## 5. 前端工作台改造

### 5.1 世界选择组件

- 新建复用组件 `WorldSelect`：
  - `props`: `{ value?: number; onChange?: (id: number | undefined) => void; allowClear: true }`
  - 数据源：`GET /worlds?status=active`。
  - 展示：`Select` + 世界名称 + 标签（主题标签/版本号）。
  - 支持搜索（按世界名称）。

### 5.2 页面嵌入

1. **故事构思页面**：
   - 在输入表单顶部增加 `WorldSelect`。
   - 创建请求 payload 时附带 `worldId`。
   - 生成结果返回后，将 `worldId` 持久化在 `StoryCard` 对象中。

2. **大纲工作台**：
   - 在大纲详情右侧信息栏新增世界显示区（名称、概述、查看按钮）。
   - 若大纲所属故事已有世界，则默认选中；允许用户更换世界（提示可能导致历史章节与世界不一致）。
   - 调用生成章节接口时携带 `worldId`。

3. **小说创作页面**：
   - 在章节信息栏添加世界选择（继承自大纲或手动选择）。
   - 正文生成请求 payload 附带 `worldId`。

### 5.3 缓存与同步

- 当用户在故事构思阶段选择世界后，将 `worldId` 保存在 `StoryCard` 中；大纲/正文页面读取对应故事时自动带出。
- 若用户更换世界，需提示“将重新注入世界信息，请确认”。
- 前端缓存世界详情：`useQuery(['worlds','full', worldId])`，避免重复请求。

### 5.4 预览与辅助信息

- 在故事/大纲/正文页面提供“查看世界设定”按钮（Drawer 或 Modal），显示全部模块的 `fullContent`，便于人工参考。
- 若世界正在重新生成（`status=GENERATING`），在 UI 中提示“世界完整信息生成中，当前展示可能为旧版本”，并可提供刷新按钮。

## 6. 回退与兼容策略

- 未选择世界：模板中的世界段落使用 `default` 提供占位文本，避免影响旧流程。
- 旧故事缺少 `worldId`：保持原逻辑；用户可在故事详情中补选世界。
- 若所选世界被删除/归档：
  - 后端在获取世界时若 404，返回错误提示“世界已不可用，请重新选择”。
  - 前端清空选择并通知用户。

## 7. 测试要点

1. **后端**：
   - Prompt Builder 在 `worldId` 存在/缺失/世界未发布等场景下的行为。
   - 模板渲染时 world 信息是否正确注入，长度是否受控。
   - 旧接口兼容性（未携带 `worldId` 时不出错）。
2. **前端**：
   - 世界选择组件的加载、搜索、清除、错误处理。
   - 三个页面切换世界后的提示与状态同步。
   - 进度提示：当世界 status=GENERATING 时的 UI 告知。
3. **集成**：
   - 实际调用 AI，检查提示词中是否包含世界设定。
   - 更换世界后重新生成章节/正文，确保引用新世界。

至此，“世界构建”模块与工作台实现闭环，用户可在创建世界后将其设定直接应用于作品创作。

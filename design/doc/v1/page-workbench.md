# 工作台（Workbench）

- **路由/文件**：`/workbench` 与 `/workbench/:tab`
- **对应设计稿文件**：`src/pages/Workbench/Workbench.tsx`
- **子组件目录**：`src/pages/Workbench/tabs/`

**整体布局**：顶部 `WorkbenchHeader`（用户名、设置、登出）；主体为 Ant Tabs（card 风格）含 5 个标签：故事构思、故事管理、大纲工作台、小说创作、素材检索。各标签间保持共享的故事/大纲选中态。

**通用依赖**：
- 认证：需 JWT。
- 故事/角色数据：`useStoryData`（`/api/v1/story-cards`、`/api/v1/stories` 系列）。
- 大纲数据：`useOutlineData`（`/api/v1/story-cards/{id}/outlines` 系列）。
- 文本润色：`RefineModal` → `POST /api/v1/ai/refine-text`，Body `{ text, instruction, contextType }`。
- 世界引用：`WorldSelect`/`WorldReferenceDrawer` 读取 `/api/v1/worlds` & `/api/v1/worlds/{id}/full`（间接）。

## 标签 1：故事构思（StoryConception）
- **对应文件**：`src/pages/Workbench/tabs/StoryConception.tsx`
- **布局**：左侧表单（想法、类型、基调、标签、可选世界）；右侧显示生成的故事卡与角色卡列表，附“添加角色”按钮。
- **主要功能**：
  - 生成故事：`POST /api/v1/conception`，Body `{ idea, genre, tone, tags[], worldId|null }`，返回 `{ storyCard, characterCards[] }` 并落库。
  - 角色与故事润色：对 synopsis/storyArc/relationships 等字段，调用 `POST /api/v1/story-cards/{id}/refine` 或 `/api/v1/character-cards/{id}/refine`。
  - 追加角色：`POST /api/v1/story-cards/{storyId}/characters`，Body 为角色卡字段（name/synopsis/details/relationships）。
- **待完善**：生成失败的重试提示；标签推荐；世界引用的提示词示例。

## 标签 2：故事管理（StoryList + StoryDetail）
- **对应文件**：`src/pages/Workbench/tabs/StoryManager.tsx`
- **布局**：左列故事列表（可删除、新建）；右侧故事详情+角色卡列表。
- **功能与接口**：
  - 列表：`GET /api/v1/story-cards`；删除 `DELETE /api/v1/stories/{id}`（删除故事及其大纲/角色）。
  - 查看详情：`GET /api/v1/story-cards/{id}` + `GET /api/v1/story-cards/{id}/character-cards`。
  - 新建故事：`POST /api/v1/stories`，Body `{ title, synopsis, genre, tone, worldId|null }`。
  - 更新故事：`PUT /api/v1/story-cards/{id}`，Body 同上并含 storyArc。
  - 角色 CRUD：`POST /api/v1/story-cards/{id}/characters`、`PUT /api/v1/character-cards/{id}`、`DELETE /api/v1/character-cards/{id}`。
  - 文本润色同构思页。
- **待完善**：批量导入角色、角色排序、删除确认的关联提示（大纲/稿件影响）。

## 标签 3：大纲工作台（OutlinePage）
- **对应文件**：`src/pages/Workbench/tabs/OutlineWorkbench.tsx`
- **布局**：三栏
  1. 左：故事选择、世界引用选择、历史大纲列表 + “为当前故事创建新大纲”按钮。
  2. 中：`OutlineTreeView` 展示章节/场景树，顶部“保存大纲”。
  3. 右：未选节点时显示章节生成表单；选中章节/场景时显示对应编辑表单。
- **功能与接口**：
  - 加载大纲列表：`GET /api/v1/story-cards/{storyId}/outlines`。
  - 创建空大纲：`POST /api/v1/story-cards/{storyId}/outlines`（可携带 worldId 后续 PUT 修正）。
  - 获取/保存大纲：`GET /api/v1/outlines/{id}`，`PUT /api/v1/outlines/{id}`（提交整棵大纲含 chapters/scenes/worldId）。
  - 删除大纲：`DELETE /api/v1/outlines/{id}`。
  - 按章生成：`POST /api/v1/outlines/{outlineId}/chapters`，Body `{ chapterNumber, sectionsPerChapter, wordsPerSection, worldId? }`，返回新 Chapter（含场景）。
  - 章节/场景编辑：表单本地更新，点击“保存大纲”时一并 PUT；Scene/Chapter 润色通过 `RefineModal` 调用 `/api/v1/ai/refine-text`（contextType 带字段名）。
  - 世界引用：变更 worldId 后即刻 `PUT /api/v1/outlines/{id}`。
- **待完善**：章节/场景局部 PATCH（当前整纲 PUT）；拖拽排序；生成/保存的乐观提示与冲突处理。

## 标签 4：小说创作（ManuscriptWriter）
- **对应文件**：`src/pages/Workbench/tabs/ManuscriptWriter.tsx`
- **布局**：
  - 顶部筛选：故事下拉、该故事的大纲下拉。
  - “已有小说”卡片：列出选中大纲下的稿件，支持新建/删除。
  - 主区三栏：左大纲树选场景；中部正文编辑+世界引用+生成/保存按钮+素材建议；右侧角色状态侧边栏与自动建议列表。
- **功能与接口**：
  - 故事列表：`GET /api/v1/stories`。
  - 大纲列表/详情：同大纲工作台（`GET /api/v1/outlines/{id}`）。
  - 稿件列表：`GET /api/v1/outlines/{outlineId}/manuscripts`；新建 `POST /api/v1/outlines/{outlineId}/manuscripts` Body `{ title, worldId|null }`；删除 `DELETE /api/v1/manuscripts/{id}`。
  - 获取稿件内容：`GET /api/v1/manuscripts/{id}` 返回 `{ manuscript, sections: Record<sceneId, ManuscriptSection> }`。
  - 场景正文生成：`POST /api/v1/manuscript/scenes/{sceneId}/generate` Body `{ worldId? }`，返回 ManuscriptSection；生成后自动触发角色变化分析。
  - 保存正文：`PUT /api/v1/manuscript/sections/{sectionId}` Body `{ content }`。
  - 角色变化分析：`POST /api/v1/manuscripts/{manuscriptId}/sections/analyze-character-changes` Body `{ chapterNumber, sectionNumber, sectionContent, characterIds[] }`；结果刷新 `GET /api/v1/manuscripts/{id}/character-change-logs` 及按角色查询。
  - 记忆驱动对话：`POST /api/v1/ai/generate-dialogue` Body `{ characterId, manuscriptId, dialogueTopic?, currentSceneDescription? }`。
  - 自动素材建议：`POST /api/v1/materials/editor/auto-hints` Body `{ text, workspaceId?, limit? }`；右侧“素材建议”展示。
  - 世界引用：`WorldSelect` 修改稿件/worldId（本地状态），若生成时带 worldId 会回写稿件与大纲。
- **待完善**：
  - 场景选择与稿件版本冲突解决；
  - 草稿自动保存/提示；
  - 角色关系/成长图谱的数据缓存与图形优化。

## 标签 5：素材检索（MaterialSearchPanel）
- **对应文件**：`src/pages/Workbench/tabs/MaterialSearchPanel.tsx`
- **布局**：搜索框 + 结果列表（标题、score、片段序号、摘要）。
- **接口**：`POST /api/v1/materials/search`，Body `{ query, limit }` 默认 10；返回 `MaterialSearchResult[]`（materialId/title/snippet/score/chunkSeq）。
- **用途**：在创作中即时拉取素材，支持与素材库页面同一后端。
- **待完善**：高亮关键词、多维筛选、直接插入正文的交互。

## 开发对接指南 (Mock vs Real)

### 1. 故事构思 (StoryConception)
- **当前 Mock**：`api.stories.create` 仅返回一个静态的、预定义的故事对象，不包含真实的 AI 生成逻辑。
- **真实对接**：
  - 后端接口 `POST /api/v1/conception` 应该是一个长连接或耗时接口（可能需要 10-30秒）。
  - 前端需实现 Loading 状态或流式响应（Server-Sent Events）来展示生成进度。
  - 生成结果应包含真实的 AI 创作内容（标题、大纲、角色列表）。

### 2. 故事管理 (StoryManager)
- **当前 Mock**：数据存储在 `MOCK_STORIES` 内存数组中，刷新页面即重置。
- **真实对接**：
  - 替换为真实的 CRUD 接口。
  - 注意：删除故事时，后端应级联删除关联的大纲、角色和稿件，前端需处理好删除后的状态刷新。

### 3. 大纲工作台 (OutlineWorkbench)
- **当前 Mock**：`MOCK_OUTLINES` 结构较为简单，树形结构的拖拽排序仅在前端模拟，未持久化。
- **真实对接**：
  - **拖拽排序**：前端 `dnd-kit` 的 `onDragEnd` 事件需调用后端接口（如 `PUT /api/v1/outlines/{id}/reorder`）来更新章节/场景的 `order` 字段。
  - **AI 生成章节**：这是一个耗时操作，建议后端使用异步任务队列，前端轮询或使用 WebSocket 接收生成结果。

### 4. 小说创作 (ManuscriptWriter)
- **当前 Mock**：`TiptapEditor` 的内容仅保存在组件 State 中，未自动保存。AI 润色/扩写功能仅使用 `setTimeout` 模拟延迟并插入占位文本。
- **真实对接**：
  - **自动保存**：需实现防抖（Debounce）机制，在用户停止输入 1-2 秒后自动调用 `PUT` 接口保存正文。
  - **AI 辅助**：
    - “润色/扩写”需调用后端 LLM 接口。
    - 建议使用流式传输（Stream）将 AI 生成的文本实时打字机式地插入编辑器，提升体验。
  - **素材建议**：右侧边栏应根据当前光标所在的段落内容，实时（或按需）调用向量检索接口推荐相关素材。

### 5. 素材检索 (MaterialSearchPanel)
- **当前 Mock**：仅使用 `filter` 在内存数组中进行简单的字符串匹配。
- **真实对接**：
  - 后端应连接 Qdrant/Elasticsearch 等向量数据库。
  - 搜索结果应包含 `score`（相关度），前端可根据分数显示匹配程度。
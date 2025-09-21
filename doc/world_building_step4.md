# 世界构建 Step 4 —— 前端界面与交互设计

## 1. 目标

实现独立的世界构建前端模块，实现以下体验：
- 在全局导航中新增“世界构建”入口，与工作台分离。
- 顶栏提供新建、草稿、已发布世界的切换与管理。
- Tab 式编辑区支持模块切换、字段编辑、AI 功能调用、Tooltip 展示。
- 底部操作条体现保存状态、草稿/正式创建按钮、进度提示。
- 生成期间自动弹出进度窗口并可断点续传。

## 2. 页面结构

```
WorldBuilderPage
├─ PageHeader (标题 + 新建按钮 + 操作说明)
├─ WorldSelectorPanel (世界选择、草稿抽屉、状态标签)
├─ Divider
├─ MetadataForm (世界基础信息)
├─ Tabs(WorldModuleTabs)
│   ├─ ModuleToolbar (模块标题、AI 自动生成按钮、状态徽章)
│   └─ ModuleContent
│       └─ FieldCard * n (字段名称、textarea、tooltip、AI 优化按钮、字数提示)
├─ StickyFooterBar
│   ├─ AutoSaveIndicator
│   ├─ Button[保存草稿]
│   └─ Button[正式创建世界]
└─ GenerationProgressModal (根据需要显示)
```

## 3. 路由与导航

- 新增路由：`/worlds`
- 更新主导航（例如侧边栏或顶部菜单），新增入口。
- 若直接访问 `/worlds` 且未登录，沿用现有登录校验逻辑。

## 4. 状态管理

使用 React Query（或自定义 hooks）管理网络状态：

| Key | 数据 | 说明 |
| --- | --- | --- |
| `worlds:list` | 世界摘要列表 | 参数：`status`。用于顶部世界选择器、草稿抽屉。 |
| `worlds:detail:{id}` | 单个世界详情 | 包含模块、字段、状态。 |
| `worlds:generation:{id}` | 生成进度 | 轮询进度窗口。 |
| `worlds:prompt-metadata` | 模块定义 & Tooltip 文案 | Step 1 的文案，可在应用初始化时加载。 |

局部状态：
- 当前选中的世界 ID (`selectedWorldId`)；`undefined` 表示尚未选择。
- 未保存字段（`dirtyFields`）与自动保存计时。
- 进度窗口显示状态。

## 5. 核心组件

### 5.1 `WorldSelectorPanel`

- 包含：
  - “新建世界”按钮：调用 `POST /worlds`，成功后刷新列表、自动选中新建世界，并弹出提示填写名称。
  - “已创建世界”下拉：`Select` + 标签显示状态（`ACTIVE`/`GENERATING`）。
  - “草稿栏”按钮：打开 `Drawer`，展示草稿列表（`status=DRAFT`）。提供“继续编辑”“重命名”“删除”。
  - 世界摘要：显示主题标签、上次更新时间、当前状态徽章。
- 交互：
  - 切换世界时，检查是否有未保存更改，若有弹出确认框。

### 5.2 `MetadataForm`

- 表单字段与 Step 1 对应：名称、概述、主题标签、创作意图、备注。
- 使用 Ant Design `Form` + `Input`/`Select`/`Tag` 组件。
- 自动保存策略：
  - onBlur 或点击“保存草稿”时调用 `PUT /worlds/{id}`。
  - 提供 `AutoSaveIndicator`（例如“已保存”/“保存中”）。

### 5.3 `WorldModuleTabs`

- Tab 顺序与 Step 1 一致。
- 每个 Tab 包含：
  - 模块标题、状态标签（`EMPTY`/`IN_PROGRESS`/`READY`/`AWAITING_GENERATION`/`COMPLETED`/`FAILED`）。
  - `Tooltip`（从 Step 1 文档读取）展示模块概述。
  - 模块工具栏按钮：
    - “AI 自动生成本模块”：调用 `POST /worlds/{id}/modules/{moduleKey}/generate`。
    - “标记为需重写”（可选，后续迭代）。
    - “查看完整信息”（若 `fullContent` 存在，则显示模态对话框预览）。

### 5.4 `FieldCard`

- 结构：
  - 左侧：字段名称 + 必填星号。
  - 右侧：问号图标 `Tooltip`，显示 Step 1 提供的文案。
  - 中部：`Input.TextArea`（支持 4-8 行自动扩展）。
  - 底部：
    - 字数提示（`value.length`/建议范围）。
    - “AI 优化”按钮：调用字段优化接口。
    - 最后更新时间（可选）。
- AI 优化交互：
  - 点击按钮 -> 打开复用的 `AiRefineModal`（与工作台一致），默认指令为“让描述更具体/逻辑自洽”等；若用户填写自定义指令，则随请求传递。
  - 返回结果后用户可选择“替换”“追加”“取消”。

### 5.5 `StickyFooterBar`

- 固定在页面底部，显示：
  - 当前世界状态（如“草稿”、“生成中”、“版本 3”）。
  - 自动保存提示。
  - “保存草稿”按钮：批量提交所有 `dirty` 模块，通过 `PUT /worlds/{id}/modules`。
  - “正式创建世界”按钮：触发 `/publish` 流程。

### 5.6 `GenerationProgressModal`

- 在以下场景打开：
  - 用户点击“正式创建世界”并通过校验。
  - 重新加载时选中 `status=GENERATING` 的世界。
- 内容：

```
+-----------------------------------------------------------+
| 世界完整信息生成进度                                      |
| 世界：永昼群星   状态：生成中   已用时：02:35             |
|-----------------------------------------------------------|
| 模块              状态            操作                     |
| 宇宙观与法则      生成中          —                        |
| 地理与生态        等待中          [提升优先级]（可选）     |
| 历史与传说        已完成          [查看结果]               |
| 势力与剧情钩子    失败            [查看错误][重新排队]     |
|-----------------------------------------------------------|
| [关闭窗口] （生成完成前不可关闭 / 或提醒继续后台执行）    |
+-----------------------------------------------------------+
```

- 状态颜色：等待灰色、生成中蓝色、已生成绿色、失败红色。
- “重新排队”按钮调用 `/generation/{moduleKey}/retry`。
- 轮询：每 3 秒调用 `GET /generation`，直至全部完成或用户手动关闭（完成后自动关闭并弹出“世界已正式创建”通知）。

## 6. 交互流程

### 6.1 新建世界
1. 用户点击“新建世界”。
2. 调用 `POST /worlds` -> 返回 ID。
3. 刷新 `worlds:list`、`worlds:detail`。
4. 自动聚焦名称输入框，提示用户填写。

### 6.2 保存草稿
1. 用户在字段中输入内容，`FieldCard` 标记 `dirty`。
2. 点击“保存草稿”或离开页面前触发自动保存：将所有 `dirty` 字段合并为 `{ moduleKey: { fields: {...} } }` 调用批量接口。
3. 成功后刷新模块状态、更新 `contentHash`，展示“已保存”。

### 6.3 模块 AI 自动生成
1. 用户点击模块工具栏按钮。
2. 前端禁用按钮并展示 Loading。
3. 调用 `POST /modules/{moduleKey}/generate`。
4. 成功后刷新模块字段，弹出通知提示生成完成；失败时显示错误消息。
5. 模块状态更新为 `READY`（若字段齐全）或 `IN_PROGRESS`。

### 6.4 字段 AI 优化
1. 点击“AI 优化”按钮。
2. 弹出 `AiRefineModal`，展示原文，提供可编辑指令。
3. 调用 `/fields/{fieldKey}/refine`，显示返回结果。
4. 用户选择“替换” -> 更新文本框并标记 `dirty`；或“插入到光标位置”。

### 6.5 正式创建
1. 点击“正式创建世界”。
2. 调用 `/publish/preview`（或 `/publish` 返回校验结果）；若存在缺失字段，弹窗列出并可点击跳转。
3. 若通过校验，调用 `/publish`，成功后：
   - 切换世界状态为 `GENERATING`。
   - 打开 `GenerationProgressModal` 并启动轮询。
4. 队列完成：
   - 刷新世界详情与列表。
   - 弹出成功消息。

### 6.6 断点续传
- 应用初始化时（或进入 `/worlds` 页面）调用 `GET /worlds?status=generating`，若存在正在生成的世界，提示用户选择。
- 当用户选择 `status=GENERATING` 的世界时，立即弹出进度窗口并开始轮询。

## 7. 视觉与可用性建议

- 字段区域采用分段卡片 + 大标题，以减少视觉压力。
- Tooltip 使用 `Tooltip` + `InfoCircleOutlined` 图标，位置靠近字段标题。
- 在 `FieldCard` 顶部显示必填提示，如“必填 · 推荐 200-400 字”。
- `AI 自动生成` 按钮使用主色幽灵按钮，避免与保存按钮冲突。
- 进度窗口使用 `Steps` 或自定义表格显示状态，配合颜色标识。

## 8. 错误与提示

- API 调用失败时使用 `message.error` 提示，并保持原有内容不变。
- 若用户离开页面而存在未保存内容，使用 `beforeunload` 或自定义弹窗提醒。
- 当世界状态为 `GENERATING` 时禁用模块编辑（灰化文本域），展示提示“生成进行中，完成后可继续编辑”。

## 9. 代码组织建议

```
src/
├─ pages/
│   └─ WorldBuilder/
│       ├─ WorldBuilderPage.tsx
│       ├─ components/
│       │   ├─ WorldSelectorPanel.tsx
│       │   ├─ MetadataForm.tsx
│       │   ├─ ModuleTabs.tsx
│       │   ├─ ModuleToolbar.tsx
│       │   ├─ FieldCard.tsx
│       │   └─ GenerationProgressModal.tsx
│       └─ hooks/
│           ├─ useWorlds.ts (封装 React Query)
│           └─ useAutoSave.ts
├─ services/worlds.ts (封装 Step 2/3 API)
└─ constants/worldModuleMetadata.ts (从后端加载或内置 Step 1 数据)
```

## 10. 测试计划

- **组件测试**：
  - `FieldCard` 渲染 Tooltip、必填标记、AI 按钮。
  - `GenerationProgressModal` 状态变更与按钮禁用逻辑。
- **集成测试**：
  - 使用 Mock Service Worker 模拟 API，验证保存、AI 生成、发布流程。
- **可用性检查**：
  - 表单 Tab 切换不会丢失未保存内容。
  - 生成中状态禁止编辑、按钮禁用提示明确。
  - 草稿删除确认弹窗。

完成本阶段后，前端界面即可与 Step 2/3 的后端能力对接，下一步进入提示词配置与帮助文档设计（Step 5）。

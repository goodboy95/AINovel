# 素材库页（MaterialPage）

- **路由/文件**：`/materials`
- **对应设计稿文件**：`src/pages/Material/MaterialPage.tsx`
- **子组件目录**：`src/pages/Material/tabs/`
- **权限**：受保护；Tab 显示受 `useCanPerform('workspace:write')` 控制（无写权限仅显示“素材列表”）。

## Tab：创建素材（MaterialCreateForm）
- **对应文件**：`src/pages/Material/tabs/MaterialCreateForm.tsx`
- **布局**：表单（标题、类型、概要、正文、标签）。
- **接口**：`POST /api/v1/materials` Body `{ title, type, summary?, content, tags? }`，返回 `MaterialResponse`。成功后触发 `material:refresh` 全局事件刷新列表。

## Tab：上传文件（MaterialUpload）
- **对应文件**：`src/pages/Material/tabs/MaterialUpload.tsx`
- **布局**：Dragger 上传 TXT、按钮“开始上传”、状态 Alert、最近一次上传信息卡片。
- **流程与接口**：
  - 选文件（仅 `.txt`，不立即上传）。
  - 点击上传 → `POST /api/v1/materials/upload`（FormData `file`）。返回 `FileImportJob { id,status,... }`。
  - 轮询 `GET /api/v1/materials/upload/{jobId}` 直到 status ∈ {COMPLETED,FAILED}。
  - 完成后派发 `material:refresh`。

## Tab：素材审核（ReviewDashboard）
- **对应文件**：`src/pages/Material/tabs/ReviewDashboard.tsx`
- **布局**：左侧待审列表，右侧“原始内容”+“解析结果校对”表单，操作按钮“批准/驳回/刷新”。
- **接口**：
  - 拉取待审：`GET /api/v1/materials/review/pending`（前端函数名 `fetchPendingMaterials`）。
  - 批准：`POST /api/v1/materials/{id}/review/approve` Body `{ title, summary, tags, type, entitiesJson, reviewNotes }`。
  - 驳回：`POST /api/v1/materials/{id}/review/reject` 同上。

## Tab：素材列表（MaterialList）
- **对应文件**：`src/pages/Material/tabs/MaterialList.tsx`
- **布局**：表格（标题/类型/标签/状态/操作），抽屉查看或编辑详情；Modal 显示相似素材、引用历史。
- **接口**：
  - 列表：`GET /api/v1/materials`。
  - 详情：`GET /api/v1/materials/{id}`；更新：`PUT /api/v1/materials/{id}` Body `{ title?, type?, summary?, tags?, content?, status?, entitiesJson? }`；删除：`DELETE /api/v1/materials/{id}`。
  - 查重：`POST /api/v1/materials/find-duplicates`（空 body），返回候选对及片段相似度；合并：`POST /api/v1/materials/merge` Body `{ sourceMaterialId, targetMaterialId, mergeTags?, mergeSummaryWhenEmpty?, note? }`。
  - 引用历史：`GET /api/v1/materials/{id}/citations`，返回被引用的文档/段落。
- **事件**：监听 `material:refresh` 以自动刷新表格。

## 公用：检索与自动提示
- **检索面板**（工作台 Tab 复用）：`POST /api/v1/materials/search` Body `{ query, limit }` → `MaterialSearchResult[]`。
- **自动提示**：写作页 `useAutoSuggestions` → `POST /api/v1/materials/editor/auto-hints` Body `{ text, workspaceId?, limit? }`。

## 待完善
- 上传支持 PDF/Doc/Markdown；
- 列表筛选/排序（按状态、类型、标签、时间）；
- 合并/删除操作的权限提示与审计；
- 审核批量操作、评论记录。

## 开发对接指南 (Mock vs Real)

### 1. 文件上传与解析
- **当前 Mock**：`api.materials.upload` 仅返回一个假的 Job ID，`getUploadStatus` 使用 `setTimeout` 模拟进度从 0% 到 100%。
- **真实对接**：
  - **上传**：前端需使用 `FormData` 封装文件，调用后端接口。后端应使用 Apache Tika 等工具解析文本。
  - **异步处理**：由于大文件解析和向量化耗时较长，后端应返回 Job ID。前端需实现真实的轮询逻辑（建议间隔 1-2秒），直到状态变为 `COMPLETED` 或 `FAILED`。

### 2. 向量检索
- **当前 Mock**：`api.materials.search` 仅做简单的字符串包含匹配。
- **真实对接**：
  - 后端应实现混合检索（Hybrid Search）：结合关键词匹配（BM25）和向量相似度（Cosine Similarity）。
  - 前端展示结果时，应利用后端返回的 `score` 字段来排序或显示置信度。

### 3. 审核流程
- **当前 Mock**：审核操作仅修改内存中对象的 `status` 字段。
- **真实对接**：
  - 批准操作可能触发“建立索引”的动作（将素材写入向量数据库），这可能也是一个耗时操作，需注意 UI 的 Loading 反馈。
# 响应式改造 Step 2 —— 工作台框架与导航体系

## 1. 背景与目标

完成 Step 1 后，基础页面已经适配移动端，但核心的「工作台」仍沿用桌面多列布局，导致在平板与手机上无法正常阅读和操作。本阶段聚焦于工作台的全局框架：头部导航、标签切换、内容容器与全局操作区域，确保在不同断点下均能获得一致、可用的体验。

### 1.1 目标拆解

- 重构 `Workbench` 布局，移除固定列宽，采用 `Flex` + 自适应容器控制。
- 统一头部导航交互，桌面保留现有信息密度，移动端提供抽屉式导航与快捷操作入口。
- 为后续模块（故事构思、大纲、小说创作等）提供标准化的内容容器与滚动策略。
- 调整 Tabs/Segmented 组件的样式与交互，支持横向滑动与指示条。
- 输出复用组件（如 `MobileDrawerNav`、`WorkbenchHeader`）与设计规范，供 Step 3-5 直接调用。

## 2. 范围界定

| 分类 | 具体内容 | 说明 |
| --- | --- | --- |
| 核心文件 | `components/Workbench.tsx`、`components/WorkbenchHeader.tsx`、`components/Layout/AppContent.tsx` *(新增)* | 重新组织布局结构 |
| 通用组件 | `components/navigation/MobileDrawerNav.tsx` *(新增)*、`components/navigation/PrimaryActionBar.tsx` *(新增)* | 移动端导航与全局操作区域 |
| 样式资源 | `src/styles/workbench.css` *(新增)*、现有模块相关 CSS/LESS | 定义头部高度、阴影、过渡效果 |
| 状态管理 | 若存在上下文/Store，需确保在抽屉切换时状态保留 | 避免导航切换导致数据丢失 |

## 3. 体验设计

### 3.1 头部结构

- **PC (`lg` 及以上)**：左侧展示 Logo + 产品名，中间为主导航（Tabs 或按钮组），右侧为用户信息、通知、全局操作按钮。
- **Tablet (`md`)**：
  - Logo 与产品名缩小，主导航切换为紧凑型 Tabs（`size="small"`）。
  - 用户信息折叠为 Avatar 图标，通知按钮隐藏至更多操作菜单。
- **Mobile (`xs/sm`)**：
  - 头部高度 56px，左侧提供返回首页/菜单按钮，中间为当前模块标题，右侧为关键操作（如「生成」入口）。
  - 主导航移动至底部 `MobileDrawerNav`，使用 Segmented 或列表形式展示模块列表。

### 3.2 内容区域

- **容器策略**：
  - `AppContent` 组件包裹实际模块内容，提供统一的 `padding` 与滚动行为。
  - 在 `md` 以下，`AppContent` 允许内部模块占满宽度，并开启 `overflow-y: auto`。
- **滚动管理**：
  - 头部与底部操作栏固定时，内容区需减去相应高度，使用 CSS 变量（如 `--app-header-height`）。
  - 移动端在 Drawer 打开时禁止背景滚动，避免穿透。

### 3.3 Tabs / Segmented 行为

- `lg` 以上沿用 Antd Tabs，设置 `tabBarGutter={16}`、`type="card"` 或 `"line"` 按设计确定。
- `md` 以下切换为 `Segmented`，支持横向滑动与图标标签。
- Tabs 状态保存在 URL query 或 Context，确保不同设备间切换时不丢失位置。

### 3.4 全局操作区域

- **PC**：放置于头部右侧，包含「生成内容」「保存草稿」「预览」等按钮。
- **Mobile**：引入 `PrimaryActionBar`，固定于底部，展示 1-3 个主操作按钮，宽度占满屏幕，支持安全区（`env(safe-area-inset-bottom)`）。
- Action Bar 与内容区通过 `AppContent` 的 `padding-bottom` 留出空间，避免遮挡。

## 4. 技术方案

### 4.1 布局重构

1. `Workbench.tsx`
   - 使用 `Layout` + `AppShell` 包裹。
   - 将原有 `Row/Col` 主体替换为 `Flex` 容器，针对不同断点调整 `gap` 和方向。
   - 提供 `renderContent()` slots，供 Step 3-5 的模块传入内容。
2. `WorkbenchHeader.tsx`
   - 引入 `useResponsive` 判断当前断点。
   - 根据断点切换渲染：Tabs / Segmented / Drawer 触发器。
   - 头部高度与背景采用 CSS 变量，支持渐变或阴影效果。
3. `MobileDrawerNav`
   - 抽象导航项数据结构：`{ key, icon, label, badge?, path }`。
   - Drawer 内容支持二级导航（如故事管理下的子页面），使用 `Collapse`。
   - 支持「最近访问」功能（可选），调用本地缓存。

### 4.2 状态与路由

- 保持当前使用的路由逻辑，通过 `useLocation` / `useNavigate` 控制导航跳转。
- Drawer 中选中项应高亮，与当前路由同步。
- 底部操作栏根据当前模块动态渲染（例如小说创作显示「生成」「保存」，故事管理显示「新增故事」）。可通过 context 或 props 注入。

### 4.3 样式与动画

- 创建 `workbench.css` 维护头部、Drawer、Action Bar 的动画与阴影。
- 使用 CSS 变量 `--app-header-height`、`--app-action-bar-height`，在不同断点赋值，供 `AppContent` 计算剩余高度。
- Drawer 出现/消失使用 Antd `Drawer` 默认过渡，额外加上背景渐隐效果。

## 5. 实施步骤与排期建议

1. **需求与设计确认 (0.5 人日)**：与产品/设计对齐移动端头部与导航交互稿。
2. **基础结构搭建 (1 人日)**：实现 `AppContent`、`MobileDrawerNav`、`PrimaryActionBar` 等新组件及样式。
3. **Workbench 重构 (1.5 人日)**：
   - 重写 `Workbench.tsx` 布局与头部逻辑。
   - 调整 Tabs/Segmented 状态同步。
4. **联调与回归 (0.5 人日)**：
   - 验证在不同断点下导航、Drawer、操作栏显示正确。
   - 确保滚动区域高度计算无异常。

## 6. 风险与应对

| 风险 | 描述 | 缓解措施 |
| --- | --- | --- |
| Drawer 动画造成性能问题 | 移动端设备性能有限，复杂动画可能卡顿 | 使用 CSS transform + opacity 动画，减少阴影模糊；必要时关闭次要动效 |
| 底部操作栏遮挡输入框 | 在手机上打开键盘可能挤压布局 | 利用 `safe-area` 与键盘事件监听，根据输入状态隐藏或收起操作栏 |
| Tabs 状态同步复杂 | 多入口导致当前激活标签不准确 | 统一由路由 query 控制当前 tab，组件只读解析 |

## 7. 验收标准与测试计划

1. **功能验收**
   - 不同断点下头部与导航显示正确，Drawer 可正常打开/关闭。
   - 底部操作栏在移动端可正常触达且不遮挡内容。
   - 切换标签后，内容滚动位置保留或按需求重置，无异常跳动。
2. **测试清单**
   - 单元测试：`WorkbenchHeader` 在不同断点渲染对应组件；`MobileDrawerNav` 导航项点击触发正确路径。
   - 手动测试：Chrome DevTools 设备模拟 + 真机验证（iPhone 12、Pixel 6、iPad Mini）。
   - 无障碍检查：Drawer 可通过键盘导航，焦点环清晰。

完成 Step 2 后，工作台骨架具备良好伸缩性，可继续推进具体模块改造（Step 3-5）。

# AINovel 前端应用

基于 React + TypeScript + Vite 构建的单页应用，提供故事构思、大纲管理、稿件创作与世界观工作台等完整创作体验。

## 技术概览

- **构建工具**：Vite
- **UI 框架**：Ant Design + 自定义组件
- **状态管理**：React Context（认证）+ 自定义 hooks（故事、大纲、世界）
- **网络层**：`fetch` + 集中封装的 `services/api.ts`，统一处理 JWT 与错误
- **类型**：`src/types.ts` 与后端 DTO 对齐

## 目录结构

```
frontend/src
├── App.tsx                # 路由与受保护页面
├── components/            # 故事工作台、设置页、弹窗等模块化组件
├── contexts/AuthContext.tsx  # 登录状态与 Token 校验
├── hooks/                 # useStoryData、useOutlineData 等数据获取逻辑
├── pages/WorldBuilder/    # 世界观工作台页面与子组件
├── services/api.ts        # 所有 REST API 封装
├── types.ts               # 共享类型定义
└── assets/、styles        # 静态资源与样式
```

- `Workbench.tsx` 负责故事构思、大纲、稿件标签页切换与弹窗管理。【F:frontend/src/components/Workbench.tsx†L1-L120】
- `WorldBuilderPage.tsx` 实现世界列表、模块编辑、自动保存与生成进度轮询。【F:frontend/src/pages/WorldBuilder/WorldBuilderPage.tsx†L1-L210】
- `services/api.ts` 集中维护所有接口调用，自动附加 `Authorization` 头并兼容 204/205 响应。【F:frontend/src/services/api.ts†L21-L90】

## 启动与构建

```bash
npm install
npm run dev       # 5173 端口，带后端代理
npm run build     # 生成 dist/ 产物
npm run preview   # 预览生产构建
```

开发服务器会将 `/api` 与 `/ws` 请求代理到 `http://localhost:8080`，请确保后端已启动或调整 `vite.config.ts`。【F:frontend/vite.config.ts†L6-L19】

## 必备环境变量

前端默认使用浏览器本地存储保存 JWT，不需要额外 `.env`。如需覆盖后端地址，可在 `vite.config.ts` 中修改代理或使用 `VITE_API_BASE`（需额外实现）。

## 与后端交互

- 所有 API 调用位于 `services/api.ts`，集中导出故事、大纲、稿件、世界观、Prompt 模板等函数。若新增接口请保持同一文件维护，方便错误处理复用。【F:frontend/src/services/api.ts†L1-L200】
- `AuthContext` 在应用初始化时调用 `/api/auth/validate` 校验 Token，未通过验证会自动清除本地 Token 并跳转登录。【F:frontend/src/contexts/AuthContext.tsx†L17-L48】
- 世界工作台通过 `fetchWorldBuildingDefinitions` 拉取字段配置，再使用 `updateWorldModules`、`generateWorldModule`、`runWorldGenerationModule` 等接口管理世界生成流程。【F:frontend/src/pages/WorldBuilder/WorldBuilderPage.tsx†L1-L240】

## 开发提示

1. **认证保护**：新页面若需登录访问，应包裹在 `ProtectedRoute` 中并在 `App.tsx` 配置路由。【F:frontend/src/App.tsx†L15-L55】
2. **统一错误提示**：API 异常默认抛出 `Error`，各组件可结合 Ant Design `message` 或 `Alert` 反馈用户。
3. **世界模块自动保存**：`WorldBuilderPage` 使用 2 秒防抖的自动保存逻辑（`AUTO_SAVE_DELAY`），修改逻辑时注意清理计时器避免内存泄漏。【F:frontend/src/pages/WorldBuilder/WorldBuilderPage.tsx†L24-L120】
4. **类型同步**：新增/修改后端 DTO 时，请同步更新 `src/types.ts`，否则 TypeScript 编译会提示缺失字段。
5. **登录流程**：`Login` 组件调用 `/api/v1/auth/login` 获取 Token 并交由 `AuthContext` 存储；注销使用 `logout()` 清除本地 Token。

## 调试建议

- 使用浏览器网络面板确认请求是否携带 `Authorization` 头。
- 遇到 `CORS` 错误时检查后端 `cors.allowed-origins` 是否包含当前前端地址。
- 世界生成卡住时，可打开“生成进度”弹窗观察模块状态并尝试重试；失败原因会通过后端响应的 `error` 字段返回。

## 文档与设计

更多系统级说明请参考仓库 `doc/architecture.md` 与 `doc/features.md`。新增页面或交互时，请同步更新相关文档并在 PR 中说明。

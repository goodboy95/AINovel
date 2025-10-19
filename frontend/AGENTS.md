# Agent 指南（前端）

## 快速了解
- React + TypeScript + Vite，UI 基于 Ant Design。
- 所有 API 调用集中在 `src/services/api.ts`，通过本地存储的 JWT 添加 `Authorization`。
- 认证上下文位于 `src/contexts/AuthContext.tsx`，`ProtectedRoute` 负责路由级访问控制。

## 常用命令
```bash
npm install
npm run dev        # 启动开发服务器（含 /api 代理）
npm run build      # 生产构建
npm run preview    # 预览 dist 输出
```

## 开发准则
- 新增页面请在 `App.tsx` 注册路由，并根据是否需要认证包裹在 `ProtectedRoute` 内。【F:frontend/src/App.tsx†L15-L55】
- API 封装统一维护在 `services/api.ts`，新增接口时遵循现有 `handleResponse` 约定，并在 `types.ts` 中补充类型定义。【F:frontend/src/services/api.ts†L21-L90】
- 表单/弹窗优先复用 Ant Design 组件，保持一致的交互体验。
- 世界观相关逻辑（自动保存、生成进度）集中在 `pages/WorldBuilder/`，修改时注意与后端流程保持一致。
- UI 状态和数据请求逻辑拆分：页面组件负责布局，自定义 hooks (`useStoryData`, `useOutlineData` 等) 负责数据流。【F:frontend/src/hooks/useStoryData.ts†L1-L116】

## 代码风格
- 使用函数式组件与 Hooks，避免类组件。
- 启用 TypeScript 严格模式，避免 `any`；如需扩展类型请更新 `types.ts`。
- CSS 优先使用模块化或 Ant Design 的样式方案，保持样式命名一致。

## 质量保障
- 若引入新的第三方库，请在 `frontend/README.md` 与 `package.json` 描述用途。
- 重要视图改动需在 `doc/features.md` 或设计文档中补充说明。
- 确保登录/登出流程仍可用（检查 Token 存取与 `AuthContext` 逻辑）。

## 与后端协作
- 新增或变更接口后，请同步更新 `doc/getting_started.md`、`doc/features.md` 中的说明，并告知后端接口变更。
- 注意与后端 `cors.allowed-origins` 设置匹配；如切换端口，请同步更新 `vite.config.ts` 的代理配置。【F:frontend/vite.config.ts†L6-L19】

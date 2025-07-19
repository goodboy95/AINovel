# 项目目录结构说明

- `frontend/`：React 18 + Vite 前端代码，包含页面、组件、样式与构建配置。
  - `src/pages/`：首页、登录注册、工作台、素材库、世界构建、设置及帮助页。
  - `src/contexts/AuthContext.tsx`：全局认证状态管理。
  - `src/lib/api.ts`：与后端交互的统一请求封装。
  - `Dockerfile`、`nginx.conf`：前端构建与部署镜像配置。
- `backend/`：Spring Boot 3 后端代码，提供认证、故事、素材、世界观、设置等 REST API。
  - `src/main/java/com/ainovel/app/`：入口与各业务模块（security、user、story、material、world、settings、manuscript）。
  - `src/main/resources/application.yml`：默认配置（可通过环境变量覆盖）。
  - `Dockerfile`：后端构建与运行镜像配置。
- `design/`：原型设计参考文件与文档。
- `sql/schema.sql`：数据库表结构参考脚本。
- `docker-compose.yml`：一键启动 MySQL、Redis、后端、前端(Nginx) 的编排文件。
- `build.sh`：本地/CI 一键构建前后端并启动容器的脚本。
- `doc/api/`：各 Controller 对应的接口说明文档。
- `doc/modules/`：功能模块说明。
- `doc/test/`：测试用例与操作步骤文档。
- `AGENTS.md`：任务与交付规范。

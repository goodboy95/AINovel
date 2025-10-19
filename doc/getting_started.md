# 快速上手指南

本文帮助开发者在本地环境搭建并运行 AINovel 项目，涵盖依赖安装、配置、安全注意事项与常见问题排查。

## 1. 环境准备

| 组件 | 推荐版本 | 说明 |
| --- | --- | --- |
| JDK | 21 | Spring Boot `pom.xml` 指定 Java 21。【F:backend/pom.xml†L16-L21】|
| Maven | 3.9+ | 仓库提供 `mvnw` 脚本，建议直接使用。|
| Node.js | 18 LTS / 20 LTS | Vite + React 构建。|
| npm | 9+ | 与 Node 同步安装。|
| MySQL | 8.0+ | 默认 JDBC URL 指向 `jdbc:mysql://localhost:3306/ainovel`。【F:backend/src/main/resources/application.properties†L5-L12】|

> **提示**：若使用容器或远程数据库，请修改 `spring.datasource.*` 并设置 `cors.allowed-origins` 指向真实前端地址。

## 2. 数据库初始化

1. 创建数据库：`CREATE DATABASE ainovel CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`
2. 创建用户并授权（示例）：
   ```sql
   CREATE USER 'ainovel'@'%' IDENTIFIED BY '强密码';
   GRANT ALL PRIVILEGES ON ainovel.* TO 'ainovel'@'%';
   FLUSH PRIVILEGES;
   ```
3. 在 `application.properties` 或环境变量中更新 `spring.datasource.username/password`，避免使用仓库内的演示凭据。【F:backend/src/main/resources/application.properties†L5-L13】
4. 默认使用 `spring.jpa.hibernate.ddl-auto=update` 自动建表，首次启动会自动生成实体表结构。生产环境建议改为 `validate` 并引入迁移工具。

## 3. 配置敏感信息

| 配置项 | 位置 | 作用 |
| --- | --- | --- |
| `app.encryption.key` | `application.properties` 或环境变量 | AES 密钥（16/24/32 字节）。用于加密 API Key，部署时务必替换默认值。【F:backend/src/main/java/com/example/ainovel/service/EncryptionService.java†L9-L24】|
| `app.jwt.secret` | `application.properties` | JWT 签名密钥，生产环境需随机化。|
| `openai.model.default` | `application.properties` | 默认模型名称，可覆盖。|
| `proxy.*` | `application.properties` | `RestTemplate` SOCKS 代理配置。若无需代理，设置 `proxy.enabled=false`。【F:backend/src/main/java/com/example/ainovel/config/RestTemplateConfig.java†L15-L44】|
| `cors.allowed-origins` | `application*.properties` | 跨域白名单。开发环境 `application-dev.properties` 已包含 Vite 默认端口。【F:backend/src/main/resources/application-dev.properties†L4-L10】|

可以通过环境变量覆盖：`SPRING_DATASOURCE_URL`、`APP_JWT_SECRET` 等，或在部署环境使用 Spring Cloud Config。

## 4. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

默认端口 8080，激活开发配置可在 `application.properties` 中设置 `spring.profiles.active=dev` 或在运行命令添加 `-Dspring.profiles.active=dev`。首次启动会：

- 检查数据库连接并创建表结构。
- 读取 `prompts/` 与 `worldbuilding/` 中的默认模板与模块定义。
- 注册 REST API，包括认证、故事、大纲、稿件、世界工作台等端点。

若需要生成可执行 jar：`./mvnw clean package`，产物位于 `target/`。

## 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

- 默认开发端口 5173，`vite.config.ts` 代理 `/api` 到 `http://localhost:8080`。【F:frontend/vite.config.ts†L6-L19】
- 访问 `http://localhost:5173`，首次进入会跳转登录页。
- 生产构建使用 `npm run build`，输出位于 `frontend/dist`，可由任意静态服务器托管；若需要与后端整合，可将 `dist` 内容复制到 `backend/src/main/resources/static/`。

## 6. 初始化账户

后端未内置默认用户，请通过以下方式之一创建：

1. 调用注册接口：
   ```bash
   curl -X POST http://localhost:8080/api/v1/auth/register \
     -H 'Content-Type: application/json' \
     -d '{"username":"demo","password":"StrongPass123"}'
   ```
2. 或直接插入数据库（不推荐，密码需使用 BCrypt 生成）。

注册后使用 `/api/v1/auth/login` 获取 JWT，并在前端登录页输入账号密码。

## 7. 必备操作流程

1. 登录后进入设置页，填写 AI Provider（默认 OpenAI）所需的 API Key、Base URL、模型名称，并点击“测试连接”确保后端可调用 LLM。【F:backend/src/main/java/com/example/ainovel/controller/SettingsController.java†L37-L50】
2. 在“故事工作台”通过构思表单生成故事与角色，或手动创建。
3. 在“大纲工作台”按章节调用 AI 生成细纲，再进入“稿件创作”生成正文、分析角色变化。
4. 在“世界工作台”创建世界设定，编辑模块后执行“生成完整稿件”，跟踪进度并发布。

## 8. 常见问题排查

| 症状 | 可能原因 | 排查建议 |
| --- | --- | --- |
| 登录后立即被登出 | JWT 秘钥不一致或 `Authorization` 头缺失 | 检查 `app.jwt.secret` 是否被覆盖；浏览器开发者工具查看请求头。|
| 调用 AI 接口失败 | API Key 配置错误、代理不可达或模型名称无效 | 查看后端日志中 `OpenAiService` 抛出的异常，确认 `settings/test` 返回成功。|
| 世界生成卡住 | 某模块任务状态为 `FAILED` | 前端进度弹窗中选择重试，或调用 `/api/v1/worlds/{id}/generation/{module}/retry`。同时检查数据库 `world_generation_job` 表。|
| 前端 404 | 直接刷新嵌套路由时未命中静态资源 | 确认后端 `WebConfig` 的 SPA 转发规则仍存在，或在生产环境使用前端服务器处理路由。【F:backend/src/main/java/com/example/ainovel/config/WebConfig.java†L24-L39】|
| CORS 报错 | 跨域白名单未包含前端地址 | 在 `application-dev.properties` 或运行参数中添加真实前端地址。|

## 9. 推荐开发流程

1. 在独立终端运行后端 `spring-boot:run` 和前端 `npm run dev`，保持热加载。
2. 使用 `npm run lint`（若已配置）保证前端代码风格，后端使用 `./mvnw test` 执行单元测试。
3. 提交前确保文档同步更新，特别是新增接口、配置项或前端模块时补充说明。

遵循上述步骤即可在本地快速搭建、配置并体验 AINovel 的完整功能链路。

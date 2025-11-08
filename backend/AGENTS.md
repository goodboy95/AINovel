# Agent 指南（后端）

## 快速了解
- Java 21 + Spring Boot 3.3，使用 Lombok、Spring Data JPA 与 Spring Security。
- 所有外部 API（OpenAI 等）通过 `service` 层调用，RestTemplate 已支持可选代理。
- 业务域划分：认证、故事/角色、大纲、稿件、世界观、Prompt 模板。

## 常用命令
```bash
./mvnw spring-boot:run           # 本地运行
./mvnw test                      # 执行测试
./mvnw clean package             # 构建 jar
```

## 开发准则
- 遵循 Controller → Service → Repository 分层，不在控制器内书写复杂业务逻辑。
- DTO 放在 `dto/`，实体在 `model/`，必要时为新接口创建 DTO，避免直接暴露实体。
- 编写新服务时优先使用构造函数注入，复用现有日志与异常处理模式。
- 需要持久化新配置时，请在 `application*.properties` 中添加默认值，并更新 `doc/getting_started.md` 与 `doc/features.md`。
- 修改或新增 REST 接口后，请同步更新 Swagger/文档（目前以手工文档为主）。
- 前端开发联调时请参考 `backend/apidoc/` 中的接口文档；后端增删改接口时务必同步维护该目录内容。

## 安全与配置
- 生产环境务必覆盖 `application.properties` 中的示例密钥与数据库凭据。
- 处理 AI 调用时要考虑失败重试与超时；如有长耗时流程，优先复用或扩展 `WorldGenerationWorkflowService` 的任务模型。

## 代码风格
- 统一使用 `@RestController` + 明确的 `@RequestMapping` 路径，保持 `/api/v1/...` 前缀。
- JSON 序列化依赖 Jackson，时间类型使用 `java.time.*`。
- 不要把敏感数据（API Key、密码）直接返回给客户端。

## 质量保障
- 若引入新的库或配置，请在 `backend/README.md` 追加说明。
- 重要改动应添加或更新单元测试 / 集成测试。
- 变更世界观或 Prompt 模板时，同步检查 `src/main/resources/prompts/` 与 `worldbuilding/` 内容是否需要更新。

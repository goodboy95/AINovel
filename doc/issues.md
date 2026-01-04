# 已解决事项（2026-01-03）
- 补齐 `deploy/nginx/ainovel.conf` 与 `deploy/nginx/ainovel_prod.conf`，`build.sh --init` 与 `build_prod.sh --init` 可安装宿主机 Nginx 配置。
- `build_prod.sh`/`build.sh` 修复为显式 `bash` 调用子脚本，避免因脚本未加可执行位导致 `Permission denied`。
- 后端新增邮件验证码注册（SMTP）、AI Copilot（OpenAI 兼容）、积分/签到/兑换码、后台管理接口，并已接入前端 `src/lib/mock-api.ts` 作为真实 API 适配层。
- `docker-compose.yml` 已注入 `SMTPandLLM.txt` 的 SMTP 与大模型配置，`build_prod.sh --init` 已成功签发并部署 `ainovel.aienie.com` 证书（有效期至 2026-04-03）。

# 待处理事项（2026-01-03）
- Playwright MCP 当前提示 `Browser is already in use`，无法执行浏览器端到端自动化测试（已改用接口级回归测试验证核心功能）。建议待浏览器资源释放后再次运行 Playwright 测试，目标地址：`https://ainovel.aienie.com/`。

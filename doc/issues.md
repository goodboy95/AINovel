# 已解决事项（2026-01-03）
- Playwright MCP 已可正常访问 `http://ainovel.seekerhut.com`，页面可加载并完成工作台跳转。
- `sudo` 可用，`build.sh --init` 已成功完成 hosts 与 Nginx 配置。
- MySQL 数据目录已切换为 `deploy/mysql/data` → `/home/duwei/ainovel-mysql-data` 的软链接，避免 Windows 挂载目录权限导致的证书/权限报错。
- `build.sh` 在非交互 sudo 环境下改用 runuser/su 切回原用户，避免 `sudo -u` 触发密码/TTY 阻塞。

# 待处理事项（2026-01-03）
- 暂无。

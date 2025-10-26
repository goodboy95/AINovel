# AI Novel 素材库开发总体计划

本文档是“小说素材库”模块的总体开发计划，旨在将 [《小说素材库功能设计方案》](./material_base_instruction.md) 转化为一套可执行、分阶段的开发任务。

## 1. 项目目标

为 AI Novel 平台增加一个强大的“素材库”模块，实现素材的统一管理、智能解析和在写作过程中的无缝检索，从而显著提升作者的创作效率和作品的连贯性。

## 2. 开发阶段划分

为确保项目平稳、有序地进行，我们将开发工作划分为三个主要阶段。每个阶段都是前一阶段的增强，可以独立开发、测试和交付。

*   **[第一阶段：MVP（最小可行产品）](./material_dev_plan_mvp.md)**
    *   **核心目标：** 跑通核心流程，实现手动录入和基础的向量检索功能。
    *   **主要产出：** 用户可以手动创建素材，上传TXT文件进行自动解析入库，并在写作时通过一个手动搜索面板来检索素材。

*   **[第二阶段：V1（功能完善）](./material_dev_plan_v1.md)**
    *   **核心目标：** 提升检索质量和自动化水平。
    *   **主要产出：** 引入混合检索（向量+全文），利用大语言模型（LLM）进行结构化信息抽取，并实现写作时的自动素材推荐。

*   **[第三阶段：V2（高级功能）](./material_dev_plan_v2.md)**
    *   **核心目标：** 增强系统的健壮性、扩展性和精细化管理能力。
    *   **主要产出：** 完善多租户与权限控制（ACL），实现素材的去重与合并，并建立初步的质量评估体系。

## 3. 环境准备与技术栈确认

在开始开发前，请确保本地开发环境已准备就绪。

### 3.1. 核心技术栈

*   **后端:** Spring Boot 3.x, Spring AI 1.0.3, Java 17+
*   **前端:** React, TypeScript, Vite, Tailwind CSS
*   **数据库:** MySQL 8.0
*   **向量库:** Qdrant (v1.7.x 或更高版本)
*   **文件处理:** Apache Tika

### 3.2. 本地环境启动

**1. 启动 Qdrant 向量数据库**

建议使用 Docker 启动 Qdrant 服务，这是最快捷的方式。

```bash
# 拉取 Qdrant 镜像
docker pull qdrant/qdrant

# 启动 Qdrant 容器
docker run -p 6333:6333 -p 6334:6334 \
    -v $(pwd)/qdrant_storage:/qdrant/storage \
    qdrant/qdrant
```
*   `6333` 是 gRPC 端口。
*   `6334` 是 REST API 端口。
*   访问 `http://localhost:6333/dashboard` 可以查看 Qdrant 的 Web UI。

**2. 启动 MySQL 数据库**

请确保本地 MySQL 8.0 服务已启动，并创建一个新的数据库（例如 `ainovel_material`）供本项目使用。

**3. 启动后端服务**

参照项目根目录的 `README.md` 文件，启动后端 Spring Boot 应用。

**4. 启动前端应用**

参照项目根目录的 `README.md` 文件，启动前端 React 应用。

## 4. 源码管理

*   建议为“素材库”功能创建一个新的 `feature/material-library` 分支。
*   每个阶段的开发任务完成后，可以合并到主开发分支。

## 5. 开发计划导航

请根据当前开发进度，点击下方链接进入对应阶段的详细开发方案：

*   **➡️ [第一阶段开发方案：MVP](./material_dev_plan_mvp.md)**
*   **➡️ [第二阶段开发方案：V1](./material_dev_plan_v1.md)**
*   **➡️ [第三阶段开发方案：V2](./material_dev_plan_v2.md)**

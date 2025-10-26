# 小说素材库功能设计方案（Spring Boot + Spring AI 1.0.3 + MySQL 8.0 + Qdrant）

> 目标：为 AI 小说网站提供一个统一、可扩展的“小说素材库”，既能**手动录入单条素材**，也能**批量上传文件由 AI 自动解析并入库**，并在**写作过程中按 AI 推断的关键字自动检索素材**，实现高质量、低延迟、可审计的 RAG（Retrieval-Augmented Generation）能力。

---

## 1. 范围与目标
- **范围**
  - 素材类型：角色、人设、关系、地点、组织、物品/道具、事件、设定/世界观、语气/文风、片段/桥段、灵感卡等。
  - 输入：
    - 手工表单录入单条素材。
    - 上传素材合集（txt/md/docx/pdf/zip/图片扫描等），自动抽取结构化素材与可检索文本块。
  - 输出：
    - 写作编辑器中实时检索建议（AI 自动触发）与手动搜索。
    - 资料卡/引用卡片（带来源与置信度）。
- **非目标（本期不做）**
  - 跨语言机器翻译、自动版权清洗、复杂本体推理。

---

## 2. 角色与核心用例
- **作者**：录入/上传素材；在写作时自动获得相关素材建议；一键插入引用。
- **编辑**：审核批量解析结果；合并重复、修正标签、设定访问权限。
- **系统**：自动切片、向量化、索引；提供低延迟检索；记录检索与引用行为用于评估。

**关键用例**
1. **手动录入单条素材**：填写表单 → 校验 → 入 MySQL（结构化）+ 入 Qdrant（向量）
2. **上传素材合集**：文件入库 → 文本抽取/切片 → LLM 结构化解析 → 去重/合并 → 向量化 → MySQL/Qdrant 落库
3. **写作时自动检索**：编辑器文本流→ Query Intent & 关键词推断 → Hybrid 检索（Qdrant + MySQL FULLTEXT）→ 召回→ 交叉重排→ 展示卡片→ 插入引用

---

## 3. 总体架构
```
[前端] 编辑器/素材管理台
   │
   ├─(REST/WS)→ [素材服务]  Spring Boot + Spring AI
   │              ├─ 解析管道（Tika/PDF/Docx/OCR）
   │              ├─ LLM 结构化抽取/标注（Spring AI Function Calling）
   │              ├─ 向量化（EmbeddingClient）
   │              ├─ 检索路由（Hybrid + 重排）
   │              └─ MCP 工具调用层
   │
   ├─ MySQL 8.0（结构化数据、作业、权限、审计、FULLTEXT）
   └─ Qdrant（向量检索，Named Vectors，多租户过滤）
```

---

## 4. 关键技术选型
- **后端框架**：Spring Boot（REST/WS、Security、Batch/Quartz）、Spring AI 1.0.3（EmbeddingClient、ChatClient、VectorStore 抽象）。
- **数据库**：MySQL 8.0（InnoDB、FULLTEXT + `WITH PARSER ngram` 适配中文、事务与审计）。
- **向量库**：Qdrant（命名向量、过滤、Payload 索引、分片/副本、HNSW）。
- **文件处理**：Apache Tika（txt/md/docx/pdf），PDFBox（补充）、Tesseract/SwiftOCR（图片/PDF OCR，可异步）。
- **重排模型**：Cross-Encoder（如 bge-reranker / e5-reranker，本地或 API）。
- **消息与任务**：Spring Batch/Quartz（导入作业）、异步事件（ApplicationEvent）。

---

## 5. 数据模型（MySQL）
> 以**结构化实体** + **非结构化块**并存；以**多租户/工作区**隔离。

### 5.1 核心表（建议部分字段）
- `workspace`：`id`, `name`, `owner_id`, `created_at`
- `user`：`id`, `email`, `display_name`, `role`
- `material`（素材主表，结构化）：
  - `id (PK)`, `workspace_id`, `title`, `type`（enum：character/place/item/event/lore/style/fragment/idea...）
  - `summary`（短摘要）、`content`（原文/长说明）
  - `source_id`（文件/URL）、`status`（draft/published/archived）
  - `tags`（逗号或 JSON）、`entities_json`（结构化解析结果）
  - `created_by`, `updated_by`, `created_at`, `updated_at`
- `material_chunk`（可检索文本块）：
  - `id (PK)`, `material_id`, `seq`, `text`, `hash`（去重）、`token_count`
  - `title_path`（层级标题路径）、`section_type`（head/body/quote）
- `file_import_job`：`id`, `workspace_id`, `uploader_id`, `status`, `file_path`, `mime`, `error`, `created_at`, `finished_at`
- `file_import_item`：`id`, `job_id`, `material_id`, `status`, `error`
- `permission`（ACL）：`id`, `resource_type`, `resource_id`, `subject_id`, `scope`（read/write/admin）
- `search_log`：`id`, `workspace_id`, `user_id`, `query`, `mode`（auto/manual），`latency_ms`, `used_in_generation`（bool）
- `citation`：`id`, `doc_id`（写作文稿）, `chunk_id`, `position`, `score`

> **FULLTEXT**：在 `material(title, summary, content)` 与 `material_chunk(text)` 上建立 FULLTEXT 索引，并使用 `WITH PARSER ngram`（中文场景）。

### 5.2 结构化 JSON Schema（LLM 抽取）
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "NovelMaterial",
  "type": "object",
  "properties": {
    "type": {"type": "string", "enum": ["character", "place", "item", "event", "lore", "style", "fragment", "idea"]},
    "title": {"type": "string"},
    "aliases": {"type": "array", "items": {"type": "string"}},
    "summary": {"type": "string"},
    "tags": {"type": "array", "items": {"type": "string"}},
    "character": {
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "age": {"type": "string"},
        "traits": {"type": "array", "items": {"type": "string"}},
        "relations": {"type": "array", "items": {"type": "object", "properties": {"to": {"type": "string"}, "relation": {"type": "string"}}}},
        "backstory": {"type": "string"}
      }
    },
    "place": {"type": "object", "properties": {"name": {"type": "string"}, "era": {"type": "string"}, "geo": {"type": "string"}}},
    "event": {"type": "object", "properties": {"time": {"type": "string"}, "participants": {"type": "array", "items": {"type": "string"}}, "outcome": {"type": "string"}}}
  },
  "required": ["type", "title"]
}
```

---

## 6. Qdrant 设计
### 6.1 Collection 与命名向量
- **Collection：`materials`**（单集合，多命名向量）
  - 命名向量：
    - `semantic`（主向量，句向量）
    - `title`（标题向量，短文本）
    - `keywords`（关键词/标签向量，可选）
  - **度量**：`Cosine`
  - **向量维度**：与所选模型一致（例如 1024 / 1536 / 3072）。
  - **Payload（过滤字段）**：
    - `workspace_id`（多租户隔离，必备）
    - `material_id`, `chunk_id`, `type`, `tags`（数组）
    - `source`（file/url）、`status`、`created_at`
    - `title_path`, `section_type`
  - **Payload 索引**：对 `workspace_id`, `type`, `tags` 建立索引以加速过滤。

> **为什么命名向量**：可同时存放多种嵌入（标题/关键词/语义），便于**多路召回 + 结果融合**，不强制同维度。

### 6.2 写入与更新
- 每个 `material_chunk` 生成 1 条 point：`id = chunkId`；携带 1~3 个命名向量与 Payload。
- 更新时按 `chunk_id` upsert；删除素材时批量删除对应 point。

### 6.3 检索
- **Filter**：`must: workspace_id = ?`，可叠加 `type in (...)`、`tags has any (...)`。
- **TopK**：每路 50~100 → 合并去重 → 重排 20 → 展示 5~10。
- **并行多路召回**：
  - 语义检索（`semantic`）
  - 标题检索（`title`）
  - 关键词检索（`keywords`）

---

## 7. Embedding 策略
### 7.1 模型建议（按中文优先）
- **通用中文/多语句向量**：如 BGE / GTE 家族（本地或托管），或等价优选。
- **短文本/标题向量**：同族模型或专用短文本模型。
- **重排（Cross-Encoder）**：bge-reranker / e5-reranker。

> **多模型并存**：命名向量允许 `semantic(1024)` 与 `title(768)` 共存；Qdrant 每个向量独立建图。

### 7.2 切片（Chunking）
- **中文建议**：字符窗口 400~700，重叠 80~120；按标题/段落优先切分。
- **分层索引**：文档→章节→块（可在 Payload 中存 `title_path`，用于 UI 折叠与父级摘要召回）。

### 7.3 去重与相似度阈值
- `hash`（MinHash/SimHash）+ 向量相似度 `cos_sim > 0.98` 视为重复，自动合并或标注同义素材。

### 7.4 查询增强
- **Query Expansion**：同义词/别名/拼音相近词扩展；从素材库“别名/标签”中学习。
- **意图分类**：`character/place/event/lore/...` 提高过滤精度。

---

## 8. Hybrid 检索与排序
### 8.1 双引擎
- **Qdrant 相似度**：召回语义相关。
- **MySQL FULLTEXT（ngram）**：精确词命中、关键名词。

### 8.2 融合策略
- **RRFA（Reciprocal Rank Fusion Averaged）** 或 **Learn-to-Rank**（若有标注数据）。
- 在无标注阶段：对两路 TopK 结果进行 **分数归一化** + **互惠排名融合**，再交叉重排。

### 8.3 交叉重排
- 对融合后的 50 条以内候选，使用 Cross-Encoder 重排，取前 10 进入 UI。

---

## 9. 上传与解析流水线
1. **接收文件**：存储至对象存储/本地磁盘，登记 `file_import_job`。
2. **文本抽取**：Tika/Docx4j/PDFBox；图片走 OCR（异步）。
3. **预处理**：去噪、编码修复、段落/标题识别。
4. **LLM 结构化解析**：按 JSON Schema 抽取实体；生成 `material`（结构化）与 `material_chunk`（可检索文本块）。
5. **去重/合并**：根据 `hash` 与相似度合并；生成变更报告供编辑审核。
6. **向量化**：三路嵌入（semantic/title/keywords），写入 Qdrant。
7. **索引与提交**：MySQL 事务提交 + Qdrant upsert 成功后标记 `file_import_item.status = success`。
8. **审计与可观测**：记录耗时/失败阶段；Prometheus 指标（吞吐、平均延迟、召回率）。

**失败恢复**：作业具备幂等性（以 `hash` 与 `chunk_id` 作为幂等键），允许断点续跑。

---

## 10. MCP 工具设计（Model Context Protocol）
> 通过 MCP 将“文件、解析、检索、持久化”等能力标准化为工具，便于 LLM 协同。

### 10.1 现成/通用 MCP Server（建议接入）
- **filesystem**：读写工作区文件、临时存储。
- **http**：下载远程资源（如在线文档）。
- **ocr**：图片/PDF OCR。
- **tika**：通用文本抽取（如果有对应 MCP 实现）。
- **qdrant**：向量库读写（search/query/upsert/delete）。
- **mysql**：结构化读写（带参数化 SQL）。

### 10.2 自研 MCP 工具（建议）
1. `material.extract` — **素材结构化解析**
   - **input**：`{ filePath|rawText, workspaceId, parseMode, schemaVersion }`
   - **output**：`{ materials: [...], chunks: [...] , warnings: [...] }`
2. `material.commit` — **入库与去重合并**
   - **input**：`{ workspaceId, materials, chunks, dedupe: {simThreshold, hashMode} }`
   - **output**：`{ upserted:{materials:n, chunks:n}, merged:{materials:n}, reportId }`
3. `material.search` — **Hybrid 检索统一入口**
   - **input**：`{ workspaceId, query, filters, topK, mode: "auto"|"manual" }`
   - **output**：`{ results:[{chunkId, materialId, score, reason, source}] }`
4. `material.embed` — **多路嵌入生成**
   - **input**：`{ texts:[...], routes:["semantic","title","keywords"], modelHints }`
   - **output**：`{ vectors:{ semantic:[[...]], title:[[...]], keywords:[[...]] } }`
5. `editor.hint` — **写作时意图与关键词建议**
   - **input**：`{ draftContext, cursor, maxHints }`
   - **output**：`{ intents:[...], keywords:[...] }`

> 通过 MCP，LLM 可直接编排“抽取→入库→检索”，并把所有调用写入审计日志。

---

## 11. Spring Boot 接口与流程
### 11.1 REST API（示例）
- `POST /api/materials` — 手动创建素材（表单 → material + chunks 可选）。
- `POST /api/imports` — 上传文件，返回 `jobId`。
- `GET /api/imports/{id}` — 查询作业状态与报告。
- `POST /api/search` — 手动检索（query + filters）。
- `POST /api/editor/auto-hints` — 编辑器自动建议（传入最近上下文）。

### 11.2 Spring AI 关键配置（示例伪代码）
```java
@Bean
EmbeddingClient embeddingClient() { /* OpenAI/Local/BGE/GTE... */ }

@Bean
QdrantVectorStore qdrantVectorStore(RestClient qdrant, EmbeddingClient embedding) {
    // 指定 collection = "materials"，使用命名向量路由
    return new QdrantVectorStore(qdrant, embedding, QdrantConfig.builder()
        .collectionName("materials").distance("Cosine").build());
}

@Service
class HybridSearchService {
  Results search(String query, Filters f, int k) {
    // 1) Qdrant：semantic/title/keywords 三路并发
    // 2) MySQL：FULLTEXT
    // 3) RRF 融合 + Cross-Encoder 重排
  }
}
```

### 11.3 MySQL FULLTEXT（中文）
```sql
ALTER TABLE material ADD FULLTEXT ft_material(title, summary, content) WITH PARSER ngram;
ALTER TABLE material_chunk ADD FULLTEXT ft_chunk(text) WITH PARSER ngram;
```

### 11.4 导入作业（Batch/Quartz）
- **步骤**：`FETCH → EXTRACT → PARSE → DEDUPE → EMBED → UPSERT → INDEX → REPORT`。
- **并发**：按文件/章节粒度并行；控制最大并发避免向量化打满。

---

## 12. 编辑器集成（写作时自动检索）
- **触发**：
  - 文档变更后 **debounce 1.5s**；
  - 光标所在段落长度 ≥ N 或出现人名/地名/设定关键词；
  - 手动触发（/素材）。
- **查询构造**：
  - 从最近 200~500 字上下文提取名词短语、NER 实体、时间/地点；
  - 生成 2~3 条候选查询（主/备），多路检索；
  - 应用 `workspace_id`、`type` 过滤。
- **展示**：卡片含标题、摘要、来源、片段；支持“插入引用/链接”、“固定到侧栏”、“不再推荐”。
- **上下文注入**：选中的素材片段拼接为 RAG 附件，带引用标记（chunkId）。

---

## 13. 权限与多租户
- **Workspace 隔离**：所有 MySQL 行与 Qdrant Payload 必带 `workspace_id`；检索必须强制过滤。
- **ACL**：资源级（material/material_chunk）read/write/admin；引用继承素材权限。
- **审计**：记录自动检索命中与最终被引用的片段，便于评估与回溯。

---

## 14. 运维与部署
- **Qdrant**：
  - 副本：`replication_factor >= 2`；
  - 分片：按数据量估算（每分片 ~ 5–20M 向量）；
  - Snapshot/备份：每日增量；
  - 资源：CPU 向量化、内存向量图、SSD 存储；
  - 监控：搜索 QPS、召回/重排延迟、内存使用、失败率。
- **应用**：
  - 限流：向量化与重排服务；
  - 缓存：相同查询/上下文的短期缓存（30–120s）。

---

## 15. 质量评估与迭代
- **离线评估**：构建金标集合（query→理想素材），指标 `MRR@10 / nDCG@10 / Recall@50`。
- **在线指标**：作者点击率、插入率、引用留存、生成质量投票。
- **A/B**：检索路由、切片大小、重排模型对比。

---

## 16. 风险与对策
- **延迟**：多路召回 + 重排可能超时 → 并行 + 超时降级（仅 semantic）；首屏 5 条，懒加载更多。
- **成本**：向量化与重排成本高 → 批量/队列、缓存 Embedding、分辨短/长文本走不同模型。
- **解析准确性**：LLM 抽取偏差 → 人审工作台 + 规则校验（必填、枚举、别名冲突）。
- **中文分词**：FULLTEXT 精度 → ngram + 自定义词典（载入人名/地名/设定词）。

---

## 17. 里程碑（建议）
- **MVP（2–3 周）**：
  - 手动录入 + 基础检索（Qdrant semantic）
  - 文件上传→文本抽取→切片→向量化→入库
  - 编辑器手动搜索面板
- **V1**：
  - Hybrid（Qdrant + FULLTEXT）+ 简易重排
  - LLM 结构化解析 + 人审工作台
  - 编辑器自动检索建议（关键词推断）
- **V2**：
  - 命名向量完善（title/keywords）、多租户 ACL、审计
  - 去重/合并、相似素材聚类
  - 评估体系与 A/B 框架

---

## 18. 示例：Qdrant 与检索伪代码
```java
// 1) 语义检索（semantic）
var sem = qdrant.search(SearchRequest.builder()
  .collection("materials")
  .vectorName("semantic")
  .vector(embeddingClient.embed(query))
  .filter(Filter.mustEq("workspace_id", ws))
  .limit(80)
  .build());

// 2) FULLTEXT（MySQL）
SELECT id, MATCH(text) AGAINST (? IN BOOLEAN MODE) AS score
FROM material_chunk
WHERE workspace_id = ?
ORDER BY score DESC LIMIT 80;

// 3) RRF 融合 + Cross-Encoder 重排（伪代码）
List<Result> fused = rrfFuse(sem, fulltext);
List<Result> reranked = crossEncoder.rank(query, fused.subList(0, 50));
return topN(reranked, 10);
```

---

## 19. UI 草图要点
- **素材管理台**：列表（类型/标签/状态/来源/更新时间）、批量审核、合并相似、导入报告。
- **编辑器侧栏**：自动建议（可切换 Tab：相关/角色/地点/设定）、引用卡片、固定与过滤。
- **素材详情**：结构化信息 + 片段树（title_path 折叠），一键插入引用。

---

## 20. 小结
本方案以 **MySQL + Qdrant** 的“双索引”作为检索底座，结合 **Spring AI 1.0.3** 的嵌入与函数调用，借助 **MCP 工具化**实现从“文件解析→结构化抽取→向量入库→混合检索→重排→编辑器引用”的全链路闭环，满足小说写作场景对**可控、可审计、低延迟**的素材检索与复用需求。


# AI Novel 素材库开发方案 - 第二阶段 (V1)

本方案基于 MVP 阶段的成果，旨在通过引入 **混合检索、LLM 结构化解析** 和 **编辑器自动建议**，全面提升素材库的检索质量与智能化水平。

**[⬆️ 返回总体计划](./material_dev_plan_overview.md)**

## 1. V1 阶段目标

1.  **后端**：
    *   实现 **Hybrid Search (混合检索)**，结合 Qdrant 的向量相似度与 MySQL 的全文检索能力，提高召回的准确性。
    *   集成 **Spring AI Function Calling**，利用大语言模型（LLM）从上传的文本中自动抽取结构化的素材信息（如角色、地点、事件）。
    *   开发一个简单的交叉重排（Cross-Encoder Reranking）服务，对检索结果进行二次排序。

2.  **前端**：
    *   开发一个 **人审工作台**，用于审核和修正 LLM 自动解析的结果。
    *   在写作工作台中，实现 **自动检索建议** 功能，根据用户当前的写作上下文，实时推荐相关素材。

---

## 2. 后端开发任务 (Spring Boot)

### **任务 2.1: 启用 MySQL 全文检索**

1.  **数据库变更**
    为 `material` 和 `material_chunk` 表添加 `FULLTEXT` 索引。请注意，这需要 MySQL 8.0 及以上版本，并使用 `ngram` 解析器以支持中文。

    ```sql
    -- 在 database.sql 或通过管理工具执行
    ALTER TABLE material ADD FULLTEXT ft_material(title, summary, content) WITH PARSER ngram;
    ALTER TABLE material_chunk ADD FULLTEXT ft_chunk(text) WITH PARSER ngram;
    ```

2.  **更新 Repository 接口**
    在 `MaterialChunkRepository.java` 中添加一个使用原生 SQL 查询的方法，以利用 `FULLTEXT` 索引。

    ```java
    // backend/src/main/java/com/example/ainovel/repository/MaterialChunkRepository.java
    public interface MaterialChunkRepository extends JpaRepository<MaterialChunk, Long> {
        @Query(value = "SELECT *, MATCH(text) AGAINST (?1 IN BOOLEAN MODE) AS score " +
                       "FROM material_chunk " +
                       "WHERE material_id IN (SELECT id FROM material WHERE workspace_id = ?2) " +
                       "AND MATCH(text) AGAINST (?1 IN BOOLEAN MODE) > 0 " +
                       "ORDER BY score DESC LIMIT ?3",
               nativeQuery = true)
        List<MaterialChunk> searchWithFullText(String query, Long workspaceId, int limit);
    }
    ```

### **任务 2.2: 实现混合检索服务**

1.  **创建 `HybridSearchService.java`**
    在 `backend/src/main/java/com/example/ainovel/service/` 目录下创建此服务，用于封装混合检索逻辑。

    ```java
    // backend/src/main/java/com/example/ainovel/service/HybridSearchService.java
    @Service
    public class HybridSearchService {
        // 注入 VectorStore, MaterialChunkRepository, RerankerService

        public List<SearchResult> search(String query, Long workspaceId) {
            // 1. 并发执行两路召回
            //    - Future<List<Document>> vectorResults = asyncVectorSearch(query, workspaceId);
            //    - Future<List<MaterialChunk>> fulltextResults = asyncFulltextSearch(query, workspaceId);

            // 2. 等待结果并去重
            List<Chunk> combinedResults = combineAndDeduplicate(vectorResults.get(), fulltextResults.get());

            // 3. (可选，V1 可简化) RRF 融合排序
            //    List<Chunk> fusedResults = reciprocalRankFusion(combinedResults);

            // 4. 调用重排服务进行最终排序
            //    return rerankerService.rerank(query, fusedResults);

            // V1 简化版：可以直接合并去重后返回
            return combinedResults;
        }
    }
    ```

2.  **(可选) 创建 `RerankerService.java`**
    对于需要更高精度的场景，可以引入一个重排模型。可以使用 Hugging Face 的 `bge-reranker` 等模型，通过 Python 微服务或 Java 的 ONNX Runtime 调用。MVP 阶段可以暂时跳过，直接返回融合后的结果。

### **任务 2.3: LLM 结构化解析**

1.  **定义 JSON Schema DTO**
    在 `backend/src/main/java/com/example/ainovel/dto/` 目录下，创建一个与 `material_base_instruction.md` 中定义的 JSON Schema 对应的 Java 类，例如 `StructuredMaterial.java`。

2.  **更新 `FileImportService.java`**
    修改文件导入的 Batch Job 处理器，加入 LLM 解析步骤。

    ```java
    // 在 Batch Job 的 Processor 中
    // ...
    // 1. Tika 提取纯文本
    String rawText = tika.parseToString(file.getInputStream());

    // 2. 构建 Function Calling 请求
    var userMessage = new UserMessage(rawText);
    var request = new Prompt(userMessage,
        AiOptions.builder().withFunction("extractMaterialInfo").build());

    // 3. 调用 ChatClient
    ChatResponse response = chatClient.call(request);

    // 4. 从 response 中解析出结构化数据 (StructuredMaterial)
    // ...

    // 5. 将结构化数据存入 material 表的 entities_json 字段
    // ...
    ```
    *   **注意**：需要在 `ChatClient` 的 Bean 配置中注册一个名为 `extractMaterialInfo` 的函数，该函数接受文本，返回 `StructuredMaterial` 对象。

### **任务 2.4: 编辑器自动建议 API**

1.  **更新 `MaterialController.java`**
    添加一个新的 API 端点，用于接收编辑器上下文并返回素材建议。

    ```java
    // backend/src/main/java/com/example/ainovel/controller/MaterialController.java
    @PostMapping("/editor/auto-hints")
    public ResponseEntity<List<SearchResult>> getAutoHints(@RequestBody EditorContextDto context) {
        // 1. (可选) 调用 LLM 从 context.text 中推断查询意图和关键词
        //    String inferredQuery = intentExtractionService.extract(context.getText());

        // 2. (简化版) 直接使用上下文的最后 N 个字符作为查询
        String query = context.getText(); // 实际应做截断和预处理

        // 3. 调用混合检索服务
        List<SearchResult> results = hybridSearchService.search(query, context.getWorkspaceId());
        return ResponseEntity.ok(results);
    }
    ```

---

## 3. 前端开发任务 (React & TypeScript)

### **任务 3.1: 人审工作台**

1.  **创建 `ReviewDashboard.tsx` 页面**
    *   在 `frontend/src/pages/Material/` 下创建 `ReviewDashboard.tsx`。
    *   该页面应能列出所有待审核的、由 LLM 解析生成的素材（例如 `status = 'pending_review'`）。
    *   提供一个界面，左侧显示原文，右侧显示 LLM 抽取的结构化 JSON 表单。
    *   用户可以修改表单内容，然后点击“批准”或“拒绝”。

2.  **更新 API 服务**
    在 `api.ts` 中添加获取待审列表、批准/拒绝素材的接口。

### **任务 3.2: 编辑器自动建议**

1.  **创建 `useAutoSuggestions.ts` Hook**
    在 `frontend/src/hooks/` 目录下创建一个自定义 Hook，用于处理自动建议的逻辑。

    ```typescript
    // frontend/src/hooks/useAutoSuggestions.ts
    import { useState, useEffect, useCallback } from 'react';
    import { debounce } from 'lodash';
    import { getAutoHints } from '../services/api';

    export const useAutoSuggestions = (contextText: string, workspaceId: number) => {
      const [suggestions, setSuggestions] = useState([]);
      const [isLoading, setIsLoading] = useState(false);

      const fetchSuggestions = useCallback(debounce(async (text) => {
        if (text.length < 50) { // 避免过短的文本触发
          setSuggestions([]);
          return;
        }
        setIsLoading(true);
        try {
          const response = await getAutoHints({ text, workspaceId });
          setSuggestions(response.data);
        } finally {
          setIsLoading(false);
        }
      }, 1500), [workspaceId]); // 1.5秒防抖

      useEffect(() => {
        fetchSuggestions(contextText);
      }, [contextText, fetchSuggestions]);

      return { suggestions, isLoading };
    };
    ```

2.  **集成到 `ManuscriptWriter.tsx`**
    在写作组件中，监听编辑器内容的变更，并将变更后的文本传递给 `useAutoSuggestions` Hook。

    ```tsx
    // frontend/src/components/ManuscriptWriter.tsx
    const ManuscriptWriter = () => {
      const [editorContent, setEditorContent] = useState('');
      const { suggestions, isLoading } = useAutoSuggestions(editorContent, currentWorkspaceId);

      // ... 编辑器 onChange 事件中更新 editorContent

      return (
        <div className="workbench-layout">
          <Editor onChange={setEditorContent} />
          <Sidebar>
            {/* ... 其他侧边栏 Tab */}
            <Tab title="素材建议">
              {isLoading && <Spinner />}
              {suggestions.map(item => (
                <SuggestionCard key={item.id} data={item} />
              ))}
            </Tab>
          </Sidebar>
        </div>
      );
    };
    ```

---

## 4. 测试与验证

1.  **混合检索**：使用相同的关键词，分别测试纯向量检索和混合检索的效果，对比混合检索是否能召回更多仅靠关键词匹配的结果。
2.  **LLM 解析**：上传一篇包含清晰角色介绍的文档，检查人审工作台中的解析结果是否准确，JSON 字段是否被正确填充。
3.  **自动建议**：在编辑器中输入一段与已有素材高度相关的内容，验证侧边栏是否能在 1-2 秒后自动弹出推荐的素材卡片。

# AI Novel 素材库开发方案 - 第三阶段 (V2)

本方案是素材库开发的最后阶段，重点在于 **系统健壮性、多租户安全、数据质量** 和 **可维护性**，旨在将素材库功能打磨至生产可用标准。

**[⬆️ 返回总体计划](./material_dev_plan_overview.md)**

## 1. V2 阶段目标

1.  **后端**：
    *   **多租户与权限**：严格实现基于 `workspace_id` 的数据隔离，并引入细粒度的 ACL (Access Control List) 权限管理。
    *   **数据质量**：开发素材去重与合并功能，避免信息冗余。
    *   **审计与评估**：建立审计日志，记录检索和引用行为，为后续的质量评估和 A/B 测试打下基础。
    *   **命名向量**：在 Qdrant 中启用命名向量（如 `title`, `keywords`），实现更丰富的多路召回策略。

2.  **前端**：
    *   **权限控制**：根据用户角色和权限，动态显示或禁用操作按钮（如编辑、删除、审核）。
    *   **数据质量**：在 UI 上提供“合并相似素材”的功能。
    *   **审计展示**：在素材详情页展示引用历史等审计信息。

---

## 2. 后端开发任务 (Spring Boot)

### **任务 3.1: 完善多租户与权限 (ACL)**

1.  **数据库表结构**
    创建 `permission` 表来存储权限信息。

    ```sql
    CREATE TABLE permission (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        resource_type VARCHAR(50) NOT NULL, -- e.g., 'MATERIAL', 'WORKSPACE'
        resource_id BIGINT NOT NULL,
        user_id BIGINT NOT NULL,
        permission_level VARCHAR(50) NOT NULL, -- e.g., 'READ', 'WRITE', 'ADMIN'
        UNIQUE KEY uk_resource_user (resource_type, resource_id, user_id)
    );
    ```

2.  **AOP (面向切面编程) 实现权限校验**
    *   创建一个自定义注解 `@CheckPermission(resourceType, level)`。
    *   编写一个 Spring AOP Aspect，拦截所有被该注解标记的 Service 方法。
    *   在 Aspect 中，从方法参数或上下文中获取 `resourceId` 和当前 `userId`，查询 `permission` 表，如果权限不足则抛出 `AccessDeniedException`。

    ```java
    // 示例：在 MaterialService 中应用
    @Service
    public class MaterialService {
        @CheckPermission(resourceType = "MATERIAL", level = "WRITE")
        public Material updateMaterial(Long materialId, MaterialDto data) {
            // ... 业务逻辑
        }
    }
    ```

3.  **强制 `workspace_id` 过滤**
    同样使用 AOP 或在 Repository 基类中，确保所有数据库查询（包括 Qdrant 的 Filter）都 **必须** 包含 `workspace_id` 过滤条件，防止数据泄露。

### **任务 3.2: 素材去重与合并**

1.  **相似度计算服务 `DeduplicationService.java`**
    *   **哈希去重**：在文件导入时，计算每个 `material_chunk` 的 `hash` (例如 SimHash)。如果 `hash` 已存在，则标记为疑似重复。
    *   **向量相似度**：对于疑似重复的 chunk，进一步计算其向量余弦相似度。如果 `cosine_similarity > 0.98`，则可认定为重复。
    *   **合并逻辑**：提供一个 `mergeMaterials(sourceId, targetId)` 方法，将一个素材的内容、标签等信息合并到另一个素材中，并归档源素材。

2.  **API 接口**
    *   `POST /api/materials/find-duplicates`：触发一次工作区内的查重任务。
    *   `POST /api/materials/merge`：执行合并操作。

### **任务 3.3: 审计与评估**

1.  **创建审计日志表**
    *   `search_log`: 记录每次检索（`query`, `user_id`, `latency_ms`, `used_in_generation`）。
    *   `citation`: 记录哪个文档的哪个位置引用了哪个 `material_chunk`。

2.  **异步记录日志**
    使用 Spring 的 `@Async` 或 `ApplicationEventPublisher` 来异步记录审计事件，避免影响主流程性能。

    ```java
    // 在 HybridSearchService 中
    public List<SearchResult> search(...) {
        // ...
        applicationEventPublisher.publishEvent(new SearchEvent(this, query, userId, results));
        return results;
    }

    @EventListener
    public void handleSearchEvent(SearchEvent event) {
        // 异步将事件内容存入 search_log 表
    }
    ```

### **任务 3.4: Qdrant 命名向量**

1.  **更新 `MaterialService` 的向量化逻辑**
    在创建或更新素材时，为不同的内容生成并上传多种命名向量。

    ```java
    // 伪代码
    void vectorizeAndUpsert(Material material, List<Chunk> chunks) {
        // ...
        List<Point> pointsToUpsert = new ArrayList<>();
        for (Chunk chunk : chunks) {
            // 生成三种向量
            Vector semanticVec = embeddingClient.embed(chunk.getText());
            Vector titleVec = titleEmbeddingClient.embed(material.getTitle());
            Vector keywordVec = keywordEmbeddingClient.embed(String.join(" ", material.getTags()));

            // 构建一个带命名向量的 Point
            Point point = Point.builder()
                .id(chunk.getId())
                .vector("semantic", semanticVec)
                .vector("title", titleVec)
                .vector("keywords", keywordVec)
                .payload(...)
                .build();
            pointsToUpsert.add(point);
        }
        qdrantClient.upsert("materials", pointsToUpsert);
    }
    ```

2.  **更新 `HybridSearchService` 的检索逻辑**
    修改检索逻辑，从单路召回变为多路并行召回，并对多路结果进行融合。

    ```java
    // 伪代码
    public List<SearchResult> search(...) {
        // 并发执行三路向量检索 + 一路全文检索
        Future<List> semanticResults = qdrant.search("semantic", ...);
        Future<List> titleResults = qdrant.search("title", ...);
        Future<List> keywordResults = qdrant.search("keywords", ...);
        Future<List> fulltextResults = mysql.fulltextSearch(...);

        // ... 等待所有结果，然后进行 RRF 融合和重排
    }
    ```

---

## 3. 前端开发任务 (React & TypeScript)

### **任务 4.1: UI 权限控制**

1.  **扩展 `AuthContext`**
    在 `AuthContext` 中，除了存储用户信息，还应增加一个字段来存储该用户在当前工作区的权限集（例如 `permissions: { 'MATERIAL_123': ['READ', 'WRITE'] }`）。

2.  **创建 `Can.tsx` 组件**
    创建一个权限控制组件，根据 `AuthContext` 中的权限信息来决定是否渲染其子组件。

    ```tsx
    // frontend/src/components/Can.tsx
    const Can = ({ perform, on, children }) => {
      const { permissions } = useAuth();
      // 检查 permissions 中是否有执行 perform 操作的权限
      const isAllowed = checkPermission(permissions, perform, on);
      return isAllowed ? <>{children}</> : null;
    };

    // 使用示例
    <Can perform="material:edit" on={material.id}>
      <Button>编辑</Button>
    </Can>
    ```

### **任务 4.2: 合并相似素材界面**

1.  在素材管理列表页，增加一个“查找相似项”的按钮。
2.  点击后，弹出一个模态框，展示相似的素材对。
3.  提供一个“合并”按钮，允许用户选择一个作为主素材，将另一个合并进去。

### **任务 4.3: 审计信息展示**

在素材的详情页面，增加一个 Tab，用于显示该素材被引用的历史记录（从 `citation` 表获取数据），帮助作者了解素材的使用情况。

---

## 4. 测试与验证

1.  **权限**：使用不同角色的账户登录，验证非管理员用户无法看到或操作其无权访问的素材。尝试直接调用 API，确认后端 AOP 拦截生效。
2.  **去重**：上传两份内容高度相似的文档，验证系统能否在“相似素材”界面中正确识别它们。执行合并操作，确认数据被正确迁移且源素材被归档。
3.  **命名向量**：构造一个仅标题或标签匹配的查询，验证多路召回是否能成功检索到该素材，而单路语义检索可能无法召回。
4.  **审计**：在编辑器中进行多次检索和引用操作，然后检查 `search_log` 和 `citation` 表，确认相关行为已被准确记录。

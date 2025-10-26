# AI Novel 素材库开发方案 - 第一阶段 (MVP)

本方案详细描述了“素材库”模块 MVP 阶段的开发任务，旨在实现核心功能的闭环：**手动录入、文件上传解析、基础向量检索**。

**[⬆️ 返回总体计划](./material_dev_plan_overview.md)**

## 1. MVP 阶段目标

1.  **后端**：搭建支持素材管理的基础服务，包括数据库表、API 接口，并集成 Qdrant 实现向量化存储与检索。
2.  **前端**：开发素材管理的基础页面（手动录入、列表展示），并在写作工作台集成一个手动搜索面板。
3.  **核心流程**：
    *   用户可以通过表单手动创建一条素材。
    *   用户可以上传一个 `.txt` 文件，系统自动提取文本、切片、向量化后存入数据库。
    *   在写作时，用户可以在搜索面板中输入关键词，从素材库中检索出最相关的内容。

---

## 2. 后端开发任务 (Spring Boot)

### **任务 1.1: 环境与依赖配置**

1.  **添加 Maven 依赖**
    在 `backend/pom.xml` 文件中，添加 Spring AI for Qdrant、Spring Batch (用于异步任务) 和 Apache Tika (用于文本提取) 的依赖。

    ```xml
    <!-- backend/pom.xml -->
    <dependencies>
        <!-- ... other dependencies ... -->

        <!-- Spring AI for Qdrant -->
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-qdrant-store</artifactId>
        </dependency>

        <!-- Spring Batch for async jobs -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-batch</artifactId>
        </dependency>

        <!-- Apache Tika for text extraction -->
        <dependency>
            <groupId>org.apache.tika</groupId>
            <artifactId>tika-core</artifactId>
            <version>2.9.1</version> <!-- 使用一个较新版本 -->
        </dependency>
        <dependency>
            <groupId>org.apache.tika</groupId>
            <artifactId>tika-parser-text-module</artifactId>
            <version>2.9.1</version>
        </dependency>

        <!-- ... other dependencies ... -->
    </dependencies>
    ```

2.  **配置 application-dev.yml**
    在 `backend/src/main/resources/application-dev.yml` 中添加 Qdrant 和 OpenAI (或其他 Embedding 模型) 的配置。

    ```yaml
    # backend/src/main/resources/application-dev.yml
    spring:
      # ...
      ai:
        openai:
          api-key: ${OPENAI_API_KEY} # 从环境变量读取
          embedding:
            options:
              model: text-embedding-3-small
        vector-store:
          qdrant:
            host: localhost
            port: 6333
            collection-name: "materials"
            distance-type: "Cosine"
    ```

### **任务 1.2: 数据库表结构**

在 `backend/src/database.sql` 或通过数据库管理工具执行以下 SQL，创建 MVP 所需的核心表。

```sql
-- 素材主表
CREATE TABLE material (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    summary TEXT,
    content TEXT,
    source_id BIGINT, -- 对应 file_import_job 的 ID
    status VARCHAR(50) DEFAULT 'published',
    tags VARCHAR(255),
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_workspace_id (workspace_id)
);

-- 素材文本块表
CREATE TABLE material_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id BIGINT NOT NULL,
    seq INT NOT NULL,
    text TEXT NOT NULL,
    hash VARCHAR(64),
    token_count INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (material_id) REFERENCES material(id) ON DELETE CASCADE
);

-- 文件导入作业表
CREATE TABLE file_import_job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_id BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    status VARCHAR(50) NOT NULL, -- PENDING, PROCESSING, COMPLETED, FAILED
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP
);
```

### **任务 1.3: 创建后端实体 (Entities) 与仓库 (Repositories)**

1.  **创建实体类**
    在 `backend/src/main/java/com/example/ainovel/model/` 目录下，创建以下 JPA 实体类：
    *   `Material.java`
    *   `MaterialChunk.java`
    *   `FileImportJob.java`

2.  **创建仓库接口**
    在 `backend/src/main/java/com/example/ainovel/repository/` 目录下，创建对应的 Spring Data JPA 接口：
    *   `MaterialRepository.java`
    *   `MaterialChunkRepository.java`
    *   `FileImportJobRepository.java`

### **任务 1.4: 核心服务层**

1.  **创建 `MaterialService.java`**
    在 `backend/src/main/java/com/example/ainovel/service/` 目录下创建 `MaterialService`，负责素材的增删改查和向量化。

    ```java
    // backend/src/main/java/com/example/ainovel/service/MaterialService.java
    @Service
    public class MaterialService {
        // 注入 MaterialRepository, MaterialChunkRepository, VectorStore

        // 手动创建素材
        public Material createMaterial(MaterialDto materialDto) {
            // 1. 保存 material 到 MySQL
            // 2. 将 material.content 切片成 chunks
            // 3. 将 chunks 向量化并存入 Qdrant
            // 4. 保存 chunks 到 MySQL
        }

        // 检索素材
        public List<Document> searchMaterials(String query, Long workspaceId) {
            // 1. 构建带 workspace_id 过滤的检索请求
            // 2. 调用 vectorStore.similaritySearch(request)
            // 3. 返回结果
        }
    }
    ```

2.  **创建 `FileImportService.java`**
    在 `backend/src/main/java/com/example/ainovel/service/` 目录下创建 `FileImportService`，处理文件上传和后台解析。

    ```java
    // backend/src/main/java/com/example/ainovel/service/FileImportService.java
    @Service
    public class FileImportService {
        // 注入 FileImportJobRepository, MaterialService, JobLauncher

        // 启动文件导入作业
        public Long startFileImportJob(MultipartFile file, Long uploaderId, Long workspaceId) {
            // 1. 保存文件到本地/对象存储
            // 2. 创建并保存一个 FileImportJob 记录，状态为 PENDING
            // 3. 异步启动一个 Spring Batch Job 来处理该文件
            // 4. 返回 jobId
        }

        // Batch Job 的核心步骤 (Step):
        // a. Reader: 读取 FileImportJob 记录
        // b. Processor:
        //    - 使用 Tika 提取文本
        //    - 调用 MaterialService 创建素材并进行向量化
        // c. Writer: 更新 FileImportJob 的状态
    }
    ```

### **任务 1.5: API 接口层**

在 `backend/src/main/java/com/example/ainovel/controller/` 目录下创建 `MaterialController.java`。

```java
// backend/src/main/java/com/example/ainovel/controller/MaterialController.java
@RestController
@RequestMapping("/api/materials")
public class MaterialController {
    // 注入 MaterialService, FileImportService

    // 手动创建素材
    @PostMapping
    public ResponseEntity<Material> createMaterial(@RequestBody MaterialDto materialDto) {
        // ...
    }

    // 手动检索
    @PostMapping("/search")
    public ResponseEntity<List<Document>> search(@RequestBody SearchRequestDto searchRequest) {
        // ...
    }

    // 上传文件
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Long>> uploadFile(@RequestParam("file") MultipartFile file) {
        // 获取当前用户ID和工作区ID
        // Long jobId = fileImportService.startFileImportJob(...)
        // return Map.of("jobId", jobId);
    }

    // 查询上传作业状态
    @GetMapping("/upload/{jobId}")
    public ResponseEntity<FileImportJob> getJobStatus(@PathVariable Long jobId) {
        // ...
    }
}
```

---

## 3. 前端开发任务 (React & TypeScript)

### **任务 2.1: 更新 API 服务**

在 `frontend/src/services/api.ts` 文件中，添加与素材库相关的 API 调用函数。

```typescript
// frontend/src/services/api.ts

// 创建素材
export const createMaterial = (materialData: any) => api.post('/materials', materialData);

// 上传文件
export const uploadMaterialFile = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/materials/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

// 检索素材
export const searchMaterials = (query: string) => api.post('/materials/search', { query });```

### **任务 2.2: 创建素材管理页面**

1.  **创建页面和组件**
    *   在 `frontend/src/pages/` 下创建新目录 `Material/`。
    *   创建页面文件 `frontend/src/pages/Material/MaterialPage.tsx`。
    *   在 `frontend/src/components/` 下创建 `MaterialCreateForm.tsx` 和 `MaterialUpload.tsx`。

2.  **`MaterialPage.tsx`**
    该页面作为素材库的管理入口，包含创建、上传和列表功能。

    ```tsx
    // frontend/src/pages/Material/MaterialPage.tsx
    import React from 'react';
    // ... import components

    const MaterialPage = () => {
      return (
        <div>
          <h1>素材库</h1>
          <Tabs>
            <TabPanel title="创建素材">
              <MaterialCreateForm />
            </TabPanel>
            <TabPanel title="上传文件">
              <MaterialUpload />
            </TabPanel>
            <TabPanel title="素材列表">
              {/* MVP 阶段列表可稍后实现 */}
            </TabPanel>
          </Tabs>
        </div>
      );
    };
    ```

3.  **添加路由**
    在 `frontend/src/App.tsx` 中为素材库页面添加新的路由。

### **任务 2.3: 编辑器集成 - 手动搜索面板**

1.  **创建 `MaterialSearchPanel.tsx` 组件**
    在 `frontend/src/components/` 目录下创建该组件。

    ```tsx
    // frontend/src/components/MaterialSearchPanel.tsx
    import React, { useState } from 'react';
    import { searchMaterials } from '../services/api';

    const MaterialSearchPanel = () => {
      const [query, setQuery] = useState('');
      const [results, setResults] = useState([]);

      const handleSearch = async () => {
        if (!query) return;
        const response = await searchMaterials(query);
        setResults(response.data);
      };

      return (
        <div>
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="搜索素材..."
          />
          <button onClick={handleSearch}>搜索</button>
          <div>
            {results.map((doc, index) => (
              <div key={index} className="search-result-item">
                {/* 显示 doc.metadata.title 和 doc.pageContent */}
              </div>
            ))}
          </div>
        </div>
      );
    };
    ```

2.  **集成到 `Workbench.tsx`**
    将 `MaterialSearchPanel` 作为侧边栏的一个 Tab 或一个可折叠的面板，添加到 `frontend/src/components/Workbench.tsx` 中。

---

## 4. 测试与验证

1.  **手动创建**：在前端“创建素材”页面填写表单，提交后检查 MySQL `material` 和 `material_chunk` 表中是否出现数据，并确认 Qdrant 中有对应的向量。
2.  **文件上传**：上传一个简单的 `.txt` 文件，轮询作业状态接口，确认作业最终状态为 `COMPLETED`，并检查数据库中是否已创建相应的素材。
3.  **检索功能**：在写作界面的搜索面板中输入与已创建素材相关的关键词，验证是否能返回预期的结果。

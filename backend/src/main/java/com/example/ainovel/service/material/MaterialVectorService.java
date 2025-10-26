package com.example.ainovel.service.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.ainovel.model.material.Material;
import com.example.ainovel.model.material.MaterialChunk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialVectorService {

    private final VectorStore vectorStore;

    public void indexMaterial(Material material, List<MaterialChunk> chunks) {
        indexMaterial(material, chunks, true);
    }

    public void indexMaterial(Material material, List<MaterialChunk> chunks, boolean includeMetadataVectors) {
        if (material == null || chunks == null || chunks.isEmpty()) {
            return;
        }
        List<Document> documents = new ArrayList<>();
        boolean titleAdded = !includeMetadataVectors;
        boolean keywordAdded = !includeMetadataVectors;
        for (MaterialChunk chunk : chunks) {
            documents.add(buildSemanticDocument(material, chunk));
            if (!titleAdded) {
                Document titleDoc = buildTitleDocument(material, chunk);
                if (titleDoc != null) {
                    documents.add(titleDoc);
                    titleAdded = true;
                }
            }
            if (!keywordAdded) {
                Document keywordDoc = buildKeywordDocument(material, chunk);
                if (keywordDoc != null) {
                    documents.add(keywordDoc);
                    keywordAdded = true;
                }
            }
        }
        try {
            vectorStore.add(documents);
        } catch (Exception ex) {
            log.warn("向量存储写入失败（materialId={}）：{}", material.getId(), ex.getMessage());
        }
    }

    private Document buildSemanticDocument(Material material, MaterialChunk chunk) {
        Map<String, Object> metadata = baseMetadata(material, chunk);
        metadata.put("vectorType", "semantic");
        return new Document(chunk.getText(), metadata);
    }

    private Document buildTitleDocument(Material material, MaterialChunk chunk) {
        if (!StringUtils.hasText(material.getTitle())) {
            return null;
        }
        Map<String, Object> metadata = baseMetadata(material, chunk);
        metadata.put("vectorType", "title");
        metadata.put("chunkSeq", -1);
        return new Document(material.getTitle(), metadata);
    }

    private Document buildKeywordDocument(Material material, MaterialChunk chunk) {
        if (!StringUtils.hasText(material.getTags())) {
            return null;
        }
        Map<String, Object> metadata = baseMetadata(material, chunk);
        metadata.put("vectorType", "keywords");
        metadata.put("chunkSeq", -2);
        String normalizedTags = material.getTags().replace(',', ' ');
        return new Document(normalizedTags, metadata);
    }

    private Map<String, Object> baseMetadata(Material material, MaterialChunk chunk) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("materialId", material.getId());
        metadata.put("workspaceId", material.getWorkspaceId());
        metadata.put("title", material.getTitle());
        metadata.put("chunkSeq", chunk.getSequence());
        if (StringUtils.hasText(material.getSummary())) {
            metadata.put("summary", material.getSummary());
        }
        metadata.put("sourceMaterialStatus", material.getStatus());
        return metadata;
    }
}

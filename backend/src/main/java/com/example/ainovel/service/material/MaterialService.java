package com.example.ainovel.service.material;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.ainovel.dto.material.MaterialCreateRequest;
import com.example.ainovel.dto.material.MaterialResponse;
import com.example.ainovel.dto.material.MaterialSearchResult;
import com.example.ainovel.model.material.Material;
import com.example.ainovel.model.material.MaterialChunk;
import com.example.ainovel.model.material.MaterialStatus;
import com.example.ainovel.repository.material.MaterialChunkRepository;
import com.example.ainovel.repository.material.MaterialRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 素材库核心服务：负责素材的保存、切片与检索。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialService {

    private static final int DEFAULT_CHUNK_SIZE = 800;
    private static final int DEFAULT_CHUNK_OVERLAP = 100;
    private static final int MAX_SNIPPET_LENGTH = 200;

    private final MaterialRepository materialRepository;
    private final MaterialChunkRepository materialChunkRepository;
    private final VectorStore vectorStore;

    /**
     * 手动创建素材。
     */
    @Transactional
    public MaterialResponse createMaterial(MaterialCreateRequest request, Long userId, Long workspaceId) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("素材标题不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("素材内容不能为空");
        }

        Material material = new Material();
        material.setWorkspaceId(workspaceId);
        material.setTitle(request.getTitle().trim());
        material.setType(StringUtils.hasText(request.getType()) ? request.getType().trim() : "manual");
        material.setSummary(StringUtils.hasText(request.getSummary()) ? request.getSummary().trim() : null);
        material.setStatus(MaterialStatus.PUBLISHED.name());
        material.setCreatedBy(userId);
        material.setUpdatedBy(userId);

        return persistMaterial(material, request.getContent(), request.getTags());
    }

    /**
     * 文件导入流程生成素材。
     */
    @Transactional
    public MaterialResponse createMaterialFromImport(String title,
                                                     String summary,
                                                     String content,
                                                     Long workspaceId,
                                                     Long userId,
                                                     Long sourceJobId,
                                                     String tags) {
        Material material = new Material();
        material.setWorkspaceId(workspaceId);
        material.setTitle(StringUtils.hasText(title) ? title.trim() : "导入素材");
        material.setType("file");
        material.setSummary(StringUtils.hasText(summary) ? summary.trim() : null);
        material.setStatus(MaterialStatus.PUBLISHED.name());
        material.setCreatedBy(userId);
        material.setUpdatedBy(userId);
        material.setSourceId(sourceJobId);

        return persistMaterial(material, content, tags);
    }

    /**
     * 向量检索素材。
     */
    @Transactional(readOnly = true)
    public List<MaterialSearchResult> searchMaterials(Long workspaceId, String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        int topK = Math.max(limit, 5);
        SearchRequest searchRequest = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .build();

        List<Document> documents = vectorStore.similaritySearch(searchRequest);
        List<MaterialSearchResult> results = new ArrayList<>();
        for (Document document : documents) {
            Long docWorkspaceId = toLong(document.getMetadata().get("workspaceId"));
            if (docWorkspaceId == null || !Objects.equals(docWorkspaceId, workspaceId)) {
                continue;
            }
            MaterialSearchResult result = new MaterialSearchResult();
            String chunkText = document.getText();
            result.setMaterialId(toLong(document.getMetadata().get("materialId")));
            result.setTitle(asString(document.getMetadata().getOrDefault("title", "")));
            result.setChunkSeq(toInteger(document.getMetadata().get("chunkSeq")));
            result.setSnippet(buildSnippet(chunkText));
            Double score = document.getScore();
            if (score == null) {
                score = toDouble(document.getMetadata().getOrDefault("score",
                    document.getMetadata().getOrDefault("distance", null)));
            }
            result.setScore(score);
            results.add(result);
            if (results.size() >= limit) {
                break;
            }
        }
        return results;
    }

    private MaterialResponse persistMaterial(Material material, String content, String rawTags) {
        String sanitizedContent = content != null ? content.trim() : "";
        material.setContent(sanitizedContent);
        material.setTags(normalizeTags(rawTags));
        Material saved = materialRepository.save(material);

        List<String> chunkTexts = chunkText(sanitizedContent);
        if (chunkTexts.isEmpty() && StringUtils.hasText(sanitizedContent)) {
            chunkTexts = List.of(sanitizedContent);
        }

        List<MaterialChunk> chunkEntities = new ArrayList<>();
        List<Document> documents = new ArrayList<>();
        int sequence = 0;
        for (String chunkText : chunkTexts) {
            MaterialChunk chunk = new MaterialChunk();
            chunk.setMaterial(saved);
            chunk.setSequence(sequence);
            chunk.setText(chunkText);
            chunk.setHash(calculateHash(chunkText));
            chunk.setTokenCount(estimateTokenCount(chunkText));
            chunkEntities.add(chunk);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("materialId", saved.getId());
            metadata.put("workspaceId", saved.getWorkspaceId());
            metadata.put("title", saved.getTitle());
            metadata.put("chunkSeq", sequence);
            if (StringUtils.hasText(saved.getSummary())) {
                metadata.put("summary", saved.getSummary());
            }
            documents.add(new Document(chunkText, metadata));
            sequence++;
        }

        if (!chunkEntities.isEmpty()) {
            materialChunkRepository.saveAll(chunkEntities);
            try {
                vectorStore.add(documents);
            } catch (Exception ex) {
                log.warn("向量存储写入失败（materialId={}）：{}", saved.getId(), ex.getMessage());
            }
        }

        saved.setChunks(chunkEntities);
        return toResponse(saved);
    }

    private List<String> chunkText(String content) {
        if (!StringUtils.hasText(content)) {
            return List.of();
        }

        String normalized = content.replace("\r", "\n");
        String[] paragraphs = normalized.split("\\n\\s*\\n");
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (!StringUtils.hasText(trimmed)) {
                continue;
            }
            if (current.length() + trimmed.length() + 1 <= DEFAULT_CHUNK_SIZE) {
                if (current.length() > 0) {
                    current.append("\n\n");
                }
                current.append(trimmed);
            } else {
                if (current.length() > 0) {
                    chunks.add(current.toString());
                }
                String remaining = trimmed;
                while (remaining.length() > DEFAULT_CHUNK_SIZE) {
                    int end = DEFAULT_CHUNK_SIZE;
                    int splitIndex = remaining.lastIndexOf(" ", end);
                    if (splitIndex <= DEFAULT_CHUNK_OVERLAP) {
                        splitIndex = end;
                    }
                    chunks.add(remaining.substring(0, splitIndex).trim());
                    remaining = remaining.substring(splitIndex).trim();
                }
                current = new StringBuilder(remaining);
            }
        }

        if (current.length() > 0) {
            chunks.add(current.toString());
        }

        return chunks;
    }

    private String normalizeTags(String rawTags) {
        if (!StringUtils.hasText(rawTags)) {
            return null;
        }
        String[] pieces = rawTags.split(",");
        List<String> normalized = new ArrayList<>();
        for (String piece : pieces) {
            String candidate = piece.trim();
            if (StringUtils.hasText(candidate)) {
                normalized.add(candidate);
            }
        }
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    private String calculateHash(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            log.warn("计算SHA-256摘要失败：{}", ex.getMessage());
            return null;
        }
    }

    private int estimateTokenCount(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        String[] tokens = text.split("\\s+");
        return tokens.length;
    }

    private String buildSnippet(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.length() <= MAX_SNIPPET_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, MAX_SNIPPET_LENGTH) + "...";
    }

    private MaterialResponse toResponse(Material material) {
        MaterialResponse response = new MaterialResponse();
        response.setId(material.getId());
        response.setWorkspaceId(material.getWorkspaceId());
        response.setTitle(material.getTitle());
        response.setType(material.getType());
        response.setSummary(material.getSummary());
        response.setTags(material.getTags());
        response.setStatus(material.getStatus());
        response.setCreatedAt(material.getCreatedAt());
        response.setUpdatedAt(material.getUpdatedAt());
        return response;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}

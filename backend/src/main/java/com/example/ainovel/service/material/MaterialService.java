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
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.ainovel.dto.material.MaterialCreateRequest;
import com.example.ainovel.dto.material.MaterialResponse;
import com.example.ainovel.dto.material.MaterialReviewDecisionRequest;
import com.example.ainovel.dto.material.MaterialReviewItem;
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
    private final HybridSearchService hybridSearchService;
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
        material.setEntitiesJson(null);
        material.setReviewNotes(null);
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
        return createMaterialFromImport(title, summary, content, workspaceId, userId, sourceJobId, tags, null, null,
            MaterialStatus.PUBLISHED);
    }

    @Transactional
    public MaterialResponse createMaterialFromImport(String title,
                                                     String summary,
                                                     String content,
                                                     Long workspaceId,
                                                     Long userId,
                                                     Long sourceJobId,
                                                     String tags,
                                                     String type,
                                                     String entitiesJson,
                                                     MaterialStatus status) {
        MaterialStatus resolvedStatus = status != null ? status : MaterialStatus.PUBLISHED;
        Material material = new Material();
        material.setWorkspaceId(workspaceId);
        material.setTitle(StringUtils.hasText(title) ? title.trim() : "导入素材");
        material.setType(StringUtils.hasText(type) ? type.trim() : "file");
        material.setSummary(StringUtils.hasText(summary) ? summary.trim() : null);
        material.setStatus(resolvedStatus.name());
        material.setEntitiesJson(StringUtils.hasText(entitiesJson) ? entitiesJson.trim() : null);
        material.setReviewNotes(resolvedStatus == MaterialStatus.PENDING_REVIEW
            ? "LLM 自动解析结果，待审核"
            : null);
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
        return hybridSearchService.search(workspaceId, query, limit);
    }

    @Transactional(readOnly = true)
    public List<MaterialReviewItem> listPendingReview(Long workspaceId) {
        return materialRepository.findByWorkspaceIdAndStatus(workspaceId, MaterialStatus.PENDING_REVIEW.name())
            .stream()
            .map(this::toReviewItem)
            .toList();
    }

    @Transactional
    public MaterialReviewItem approveMaterial(Long materialId,
                                              Long workspaceId,
                                              Long reviewerId,
                                              MaterialReviewDecisionRequest request) {
        Material material = getMaterialForWorkspace(materialId, workspaceId);
        MaterialStatus previousStatus = resolveStatus(material.getStatus());
        if (previousStatus != MaterialStatus.PENDING_REVIEW && previousStatus != MaterialStatus.DRAFT) {
            throw new IllegalStateException("当前状态不支持审核通过");
        }

        applyReviewUpdates(material, request);
        material.setStatus(MaterialStatus.PUBLISHED.name());
        material.setReviewNotes(request != null && StringUtils.hasText(request.getReviewNotes())
            ? request.getReviewNotes().trim()
            : null);
        material.setUpdatedBy(reviewerId);
        Material saved = materialRepository.save(material);

        if (previousStatus != MaterialStatus.PUBLISHED) {
            List<MaterialChunk> chunks = materialChunkRepository.findByMaterialIdOrderBySequenceAsc(saved.getId());
            indexMaterialChunks(saved, chunks);
        }
        return toReviewItem(saved);
    }

    @Transactional
    public MaterialReviewItem rejectMaterial(Long materialId,
                                             Long workspaceId,
                                             Long reviewerId,
                                             MaterialReviewDecisionRequest request) {
        Material material = getMaterialForWorkspace(materialId, workspaceId);
        MaterialStatus previousStatus = resolveStatus(material.getStatus());
        if (previousStatus != MaterialStatus.PENDING_REVIEW && previousStatus != MaterialStatus.DRAFT) {
            throw new IllegalStateException("当前状态不支持驳回");
        }
        applyReviewUpdates(material, request);
        material.setStatus(MaterialStatus.REJECTED.name());
        material.setReviewNotes(request != null && StringUtils.hasText(request.getReviewNotes())
            ? request.getReviewNotes().trim()
            : null);
        material.setUpdatedBy(reviewerId);
        Material saved = materialRepository.save(material);
        return toReviewItem(saved);
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
        int sequence = 0;
        for (String chunkText : chunkTexts) {
            MaterialChunk chunk = new MaterialChunk();
            chunk.setMaterial(saved);
            chunk.setSequence(sequence);
            chunk.setText(chunkText);
            chunk.setHash(calculateHash(chunkText));
            chunk.setTokenCount(estimateTokenCount(chunkText));
            chunkEntities.add(chunk);
            sequence++;
        }

        if (!chunkEntities.isEmpty()) {
            materialChunkRepository.saveAll(chunkEntities);
            indexMaterialChunks(saved, chunkEntities);
        }

        saved.setChunks(chunkEntities);
        return toResponse(saved);
    }

    private void indexMaterialChunks(Material material, List<MaterialChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        if (!MaterialStatus.PUBLISHED.name().equals(material.getStatus())) {
            return;
        }
        List<Document> documents = new ArrayList<>(chunks.size());
        for (MaterialChunk chunk : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("materialId", material.getId());
            metadata.put("workspaceId", material.getWorkspaceId());
            metadata.put("title", material.getTitle());
            metadata.put("chunkSeq", chunk.getSequence());
            if (StringUtils.hasText(material.getSummary())) {
                metadata.put("summary", material.getSummary());
            }
            documents.add(new Document(chunk.getText(), metadata));
        }
        if (documents.isEmpty()) {
            return;
        }
        try {
            vectorStore.add(documents);
        } catch (Exception ex) {
            log.warn("向量存储写入失败（materialId={}）：{}", material.getId(), ex.getMessage());
        }
    }

    private Material getMaterialForWorkspace(Long materialId, Long workspaceId) {
        Material material = materialRepository.findById(materialId)
            .orElseThrow(() -> new IllegalArgumentException("未找到对应素材"));
        if (!Objects.equals(material.getWorkspaceId(), workspaceId)) {
            throw new IllegalArgumentException("无权操作该素材");
        }
        return material;
    }

    private void applyReviewUpdates(Material material, MaterialReviewDecisionRequest request) {
        if (request == null) {
            return;
        }
        if (StringUtils.hasText(request.getTitle())) {
            material.setTitle(request.getTitle().trim());
        }
        if (StringUtils.hasText(request.getType())) {
            material.setType(request.getType().trim());
        }
        if (request.getSummary() != null) {
            material.setSummary(StringUtils.hasText(request.getSummary()) ? request.getSummary().trim() : null);
        }
        if (request.getTags() != null) {
            material.setTags(normalizeTags(request.getTags()));
        }
        if (request.getEntitiesJson() != null) {
            material.setEntitiesJson(StringUtils.hasText(request.getEntitiesJson())
                ? request.getEntitiesJson().trim()
                : null);
        }
    }

    private MaterialReviewItem toReviewItem(Material material) {
        MaterialReviewItem item = new MaterialReviewItem();
        item.setId(material.getId());
        item.setWorkspaceId(material.getWorkspaceId());
        item.setTitle(material.getTitle());
        item.setType(material.getType());
        item.setSummary(material.getSummary());
        item.setTags(material.getTags());
        item.setContent(material.getContent());
        item.setEntitiesJson(material.getEntitiesJson());
        item.setStatus(material.getStatus());
        item.setCreatedAt(material.getCreatedAt());
        item.setUpdatedAt(material.getUpdatedAt());
        item.setReviewNotes(material.getReviewNotes());
        return item;
    }

    private MaterialStatus resolveStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return MaterialStatus.DRAFT;
        }
        try {
            return MaterialStatus.valueOf(status);
        } catch (IllegalArgumentException ignored) {
            return MaterialStatus.DRAFT;
        }
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

    private MaterialResponse toResponse(Material material) {
        MaterialResponse response = new MaterialResponse();
        response.setId(material.getId());
        response.setWorkspaceId(material.getWorkspaceId());
        response.setTitle(material.getTitle());
        response.setType(material.getType());
        response.setSummary(material.getSummary());
        response.setTags(material.getTags());
        response.setStatus(material.getStatus());
        response.setEntitiesJson(material.getEntitiesJson());
        response.setReviewNotes(material.getReviewNotes());
        response.setCreatedAt(material.getCreatedAt());
        response.setUpdatedAt(material.getUpdatedAt());
        return response;
    }

}

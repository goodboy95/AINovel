package com.example.ainovel.service.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.ainovel.dto.material.MaterialSearchResult;
import com.example.ainovel.model.material.MaterialChunk;
import com.example.ainovel.model.material.MaterialStatus;
import com.example.ainovel.repository.material.MaterialChunkRepository;
import com.example.ainovel.service.material.event.MaterialSearchEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 混合检索服务：结合向量与全文检索的结果，提供更高召回率。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridSearchService {

    private static final int DEFAULT_VECTOR_FACTOR = 3;
    private static final int DEFAULT_FULLTEXT_FACTOR = 2;
    private static final double RRF_K = 60.0;
    private final VectorStore vectorStore;
    private final MaterialChunkRepository materialChunkRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<MaterialSearchResult> search(Long workspaceId, Long userId, String query, int limit) {
        if (workspaceId == null || workspaceId <= 0) {
            throw new IllegalArgumentException("workspaceId 无效");
        }
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        int normalizedLimit = Math.min(Math.max(limit, 5), 50);
        long start = System.currentTimeMillis();

        CompletableFuture<List<MaterialSearchResult>> semanticFuture =
            CompletableFuture.supplyAsync(() -> vectorSearch(query, workspaceId, normalizedLimit, "semantic"));
        CompletableFuture<List<MaterialSearchResult>> titleFuture =
            CompletableFuture.supplyAsync(() -> vectorSearch(query, workspaceId, normalizedLimit, "title"));
        CompletableFuture<List<MaterialSearchResult>> keywordFuture =
            CompletableFuture.supplyAsync(() -> vectorSearch(query, workspaceId, normalizedLimit, "keywords"));
        CompletableFuture<List<MaterialSearchResult>> fulltextFuture =
            CompletableFuture.supplyAsync(() -> fulltextSearch(query, workspaceId, normalizedLimit));

        Map<String, List<MaterialSearchResult>> sourceResults = new HashMap<>();
        try {
            sourceResults.put("semantic", semanticFuture.get());
            sourceResults.put("title", titleFuture.get());
            sourceResults.put("keywords", keywordFuture.get());
            sourceResults.put("fulltext", fulltextFuture.get());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("混合检索被中断：{}", ex.getMessage());
        } catch (ExecutionException ex) {
            log.warn("混合检索执行失败：{}", ex.getMessage());
        }

        List<MaterialSearchResult> fused = fuseResults(sourceResults, normalizedLimit);
        long duration = System.currentTimeMillis() - start;
        eventPublisher.publishEvent(new MaterialSearchEvent(workspaceId, userId, query, duration, false));
        return fused;
    }

    private List<MaterialSearchResult> vectorSearch(String query, Long workspaceId, int limit, String vectorType) {
        int topK = Math.max(limit * DEFAULT_VECTOR_FACTOR, limit);
        SearchRequest request = SearchRequest.builder()
            .query(query)
            .topK(topK)
            .build();
        List<Document> documents = vectorStore.similaritySearch(request);
        List<MaterialSearchResult> results = new ArrayList<>();
        for (Document document : documents) {
            Long docWorkspaceId = toLong(document.getMetadata().get("workspaceId"));
            if (!Objects.equals(docWorkspaceId, workspaceId)) {
                continue;
            }
            String docType = asString(document.getMetadata().getOrDefault("vectorType", "semantic"));
            if (!matchesVectorType(docType, vectorType)) {
                continue;
            }
            MaterialSearchResult result = new MaterialSearchResult();
            result.setMaterialId(toLong(document.getMetadata().get("materialId")));
            result.setTitle(asString(document.getMetadata().getOrDefault("title", "")));
            result.setChunkSeq(toInteger(document.getMetadata().get("chunkSeq")));
            result.setSnippet(buildSnippet(document.getText()));
            Double score = document.getScore();
            if (score == null) {
                score = toDouble(document.getMetadata().getOrDefault("score",
                    document.getMetadata().getOrDefault("distance", null)));
            }
            result.setScore(score);
            results.add(result);
        }
        return results;
    }

    private boolean matchesVectorType(String documentType, String expectedType) {
        if (Objects.equals(expectedType, "semantic") && !StringUtils.hasText(documentType)) {
            return true;
        }
        return Objects.equals(documentType, expectedType);
    }

    private List<MaterialSearchResult> fulltextSearch(String query, Long workspaceId, int limit) {
        int rowLimit = Math.max(limit * DEFAULT_FULLTEXT_FACTOR, limit);
        List<Object[]> rows = materialChunkRepository.searchWithFullText(query, workspaceId, rowLimit);
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> chunkIds = rows.stream()
            .map(row -> toLong(row != null && row.length > 0 ? row[0] : null))
            .filter(Objects::nonNull)
            .toList();
        if (chunkIds.isEmpty()) {
            return List.of();
        }
        Map<Long, MaterialChunk> chunkMap = materialChunkRepository.findAllById(chunkIds).stream()
            .filter(chunk -> chunk.getMaterial() != null
                && Objects.equals(chunk.getMaterial().getWorkspaceId(), workspaceId)
                && MaterialStatus.PUBLISHED.name().equals(chunk.getMaterial().getStatus()))
            .collect(Collectors.toMap(MaterialChunk::getId, Function.identity()));

        List<MaterialSearchResult> results = new ArrayList<>();
        for (Object[] row : rows) {
            Long chunkId = toLong(row != null && row.length > 0 ? row[0] : null);
            if (chunkId == null) {
                continue;
            }
            MaterialChunk chunk = chunkMap.get(chunkId);
            if (chunk == null) {
                continue;
            }
            Double score = toDouble(row != null && row.length > 1 ? row[1] : null);
            MaterialSearchResult result = new MaterialSearchResult();
            result.setMaterialId(chunk.getMaterial().getId());
            result.setTitle(chunk.getMaterial().getTitle());
            result.setChunkSeq(chunk.getSequence());
            result.setSnippet(buildSnippet(chunk.getText()));
            result.setScore(score);
            results.add(result);
        }
        return results;
    }

    private List<MaterialSearchResult> fuseResults(Map<String, List<MaterialSearchResult>> sources, int limit) {
        Map<String, Double> scoreMap = new HashMap<>();
        Map<String, MaterialSearchResult> representatives = new HashMap<>();
        sources.forEach((channel, list) -> {
            if (list == null) {
                return;
            }
            for (int i = 0; i < list.size(); i++) {
                MaterialSearchResult result = list.get(i);
                String key = buildKey(result);
                double rankScore = 1.0 / (RRF_K + (i + 1));
                scoreMap.merge(key, rankScore, Double::sum);
                representatives.putIfAbsent(key, result);
            }
        });
        return scoreMap.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .map(entry -> {
                MaterialSearchResult base = representatives.get(entry.getKey());
                MaterialSearchResult enriched = new MaterialSearchResult();
                enriched.setMaterialId(base.getMaterialId());
                enriched.setTitle(base.getTitle());
                enriched.setChunkSeq(base.getChunkSeq());
                enriched.setSnippet(base.getSnippet());
                enriched.setScore(entry.getValue());
                return enriched;
            })
            .toList();
    }

    private String buildKey(MaterialSearchResult result) {
        Long materialId = result.getMaterialId() != null ? result.getMaterialId() : -1L;
        Integer seq = result.getChunkSeq() != null ? result.getChunkSeq() : -1;
        int snippetHash = result.getSnippet() != null ? result.getSnippet().hashCode() : 0;
        return materialId + ":" + seq + ":" + snippetHash;
    }

    private String buildSnippet(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.trim();
        if (normalized.length() <= 200) {
            return normalized;
        }
        return normalized.substring(0, 200) + "...";
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

package com.example.ainovel.service.material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.ainovel.dto.material.MaterialDuplicateCandidate;
import com.example.ainovel.dto.material.MaterialMergeRequest;
import com.example.ainovel.dto.material.MaterialResponse;
import com.example.ainovel.model.material.Material;
import com.example.ainovel.model.material.MaterialChunk;
import com.example.ainovel.model.material.MaterialStatus;
import com.example.ainovel.repository.material.MaterialChunkRepository;
import com.example.ainovel.repository.material.MaterialRepository;
import com.example.ainovel.security.PermissionLevel;
import com.example.ainovel.security.annotation.CheckPermission;
import com.example.ainovel.security.annotation.PermissionResourceId;
import com.example.ainovel.service.security.PermissionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeduplicationService {

    private static final double DUPLICATE_THRESHOLD = 0.98;

    private final MaterialRepository materialRepository;
    private final MaterialChunkRepository materialChunkRepository;
    private final PermissionService permissionService;
    private final MaterialVectorService materialVectorService;

    @CheckPermission(resourceType = PermissionService.RESOURCE_WORKSPACE, level = PermissionLevel.WRITE)
    @Transactional(readOnly = true)
    public List<MaterialDuplicateCandidate> findDuplicates(
        @PermissionResourceId(PermissionService.RESOURCE_WORKSPACE) Long workspaceId) {
        List<MaterialChunk> chunks = materialChunkRepository.findAllByWorkspaceId(workspaceId);
        if (chunks.isEmpty()) {
            return List.of();
        }
        Map<String, List<MaterialChunk>> byHash = chunks.stream()
            .filter(chunk -> chunk.getMaterial() != null)
            .collect(Collectors.groupingBy(chunk -> StringUtils.hasText(chunk.getHash()) ? chunk.getHash() : ""));

        Map<String, MaterialDuplicateCandidate> candidates = new HashMap<>();
        for (Map.Entry<String, List<MaterialChunk>> entry : byHash.entrySet()) {
            List<MaterialChunk> duplicates = entry.getValue();
            if (duplicates.size() < 2) {
                continue;
            }
            for (int i = 0; i < duplicates.size(); i++) {
                for (int j = i + 1; j < duplicates.size(); j++) {
                    MaterialChunk left = duplicates.get(i);
                    MaterialChunk right = duplicates.get(j);
                    if (left.getMaterial() == null || right.getMaterial() == null) {
                        continue;
                    }
                    if (Objects.equals(left.getMaterial().getId(), right.getMaterial().getId())) {
                        continue;
                    }
                    double similarity = Objects.equals(entry.getKey(), "")
                        ? cosineSimilarity(left.getText(), right.getText())
                        : 1.0;
                    if (similarity < DUPLICATE_THRESHOLD) {
                        continue;
                    }
                    registerCandidate(candidates, left, right, similarity);
                }
            }
        }
        return candidates.values().stream()
            .sorted(Comparator.comparingDouble(MaterialDuplicateCandidate::getSimilarity).reversed())
            .limit(50)
            .toList();
    }

    @CheckPermission(resourceType = PermissionService.RESOURCE_WORKSPACE, level = PermissionLevel.WRITE)
    @Transactional
    public MaterialResponse mergeMaterials(
        @PermissionResourceId(PermissionService.RESOURCE_WORKSPACE) Long workspaceId,
        Long userId,
        MaterialMergeRequest request) {
        if (request == null
            || request.getSourceMaterialId() == null
            || request.getTargetMaterialId() == null) {
            throw new IllegalArgumentException("缺少合并目标");
        }
        if (Objects.equals(request.getSourceMaterialId(), request.getTargetMaterialId())) {
            throw new IllegalArgumentException("源素材与目标素材不能相同");
        }

        Material source = getMaterialForWorkspace(request.getSourceMaterialId(), workspaceId);
        Material target = getMaterialForWorkspace(request.getTargetMaterialId(), workspaceId);
        if (!MaterialStatus.PUBLISHED.name().equals(target.getStatus())) {
            throw new IllegalStateException("目标素材必须处于已发布状态");
        }

        Map<String, Long> context = Map.of(PermissionService.RESOURCE_WORKSPACE, workspaceId);
        permissionService.assertPermission(userId, "MATERIAL", target.getId(), PermissionLevel.WRITE, context);
        permissionService.assertPermission(userId, "MATERIAL", source.getId(), PermissionLevel.WRITE, context);

        List<MaterialChunk> targetChunks = materialChunkRepository.findByMaterialIdOrderBySequenceAsc(target.getId());
        List<MaterialChunk> sourceChunks = materialChunkRepository.findByMaterialIdOrderBySequenceAsc(source.getId());
        Set<String> existingHashes = targetChunks.stream()
            .map(MaterialChunk::getHash)
            .filter(StringUtils::hasText)
            .collect(Collectors.toSet());

        List<MaterialChunk> appendedChunks = new ArrayList<>();
        int sequence = targetChunks.isEmpty() ? 0 : targetChunks.get(targetChunks.size() - 1).getSequence() + 1;
        for (MaterialChunk chunk : sourceChunks) {
            if (StringUtils.hasText(chunk.getHash()) && existingHashes.contains(chunk.getHash())) {
                continue;
            }
            MaterialChunk clone = new MaterialChunk();
            clone.setMaterial(target);
            clone.setSequence(sequence++);
            clone.setText(chunk.getText());
            clone.setHash(chunk.getHash());
            clone.setTokenCount(chunk.getTokenCount());
            appendedChunks.add(clone);
        }
        if (!appendedChunks.isEmpty()) {
            materialChunkRepository.saveAll(appendedChunks);
        }

        target.setContent(mergeContent(target.getContent(), appendedChunks));
        if (request.isMergeTags()) {
            target.setTags(mergeTags(target.getTags(), source.getTags()));
        }
        if (request.isMergeSummaryWhenEmpty() && !StringUtils.hasText(target.getSummary())) {
            target.setSummary(source.getSummary());
        }
        target.setUpdatedBy(userId);
        Material savedTarget = materialRepository.save(target);

        source.setStatus(MaterialStatus.ARCHIVED.name());
        source.setReviewNotes(buildMergeNote(request));
        source.setUpdatedBy(userId);
        materialRepository.save(source);
        permissionService.revokePermissionsForMaterial(source.getId());

        if (!appendedChunks.isEmpty() || request.isMergeTags()) {
            List<MaterialChunk> updatedChunks = materialChunkRepository.findByMaterialIdOrderBySequenceAsc(savedTarget.getId());
            materialVectorService.indexMaterial(savedTarget, updatedChunks, true);
        }

        return toResponse(savedTarget);
    }

    private void registerCandidate(Map<String, MaterialDuplicateCandidate> candidates,
                                   MaterialChunk left,
                                   MaterialChunk right,
                                   double similarity) {
        long firstId = Math.min(left.getMaterial().getId(), right.getMaterial().getId());
        long secondId = Math.max(left.getMaterial().getId(), right.getMaterial().getId());
        String key = firstId + "::" + secondId;
        MaterialDuplicateCandidate candidate = candidates.computeIfAbsent(key, k -> {
            MaterialDuplicateCandidate dto = new MaterialDuplicateCandidate();
            dto.setMaterialId(firstId);
            dto.setMaterialTitle(firstId == left.getMaterial().getId()
                ? left.getMaterial().getTitle()
                : right.getMaterial().getTitle());
            dto.setDuplicateMaterialId(secondId);
            dto.setDuplicateTitle(secondId == left.getMaterial().getId()
                ? left.getMaterial().getTitle()
                : right.getMaterial().getTitle());
            dto.setSimilarity(similarity);
            return dto;
        });
        candidate.setSimilarity(Math.max(candidate.getSimilarity(), similarity));

        MaterialDuplicateCandidate.DuplicateChunk chunkDto = new MaterialDuplicateCandidate.DuplicateChunk();
        chunkDto.setMaterialChunkId(left.getId());
        chunkDto.setDuplicateChunkId(right.getId());
        chunkDto.setSimilarity(similarity);
        chunkDto.setSnippet(buildSnippet(left.getText()));
        candidate.getOverlappingChunks().add(chunkDto);
    }

    private Material getMaterialForWorkspace(Long materialId, Long workspaceId) {
        Material material = materialRepository.findById(materialId)
            .orElseThrow(() -> new IllegalArgumentException("未找到对应素材"));
        if (!Objects.equals(material.getWorkspaceId(), workspaceId)) {
            throw new AccessDeniedException("素材不属于当前工作区");
        }
        return material;
    }

    private double cosineSimilarity(String left, String right) {
        Map<String, Integer> leftFreq = termFrequency(left);
        Map<String, Integer> rightFreq = termFrequency(right);
        if (leftFreq.isEmpty() || rightFreq.isEmpty()) {
            return 0.0;
        }
        Set<String> union = new HashSet<>();
        union.addAll(leftFreq.keySet());
        union.addAll(rightFreq.keySet());
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (String token : union) {
            int leftValue = leftFreq.getOrDefault(token, 0);
            int rightValue = rightFreq.getOrDefault(token, 0);
            dot += (double) leftValue * rightValue;
        }
        for (int value : leftFreq.values()) {
            leftNorm += Math.pow(value, 2);
        }
        for (int value : rightFreq.values()) {
            rightNorm += Math.pow(value, 2);
        }
        double denominator = Math.sqrt(leftNorm) * Math.sqrt(rightNorm);
        if (denominator == 0) {
            return 0.0;
        }
        return dot / denominator;
    }

    private Map<String, Integer> termFrequency(String text) {
        Map<String, Integer> freq = new HashMap<>();
        if (!StringUtils.hasText(text)) {
            return freq;
        }
        String[] tokens = text.toLowerCase().split("\\W+");
        for (String token : tokens) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            freq.merge(token, 1, Integer::sum);
        }
        return freq;
    }

    private String mergeContent(String original, List<MaterialChunk> appended) {
        if (appended.isEmpty()) {
            return original;
        }
        StringBuilder builder = new StringBuilder(StringUtils.hasText(original) ? original.trim() : "");
        for (MaterialChunk chunk : appended) {
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append(chunk.getText());
        }
        return builder.toString();
    }

    private String mergeTags(String targetTags, String sourceTags) {
        Set<String> merged = new HashSet<>();
        if (StringUtils.hasText(targetTags)) {
            merged.addAll(List.of(targetTags.split(",")));
        }
        if (StringUtils.hasText(sourceTags)) {
            merged.addAll(List.of(sourceTags.split(",")));
        }
        return merged.stream()
            .map(String::trim)
            .filter(StringUtils::hasText)
            .sorted()
            .collect(Collectors.joining(","));
    }

    private String buildMergeNote(MaterialMergeRequest request) {
        if (request == null) {
            return "已合并到其它素材";
        }
        if (StringUtils.hasText(request.getNote())) {
            return request.getNote();
        }
        return "已合并到素材 " + request.getTargetMaterialId();
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

    private String buildSnippet(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 200) + "...";
    }
}

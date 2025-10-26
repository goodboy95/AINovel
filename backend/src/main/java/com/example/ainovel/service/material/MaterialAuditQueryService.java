package com.example.ainovel.service.material;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ainovel.dto.material.MaterialCitationDto;
import com.example.ainovel.model.audit.Citation;
import com.example.ainovel.model.material.Material;
import com.example.ainovel.repository.audit.CitationRepository;
import com.example.ainovel.repository.material.MaterialRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MaterialAuditQueryService {

    private final CitationRepository citationRepository;
    private final MaterialRepository materialRepository;

    @Transactional(readOnly = true)
    public List<MaterialCitationDto> listCitations(Long workspaceId, Long materialId) {
        Material material = materialRepository.findById(materialId)
            .orElseThrow(() -> new IllegalArgumentException("未找到对应素材"));
        if (!Objects.equals(material.getWorkspaceId(), workspaceId)) {
            throw new IllegalArgumentException("无权查看该素材的引用历史");
        }
        return citationRepository.findByMaterialIdOrderByCreatedAtDesc(materialId).stream()
            .map(this::toDto)
            .toList();
    }

    private MaterialCitationDto toDto(Citation citation) {
        MaterialCitationDto dto = new MaterialCitationDto();
        dto.setId(citation.getId());
        dto.setMaterialId(citation.getMaterialId());
        dto.setChunkId(citation.getChunkId());
        dto.setChunkSeq(citation.getChunkSeq());
        dto.setDocumentType(citation.getDocumentType());
        dto.setDocumentId(citation.getDocumentId());
        dto.setUserId(citation.getUserId());
        dto.setUsageContext(citation.getUsageContext());
        dto.setCreatedAt(citation.getCreatedAt());
        return dto;
    }
}


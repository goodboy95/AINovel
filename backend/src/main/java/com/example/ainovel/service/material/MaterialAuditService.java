package com.example.ainovel.service.material;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ainovel.model.audit.Citation;
import com.example.ainovel.model.audit.SearchLog;
import com.example.ainovel.repository.audit.CitationRepository;
import com.example.ainovel.repository.audit.SearchLogRepository;
import com.example.ainovel.service.material.event.MaterialCitationEvent;
import com.example.ainovel.service.material.event.MaterialSearchEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialAuditService {

    private final SearchLogRepository searchLogRepository;
    private final CitationRepository citationRepository;

    @Async
    @Transactional
    @EventListener
    public void handleSearchEvent(MaterialSearchEvent event) {
        try {
            SearchLog log = new SearchLog();
            log.setWorkspaceId(event.workspaceId());
            log.setUserId(event.userId());
            log.setQuery(event.query());
            log.setLatencyMs(event.latencyMs());
            log.setUsedInGeneration(event.usedInGeneration());
            searchLogRepository.save(log);
        } catch (Exception ex) {
            log.warn("记录检索审计日志失败: {}", ex.getMessage());
        }
    }

    @Async
    @Transactional
    @EventListener
    public void handleCitationEvent(MaterialCitationEvent event) {
        try {
            Citation citation = new Citation();
            citation.setWorkspaceId(event.workspaceId());
            citation.setUserId(event.userId());
            citation.setDocumentType(event.documentType());
            citation.setDocumentId(event.documentId());
            citation.setMaterialId(event.materialId());
            citation.setChunkId(event.chunkId());
            citation.setChunkSeq(event.chunkSeq());
            citation.setUsageContext(event.usageContext());
            citationRepository.save(citation);
        } catch (Exception ex) {
            log.warn("记录引用审计日志失败: {}", ex.getMessage());
        }
    }

    @Transactional
    public void recordCitation(Long workspaceId,
                               Long userId,
                               String documentType,
                               Long documentId,
                               Long materialId,
                               Long chunkId,
                               Integer chunkSeq,
                               String usageContext) {
        handleCitationEvent(new MaterialCitationEvent(workspaceId, userId, documentType, documentId, materialId,
            chunkId, chunkSeq, usageContext));
    }
}

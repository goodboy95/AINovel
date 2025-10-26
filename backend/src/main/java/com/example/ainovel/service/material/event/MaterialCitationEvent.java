package com.example.ainovel.service.material.event;

public record MaterialCitationEvent(Long workspaceId,
                                    Long userId,
                                    String documentType,
                                    Long documentId,
                                    Long materialId,
                                    Long chunkId,
                                    Integer chunkSeq,
                                    String usageContext) {
}


package com.ainovel.app.material.dto;

import java.util.UUID;

public record MaterialSearchResultDto(UUID materialId, String title, String snippet, double score, Integer chunkSeq) {}

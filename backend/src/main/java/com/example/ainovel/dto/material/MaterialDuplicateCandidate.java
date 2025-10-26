package com.example.ainovel.dto.material;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class MaterialDuplicateCandidate {

    private Long materialId;
    private String materialTitle;
    private Long duplicateMaterialId;
    private String duplicateTitle;
    private double similarity;
    private List<DuplicateChunk> overlappingChunks = new ArrayList<>();

    @Data
    public static class DuplicateChunk {
        private Long materialChunkId;
        private Long duplicateChunkId;
        private double similarity;
        private String snippet;
    }
}


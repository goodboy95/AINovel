package com.example.ainovel.repository.material;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.ainovel.model.material.MaterialChunk;

@Repository
public interface MaterialChunkRepository extends JpaRepository<MaterialChunk, Long> {

    List<MaterialChunk> findByMaterialIdOrderBySequenceAsc(Long materialId);

    void deleteByMaterialId(Long materialId);

    @Query("""
        SELECT mc
          FROM MaterialChunk mc
          JOIN FETCH mc.material m
         WHERE m.workspaceId = :workspaceId
        """)
    List<MaterialChunk> findAllByWorkspaceId(Long workspaceId);

    @Query(value = """
        SELECT mc.id,
               MATCH (mc.text) AGAINST (?1 IN BOOLEAN MODE) AS score
          FROM material_chunks mc
          JOIN materials m ON mc.material_id = m.id
         WHERE m.workspace_id = ?2
           AND MATCH (mc.text) AGAINST (?1 IN BOOLEAN MODE) > 0
           AND m.status = 'PUBLISHED'
         ORDER BY score DESC
         LIMIT ?3
        """, nativeQuery = true)
    List<Object[]> searchWithFullText(String query, Long workspaceId, int limit);
}

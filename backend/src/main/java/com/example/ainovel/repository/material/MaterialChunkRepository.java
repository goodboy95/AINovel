package com.example.ainovel.repository.material;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ainovel.model.material.MaterialChunk;

@Repository
public interface MaterialChunkRepository extends JpaRepository<MaterialChunk, Long> {

    List<MaterialChunk> findByMaterialIdOrderBySequenceAsc(Long materialId);
}


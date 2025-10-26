package com.example.ainovel.repository.audit;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ainovel.model.audit.Citation;

@Repository
public interface CitationRepository extends JpaRepository<Citation, Long> {

    List<Citation> findByMaterialIdOrderByCreatedAtDesc(Long materialId);
}


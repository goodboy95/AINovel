package com.example.ainovel.repository;

import com.example.ainovel.model.Manuscript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ManuscriptRepository extends JpaRepository<Manuscript, Long> {
    List<Manuscript> findByOutlineCardId(Long outlineId);
    Optional<Manuscript> findFirstByOutlineCardIdOrderByCreatedAtDesc(Long outlineId);
}
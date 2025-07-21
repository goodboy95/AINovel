package com.example.ainovel.repository;

import com.example.ainovel.model.ManuscriptSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManuscriptSectionRepository extends JpaRepository<ManuscriptSection, Long> {
    List<ManuscriptSection> findBySceneOutlineChapterOutlineCardId(Long outlineCardId);
}

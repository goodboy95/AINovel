package com.example.ainovel.repository;

import com.example.ainovel.model.OutlineChapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for {@link OutlineChapter} entities.
 */
public interface OutlineChapterRepository extends JpaRepository<OutlineChapter, Long> {

    /**
     * Finds all chapters associated with a specific outline.
     * @param outlineCardId The ID of the outline card.
     * @return A list of chapters.
     */
    List<OutlineChapter> findByOutlineCardId(Long outlineCardId);
}

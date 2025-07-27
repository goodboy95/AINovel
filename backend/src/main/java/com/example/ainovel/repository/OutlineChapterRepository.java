package com.example.ainovel.repository;

import com.example.ainovel.model.OutlineChapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

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

    /**
     * Finds a specific chapter by its outline ID and chapter number.
     * @param outlineCardId The ID of the outline card.
     * @param chapterNumber The number of the chapter.
     * @return An Optional containing the found chapter, or empty if not found.
     */
    Optional<OutlineChapter> findByOutlineCardIdAndChapterNumber(Long outlineCardId, int chapterNumber);
}

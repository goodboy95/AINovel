package com.example.ainovel.repository;

import com.example.ainovel.model.OutlineScene;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link OutlineScene} entities.
 */
public interface OutlineSceneRepository extends JpaRepository<OutlineScene, Long> {

    /**
     * Finds all scenes for a specific chapter.
     * @param chapterId The ID of the chapter.
     * @return A list of scenes.
     */
    List<OutlineScene> findByOutlineChapterId(Long chapterId);

    /**
     * Finds the first scene in a chapter with a scene number less than the given value, ordered by scene number descending.
     * This is useful for finding the immediately preceding scene.
     * @param outlineChapterId The ID of the chapter.
     * @param sceneNumber The scene number to compare against.
     * @return An optional containing the preceding scene if found.
     */
    Optional<OutlineScene> findFirstByOutlineChapterIdAndSceneNumberLessThanOrderBySceneNumberDesc(Long outlineChapterId, Integer sceneNumber);

    /**
     * Finds all scenes in a chapter with a scene number less than the given value, ordered by scene number ascending.
     * This is useful for gathering context from all previous scenes in a chapter.
     * @param outlineChapterId The ID of the chapter.
     * @param sceneNumber The scene number to compare against.
     * @return A list of preceding scenes in the chapter.
     */
    List<OutlineScene> findByOutlineChapterIdAndSceneNumberLessThanOrderBySceneNumberAsc(Long outlineChapterId, Integer sceneNumber);

    /**
     * Finds all scenes for a given list of chapter IDs.
     * @param chapterIds The list of chapter IDs.
     * @return A list of scenes.
     */
    List<OutlineScene> findByOutlineChapterIdIn(List<Long> chapterIds);
}

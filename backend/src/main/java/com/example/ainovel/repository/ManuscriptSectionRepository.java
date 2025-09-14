package com.example.ainovel.repository;

import com.example.ainovel.model.ManuscriptSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link ManuscriptSection} entities.
 */
public interface ManuscriptSectionRepository extends JpaRepository<ManuscriptSection, Long> {

    /**
     * Finds all manuscript sections for a given list of scene IDs.
     * @param sceneIds The list of scene IDs.
     * @return A list of manuscript sections.
     */
    List<ManuscriptSection> findBySceneIdIn(List<Long> sceneIds);

    /**
     * Finds all manuscript sections for a specific scene.
     * @param sceneId The ID of the scene.
     * @return A list of manuscript sections.
     */
    List<ManuscriptSection> findBySceneId(Long sceneId);

    /**
     * Finds all sections by manuscript id.
     */
    List<ManuscriptSection> findByManuscript_Id(Long manuscriptId);

    /**
     * Finds the most recent, active manuscript section for a specific scene.
     * @param sceneId The ID of the scene.
     * @return An optional containing the latest active manuscript section if found.
     */
    Optional<ManuscriptSection> findFirstBySceneIdAndIsActiveTrueOrderByVersionDesc(Long sceneId);

    /**
     * Finds the most recent, active manuscript section for a specific scene within a given manuscript.
     */
    Optional<ManuscriptSection> findFirstByManuscript_IdAndSceneIdAndIsActiveTrueOrderByVersionDesc(Long manuscriptId, Long sceneId);
}

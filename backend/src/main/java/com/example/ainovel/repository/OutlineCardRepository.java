package com.example.ainovel.repository;

import com.example.ainovel.model.OutlineCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository interface for {@link OutlineCard} entities.
 */
public interface OutlineCardRepository extends JpaRepository<OutlineCard, Long> {

    /**
     * Finds all outlines associated with a specific story card.
     * @param storyCardId The ID of the story card.
     * @return A list of outlines.
     */
    List<OutlineCard> findByStoryCardId(Long storyCardId);
}

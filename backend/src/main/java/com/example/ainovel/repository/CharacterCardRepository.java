package com.example.ainovel.repository;

import com.example.ainovel.model.CharacterCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * Repository interface for {@link CharacterCard} entities.
 */
public interface CharacterCardRepository extends JpaRepository<CharacterCard, Long> {

    /**
     * Finds all character cards associated with a specific story card.
     * @param storyCardId The ID of the story card.
     * @return A list of character cards.
     */
    List<CharacterCard> findByStoryCardId(Long storyCardId);
}

package com.example.ainovel.repository;

import com.example.ainovel.model.CharacterCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CharacterCardRepository extends JpaRepository<CharacterCard, Long> {
    List<CharacterCard> findByStoryCardId(Long storyCardId);
}

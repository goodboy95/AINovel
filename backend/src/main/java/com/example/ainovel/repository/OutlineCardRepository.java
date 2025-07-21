package com.example.ainovel.repository;

import com.example.ainovel.model.OutlineCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutlineCardRepository extends JpaRepository<OutlineCard, Long> {
    List<OutlineCard> findByStoryCardId(Long storyCardId);
}

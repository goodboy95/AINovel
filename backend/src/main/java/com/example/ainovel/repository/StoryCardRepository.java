package com.example.ainovel.repository;

import com.example.ainovel.model.StoryCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StoryCardRepository extends JpaRepository<StoryCard, Long> {
    List<StoryCard> findByUserId(Long userId);
    boolean existsByIdAndUserId(Long id, Long userId);
}

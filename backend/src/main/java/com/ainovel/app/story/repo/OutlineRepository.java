package com.ainovel.app.story.repo;

import com.ainovel.app.story.model.Outline;
import com.ainovel.app.story.model.Story;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutlineRepository extends JpaRepository<Outline, UUID> {
    List<Outline> findByStory(Story story);
}

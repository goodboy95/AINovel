package com.ainovel.app.story.repo;

import com.ainovel.app.story.model.Story;
import com.ainovel.app.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StoryRepository extends JpaRepository<Story, UUID> {
    List<Story> findByUser(User user);
}

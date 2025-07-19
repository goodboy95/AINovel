package com.ainovel.app.manuscript.repo;

import com.ainovel.app.manuscript.model.Manuscript;
import com.ainovel.app.story.model.Outline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ManuscriptRepository extends JpaRepository<Manuscript, UUID> {
    List<Manuscript> findByOutline(Outline outline);
}

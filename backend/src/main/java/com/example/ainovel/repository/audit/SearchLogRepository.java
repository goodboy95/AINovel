package com.example.ainovel.repository.audit;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ainovel.model.audit.SearchLog;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    List<SearchLog> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
}


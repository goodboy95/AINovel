package com.example.ainovel.repository.material;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ainovel.model.material.FileImportJob;

@Repository
public interface FileImportJobRepository extends JpaRepository<FileImportJob, Long> {

    Optional<FileImportJob> findByIdAndWorkspaceId(Long id, Long workspaceId);
}


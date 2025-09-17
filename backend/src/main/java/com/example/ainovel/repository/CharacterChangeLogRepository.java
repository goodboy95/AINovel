package com.example.ainovel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ainovel.model.CharacterChangeLog;

public interface CharacterChangeLogRepository extends JpaRepository<CharacterChangeLog, Long> {

    Optional<CharacterChangeLog> findFirstByCharacter_IdAndManuscript_IdAndDeletedAtIsNullOrderByChapterNumberDescSectionNumberDescCreatedAtDesc(
            Long characterId,
            Long manuscriptId);

    List<CharacterChangeLog> findByManuscript_IdAndSceneIdAndDeletedAtIsNullOrderByCharacter_Id(
            Long manuscriptId,
            Long sceneId);

    List<CharacterChangeLog> findByCharacter_IdAndManuscript_IdAndDeletedAtIsNullOrderByChapterNumberAscSectionNumberAsc(
            Long characterId,
            Long manuscriptId);
}


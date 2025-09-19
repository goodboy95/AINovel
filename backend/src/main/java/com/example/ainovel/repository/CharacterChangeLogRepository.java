package com.example.ainovel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.ainovel.model.CharacterChangeLog;

/**
 * Repository for character change logs.
 */
public interface CharacterChangeLogRepository extends JpaRepository<CharacterChangeLog, Long> {

    Optional<CharacterChangeLog> findFirstByManuscript_IdAndCharacter_IdOrderByCreatedAtDesc(Long manuscriptId, Long characterId);

    List<CharacterChangeLog> findByManuscript_IdAndCharacter_IdOrderByCreatedAtDesc(Long manuscriptId, Long characterId);

    List<CharacterChangeLog> findByManuscript_IdAndChapterNumberAndSectionNumber(Long manuscriptId, Integer chapterNumber, Integer sectionNumber);

    List<CharacterChangeLog> findByManuscript_IdOrderByChapterNumberAscSectionNumberAscCreatedAtAsc(Long manuscriptId);
}

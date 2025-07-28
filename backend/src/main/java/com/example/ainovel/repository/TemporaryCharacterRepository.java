package com.example.ainovel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ainovel.model.TemporaryCharacter;

@Repository
public interface TemporaryCharacterRepository extends JpaRepository<TemporaryCharacter, Long> {
}
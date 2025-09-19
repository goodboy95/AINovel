package com.example.ainovel.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Represents the state of a core character within a specific outline scene.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "scene_characters")
public class SceneCharacter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scene_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    private OutlineScene scene;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_card_id")
    @ToString.Exclude
    private CharacterCard characterCard;

    @Column(name = "character_name", nullable = false, length = 255)
    private String characterName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String status;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String thought;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String action;
}

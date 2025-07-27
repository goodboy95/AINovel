package com.example.ainovel.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Represents a single scene within a chapter of an outline.
 */
@Data
@Entity
@Table(name = "outline_scenes")
public class OutlineScene {

    /**
     * The unique identifier for the scene.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The chapter this scene belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    @JsonBackReference
    @lombok.ToString.Exclude
    private OutlineChapter outlineChapter;

    /**
     * The sequential number of the scene within the chapter.
     */
    @Column(name = "scene_number", nullable = false)
    private Integer sceneNumber;

    /**
     * A synopsis of the events that occur in this scene.
     */
    @Lob
    private String synopsis;

    /**
     * The estimated number of words to be written for this scene.
     */
    @Column(name = "expected_words")
    private Integer expectedWords;

    /**
     * A list of characters present in this scene.
     */
    @Lob
    @Column(name = "present_characters")
    private String presentCharacters;

    /**
     * Detailed description of the characters' states, thoughts, and actions in this scene.
     */
    @Lob
    @Column(name = "character_states")
    private String characterStates;
}

package com.example.ainovel.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

/**
 * Represents a character card, containing detailed information about a character in a story.
 */
@Data
@Entity
@Table(name = "character_cards")
public class CharacterCard {

    /**
     * The unique identifier for the character card.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who owns this character card.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The story card this character belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_card_id", nullable = false)
    @JsonBackReference
    private StoryCard storyCard;

    /**
     * The name of the character.
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * A brief synopsis of the character (e.g., age, gender, appearance, personality).
     */
    @Column(columnDefinition = "TEXT")
    private String synopsis;

    /**
     * Detailed background story of the character.
     */
    @Column(columnDefinition = "TEXT")
    private String details;

    /**
     * The character's relationships with other characters.
     */
    @Column(columnDefinition = "TEXT")
    private String relationships;

    /**
     * URL for the character's avatar image.
     */
    @Column(length = 255)
    private String avatarUrl;

    /**
     * Timestamp of when the character card was created.
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last update to the character card.
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

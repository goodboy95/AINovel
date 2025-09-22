package com.example.ainovel.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a story outline, which contains a collection of chapters and scenes.
 */
@Data
@Entity
@Table(name = "outline_cards")
public class OutlineCard {

    /**
     * The unique identifier for the outline.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who owns this outline.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The story card this outline belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_card_id", nullable = false)
    @JsonBackReference
    private StoryCard storyCard;

    /**
     * The title of the outline.
     */
    @Column(nullable = false)
    private String title;

    /**
     * The narrative point of view for the story (e.g., "First Person", "Third Person Limited").
     */
    @Column(name = "point_of_view")
    private String pointOfView;

    /**
     * The list of chapters that make up this outline.
     */
    @OneToMany(mappedBy = "outlineCard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("chapterNumber ASC")
    @JsonManagedReference
    @ToString.Exclude
    private List<OutlineChapter> chapters;

    /**
     * The list of manuscripts created under this outline.
     * Cascade and orphanRemoval ensure manuscripts are deleted with the outline.
     */
    @OneToMany(mappedBy = "outlineCard", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Manuscript> manuscripts;

    @Column(name = "world_id")
    private Long worldId;

    /**
     * Timestamp of when the outline was created.
     */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last update to the outline.
     */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public Long getWorldId() {
        return worldId;
    }

    public void setWorldId(Long worldId) {
        this.worldId = worldId;
    }
}

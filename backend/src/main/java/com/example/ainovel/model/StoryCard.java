package com.example.ainovel.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "story_cards")
public class StoryCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(length = 50)
    private String genre;

    @Column(length = 50)
    private String tone;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(columnDefinition = "TEXT")
    private String storyArc;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "storyCard", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<CharacterCard> characters;

    @OneToMany(mappedBy = "storyCard", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<OutlineCard> outlines;

    // Getters and Setters

    public List<OutlineCard> getOutlines() {
        return outlines;
    }

    public void setOutlines(List<OutlineCard> outlines) {
        this.outlines = outlines;
    }

    public List<CharacterCard> getCharacters() {
        return characters;
    }

    public void setCharacters(List<CharacterCard> characters) {
        this.characters = characters;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getStoryArc() {
        return storyArc;
    }

    public void setStoryArc(String storyArc) {
        this.storyArc = storyArc;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

package com.example.ainovel.model;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "outline_cards")
public class OutlineCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "story_card_id", nullable = false)
    private StoryCard storyCard;

    @Column(nullable = false)
    private String title;

    @Column(name = "point_of_view")
    private String pointOfView;

    @OneToMany(mappedBy = "outlineCard", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("chapterNumber ASC")
    private List<OutlineChapter> chapters;

    @Column(name = "created_at", updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    @Column(name = "updated_at", updatable = false, insertable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Timestamp updatedAt;

    // Getters and Setters
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

    public StoryCard getStoryCard() {
        return storyCard;
    }

    public void setStoryCard(StoryCard storyCard) {
        this.storyCard = storyCard;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPointOfView() {
        return pointOfView;
    }

    public void setPointOfView(String pointOfView) {
        this.pointOfView = pointOfView;
    }

    public List<OutlineChapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<OutlineChapter> chapters) {
        this.chapters = chapters;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }
}

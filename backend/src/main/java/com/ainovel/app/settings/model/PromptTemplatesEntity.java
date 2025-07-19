package com.ainovel.app.settings.model;

import com.ainovel.app.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prompt_templates")
public class PromptTemplatesEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Lob
    private String storyCreation;

    @Lob
    private String outlineChapter;

    @Lob
    private String manuscriptSection;

    @Lob
    private String refineWithInstruction;

    @Lob
    private String refineWithoutInstruction;

    @UpdateTimestamp
    private Instant updatedAt;

    public PromptTemplatesEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getStoryCreation() { return storyCreation; }
    public void setStoryCreation(String storyCreation) { this.storyCreation = storyCreation; }
    public String getOutlineChapter() { return outlineChapter; }
    public void setOutlineChapter(String outlineChapter) { this.outlineChapter = outlineChapter; }
    public String getManuscriptSection() { return manuscriptSection; }
    public void setManuscriptSection(String manuscriptSection) { this.manuscriptSection = manuscriptSection; }
    public String getRefineWithInstruction() { return refineWithInstruction; }
    public void setRefineWithInstruction(String refineWithInstruction) { this.refineWithInstruction = refineWithInstruction; }
    public String getRefineWithoutInstruction() { return refineWithoutInstruction; }
    public void setRefineWithoutInstruction(String refineWithoutInstruction) { this.refineWithoutInstruction = refineWithoutInstruction; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

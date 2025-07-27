package com.example.ainovel.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

/**
 * Represents a single chapter within a story outline.
 */
@Data
@Entity
@Table(name = "outline_chapters")
public class OutlineChapter {

    /**
     * The unique identifier for the chapter.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The outline card this chapter belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outline_card_id", nullable = false)
    @JsonBackReference
    @ToString.Exclude
    private OutlineCard outlineCard;

    /**
     * The sequential number of the chapter in the outline.
     */
    @Column(name = "chapter_number", nullable = false)
    private Integer chapterNumber;

    /**
     * The title of the chapter.
     */
    private String title;

    /**
     * A synopsis of the events that occur in this chapter.
     */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String synopsis;

   /**
    * Stores user-defined settings for the chapter, like sections per chapter or words per section.
    * Mapped to a JSON column in the database.
    */
   @JdbcTypeCode(SqlTypes.JSON)
   @Column(columnDefinition = "json")
   private Map<String, Object> settings;

    /**
     * The list of scenes within this chapter.
     */
    @OneToMany(mappedBy = "outlineChapter", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("sceneNumber ASC")
    @JsonManagedReference
    @ToString.Exclude
    private List<OutlineScene> scenes;
}

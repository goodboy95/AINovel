package com.example.ainovel.dto;

import java.util.List;

import lombok.Data;

/**
 * Data Transfer Object for chapter data.
 */
@Data
public class ChapterDto {
    /**
     * The unique identifier for the chapter.
     */
    private Long id;

    /**
     * The sequential number of the chapter.
     */
    private Integer chapterNumber;

    /**
     * The title of the chapter.
     */
    private String title;

    /**
     * A synopsis of the chapter.
     */
    private String synopsis;

    /**
     * A list of scenes within this chapter.
     */
    private List<SceneDto> scenes;

    /**
     * User-defined settings for the chapter, stored as a JSON string.
     */
    private String settings;
}

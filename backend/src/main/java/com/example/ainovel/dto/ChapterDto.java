package com.example.ainovel.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

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
     * User-defined settings for the chapter.
     */
    private Map<String, Object> settings;
}

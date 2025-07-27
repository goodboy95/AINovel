package com.example.ainovel.dto;

import lombok.Data;

/**
 * Data Transfer Object for a story conception request.
 */
@Data
public class ConceptionRequest {
    /**
     * The core idea or prompt for the story.
     */
    private String idea;

    /**
     * The genre of the story (e.g., Fantasy, Sci-Fi).
     */
    private String genre;

    /**
     * The tone of the story (e.g., Adventurous, Humorous).
     */
    private String tone;
}

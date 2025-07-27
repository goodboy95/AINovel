package com.example.ainovel.dto;

import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.StoryCard;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Data Transfer Object for a story conception response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConceptionResponse {
    /**
     * The generated story card.
     */
    private StoryCard storyCard;

    /**
     * A list of generated character cards for the story.
     */
    private List<CharacterCard> characterCards;
}

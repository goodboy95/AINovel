package com.example.ainovel.dto;

import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.StoryCard;
import java.util.List;

public class ConceptionResponse {
    private StoryCard storyCard;
    private List<CharacterCard> characterCards;

    public ConceptionResponse() {
    }

    public ConceptionResponse(StoryCard storyCard, List<CharacterCard> characterCards) {
        this.storyCard = storyCard;
        this.characterCards = characterCards;
    }

    // Getters and Setters

    public StoryCard getStoryCard() {
        return storyCard;
    }

    public void setStoryCard(StoryCard storyCard) {
        this.storyCard = storyCard;
    }

    public List<CharacterCard> getCharacterCards() {
        return characterCards;
    }

    public void setCharacterCards(List<CharacterCard> characterCards) {
        this.characterCards = characterCards;
    }
}

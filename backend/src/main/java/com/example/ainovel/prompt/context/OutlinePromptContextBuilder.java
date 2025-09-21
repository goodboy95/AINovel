package com.example.ainovel.prompt.context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.ainovel.dto.GenerateChapterRequest;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.StoryCard;

@Component
public class OutlinePromptContextBuilder {

    public Map<String, Object> build(StoryCard storyCard, OutlineCard outlineCard,
                                     GenerateChapterRequest request, String previousChapterSynopsis) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> story = new LinkedHashMap<>();
        story.put("title", safeString(storyCard.getTitle()));
        story.put("synopsis", safeString(storyCard.getSynopsis()));
        story.put("genre", safeString(storyCard.getGenre()));
        story.put("tone", safeString(storyCard.getTone()));
        story.put("storyArc", safeString(storyCard.getStoryArc()));
        root.put("story", story);

        Map<String, Object> outline = new LinkedHashMap<>();
        outline.put("title", safeString(outlineCard.getTitle()));
        outline.put("pointOfView", safeString(outlineCard.getPointOfView()));
        root.put("outline", outline);

        Map<String, Object> chapter = new LinkedHashMap<>();
        chapter.put("number", request.getChapterNumber());
        chapter.put("sectionsPerChapter", request.getSectionsPerChapter());
        chapter.put("wordsPerSection", request.getWordsPerSection());
        chapter.put("previousSynopsis", previousChapterSynopsis);
        root.put("chapter", chapter);

        List<CharacterCard> characters = storyCard.getCharacters() == null ? List.of() : storyCard.getCharacters();
        List<Map<String, Object>> characterList = characters.stream()
                .map(this::toCharacterMap)
                .collect(Collectors.toList());
        List<String> characterNames = characters.stream()
                .map(CharacterCard::getName)
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .collect(Collectors.toList());
        String characterSummary = characters.isEmpty()
                ? "无角色信息。"
                : characters.stream()
                        .map(c -> "- " + safeString(c.getName()) + ": " + safeString(c.getSynopsis()))
                        .collect(Collectors.joining("\n"));

        Map<String, Object> characterContext = new LinkedHashMap<>();
        characterContext.put("list", characterList);
        characterContext.put("names", characterNames);
        characterContext.put("summary", characterSummary);
        root.put("characters", characterContext);

        root.put("request", Map.of("raw", request));
        return root;
    }

    private Map<String, Object> toCharacterMap(CharacterCard card) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", card.getId());
        map.put("name", safeString(card.getName()));
        map.put("synopsis", safeString(card.getSynopsis()));
        map.put("details", safeString(card.getDetails()));
        map.put("relationships", safeString(card.getRelationships()));
        return map;
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }
}

package com.example.ainovel.prompt.context;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.dto.GenerateChapterRequest;
import com.example.ainovel.dto.RefineRequest;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.CharacterChangeLog;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.OutlineScene;
import com.example.ainovel.model.SceneCharacter;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.TemporaryCharacter;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromptContextFactory {

    private final StoryPromptContextBuilder storyPromptContextBuilder;
    private final OutlinePromptContextBuilder outlinePromptContextBuilder;
    private final ManuscriptPromptContextBuilder manuscriptPromptContextBuilder;
    private final RefinePromptContextBuilder refinePromptContextBuilder;

    public Map<String, Object> buildStoryCreationContext(ConceptionRequest request) {
        return storyPromptContextBuilder.build(request);
    }

    public Map<String, Object> buildOutlineChapterContext(StoryCard storyCard, OutlineCard outlineCard,
                                                          GenerateChapterRequest request, String previousChapterSynopsis) {
        return outlinePromptContextBuilder.build(storyCard, outlineCard, request, previousChapterSynopsis);
    }

    public Map<String, Object> buildManuscriptSectionContext(
            OutlineScene scene,
            StoryCard story,
            List<CharacterCard> allCharacters,
            List<SceneCharacter> sceneCharacters,
            List<TemporaryCharacter> temporaryCharacters,
            String previousSectionContent,
            List<OutlineScene> previousChapterOutline,
            List<OutlineScene> currentChapterOutline,
            int chapterNumber,
            int totalChapters,
            int sceneNumber,
            int totalScenesInChapter,
            Map<Long, CharacterChangeLog> latestCharacterLogs
    ) {
        return manuscriptPromptContextBuilder.build(
                scene,
                story,
                allCharacters,
                sceneCharacters,
                temporaryCharacters,
                previousSectionContent,
                previousChapterOutline,
                currentChapterOutline,
                chapterNumber,
                totalChapters,
                sceneNumber,
                totalScenesInChapter,
                latestCharacterLogs
        );
    }

    public Map<String, Object> buildRefineContext(RefineRequest request) {
        return refinePromptContextBuilder.build(request);
    }
}

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
import com.example.ainovel.service.world.WorkspaceWorldContext;
import com.example.ainovel.service.world.WorkspaceWorldContextService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromptContextFactory {

    private final StoryPromptContextBuilder storyPromptContextBuilder;
    private final OutlinePromptContextBuilder outlinePromptContextBuilder;
    private final ManuscriptPromptContextBuilder manuscriptPromptContextBuilder;
    private final RefinePromptContextBuilder refinePromptContextBuilder;
    private final WorkspaceWorldContextService workspaceWorldContextService;

    public Map<String, Object> buildStoryCreationContext(Long userId, ConceptionRequest request) {
        WorkspaceWorldContext worldContext = workspaceWorldContextService
                .getContext(userId, request.getWorldId())
                .orElse(null);
        return storyPromptContextBuilder.build(request, worldContext);
    }

    public Map<String, Object> buildOutlineChapterContext(Long userId,
                                                          StoryCard storyCard,
                                                          OutlineCard outlineCard,
                                                          GenerateChapterRequest request,
                                                          String previousChapterSynopsis,
                                                          Long worldId) {
        Long resolved = worldId != null ? worldId
                : (outlineCard != null && outlineCard.getWorldId() != null
                ? outlineCard.getWorldId()
                : storyCard.getWorldId());
        WorkspaceWorldContext worldContext = workspaceWorldContextService
                .getContext(userId, resolved)
                .orElse(null);
        return outlinePromptContextBuilder.build(storyCard, outlineCard, request, previousChapterSynopsis, worldContext);
    }

    public Map<String, Object> buildManuscriptSectionContext(
            Long userId,
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
            Map<Long, CharacterChangeLog> latestCharacterLogs,
            Long worldId
    ) {
        WorkspaceWorldContext worldContext = workspaceWorldContextService
                .getContext(userId, worldId)
                .orElse(null);
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
                latestCharacterLogs,
                worldContext
        );
    }

    public Map<String, Object> buildRefineContext(RefineRequest request) {
        return refinePromptContextBuilder.build(request);
    }
}

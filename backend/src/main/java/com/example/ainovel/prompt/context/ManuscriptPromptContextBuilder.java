package com.example.ainovel.prompt.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.ainovel.dto.RelationshipChangeDto;
import com.example.ainovel.model.CharacterCard;
import com.example.ainovel.model.CharacterChangeLog;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.OutlineChapter;
import com.example.ainovel.model.OutlineScene;
import com.example.ainovel.model.SceneCharacter;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.TemporaryCharacter;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ManuscriptPromptContextBuilder {

    private final ObjectMapper objectMapper;

    public Map<String, Object> build(
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
        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> storyMap = new LinkedHashMap<>();
        storyMap.put("title", safeString(story.getTitle()));
        storyMap.put("genre", safeString(story.getGenre()));
        storyMap.put("tone", safeString(story.getTone()));
        storyMap.put("synopsis", safeString(story.getSynopsis()));
        root.put("story", storyMap);

        OutlineChapter outlineChapter = scene.getOutlineChapter();
        OutlineCard outline = outlineChapter != null ? outlineChapter.getOutlineCard() : null;
        Map<String, Object> outlineMap = new LinkedHashMap<>();
        outlineMap.put("pointOfView", outline != null ? safeString(outline.getPointOfView()) : "第三人称有限视角");
        outlineMap.put("title", outline != null ? safeString(outline.getTitle()) : "");
        root.put("outline", outlineMap);

        Map<String, Object> chapter = new LinkedHashMap<>();
        chapter.put("number", chapterNumber);
        chapter.put("total", totalChapters);
        chapter.put("title", outlineChapter != null ? safeString(outlineChapter.getTitle()) : "");
        root.put("chapter", chapter);

        Map<String, Object> sceneMap = new LinkedHashMap<>();
        sceneMap.put("number", sceneNumber);
        sceneMap.put("totalInChapter", totalScenesInChapter);
        sceneMap.put("synopsis", safeString(scene.getSynopsis()));
        sceneMap.put("expectedWords", scene.getExpectedWords() != null ? scene.getExpectedWords() : 1200);
        sceneMap.put("coreCharacters", toSceneCharacterMaps(sceneCharacters));
        sceneMap.put("coreCharacterSummary", formatSceneCharacters(sceneCharacters));
        sceneMap.put("temporaryCharacters", toTemporaryCharacterMaps(temporaryCharacters));
        sceneMap.put("temporaryCharacterSummary", formatTemporaryCharacters(temporaryCharacters));
        sceneMap.put("presentCharacters", formatPresentCharacters(sceneCharacters, scene.getPresentCharacters()));
        root.put("scene", sceneMap);

        Map<String, Object> charactersMap = new LinkedHashMap<>();
        charactersMap.put("all", toCharacterMaps(allCharacters));
        charactersMap.put("allSummary", formatCharacters(allCharacters, latestCharacterLogs));
        charactersMap.put("present", extractPresentCharacters(sceneCharacters, scene.getPresentCharacters()));
        root.put("characters", charactersMap);

        Map<String, Object> previousSection = new LinkedHashMap<>();
        previousSection.put("content", previousSectionContent == null ? "无" : previousSectionContent);
        root.put("previousSection", previousSection);

        Map<String, Object> previousChapter = new LinkedHashMap<>();
        previousChapter.put("scenes", toOutlineSceneMaps(previousChapterOutline));
        previousChapter.put("summary", formatOutlineScenes(previousChapterOutline));
        root.put("previousChapter", previousChapter);

        Map<String, Object> currentChapter = new LinkedHashMap<>();
        currentChapter.put("scenes", toOutlineSceneMaps(currentChapterOutline));
        currentChapter.put("summary", formatOutlineScenes(currentChapterOutline));
        root.put("currentChapter", currentChapter);

        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("latestByCharacter", buildLatestLogMap(latestCharacterLogs));
        root.put("log", logMap);

        return root;
    }

    private List<Map<String, Object>> toCharacterMaps(List<CharacterCard> characters) {
        if (characters == null) {
            return List.of();
        }
        return characters.stream()
                .map(character -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", character.getId());
                    map.put("name", safeString(character.getName()));
                    map.put("synopsis", safeString(character.getSynopsis()));
                    map.put("details", safeString(character.getDetails()));
                    map.put("relationships", safeString(character.getRelationships()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> toSceneCharacterMaps(List<SceneCharacter> sceneCharacters) {
        if (sceneCharacters == null) {
            return List.of();
        }
        return sceneCharacters.stream()
                .map(sc -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("characterName", safeString(sc.getCharacterName()));
                    map.put("status", safeString(sc.getStatus()));
                    map.put("thought", safeString(sc.getThought()));
                    map.put("action", safeString(sc.getAction()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> toTemporaryCharacterMaps(List<TemporaryCharacter> temporaryCharacters) {
        if (temporaryCharacters == null) {
            return List.of();
        }
        return temporaryCharacters.stream()
                .map(tc -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", safeString(tc.getName()));
                    map.put("summary", safeString(tc.getSummary()));
                    map.put("details", safeString(tc.getDetails()));
                    map.put("relationships", safeString(tc.getRelationships()));
                    map.put("status", safeString(tc.getStatus()));
                    map.put("thought", safeString(tc.getThought()));
                    map.put("action", safeString(tc.getAction()));
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> toOutlineSceneMaps(List<OutlineScene> scenes) {
        if (scenes == null) {
            return List.of();
        }
        return scenes.stream()
                .map(scene -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("sceneNumber", scene.getSceneNumber());
                    map.put("synopsis", safeString(scene.getSynopsis()));
                    map.put("presentCharacters", safeString(scene.getPresentCharacters()));
                    map.put("expectedWords", scene.getExpectedWords());
                    return map;
                })
                .collect(Collectors.toList());
    }

    private List<String> extractPresentCharacters(List<SceneCharacter> sceneCharacters, String fallback) {
        if (sceneCharacters != null && !sceneCharacters.isEmpty()) {
            LinkedHashSet<String> names = sceneCharacters.stream()
                    .map(SceneCharacter::getCharacterName)
                    .filter(name -> name != null && !name.isBlank())
                    .map(String::trim)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!names.isEmpty()) {
                return new ArrayList<>(names);
            }
        }
        if (fallback == null || fallback.isBlank()) {
            return List.of();
        }
        String[] parts = fallback.split(",|，|、");
        List<String> results = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                results.add(trimmed);
            }
        }
        return results;
    }

    private Map<String, String> buildLatestLogMap(Map<Long, CharacterChangeLog> latestLogs) {
        if (latestLogs == null || latestLogs.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        latestLogs.forEach((id, log) -> {
            if (log == null) {
                return;
            }
            String name = log.getCharacter() != null ? safeString(log.getCharacter().getName()) : String.valueOf(id);
            result.put(name == null || name.isBlank() ? String.valueOf(id) : name, summarizeCharacterGrowth(log));
        });
        return result;
    }

    private String formatCharacters(List<CharacterCard> allCharacters, Map<Long, CharacterChangeLog> latestLogs) {
        if (allCharacters == null || allCharacters.isEmpty()) {
            return "无";
        }
        return allCharacters.stream()
                .map(c -> String.format(
                        "- %s\n  - 概述: %s\n  - 详细背景: %s\n  - 关系: %s\n  - 最近成长轨迹: %s\n  - 最新关系图谱: %s",
                        safeString(c.getName()),
                        nullToNA(c.getSynopsis()),
                        nullToNA(c.getDetails()),
                        nullToNA(c.getRelationships()),
                        summarizeCharacterGrowth(latestLogs != null ? latestLogs.get(c.getId()) : null),
                        summarizeRelationshipMap(latestLogs != null ? latestLogs.get(c.getId()) : null)
                ))
                .collect(Collectors.joining("\n"));
    }

    private String formatOutlineScenes(List<OutlineScene> scenes) {
        if (scenes == null || scenes.isEmpty()) {
            return "无";
        }
        return scenes.stream()
                .map(sc -> String.format(
                        "第 %d 节:\n- 梗概: %s\n- 核心出场人物: %s\n- 核心人物状态卡:\n%s\n- 临时人物:\n%s",
                        sc.getSceneNumber(),
                        nullToNA(sc.getSynopsis()),
                        nullToNA(formatPresentCharacters(sc.getSceneCharacters(), sc.getPresentCharacters())),
                        formatSceneCharacters(sc.getSceneCharacters()),
                        formatTemporaryCharacters(sc.getTemporaryCharacters())
                ))
                .collect(Collectors.joining("\n\n"));
    }

    private String formatTemporaryCharacters(List<TemporaryCharacter> temporaryCharacters) {
        if (temporaryCharacters == null || temporaryCharacters.isEmpty()) {
            return "无";
        }
        return temporaryCharacters.stream()
                .map(tc -> String.format(
                        "- %s\n  - 概要: %s\n  - 详情: %s\n  - 关系: %s\n  - 在本节中的状态: %s\n  - 在本节中的想法: %s\n  - 在本节中的行动: %s",
                        safeString(tc.getName()),
                        nullToNA(tc.getSummary()),
                        nullToNA(tc.getDetails()),
                        nullToNA(tc.getRelationships()),
                        nullToNA(tc.getStatus()),
                        nullToNA(tc.getThought()),
                        nullToNA(tc.getAction())
                ))
                .collect(Collectors.joining("\n"));
    }

    private String formatSceneCharacters(List<SceneCharacter> sceneCharacters) {
        if (sceneCharacters == null || sceneCharacters.isEmpty()) {
            return "无";
        }
        return sceneCharacters.stream()
                .map(sc -> String.format(
                        "- %s\n  - 状态: %s\n  - 想法: %s\n  - 行动: %s",
                        safeString(sc.getCharacterName()).isEmpty() ? "未知角色" : safeString(sc.getCharacterName()),
                        nullToNA(sc.getStatus()),
                        nullToNA(sc.getThought()),
                        nullToNA(sc.getAction())
                ))
                .collect(Collectors.joining("\n"));
    }

    private String formatPresentCharacters(List<SceneCharacter> sceneCharacters, String fallback) {
        List<String> names = extractPresentCharacters(sceneCharacters, fallback);
        if (names.isEmpty()) {
            return fallback == null ? "" : fallback;
        }
        return String.join(", ", names);
    }

    private String summarizeCharacterGrowth(CharacterChangeLog log) {
        if (log == null) {
            return "暂无最新变化记录";
        }
        String combined = java.util.stream.Stream.of(log.getCharacterChanges(), log.getNewlyKnownInfo())
                .filter(text -> text != null && !text.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(" / "));
        if (combined == null || combined.isBlank()) {
            combined = nullToNA(log.getCharacterDetailsAfter());
        }
        return nullToNA(combined);
    }

    private String summarizeRelationshipMap(CharacterChangeLog log) {
        if (log == null || log.getRelationshipChangesJson() == null || log.getRelationshipChangesJson().isBlank()) {
            return "暂无新的关系变更";
        }
        try {
            RelationshipChangeDto[] changes = objectMapper.readValue(
                    log.getRelationshipChangesJson(),
                    RelationshipChangeDto[].class
            );
            if (changes.length == 0) {
                return "暂无新的关系变更";
            }
            return Arrays.stream(changes)
                    .map(change -> String.format(
                            "与角色ID %d: %s -> %s（原因: %s）",
                            change.getTargetCharacterId(),
                            defaultString(change.getPreviousRelationship(), "未知"),
                            defaultString(change.getCurrentRelationship(), "未知"),
                            defaultString(change.getChangeReason(), "未说明")
                    ))
                    .collect(Collectors.joining("；"));
        } catch (Exception e) {
            return "暂无新的关系变更";
        }
    }

    private String nullToNA(String value) {
        String trimmed = safeString(value);
        return trimmed.isEmpty() ? "无" : trimmed;
    }

    private String defaultString(String value, String fallback) {
        String trimmed = safeString(value);
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }
}

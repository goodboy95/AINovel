package com.ainovel.app.story;

import com.ainovel.app.story.dto.*;
import com.ainovel.app.story.model.Outline;
import com.ainovel.app.story.model.Story;
import com.ainovel.app.story.repo.OutlineRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OutlineService {
    @Autowired
    private OutlineRepository outlineRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<OutlineDto> listByStory(Story story) {
        return outlineRepository.findByStory(story).stream().map(this::toDto).toList();
    }

    public OutlineDto get(UUID id) {
        return toDto(outlineRepository.findById(id).orElseThrow(() -> new RuntimeException("大纲不存在")));
    }

    @Transactional
    public OutlineDto createOutline(Story story, OutlineCreateRequest request) {
        Map<String, Object> content = new HashMap<>();
        content.put("chapters", new ArrayList<>());
        Outline outline = new Outline();
        outline.setStory(story);
        outline.setTitle(request.title() == null ? "新大纲" : request.title());
        outline.setWorldId(request.worldId());
        outline.setContentJson(writeJson(content));
        outlineRepository.save(outline);
        return toDto(outline);
    }

    @Transactional
    public OutlineDto saveOutline(UUID outlineId, OutlineSaveRequest request) {
        Outline outline = outlineRepository.findById(outlineId).orElseThrow(() -> new RuntimeException("大纲不存在"));
        outline.setTitle(request.title() != null ? request.title() : outline.getTitle());
        outline.setWorldId(request.worldId());
        Map<String, Object> content = new HashMap<>();
        content.put("chapters", request.chapters());
        outline.setContentJson(writeJson(content));
        outlineRepository.save(outline);
        return toDto(outline);
    }

    @Transactional
    public void deleteOutline(UUID outlineId) {
        outlineRepository.deleteById(outlineId);
    }

    @Transactional
    public OutlineDto addGeneratedChapter(UUID outlineId, OutlineChapterGenerateRequest request) {
        Outline outline = outlineRepository.findById(outlineId).orElseThrow(() -> new RuntimeException("大纲不存在"));
        OutlineDto dto = toDto(outline);
        int order = dto.chapters() == null ? 1 : dto.chapters().size() + 1;
        UUID chapterId = UUID.randomUUID();
        List<OutlineDto.SceneDto> scenes = new ArrayList<>();
        int scenesCount = request.sectionsPerChapter() != null ? request.sectionsPerChapter() : 2;
        for (int i = 1; i <= scenesCount; i++) {
            scenes.add(new OutlineDto.SceneDto(UUID.randomUUID(), "场景 " + i, "根据第 " + i + " 段生成的摘要", null, i));
        }
        OutlineDto.ChapterDto newChapter = new OutlineDto.ChapterDto(
                chapterId,
                "第" + (request.chapterNumber() != null ? request.chapterNumber() : order) + "章",
                "AI 生成的章节摘要",
                order,
                scenes
        );
        List<OutlineDto.ChapterDto> updated = new ArrayList<>(dto.chapters() != null ? dto.chapters() : List.of());
        updated.add(newChapter);
        OutlineSaveRequest saveRequest = new OutlineSaveRequest(outline.getTitle(), outline.getWorldId(),
                updated.stream().map(c -> new OutlineSaveRequest.ChapterPayload(c.id(), c.title(), c.summary(), c.order(),
                        c.scenes().stream().map(s -> new OutlineSaveRequest.ScenePayload(s.id(), s.title(), s.summary(), s.content(), s.order())).toList())).toList());
        return saveOutline(outlineId, saveRequest);
    }

    private OutlineDto toDto(Outline outline) {
        Map<String, Object> content = readJson(outline.getContentJson());
        List<OutlineSaveRequest.ChapterPayload> chapters = objectMapper.convertValue(
                content.getOrDefault("chapters", new ArrayList<>()),
                new TypeReference<List<OutlineSaveRequest.ChapterPayload>>() {}
        );
        List<OutlineDto.ChapterDto> chapterDtos = chapters.stream().map(c -> new OutlineDto.ChapterDto(
                c.id() != null ? c.id() : UUID.randomUUID(),
                c.title(), c.summary(), c.order(),
                c.scenes() == null ? List.of() : c.scenes().stream().map(s -> new OutlineDto.SceneDto(
                        s.id() != null ? s.id() : UUID.randomUUID(),
                        s.title(), s.summary(), s.content(), s.order()
                )).toList()
        )).toList();
        return new OutlineDto(outline.getId(), outline.getStory().getId(), outline.getTitle(), outline.getWorldId(), chapterDtos, outline.getUpdatedAt());
    }

    private Map<String, Object> readJson(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String writeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}

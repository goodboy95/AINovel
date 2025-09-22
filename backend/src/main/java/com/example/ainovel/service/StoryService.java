package com.example.ainovel.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ainovel.dto.StoryCardDto;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.User;
import com.example.ainovel.repository.StoryCardRepository;
import com.example.ainovel.service.world.WorldService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryCardRepository storyCardRepository;
    private final WorldService worldService;

    /**
     * 创建新的故事卡并保存
     * @param user 已认证用户
     * @param dto  前端提交的故事数据
     * @return 已保存的故事卡
     */
    @Transactional
    public StoryCard createStory(User user, StoryCardDto dto) {
        StoryCard story = new StoryCard();
        story.setUser(user);
        story.setTitle(normalizeTitle(dto.getTitle()));
        story.setSynopsis(safeTrim(dto.getSynopsis()));
        story.setGenre(safeTrim(dto.getGenre()));
        story.setTone(safeTrim(dto.getTone()));
        if (dto.getWorldId() != null) {
            worldService.ensureSelectableWorld(dto.getWorldId(), user.getId());
        }
        story.setWorldId(dto.getWorldId());
        // storyArc 由 AI 构思流程生成，这里留空
        return storyCardRepository.save(story);
    }

    /**
     * 获取当前用户的故事列表（精简 DTO）
     */
    @Transactional(readOnly = true)
    public List<StoryCardDto> getUserStories(Long userId) {
        List<StoryCard> list = storyCardRepository.findByUserId(userId);
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * 删除指定故事（级联删除其下大纲与角色）
     */
    @Transactional
    public void deleteStory(Long storyId, Long userId) {
        if (!storyCardRepository.existsByIdAndUserId(storyId, userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized access or StoryCard not found");
        }
        storyCardRepository.deleteById(storyId);
    }

    private StoryCardDto toDto(StoryCard s) {
        StoryCardDto dto = new StoryCardDto();
        dto.setId(s.getId());
        dto.setTitle(s.getTitle());
        dto.setSynopsis(s.getSynopsis());
        dto.setGenre(s.getGenre());
        dto.setTone(s.getTone());
        dto.setWorldId(s.getWorldId());
        return dto;
    }

    private String normalizeTitle(String title) {
        String t = safeTrim(title);
        return (t == null || t.isEmpty()) ? "未命名故事" : t;
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}

package com.example.ainovel.service;

import com.example.ainovel.dto.StoryCardDto;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.User;
import com.example.ainovel.repository.StoryCardRepository;
import com.example.ainovel.service.world.WorldService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理故事卡片的核心业务逻辑，负责对外暴露可复用的服务方法。
 */
@Service
@RequiredArgsConstructor
@Slf4j
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
        log.info("Creating story for user {}", user.getId());
        StoryCard story = new StoryCard();
        story.setUser(user);
        story.setTitle(normalizeTitle(dto.getTitle()));
        story.setSynopsis(safeTrim(dto.getSynopsis()));
        story.setGenre(safeTrim(dto.getGenre()));
        story.setTone(safeTrim(dto.getTone()));
        if (dto.getWorldId() != null) {
            log.debug("Validating selectable world {} for user {}", dto.getWorldId(), user.getId());
            worldService.ensureSelectableWorld(dto.getWorldId(), user.getId());
        }
        story.setWorldId(dto.getWorldId());
        // storyArc 由 AI 构思流程生成，这里留空
        StoryCard saved = storyCardRepository.save(story);
        log.debug("Story {} saved for user {}", saved.getId(), user.getId());
        return saved;
    }

    /**
     * 获取当前用户的故事列表（精简 DTO）
     */
    @Transactional(readOnly = true)
    public List<StoryCardDto> getUserStories(Long userId) {
        log.debug("Loading story list for user {}", userId);
        List<StoryCard> list = storyCardRepository.findByUserId(userId);
        List<StoryCardDto> result = list.stream().map(this::toDto).collect(Collectors.toList());
        log.debug("Returning {} stories for user {}", result.size(), userId);
        return result;
    }

    /**
     * 删除指定故事（级联删除其下大纲与角色）
     */
    @Transactional
    public void deleteStory(Long storyId, Long userId) {
        if (!storyCardRepository.existsByIdAndUserId(storyId, userId)) {
            log.warn("User {} attempted to delete non-existing or unauthorized story {}", userId, storyId);
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized access or StoryCard not found");
        }
        storyCardRepository.deleteById(storyId);
        log.info("Story {} deleted for user {}", storyId, userId);
    }

    /**
     * 将实体转换为 DTO，便于前端展示。
     */
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

    /**
     * 标题如果为空则生成一个默认的名称，避免前端出现空标题。
     */
    private String normalizeTitle(String title) {
        String t = safeTrim(title);
        return (t == null || t.isEmpty()) ? "未命名故事" : t;
    }

    /**
     * 统一执行 null 安全的 trim 操作，减少重复空值判断。
     */
    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}

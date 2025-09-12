package com.example.ainovel.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ainovel.dto.StoryCardDto;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.User;
import com.example.ainovel.repository.StoryCardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryCardRepository storyCardRepository;

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
        // storyArc 由 AI 构思流程生成，这里留空
        return storyCardRepository.save(story);
    }

    private String normalizeTitle(String title) {
        String t = safeTrim(title);
        return (t == null || t.isEmpty()) ? "未命名故事" : t;
    }

    private String safeTrim(String s) {
        return s == null ? null : s.trim();
    }
}
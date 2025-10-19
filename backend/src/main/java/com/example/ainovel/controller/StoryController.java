package com.example.ainovel.controller;

import com.example.ainovel.dto.StoryCardDto;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.User;
import com.example.ainovel.service.StoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for StoryCard CRUD.
 */
@RestController
@RequestMapping("/api/v1/stories")
public class StoryController {
    private final StoryService storyService;
    private static final Logger log = LoggerFactory.getLogger(StoryController.class);

    /**
     * 构造函数
     * @param storyService 故事服务
     */
    public StoryController(StoryService storyService) {
        this.storyService = storyService;
    }

    /**
     * 创建新的故事卡
     * @param dto 请求体
     * @param user 已认证用户
     * @return 201 已创建的 StoryCard
     */
    @PostMapping
    public ResponseEntity<StoryCard> createStory(@RequestBody StoryCardDto dto, @AuthenticationPrincipal User user) {
        log.info("User {} is creating a new story", user.getId());
        StoryCard created = storyService.createStory(user, dto);
        log.debug("Story {} created for user {}", created.getId(), user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * 获取当前用户的故事列表
     * GET /api/v1/stories
     */
    @GetMapping
    public ResponseEntity<List<StoryCardDto>> listStories(@AuthenticationPrincipal User user) {
        log.debug("Fetching stories for user {}", user.getId());
        List<StoryCardDto> stories = storyService.getUserStories(user.getId());
        log.debug("Found {} stories for user {}", stories.size(), user.getId());
        return ResponseEntity.ok(stories);
    }

    /**
     * 删除指定故事（级联删除其下大纲与角色）
     * DELETE /api/v1/stories/{storyId}
     */
    @DeleteMapping("/{storyId}")
    public ResponseEntity<Void> deleteStory(@PathVariable Long storyId, @AuthenticationPrincipal User user) {
        log.info("User {} requested deletion of story {}", user.getId(), storyId);
        storyService.deleteStory(storyId, user.getId());
        log.debug("Story {} deleted for user {}", storyId, user.getId());
        return ResponseEntity.noContent().build();
    }
}

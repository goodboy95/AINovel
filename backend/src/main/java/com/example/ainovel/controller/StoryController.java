package com.example.ainovel.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ainovel.dto.StoryCardDto;
import com.example.ainovel.model.StoryCard;
import com.example.ainovel.model.User;
import com.example.ainovel.service.StoryService;

/**
 * Controller for StoryCard CRUD.
 */
@RestController
@RequestMapping("/api/v1/stories")
public class StoryController {
    private final StoryService storyService;

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
        StoryCard created = storyService.createStory(user, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
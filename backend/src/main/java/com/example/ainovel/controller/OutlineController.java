package com.example.ainovel.controller;

import com.example.ainovel.dto.OutlineDto;
import com.example.ainovel.dto.OutlineRequest;
import com.example.ainovel.model.User;
import com.example.ainovel.service.OutlineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.example.ainovel.dto.OutlineDto;
import com.example.ainovel.dto.OutlineRequest;
import com.example.ainovel.model.OutlineCard;
import com.example.ainovel.model.User;
import com.example.ainovel.service.OutlineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class OutlineController {

    @Autowired
    private OutlineService outlineService;

    @PostMapping("/outlines")
    public ResponseEntity<OutlineDto> createOutline(@RequestBody OutlineRequest outlineRequest, @AuthenticationPrincipal User user) {
        OutlineDto outline = outlineService.createOutline(outlineRequest, user.getId());
        return ResponseEntity.ok(outline);
    }

    @GetMapping("/outlines/{id}")
    public ResponseEntity<OutlineDto> getOutlineById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        OutlineDto outline = outlineService.getOutlineById(id, user.getId());
        return ResponseEntity.ok(outline);
    }

    @GetMapping("/story-cards/{storyCardId}/outlines")
    public ResponseEntity<List<OutlineDto>> getOutlinesByStoryCardId(@PathVariable Long storyCardId, @AuthenticationPrincipal User user) {
        List<OutlineDto> outlines = outlineService.getOutlinesByStoryCardId(storyCardId, user.getId());
        return ResponseEntity.ok(outlines);
    }

    @PutMapping("/outlines/{id}")
    public ResponseEntity<OutlineDto> updateOutline(@PathVariable Long id, @RequestBody OutlineCard outlineDetails, @AuthenticationPrincipal User user) {
        OutlineDto updatedOutline = outlineService.updateOutline(id, outlineDetails, user.getId());
        return ResponseEntity.ok(updatedOutline);
    }

    @DeleteMapping("/outlines/{id}")
    public ResponseEntity<Void> deleteOutline(@PathVariable Long id, @AuthenticationPrincipal User user) {
        outlineService.deleteOutline(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}

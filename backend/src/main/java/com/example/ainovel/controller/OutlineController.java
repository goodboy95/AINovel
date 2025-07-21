package com.example.ainovel.controller;

import com.example.ainovel.dto.OutlineDto;
import com.example.ainovel.dto.OutlineRequest;
import com.example.ainovel.model.User;
import com.example.ainovel.service.OutlineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/outlines")
public class OutlineController {

    @Autowired
    private OutlineService outlineService;

    @PostMapping
    public ResponseEntity<OutlineDto> createOutline(@RequestBody OutlineRequest outlineRequest, @AuthenticationPrincipal User user) {
        OutlineDto outline = outlineService.createOutline(outlineRequest, user.getId());
        return ResponseEntity.ok(outline);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OutlineDto> getOutlineById(@PathVariable Long id, @AuthenticationPrincipal User user) {
        OutlineDto outline = outlineService.getOutlineById(id, user.getId());
        return ResponseEntity.ok(outline);
    }
}

package com.ainovel.app.story;

import com.ainovel.app.common.RefineRequest;
import com.ainovel.app.story.dto.*;
import com.ainovel.app.story.model.Story;
import com.ainovel.app.story.repo.StoryRepository;
import com.ainovel.app.user.User;
import com.ainovel.app.user.UserRepository;
import com.ainovel.app.ai.AiService;
import com.ainovel.app.ai.dto.AiRefineRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
public class StoryController {
    @Autowired
    private StoryService storyService;
    @Autowired
    private OutlineService outlineService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StoryRepository storyRepository;
    @Autowired
    private AiService aiService;

    private User currentUser(UserDetails details) {
        return userRepository.findByUsername(details.getUsername()).orElseThrow();
    }

    @GetMapping("/story-cards")
    public List<StoryDto> listStories(@AuthenticationPrincipal UserDetails principal) {
        return storyService.listStories(currentUser(principal));
    }

    @GetMapping("/story-cards/{id}")
    public StoryDto getStory(@PathVariable UUID id) { return storyService.getStory(id); }

    @PostMapping("/stories")
    public StoryDto createStory(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody StoryCreateRequest request) {
        return storyService.createStory(currentUser(principal), request);
    }

    @PutMapping("/story-cards/{id}")
    public StoryDto updateStory(@PathVariable UUID id, @RequestBody StoryUpdateRequest request) { return storyService.updateStory(id, request); }

    @DeleteMapping("/stories/{id}")
    public ResponseEntity<Void> deleteStory(@PathVariable UUID id) { storyService.deleteStory(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/story-cards/{id}/character-cards")
    public List<CharacterDto> listCharacters(@PathVariable UUID id) { return storyService.listCharacters(id); }

    @PostMapping("/story-cards/{id}/characters")
    public CharacterDto addCharacter(@PathVariable UUID id, @Valid @RequestBody CharacterRequest request) { return storyService.addCharacter(id, request); }

    @PutMapping("/character-cards/{id}")
    public CharacterDto updateCharacter(@PathVariable UUID id, @RequestBody CharacterRequest request) { return storyService.updateCharacter(id, request); }

    @DeleteMapping("/character-cards/{id}")
    public ResponseEntity<Void> deleteCharacter(@PathVariable UUID id) { storyService.deleteCharacter(id); return ResponseEntity.noContent().build(); }

    @PostMapping("/story-cards/{id}/refine")
    public ResponseEntity<String> refineStory(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id, @RequestBody RefineRequest request) {
        return ResponseEntity.ok(storyService.refineStory(currentUser(principal), id, request));
    }

    @PostMapping("/character-cards/{id}/refine")
    public ResponseEntity<String> refineCharacter(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id, @RequestBody RefineRequest request) {
        return ResponseEntity.ok(storyService.refineCharacter(currentUser(principal), id, request));
    }

    @PostMapping("/conception")
    public ResponseEntity<Map<String, Object>> conception(@AuthenticationPrincipal UserDetails principal, @RequestBody StoryCreateRequest request) {
        return ResponseEntity.ok(storyService.conception(currentUser(principal), request));
    }

    @GetMapping("/story-cards/{storyId}/outlines")
    public List<OutlineDto> listOutlines(@PathVariable UUID storyId) {
        Story entity = storyRepository.getReferenceById(storyId);
        return outlineService.listByStory(entity);
    }

    @PostMapping("/story-cards/{storyId}/outlines")
    public OutlineDto createOutline(@PathVariable UUID storyId, @RequestBody OutlineCreateRequest request) {
        Story story = storyRepository.getReferenceById(storyId);
        return outlineService.createOutline(story, request);
    }

    @GetMapping("/outlines/{id}")
    public OutlineDto getOutline(@PathVariable UUID id) { return outlineService.get(id); }

    @PutMapping("/outlines/{id}")
    public OutlineDto saveOutline(@PathVariable UUID id, @RequestBody OutlineSaveRequest request) { return outlineService.saveOutline(id, request); }

    @DeleteMapping("/outlines/{id}")
    public ResponseEntity<Void> deleteOutline(@PathVariable UUID id) { outlineService.deleteOutline(id); return ResponseEntity.noContent().build(); }

    @PostMapping("/outlines/{outlineId}/chapters")
    public OutlineDto generateChapter(@PathVariable UUID outlineId, @RequestBody OutlineChapterGenerateRequest request) {
        return outlineService.addGeneratedChapter(outlineId, request);
    }

    @PutMapping("/chapters/{id}")
    public ResponseEntity<OutlineDto> updateChapter(@PathVariable UUID id, @RequestBody ChapterUpdateRequest request) {
        return ResponseEntity.ok(outlineService.updateChapter(id, request));
    }

    @PutMapping("/scenes/{id}")
    public ResponseEntity<OutlineDto> updateScene(@PathVariable UUID id, @RequestBody SceneUpdateRequest request) {
        return ResponseEntity.ok(outlineService.updateScene(id, request));
    }

    @PostMapping("/outlines/scenes/{id}/refine")
    public ResponseEntity<String> refineScene(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id, @RequestBody RefineRequest request) {
        String instruction = request.instruction() == null ? "" : request.instruction();
        return ResponseEntity.ok(aiService.refine(currentUser(principal), new AiRefineRequest(request.text(), instruction, null)).result());
    }
}

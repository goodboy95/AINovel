package com.ainovel.app.world;

import com.ainovel.app.common.RefineRequest;
import com.ainovel.app.user.User;
import com.ainovel.app.user.UserRepository;
import com.ainovel.app.world.dto.*;
import com.ainovel.app.ai.dto.AiRefineResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/worlds")
public class WorldController {
    @Autowired
    private WorldService worldService;
    @Autowired
    private UserRepository userRepository;

    private User currentUser(UserDetails details) { return userRepository.findByUsername(details.getUsername()).orElseThrow(); }

    @GetMapping
    public List<WorldDto> list(@AuthenticationPrincipal UserDetails principal) { return worldService.list(currentUser(principal)); }

    @PostMapping
    public WorldDetailDto create(@AuthenticationPrincipal UserDetails principal, @Valid @RequestBody WorldCreateRequest request) {
        return worldService.create(currentUser(principal), request);
    }

    @GetMapping("/{id}")
    public WorldDetailDto detail(@PathVariable UUID id) { return worldService.get(id); }

    @PutMapping("/{id}")
    public WorldDetailDto update(@PathVariable UUID id, @RequestBody WorldUpdateRequest request) { return worldService.update(id, request); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) { worldService.delete(id); return ResponseEntity.noContent().build(); }

    @PutMapping("/{id}/modules")
    public WorldDetailDto updateModules(@PathVariable UUID id, @RequestBody WorldModulesUpdateRequest request) { return worldService.updateModules(id, request); }

    @PutMapping("/{id}/modules/{moduleKey}")
    public WorldDetailDto updateModule(@PathVariable UUID id, @PathVariable String moduleKey, @RequestBody WorldModuleUpdateRequest request) { return worldService.updateModule(id, moduleKey, request); }

    @PostMapping("/{id}/modules/{moduleKey}/fields/{fieldKey}/refine")
    public ResponseEntity<AiRefineResponse> refineField(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id, @PathVariable String moduleKey, @PathVariable String fieldKey, @RequestBody RefineRequest request) {
        return ResponseEntity.ok(worldService.refineField(currentUser(principal), id, moduleKey, fieldKey, request.text(), request.instruction()));
    }

    @GetMapping("/{id}/publish/preview")
    public WorldPublishPreviewResponse preview(@PathVariable UUID id) { return worldService.preview(id); }

    @PostMapping("/{id}/publish")
    public WorldDetailDto publish(@PathVariable UUID id) { return worldService.publish(id); }

    @GetMapping("/{id}/generation")
    public WorldGenerationStatus generation(@PathVariable UUID id) { return worldService.generationStatus(id); }

    @PostMapping("/{id}/generation/{moduleKey}")
    public WorldDetailDto generate(@PathVariable UUID id, @PathVariable String moduleKey) { return worldService.generateModule(id, moduleKey); }

    @PostMapping("/{id}/generation/{moduleKey}/retry")
    public WorldDetailDto retry(@PathVariable UUID id, @PathVariable String moduleKey) { return worldService.retryModule(id, moduleKey); }
}

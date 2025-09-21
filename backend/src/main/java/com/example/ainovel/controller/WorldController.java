package com.example.ainovel.controller;

import com.example.ainovel.dto.world.WorldDetailResponse;
import com.example.ainovel.dto.world.WorldModulesBatchUpdateRequest;
import com.example.ainovel.dto.world.WorldModuleResponse;
import com.example.ainovel.dto.world.WorldModuleSummary;
import com.example.ainovel.dto.world.WorldModuleUpdateRequest;
import com.example.ainovel.dto.world.WorldPublishPreviewResponse;
import com.example.ainovel.dto.world.WorldPublishResponse;
import com.example.ainovel.dto.world.WorldGenerationStatusResponse;
import com.example.ainovel.dto.world.WorldFieldRefineRequest;
import com.example.ainovel.dto.world.WorldFieldRefineResponse;
import com.example.ainovel.dto.world.WorldSummaryResponse;
import com.example.ainovel.dto.world.WorldUpsertRequest;
import com.example.ainovel.model.User;
import com.example.ainovel.model.world.WorldModule;
import com.example.ainovel.model.world.WorldModuleStatus;
import com.example.ainovel.model.world.WorldStatus;
import com.example.ainovel.service.world.WorldAggregate;
import com.example.ainovel.service.world.WorldDtoMapper;
import com.example.ainovel.service.world.WorldFieldRefineService;
import com.example.ainovel.service.world.WorldModuleService;
import com.example.ainovel.service.world.WorldModuleGenerationService;
import com.example.ainovel.service.world.WorldPublicationService;
import com.example.ainovel.service.world.WorldService;
import com.example.ainovel.service.world.WorldGenerationWorkflowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/worlds")
public class WorldController {

    private final WorldService worldService;
    private final WorldModuleService worldModuleService;
    private final WorldModuleGenerationService worldModuleGenerationService;
    private final WorldFieldRefineService worldFieldRefineService;
    private final WorldPublicationService worldPublicationService;
    private final WorldGenerationWorkflowService worldGenerationWorkflowService;
    private final WorldDtoMapper worldDtoMapper;

    public WorldController(WorldService worldService,
                           WorldModuleService worldModuleService,
                           WorldModuleGenerationService worldModuleGenerationService,
                           WorldFieldRefineService worldFieldRefineService,
                           WorldPublicationService worldPublicationService,
                           WorldGenerationWorkflowService worldGenerationWorkflowService,
                           WorldDtoMapper worldDtoMapper) {
        this.worldService = worldService;
        this.worldModuleService = worldModuleService;
        this.worldModuleGenerationService = worldModuleGenerationService;
        this.worldFieldRefineService = worldFieldRefineService;
        this.worldPublicationService = worldPublicationService;
        this.worldGenerationWorkflowService = worldGenerationWorkflowService;
        this.worldDtoMapper = worldDtoMapper;
    }

    @PostMapping
    public ResponseEntity<WorldDetailResponse> createWorld(@AuthenticationPrincipal User user,
                                                           @RequestBody WorldUpsertRequest request) {
        WorldAggregate aggregate = worldService.createWorld(user, request);
        WorldDetailResponse response = worldDtoMapper.toDetail(aggregate.world(), aggregate.modules());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WorldSummaryResponse>> listWorlds(@AuthenticationPrincipal User user,
                                                                 @RequestParam(value = "status", required = false) String status) {
        WorldStatus filter = parseStatus(status);
        List<WorldAggregate> aggregates = worldService.listWorlds(user.getId(), filter);
        List<WorldSummaryResponse> responses = aggregates.stream()
                .map(aggregate -> worldDtoMapper.toSummary(aggregate.world(), aggregate.modules()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{worldId}")
    public ResponseEntity<WorldDetailResponse> getWorld(@PathVariable Long worldId,
                                                        @AuthenticationPrincipal User user) {
        WorldAggregate aggregate = worldService.getWorld(worldId, user.getId());
        WorldDetailResponse response = worldDtoMapper.toDetail(aggregate.world(), aggregate.modules());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{worldId}")
    public ResponseEntity<WorldDetailResponse> updateWorld(@PathVariable Long worldId,
                                                           @AuthenticationPrincipal User user,
                                                           @RequestBody WorldUpsertRequest request) {
        WorldAggregate aggregate = worldService.updateWorld(worldId, user.getId(), request);
        WorldDetailResponse response = worldDtoMapper.toDetail(aggregate.world(), aggregate.modules());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{worldId}")
    public ResponseEntity<Void> deleteWorld(@PathVariable Long worldId,
                                            @AuthenticationPrincipal User user) {
        worldService.deleteWorld(worldId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{worldId}/modules/{moduleKey}")
    public ResponseEntity<WorldModuleResponse> updateModule(@PathVariable Long worldId,
                                                            @PathVariable String moduleKey,
                                                            @AuthenticationPrincipal User user,
                                                            @RequestBody WorldModuleUpdateRequest request) {
        WorldModule module = worldModuleService.updateModule(worldId, user.getId(), moduleKey, request);
        return ResponseEntity.ok(worldDtoMapper.toModuleResponse(module));
    }

    @PutMapping("/{worldId}/modules")
    public ResponseEntity<List<WorldModuleResponse>> updateModules(@PathVariable Long worldId,
                                                                   @AuthenticationPrincipal User user,
                                                                   @RequestBody WorldModulesBatchUpdateRequest request) {
        List<WorldModule> modules = worldModuleService.updateModules(worldId, user.getId(), request);
        return ResponseEntity.ok(worldDtoMapper.toModuleResponses(modules));
    }

    @PostMapping("/{worldId}/modules/{moduleKey}/generate")
    public ResponseEntity<WorldModuleResponse> generateModule(@PathVariable Long worldId,
                                                               @PathVariable String moduleKey,
                                                               @AuthenticationPrincipal User user) {
        WorldModule module = worldModuleGenerationService.generateModule(worldId, moduleKey, user.getId());
        return ResponseEntity.ok(worldDtoMapper.toModuleResponse(module));
    }

    @PostMapping("/{worldId}/modules/{moduleKey}/fields/{fieldKey}/refine")
    public ResponseEntity<WorldFieldRefineResponse> refineField(@PathVariable Long worldId,
                                                                @PathVariable String moduleKey,
                                                                @PathVariable String fieldKey,
                                                                @AuthenticationPrincipal User user,
                                                                @RequestBody WorldFieldRefineRequest request) {
        String result = worldFieldRefineService.refineField(worldId, moduleKey, fieldKey, user.getId(), request);
        return ResponseEntity.ok(new WorldFieldRefineResponse().setResult(result));
    }

    @GetMapping("/{worldId}/publish/preview")
    public ResponseEntity<WorldPublishPreviewResponse> previewPublish(@PathVariable Long worldId,
                                                                      @AuthenticationPrincipal User user) {
        WorldPublicationService.PublicationAnalysis analysis = worldPublicationService.preview(worldId, user.getId());
        return ResponseEntity.ok(toPreviewResponse(analysis));
    }

    @PostMapping("/{worldId}/publish")
    public ResponseEntity<WorldPublishResponse> publish(@PathVariable Long worldId,
                                                        @AuthenticationPrincipal User user) {
        WorldPublicationService.PublicationAnalysis analysis = worldPublicationService.preparePublish(worldId, user.getId());
        WorldPublishResponse response = new WorldPublishResponse()
                .setWorldId(analysis.world().getId())
                .setModulesToGenerate(toModuleSummaries(analysis.modulesToGenerate()))
                .setModulesToReuse(toModuleSummaries(analysis.modulesToReuse()));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{worldId}/generation")
    public ResponseEntity<WorldGenerationStatusResponse> getGenerationStatus(@PathVariable Long worldId,
                                                                             @AuthenticationPrincipal User user) {
        WorldGenerationStatusResponse status = worldGenerationWorkflowService.getStatus(worldId, user.getId());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/{worldId}/generation/{moduleKey}/retry")
    public ResponseEntity<Void> retryGeneration(@PathVariable Long worldId,
                                                @PathVariable String moduleKey,
                                                @AuthenticationPrincipal User user) {
        worldGenerationWorkflowService.retryModule(worldId, moduleKey, user.getId());
        return ResponseEntity.noContent().build();
    }

    private WorldPublishPreviewResponse toPreviewResponse(WorldPublicationService.PublicationAnalysis analysis) {
        Map<String, WorldModuleStatus> statusMap = new LinkedHashMap<>();
        for (WorldModule module : analysis.modules()) {
            statusMap.put(module.getModuleKey(), module.getStatus());
        }
        WorldPublishPreviewResponse response = new WorldPublishPreviewResponse()
                .setReady(analysis.missingFields().isEmpty())
                .setModuleStatuses(statusMap)
                .setMissingFields(worldDtoMapper.buildMissingFields(analysis.missingFields()))
                .setModulesToGenerate(toModuleSummaries(analysis.modulesToGenerate()))
                .setModulesToReuse(toModuleSummaries(analysis.modulesToReuse()));
        return response;
    }

    private List<WorldModuleSummary> toModuleSummaries(List<WorldModule> modules) {
        return modules.stream()
                .map(module -> worldDtoMapper.toModuleSummary(module.getModuleKey()))
                .collect(Collectors.toList());
    }

    private WorldStatus parseStatus(String statusParam) {
        if (statusParam == null || statusParam.isBlank() || "all".equalsIgnoreCase(statusParam)) {
            return null;
        }
        return WorldStatus.fromString(statusParam)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "无效的状态参数"));
    }
}

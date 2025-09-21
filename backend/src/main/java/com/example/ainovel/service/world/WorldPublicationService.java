package com.example.ainovel.service.world;

import com.example.ainovel.exception.ResourceNotFoundException;
import com.example.ainovel.model.world.World;
import com.example.ainovel.model.world.WorldModule;
import com.example.ainovel.model.world.WorldModuleStatus;
import com.example.ainovel.model.world.WorldStatus;
import com.example.ainovel.repository.WorldModuleRepository;
import com.example.ainovel.repository.WorldRepository;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinition;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinitionRegistry;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorldPublicationService {

    private final WorldRepository worldRepository;
    private final WorldModuleRepository worldModuleRepository;
    private final WorldModuleDefinitionRegistry definitionRegistry;
    private final WorldGenerationWorkflowService generationWorkflowService;

    public WorldPublicationService(WorldRepository worldRepository,
                                   WorldModuleRepository worldModuleRepository,
                                   WorldModuleDefinitionRegistry definitionRegistry,
                                   WorldGenerationWorkflowService generationWorkflowService) {
        this.worldRepository = worldRepository;
        this.worldModuleRepository = worldModuleRepository;
        this.definitionRegistry = definitionRegistry;
        this.generationWorkflowService = generationWorkflowService;
    }

    @Transactional(readOnly = true)
    public PublicationAnalysis preview(Long worldId, Long userId) {
        World world = loadWorld(worldId, userId);
        List<WorldModule> modules = worldModuleRepository.findByWorldId(worldId);
        return analyse(world, modules);
    }

    public PublicationAnalysis preparePublish(Long worldId, Long userId) {
        World world = loadWorld(worldId, userId);
        if (world.getStatus() == WorldStatus.GENERATING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "世界正在生成中，无法再次发布");
        }
        List<WorldModule> modules = worldModuleRepository.findByWorldId(worldId);
        PublicationAnalysis analysis = analyse(world, modules);
        if (!analysis.missingFields().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "存在未填写完整的模块字段");
        }
        LocalDateTime now = LocalDateTime.now();
        world.setStatus(WorldStatus.GENERATING);
        world.setLastEditedBy(userId);
        world.setLastEditedAt(now);
        List<WorldModule> modulesToGenerate = analysis.modulesToGenerate();
        if (modulesToGenerate.isEmpty()) {
            world.setStatus(WorldStatus.ACTIVE);
            world.setVersion((world.getVersion() == null ? 0 : world.getVersion()) + 1);
            world.setPublishedAt(now);
            worldRepository.save(world);
            return analysis;
        }
        for (WorldModule module : modulesToGenerate) {
            module.setStatus(WorldModuleStatus.AWAITING_GENERATION);
            module.setLastEditedBy(userId);
            module.setLastEditedAt(now);
        }
        worldRepository.save(world);
        worldModuleRepository.saveAll(modulesToGenerate);
        generationWorkflowService.initializeJobs(world, modulesToGenerate);
        return analysis;
    }

    private PublicationAnalysis analyse(World world, List<WorldModule> modules) {
        Map<String, WorldModule> moduleMap = modules.stream()
                .collect(Collectors.toMap(WorldModule::getModuleKey, module -> module));
        Map<String, List<String>> missing = new LinkedHashMap<>();
        Set<WorldModule> toGenerate = new LinkedHashSet<>();
        List<WorldModule> toReuse = new ArrayList<>();

        for (WorldModuleDefinition definition : definitionRegistry.getAll()) {
            WorldModule module = moduleMap.get(definition.key());
            if (module == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "缺少模块数据: " + definition.key());
            }
            List<String> missingFields = collectMissingFields(definition, module);
            if (!missingFields.isEmpty()) {
                missing.put(definition.key(), missingFields);
            }
            if (shouldGenerate(world, module)) {
                toGenerate.add(module);
            } else if (module.getStatus() == WorldModuleStatus.COMPLETED) {
                toReuse.add(module);
            }
        }
        return new PublicationAnalysis(world, modules, missing, new ArrayList<>(toGenerate), toReuse);
    }

    private List<String> collectMissingFields(WorldModuleDefinition definition, WorldModule module) {
        List<String> result = new ArrayList<>();
        Map<String, String> fields = module.getFields();
        for (WorldModuleDefinition.FieldDefinition field : definition.fields()) {
            String value = fields == null ? null : fields.get(field.key());
            if (!field.required()) {
                continue;
            }
            if (value == null || value.isBlank()) {
                result.add(field.key());
            }
        }
        return result;
    }

    private boolean shouldGenerate(World world, WorldModule module) {
        WorldModuleStatus status = module.getStatus();
        if (world.getVersion() == null || world.getVersion() == 0) {
            return status == WorldModuleStatus.READY || status == WorldModuleStatus.AWAITING_GENERATION;
        }
        if (status == WorldModuleStatus.AWAITING_GENERATION || status == WorldModuleStatus.READY) {
            return true;
        }
        if (status == WorldModuleStatus.FAILED) {
            return true;
        }
        return false;
    }

    private World loadWorld(Long worldId, Long userId) {
        return worldRepository.findByIdAndUserId(worldId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("世界不存在或无权访问"));
    }

    public record PublicationAnalysis(
            World world,
            List<WorldModule> modules,
            Map<String, List<String>> missingFields,
            List<WorldModule> modulesToGenerate,
            List<WorldModule> modulesToReuse
    ) {
    }
}

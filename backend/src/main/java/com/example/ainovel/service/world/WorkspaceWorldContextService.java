package com.example.ainovel.service.world;

import com.example.ainovel.model.world.World;
import com.example.ainovel.model.world.WorldModule;
import com.example.ainovel.model.world.WorldStatus;
import com.example.ainovel.repository.WorldModuleRepository;
import com.example.ainovel.repository.WorldRepository;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinition;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinitionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class WorkspaceWorldContextService {

    private final WorldRepository worldRepository;
    private final WorldModuleRepository worldModuleRepository;
    private final WorldModuleDefinitionRegistry definitionRegistry;

    public WorkspaceWorldContextService(WorldRepository worldRepository,
                                        WorldModuleRepository worldModuleRepository,
                                        WorldModuleDefinitionRegistry definitionRegistry) {
        this.worldRepository = worldRepository;
        this.worldModuleRepository = worldModuleRepository;
        this.definitionRegistry = definitionRegistry;
    }

    public Optional<WorkspaceWorldContext> getContext(Long userId, Long worldId) {
        if (userId == null || worldId == null) {
            return Optional.empty();
        }
        Optional<World> worldOpt = worldRepository.findByIdAndUserIdAndStatus(worldId, userId, WorldStatus.ACTIVE);
        if (worldOpt.isEmpty()) {
            return Optional.empty();
        }
        World world = worldOpt.get();
        List<WorldModule> modules = worldModuleRepository.findByWorldId(world.getId());
        Map<String, WorkspaceWorldModule> moduleMap = new LinkedHashMap<>();
        for (WorldModuleDefinition definition : definitionRegistry.getAll()) {
            WorldModule module = modules.stream()
                    .filter(item -> definition.key().equals(item.getModuleKey()))
                    .findFirst()
                    .orElse(null);
            String content = module == null ? "" : Optional.ofNullable(module.getFullContent()).orElse("");
            String excerpt = buildExcerpt(content);
            WorkspaceWorldModule view = new WorkspaceWorldModule(
                    definition.key(),
                    definition.label(),
                    content,
                    excerpt,
                    module != null && module.getFullContentUpdatedAt() != null
                            ? module.getFullContentUpdatedAt().atZone(ZoneId.systemDefault()).toInstant()
                            : null
            );
            moduleMap.put(definition.key(), view);
        }

        WorkspaceWorldModulesView modulesView = new WorkspaceWorldModulesView(moduleMap);
        WorkspaceWorldContext context = new WorkspaceWorldContext(
                world.getId(),
                world.getName(),
                world.getTagline(),
                world.getThemes(),
                world.getCreativeIntent(),
                modulesView
        );
        return Optional.of(context);
    }

    private String buildExcerpt(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.trim();
        if (normalized.length() <= 200) {
            return normalized;
        }
        return normalized.substring(0, 200);
    }
}

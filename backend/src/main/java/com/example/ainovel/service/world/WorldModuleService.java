package com.example.ainovel.service.world;

import com.example.ainovel.dto.world.WorldModulesBatchUpdateRequest;
import com.example.ainovel.dto.world.WorldModuleUpdateRequest;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class WorldModuleService {

    private final WorldRepository worldRepository;
    private final WorldModuleRepository worldModuleRepository;
    private final WorldModuleDefinitionRegistry definitionRegistry;

    public WorldModuleService(WorldRepository worldRepository,
                              WorldModuleRepository worldModuleRepository,
                              WorldModuleDefinitionRegistry definitionRegistry) {
        this.worldRepository = worldRepository;
        this.worldModuleRepository = worldModuleRepository;
        this.definitionRegistry = definitionRegistry;
    }

    public WorldModule updateModule(Long worldId, Long userId, String moduleKey, WorldModuleUpdateRequest request) {
        World world = loadWorld(worldId, userId);
        ensureEditable(world);
        WorldModule module = worldModuleRepository.findByWorldIdAndModuleKey(worldId, moduleKey)
                .orElseThrow(() -> new ResourceNotFoundException("指定模块不存在"));
        boolean changed = applyFieldUpdates(world, module, request == null ? Map.of() : request.getFields(), userId);
        if (changed) {
            worldModuleRepository.save(module);
            worldRepository.save(world);
        }
        return module;
    }

    public List<WorldModule> updateModules(Long worldId, Long userId, WorldModulesBatchUpdateRequest request) {
        World world = loadWorld(worldId, userId);
        ensureEditable(world);
        List<WorldModule> modules = worldModuleRepository.findByWorldId(worldId);
        Map<String, WorldModule> moduleMap = modules.stream()
                .collect(java.util.stream.Collectors.toMap(WorldModule::getModuleKey, module -> module));
        List<WorldModule> updated = new ArrayList<>();
        if (request != null && request.getModules() != null) {
            for (WorldModulesBatchUpdateRequest.ModuleUpdate update : request.getModules()) {
                if (update == null || update.getKey() == null) {
                    continue;
                }
                WorldModule module = moduleMap.get(update.getKey());
                if (module == null) {
                    throw new ResourceNotFoundException("模块 " + update.getKey() + " 不存在");
                }
                if (applyFieldUpdates(world, module, update.getFields(), userId)) {
                    updated.add(module);
                }
            }
        }
        if (!updated.isEmpty()) {
            worldModuleRepository.saveAll(updated);
            worldRepository.save(world);
        }
        return modules;
    }

    private boolean applyFieldUpdates(World world, WorldModule module, Map<String, String> updates, Long userId) {
        if (updates == null || updates.isEmpty()) {
            return false;
        }
        LinkedHashMap<String, String> current = module.getFields() == null ? new LinkedHashMap<>()
                : new LinkedHashMap<>(module.getFields());
        boolean changed = false;
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String fieldKey = entry.getKey();
            if (fieldKey == null) {
                continue;
            }
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            WorldModuleDefinition.FieldDefinition fieldDefinition;
            try {
                fieldDefinition = definitionRegistry.requireField(module.getModuleKey(), fieldKey);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                if (current.remove(fieldKey) != null) {
                    changed = true;
                }
                continue;
            }
            validateLength(trimmed, fieldDefinition);
            String existing = current.get(fieldKey);
            if (!Objects.equals(existing, trimmed)) {
                current.put(fieldKey, trimmed);
                changed = true;
            }
        }
        if (!changed) {
            return false;
        }
        module.setFields(current);
        String newHash = WorldModuleContentHelper.computeContentHash(current);
        boolean contentChanged = !Objects.equals(module.getContentHash(), newHash);
        module.setContentHash(newHash);
        LocalDateTime now = LocalDateTime.now();
        module.setLastEditedBy(userId);
        module.setLastEditedAt(now);
        WorldModuleStatus newStatus = WorldModuleContentHelper.determineStatus(definitionRegistry, world,
                module.getModuleKey(), current, contentChanged);
        module.setStatus(newStatus);
        if (contentChanged && world.getStatus() == WorldStatus.ACTIVE) {
            world.setStatus(WorldStatus.DRAFT);
        }
        if (contentChanged) {
            world.setLastEditedBy(userId);
            world.setLastEditedAt(now);
        }
        return true;
    }

    private void validateLength(String value, WorldModuleDefinition.FieldDefinition fieldDefinition) {
        int length = value.length();
        if (fieldDefinition.minLength() > 0 && length < fieldDefinition.minLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldDefinition.label() + " 内容至少需要 " + fieldDefinition.minLength() + " 字");
        }
        if (fieldDefinition.maxLength() > 0 && length > fieldDefinition.maxLength()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldDefinition.label() + " 内容需不超过 " + fieldDefinition.maxLength() + " 字");
        }
    }

    private World loadWorld(Long worldId, Long userId) {
        return worldRepository.findByIdAndUserId(worldId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("世界不存在或无权访问"));
    }

    private void ensureEditable(World world) {
        if (world.getStatus() == WorldStatus.GENERATING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "世界正在生成中，无法修改");
        }
    }

}

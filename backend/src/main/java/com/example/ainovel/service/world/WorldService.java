package com.example.ainovel.service.world;

import com.example.ainovel.dto.world.WorldUpsertRequest;
import com.example.ainovel.exception.ResourceNotFoundException;
import com.example.ainovel.model.User;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorldService {

    private static final int MAX_THEMES = 5;
    private static final int MIN_THEMES = 1;
    private static final int MAX_THEME_LENGTH = 30;

    private final WorldRepository worldRepository;
    private final WorldModuleRepository worldModuleRepository;
    private final WorldModuleDefinitionRegistry definitionRegistry;

    public WorldService(WorldRepository worldRepository,
                        WorldModuleRepository worldModuleRepository,
                        WorldModuleDefinitionRegistry definitionRegistry) {
        this.worldRepository = worldRepository;
        this.worldModuleRepository = worldModuleRepository;
        this.definitionRegistry = definitionRegistry;
    }

    public WorldAggregate createWorld(User user, WorldUpsertRequest request) {
        validateBasicInfo(request);
        World world = new World();
        world.setUser(user);
        applyBasicInfo(world, request);
        world.setStatus(WorldStatus.DRAFT);
        world.setVersion(0);
        LocalDateTime now = LocalDateTime.now();
        world.setLastEditedBy(user.getId());
        world.setLastEditedAt(now);

        List<WorldModule> modules = new ArrayList<>();
        for (WorldModuleDefinition definition : definitionRegistry.getAll()) {
            WorldModule module = new WorldModule();
            module.setWorld(world);
            module.setModuleKey(definition.key());
            module.setStatus(WorldModuleStatus.EMPTY);
            module.setFields(new LinkedHashMap<>());
            module.setLastEditedBy(user.getId());
            module.setLastEditedAt(now);
            modules.add(module);
            world.getModules().add(module);
        }

        World saved = worldRepository.save(world);
        List<WorldModule> persistedModules = worldModuleRepository.findByWorldId(saved.getId());
        return new WorldAggregate(saved, persistedModules);
    }

    @Transactional(readOnly = true)
    public WorldAggregate getWorld(Long worldId, Long userId) {
        World world = loadWorld(worldId, userId);
        List<WorldModule> modules = worldModuleRepository.findByWorldId(worldId);
        return new WorldAggregate(world, modules);
    }

    @Transactional(readOnly = true)
    public List<WorldAggregate> listWorlds(Long userId, WorldStatus statusFilter) {
        List<World> worlds;
        if (statusFilter == null) {
            worlds = worldRepository.findAllByUserIdOrderByUpdatedAtDesc(userId);
        } else {
            worlds = worldRepository.findAllByUserIdAndStatusOrderByUpdatedAtDesc(userId, statusFilter);
        }
        if (worlds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> worldIds = worlds.stream().map(World::getId).toList();
        Map<Long, List<WorldModule>> moduleMap = worldModuleRepository.findByWorldIdIn(worldIds)
                .stream()
                .collect(Collectors.groupingBy(module -> module.getWorld().getId()));
        return worlds.stream()
                .map(world -> new WorldAggregate(world, moduleMap.getOrDefault(world.getId(), List.of())))
                .collect(Collectors.toList());
    }

    public WorldAggregate updateWorld(Long worldId, Long userId, WorldUpsertRequest request) {
        validateBasicInfo(request);
        World world = loadWorld(worldId, userId);
        if (world.getStatus() == WorldStatus.GENERATING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "世界正在生成中，无法修改");
        }
        boolean changed = applyBasicInfo(world, request);
        if (changed) {
            if (world.getStatus() == WorldStatus.ACTIVE) {
                world.setStatus(WorldStatus.DRAFT);
            }
            world.setLastEditedBy(userId);
            world.setLastEditedAt(LocalDateTime.now());
        }
        World saved = worldRepository.save(world);
        List<WorldModule> modules = worldModuleRepository.findByWorldId(worldId);
        return new WorldAggregate(saved, modules);
    }

    public void deleteWorld(Long worldId, Long userId) {
        World world = loadWorld(worldId, userId);
        if (world.getStatus() != WorldStatus.DRAFT || (world.getVersion() != null && world.getVersion() > 0)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "仅允许删除未发布的草稿世界");
        }
        worldRepository.delete(world);
    }

    private World loadWorld(Long worldId, Long userId) {
        return worldRepository.findByIdAndUserId(worldId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("世界不存在或无权访问"));
    }

    private void validateBasicInfo(WorldUpsertRequest request) {
        String name = safeTrim(request.getName());
        if (name == null || name.length() < 1 || name.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "世界名称需 1-80 字符");
        }
        String tagline = safeTrim(request.getTagline());
        if (tagline == null || tagline.length() < 20 || tagline.length() > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "一句话概述需 20-180 字");
        }
        String creativeIntent = safeTrim(request.getCreativeIntent());
        if (creativeIntent == null || creativeIntent.length() < 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "创作意图需至少 150 字");
        }
        String notes = safeTrim(request.getNotes());
        if (notes != null && notes.length() > 300) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "开发者备注最长 300 字");
        }
        List<String> themes = sanitizeThemes(request.getThemes());
        if (themes.size() < MIN_THEMES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少提供 1 个主题标签");
        }
    }

    private boolean applyBasicInfo(World world, WorldUpsertRequest request) {
        boolean changed = false;
        String name = safeTrim(request.getName());
        if (!Objects.equals(world.getName(), name)) {
            world.setName(name);
            changed = true;
        }
        String tagline = safeTrim(request.getTagline());
        if (!Objects.equals(world.getTagline(), tagline)) {
            world.setTagline(tagline);
            changed = true;
        }
        String creativeIntent = safeTrim(request.getCreativeIntent());
        if (!Objects.equals(world.getCreativeIntent(), creativeIntent)) {
            world.setCreativeIntent(creativeIntent);
            changed = true;
        }
        String notes = safeTrim(request.getNotes());
        if (!Objects.equals(world.getNotes(), notes)) {
            world.setNotes(notes);
            changed = true;
        }
        List<String> themes = sanitizeThemes(request.getThemes());
        if (!Objects.equals(world.getThemes(), themes)) {
            world.setThemes(new ArrayList<>(themes));
            changed = true;
        }
        return changed;
    }

    private List<String> sanitizeThemes(List<String> rawThemes) {
        if (rawThemes == null) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String theme : rawThemes) {
            String value = safeTrim(theme);
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (value.length() > MAX_THEME_LENGTH) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "主题标签长度需在 1-" + MAX_THEME_LENGTH + " 字之间");
            }
            cleaned.add(value);
        }
        if (cleaned.size() > MAX_THEMES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "主题标签最多 5 个");
        }
        // 去重但保留顺序
        List<String> distinct = new ArrayList<>();
        Set<String> seen = new java.util.LinkedHashSet<>();
        for (String item : cleaned) {
            if (seen.add(item)) {
                distinct.add(item);
            }
        }
        return distinct;
    }

    private String safeTrim(String value) {
        return value == null ? null : value.trim();
    }
}

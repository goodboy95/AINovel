package com.ainovel.app.world;

import com.ainovel.app.world.dto.*;
import com.ainovel.app.world.model.World;
import com.ainovel.app.world.repo.WorldRepository;
import com.ainovel.app.user.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class WorldService {
    @Autowired
    private WorldRepository worldRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<WorldDefinitionDto> definitions() {
        return List.of(
                new WorldDefinitionDto("geography", "地理环境", "定义世界的地形、气候、重要地点", List.of(
                        new WorldDefinitionDto.Field("terrain", "地形地貌", "textarea", "例如：群山、海岛"),
                        new WorldDefinitionDto.Field("climate", "气候特征", "textarea", "例如：季风、极寒"),
                        new WorldDefinitionDto.Field("locations", "重要地点", "textarea", "列出关键城市")
                )),
                new WorldDefinitionDto("society", "社会体系", "政治、经济、文化", List.of(
                        new WorldDefinitionDto.Field("politics", "政治体制", "textarea", "君主制/联邦"),
                        new WorldDefinitionDto.Field("economy", "经济结构", "textarea", "主要产业"),
                        new WorldDefinitionDto.Field("culture", "文化", "textarea", "风俗")
                )),
                new WorldDefinitionDto("magic_tech", "魔法/科技", "魔法或科技体系", List.of(
                        new WorldDefinitionDto.Field("system_name", "体系名称", "text", ""),
                        new WorldDefinitionDto.Field("rules", "规则", "textarea", "力量来源"),
                        new WorldDefinitionDto.Field("limitations", "限制", "textarea", "代价")
                ))
        );
    }

    public List<WorldDto> list(User user) {
        return worldRepository.findByUser(user).stream()
                .map(w -> new WorldDto(w.getId(), w.getName(), w.getTagline(), w.getStatus(), w.getVersion(), w.getUpdatedAt()))
                .toList();
    }

    @Transactional
    public WorldDetailDto create(User user, WorldCreateRequest request) {
        World world = new World();
        world.setUser(user);
        world.setName(request.name());
        world.setTagline(request.tagline());
        world.setThemesJson(writeJson(request.themes()));
        world.setCreativeIntent(request.creativeIntent());
        world.setNotes(request.notes());
        world.setStatus("draft");
        world.setVersion("0.1.0");
        world.setModulesJson(writeJson(new HashMap<String, Map<String, String>>()));
        world.setModuleProgressJson(writeJson(new HashMap<String, String>()));
        worldRepository.save(world);
        return toDetail(world);
    }

    public WorldDetailDto get(UUID id) {
        return toDetail(worldRepository.findById(id).orElseThrow(() -> new RuntimeException("世界不存在")));
    }

    @Transactional
    public WorldDetailDto update(UUID id, WorldUpdateRequest request) {
        World world = worldRepository.findById(id).orElseThrow(() -> new RuntimeException("世界不存在"));
        if (request.name() != null) world.setName(request.name());
        if (request.tagline() != null) world.setTagline(request.tagline());
        if (request.themes() != null) world.setThemesJson(writeJson(request.themes()));
        if (request.creativeIntent() != null) world.setCreativeIntent(request.creativeIntent());
        if (request.notes() != null) world.setNotes(request.notes());
        worldRepository.save(world);
        return toDetail(world);
    }

    @Transactional
    public void delete(UUID id) {
        World world = worldRepository.findById(id).orElseThrow();
        if (!"draft".equalsIgnoreCase(world.getStatus())) {
            throw new RuntimeException("仅草稿世界可删除");
        }
        worldRepository.delete(world);
    }

    @Transactional
    public WorldDetailDto updateModules(UUID id, WorldModulesUpdateRequest request) {
        World world = worldRepository.findById(id).orElseThrow();
        Map<String, Map<String, String>> modules = readModules(world.getModulesJson());
        if (request.modules() != null) modules.putAll(request.modules());
        world.setModulesJson(writeJson(modules));
        worldRepository.save(world);
        return toDetail(world);
    }

    @Transactional
    public WorldDetailDto updateModule(UUID id, String moduleKey, WorldModuleUpdateRequest request) {
        World world = worldRepository.findById(id).orElseThrow();
        Map<String, Map<String, String>> modules = readModules(world.getModulesJson());
        modules.put(moduleKey, request.fields());
        world.setModulesJson(writeJson(modules));
        worldRepository.save(world);
        return toDetail(world);
    }

    @Transactional
    public String refineField(UUID id, String moduleKey, String fieldKey, String text, String instruction) {
        return "【精修】" + text;
    }

    public WorldPublishPreviewResponse preview(UUID id) {
        World world = worldRepository.findById(id).orElseThrow();
        Map<String, Map<String, String>> modules = readModules(world.getModulesJson());
        List<String> missing = new ArrayList<>();
        for (WorldDefinitionDto def : definitions()) {
            if (!modules.containsKey(def.key())) missing.add(def.key());
        }
        return new WorldPublishPreviewResponse(missing, missing);
    }

    @Transactional
    public WorldDetailDto publish(UUID id) {
        World world = worldRepository.findById(id).orElseThrow();
        world.setStatus("generating");
        Map<String, String> progress = new HashMap<>();
        definitions().forEach(d -> progress.put(d.key(), "GENERATING"));
        world.setModuleProgressJson(writeJson(progress));
        worldRepository.save(world);
        return toDetail(world);
    }

    public WorldGenerationStatus generationStatus(UUID id) {
        World world = worldRepository.findById(id).orElseThrow();
        Map<String, String> progress = readProgress(world.getModuleProgressJson());
        List<WorldGenerationStatus.ModuleStatus> modules = progress.entrySet().stream()
                .map(e -> new WorldGenerationStatus.ModuleStatus(e.getKey(), e.getValue(), 1, null))
                .toList();
        return new WorldGenerationStatus(modules);
    }

    @Transactional
    public WorldDetailDto generateModule(UUID id, String moduleKey) {
        World world = worldRepository.findById(id).orElseThrow();
        Map<String, String> progress = readProgress(world.getModuleProgressJson());
        progress.put(moduleKey, "COMPLETED");
        world.setModuleProgressJson(writeJson(progress));

        Map<String, Map<String, String>> modules = readModules(world.getModulesJson());
        modules.putIfAbsent(moduleKey, new HashMap<>());
        modules.get(moduleKey).put("auto", "AI 生成的占位内容");
        world.setModulesJson(writeJson(modules));
        worldRepository.save(world);
        return toDetail(world);
    }

    @Transactional
    public WorldDetailDto retryModule(UUID id, String moduleKey) {
        return generateModule(id, moduleKey);
    }

    private WorldDetailDto toDetail(World world) {
        return new WorldDetailDto(
                world.getId(),
                world.getName(),
                world.getTagline(),
                world.getStatus(),
                world.getVersion(),
                readThemes(world.getThemesJson()),
                world.getCreativeIntent(),
                world.getNotes(),
                readModules(world.getModulesJson()),
                readProgress(world.getModuleProgressJson()),
                world.getUpdatedAt()
        );
    }

    private List<String> readThemes(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try { return objectMapper.readValue(json, new TypeReference<>() {});} catch (Exception e) { return new ArrayList<>(); }
    }

    private Map<String, Map<String, String>> readModules(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try { return objectMapper.readValue(json, new TypeReference<>() {});} catch (Exception e) { return new HashMap<>(); }
    }

    private Map<String, String> readProgress(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try { return objectMapper.readValue(json, new TypeReference<>() {});} catch (Exception e) { return new HashMap<>(); }
    }

    private String writeJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj);} catch (Exception e) { return "{}"; }
    }
}

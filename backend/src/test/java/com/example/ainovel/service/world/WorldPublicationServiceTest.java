package com.example.ainovel.service.world;

import com.example.ainovel.model.User;
import com.example.ainovel.model.world.World;
import com.example.ainovel.model.world.WorldModule;
import com.example.ainovel.model.world.WorldModuleStatus;
import com.example.ainovel.model.world.WorldStatus;
import com.example.ainovel.repository.WorldModuleRepository;
import com.example.ainovel.repository.WorldRepository;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinition;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinitionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldPublicationServiceTest {

    @Mock
    private WorldRepository worldRepository;
    @Mock
    private WorldModuleRepository worldModuleRepository;
    @Mock
    private WorldModuleDefinitionRegistry definitionRegistry;
    @Mock
    private WorldGenerationWorkflowService generationWorkflowService;

    private WorldPublicationService service;

    @BeforeEach
    void setUp() {
        service = new WorldPublicationService(worldRepository, worldModuleRepository, definitionRegistry,
                generationWorkflowService);
    }

    @Test
    void preparePublishShouldQueueReadyModules() {
        World world = createWorld(1L, 99L, WorldStatus.DRAFT, 0);
        WorldModule cosmos = createModule(world, "cosmos", WorldModuleStatus.READY,
                Map.of("cosmos_structure", "广袤的浮空大陆"));
        WorldModule geography = createModule(world, "geography", WorldModuleStatus.COMPLETED,
                Map.of("landmarks", "光之城"));

        when(worldRepository.findByIdAndUserId(1L, 99L)).thenReturn(Optional.of(world));
        when(worldModuleRepository.findByWorldId(1L)).thenReturn(List.of(cosmos, geography));
        when(definitionRegistry.getAll()).thenReturn(List.of(
                new WorldModuleDefinition("cosmos", "宇宙观", List.of(new WorldModuleDefinition.FieldDefinition(
                        "cosmos_structure", "宇宙结构", true, 10, 500)), 1),
                new WorldModuleDefinition("geography", "地理", List.of(new WorldModuleDefinition.FieldDefinition(
                        "landmarks", "地标", true, 5, 400)), 2)
        ));

        WorldPublicationService.PublicationAnalysis analysis = service.preparePublish(1L, 99L);

        assertThat(world.getStatus()).isEqualTo(WorldStatus.GENERATING);
        assertThat(cosmos.getStatus()).isEqualTo(WorldModuleStatus.AWAITING_GENERATION);
        assertThat(cosmos.getLastEditedBy()).isEqualTo(99L);
        assertThat(analysis.modulesToGenerate()).containsExactly(cosmos);
        assertThat(analysis.modulesToReuse()).containsExactly(geography);
        verify(worldModuleRepository).saveAll(anyIterable());
        verify(worldRepository).save(world);
        verify(generationWorkflowService).initializeJobs(world, List.of(cosmos));
    }

    @Test
    void preparePublishShouldRejectMissingFields() {
        World world = createWorld(2L, 88L, WorldStatus.DRAFT, 0);
        WorldModule cosmos = createModule(world, "cosmos", WorldModuleStatus.READY, new LinkedHashMap<>());
        when(worldRepository.findByIdAndUserId(2L, 88L)).thenReturn(Optional.of(world));
        when(worldModuleRepository.findByWorldId(2L)).thenReturn(List.of(cosmos));
        when(definitionRegistry.getAll()).thenReturn(List.of(
                new WorldModuleDefinition("cosmos", "宇宙观", List.of(new WorldModuleDefinition.FieldDefinition(
                        "cosmos_structure", "宇宙结构", true, 10, 500)), 1)
        ));

        assertThatThrownBy(() -> service.preparePublish(2L, 88L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void preparePublishShouldActivateImmediatelyWhenNoGenerationNeeded() {
        World world = createWorld(3L, 77L, WorldStatus.DRAFT, 1);
        WorldModule cosmos = createModule(world, "cosmos", WorldModuleStatus.COMPLETED,
                Map.of("cosmos_structure", "浮空大陆"));
        WorldModule geography = createModule(world, "geography", WorldModuleStatus.COMPLETED,
                Map.of("landmarks", "星辉岛"));

        when(worldRepository.findByIdAndUserId(3L, 77L)).thenReturn(Optional.of(world));
        when(worldModuleRepository.findByWorldId(3L)).thenReturn(List.of(cosmos, geography));
        when(definitionRegistry.getAll()).thenReturn(List.of(
                new WorldModuleDefinition("cosmos", "宇宙观", List.of(new WorldModuleDefinition.FieldDefinition(
                        "cosmos_structure", "宇宙结构", true, 10, 500)), 1),
                new WorldModuleDefinition("geography", "地理", List.of(new WorldModuleDefinition.FieldDefinition(
                        "landmarks", "地标", true, 5, 400)), 2)
        ));

        WorldPublicationService.PublicationAnalysis analysis = service.preparePublish(3L, 77L);

        assertThat(world.getStatus()).isEqualTo(WorldStatus.ACTIVE);
        assertThat(world.getVersion()).isEqualTo(2);
        assertThat(world.getPublishedAt()).isNotNull();
        assertThat(analysis.modulesToGenerate()).isEmpty();
        verify(worldModuleRepository, never()).saveAll(anyIterable());
        verify(generationWorkflowService, never()).initializeJobs(eq(world), any());
        verify(worldRepository).save(world);
    }

    private World createWorld(Long worldId, Long userId, WorldStatus status, int version) {
        World world = new World();
        world.setId(worldId);
        world.setStatus(status);
        world.setVersion(version);
        User owner = new User();
        owner.setId(userId);
        world.setUser(owner);
        world.setName("永昼群星");
        world.setTagline("光影交错的天空之城");
        world.setCreativeIntent("打造可复用的幻想世界");
        world.setThemes(List.of("幻想"));
        return world;
    }

    private WorldModule createModule(World world, String key, WorldModuleStatus status, Map<String, String> fields) {
        WorldModule module = new WorldModule();
        module.setWorld(world);
        module.setModuleKey(key);
        module.setStatus(status);
        module.setFields(new LinkedHashMap<>(fields));
        module.setLastEditedAt(LocalDateTime.now());
        module.setLastEditedBy(world.getUser() == null ? null : world.getUser().getId());
        return module;
    }
}

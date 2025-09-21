package com.example.ainovel.service.world;

import com.example.ainovel.model.User;
import com.example.ainovel.model.world.World;
import com.example.ainovel.model.world.WorldGenerationJob;
import com.example.ainovel.model.world.WorldGenerationJobStatus;
import com.example.ainovel.model.world.WorldGenerationJobType;
import com.example.ainovel.model.world.WorldModule;
import com.example.ainovel.model.world.WorldModuleStatus;
import com.example.ainovel.model.world.WorldStatus;
import com.example.ainovel.repository.WorldGenerationJobRepository;
import com.example.ainovel.repository.WorldModuleRepository;
import com.example.ainovel.repository.WorldRepository;
import com.example.ainovel.service.AiService;
import com.example.ainovel.service.SettingsService;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinitionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldGenerationWorkflowServiceTest {

    @Mock
    private WorldGenerationJobRepository jobRepository;
    @Mock
    private WorldRepository worldRepository;
    @Mock
    private WorldModuleRepository worldModuleRepository;
    @Mock
    private WorldPromptTemplateService templateService;
    @Mock
    private WorldPromptContextBuilder contextBuilder;
    @Mock
    private WorldModuleDefinitionRegistry definitionRegistry;
    @Mock
    private AiService aiService;
    @Mock
    private SettingsService settingsService;

    private WorldGenerationWorkflowService service;

    @BeforeEach
    void setUp() {
        service = new WorldGenerationWorkflowService(jobRepository, worldRepository, worldModuleRepository,
                templateService, contextBuilder, definitionRegistry, aiService, settingsService, 120);
    }

    @Test
    void initializeJobsShouldCreateSequentialEntries() {
        World world = new World();
        world.setId(5L);
        WorldModule first = new WorldModule();
        first.setWorld(world);
        first.setModuleKey("cosmos");
        WorldModule second = new WorldModule();
        second.setWorld(world);
        second.setModuleKey("geography");

        service.initializeJobs(world, List.of(first, second));

        verify(jobRepository).deleteByWorldId(5L);
        ArgumentCaptor<Iterable<WorldGenerationJob>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(jobRepository).saveAll(captor.capture());
        List<WorldGenerationJob> jobs = StreamSupport.stream(captor.getValue().spliterator(), false).toList();
        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).getModuleKey()).isEqualTo("cosmos");
        assertThat(jobs.get(0).getSequence()).isEqualTo(1);
        assertThat(jobs.get(0).getJobType()).isEqualTo(WorldGenerationJobType.MODULE_FINAL);
        assertThat(jobs.get(0).getStatus()).isEqualTo(WorldGenerationJobStatus.WAITING);
        assertThat(jobs.get(1).getSequence()).isEqualTo(2);
    }

    @Test
    void processQueueShouldGenerateModuleAndFinalizeWorld() {
        World world = createWorld(7L, 321L, WorldStatus.GENERATING, 0);
        WorldGenerationJob job = createJob(world, "cosmos");
        WorldModule module = createModule(world, "cosmos", WorldModuleStatus.AWAITING_GENERATION,
                Map.of("cosmos_structure", "原始设定"));
        List<WorldModule> modules = List.of(module);

        when(jobRepository.fetchNextJobForUpdate()).thenReturn(Optional.of(job));
        when(worldModuleRepository.findByWorldId(7L)).thenReturn(modules);
        when(contextBuilder.buildModuleContext(world, module, modules)).thenReturn(Map.of("key", "value"));
        when(templateService.renderFinalTemplate(eq("cosmos"), anyMap(), eq(321L))).thenReturn("prompt");
        when(settingsService.getDecryptedApiKeyByUserId(321L)).thenReturn("api-key");
        when(aiService.generate("prompt", "api-key", null, null)).thenReturn("  生成后的完整信息  ");
        when(jobRepository.countByWorldIdAndStatusIn(eq(7L), anyCollection())).thenReturn(0L);

        service.processQueue();

        assertThat(job.getStatus()).isEqualTo(WorldGenerationJobStatus.SUCCEEDED);
        assertThat(job.getAttempts()).isEqualTo(1);
        assertThat(job.getStartedAt()).isNotNull();
        assertThat(job.getFinishedAt()).isNotNull();
        assertThat(module.getStatus()).isEqualTo(WorldModuleStatus.COMPLETED);
        assertThat(module.getFullContent()).isEqualTo("生成后的完整信息");
        assertThat(world.getStatus()).isEqualTo(WorldStatus.ACTIVE);
        assertThat(world.getVersion()).isEqualTo(1);
        assertThat(world.getPublishedAt()).isNotNull();
        verify(worldModuleRepository).save(module);
        verify(worldRepository).save(world);
    }

    @Test
    void processQueueShouldMarkFailureWhenAiThrows() {
        World world = createWorld(8L, 654L, WorldStatus.GENERATING, 1);
        WorldGenerationJob job = createJob(world, "cosmos");
        WorldModule module = createModule(world, "cosmos", WorldModuleStatus.AWAITING_GENERATION,
                Map.of("cosmos_structure", "原始设定"));
        List<WorldModule> modules = List.of(module);

        when(jobRepository.fetchNextJobForUpdate()).thenReturn(Optional.of(job));
        when(worldModuleRepository.findByWorldId(8L)).thenReturn(modules);
        when(contextBuilder.buildModuleContext(world, module, modules)).thenReturn(Map.of("key", "value"));
        when(templateService.renderFinalTemplate(eq("cosmos"), anyMap(), eq(654L))).thenReturn("prompt");
        when(settingsService.getDecryptedApiKeyByUserId(654L)).thenReturn("api-key");
        when(aiService.generate("prompt", "api-key", null, null)).thenThrow(new IllegalStateException("AI 错误"));
        when(worldModuleRepository.findByWorldIdAndModuleKey(8L, "cosmos")).thenReturn(Optional.of(module));

        service.processQueue();

        assertThat(job.getStatus()).isEqualTo(WorldGenerationJobStatus.FAILED);
        assertThat(job.getFinishedAt()).isNotNull();
        assertThat(job.getLastError()).contains("AI 错误");
        assertThat(module.getStatus()).isEqualTo(WorldModuleStatus.FAILED);
        verify(worldModuleRepository).save(module);
    }

    @Test
    void retryModuleShouldResetStatusAndWorldState() {
        World world = createWorld(9L, 777L, WorldStatus.DRAFT, 2);
        WorldGenerationJob job = createJob(world, "cosmos");
        job.setStatus(WorldGenerationJobStatus.FAILED);
        job.setAttempts(1);
        job.setLastError("异常");
        job.setStartedAt(LocalDateTime.now());
        job.setFinishedAt(LocalDateTime.now());

        WorldModule module = createModule(world, "cosmos", WorldModuleStatus.FAILED,
                Map.of("cosmos_structure", "原始设定"));

        when(worldRepository.findByIdAndUserId(9L, 777L)).thenReturn(Optional.of(world));
        when(jobRepository.findByWorldIdAndModuleKey(9L, "cosmos")).thenReturn(Optional.of(job));
        when(worldModuleRepository.findByWorldIdAndModuleKey(9L, "cosmos")).thenReturn(Optional.of(module));

        service.retryModule(9L, "cosmos", 777L);

        assertThat(job.getStatus()).isEqualTo(WorldGenerationJobStatus.WAITING);
        assertThat(job.getLastError()).isNull();
        assertThat(job.getStartedAt()).isNull();
        assertThat(job.getFinishedAt()).isNull();
        assertThat(module.getStatus()).isEqualTo(WorldModuleStatus.AWAITING_GENERATION);
        assertThat(module.getLastEditedBy()).isEqualTo(777L);
        assertThat(module.getLastEditedAt()).isNotNull();
        assertThat(world.getStatus()).isEqualTo(WorldStatus.GENERATING);
        verify(jobRepository).save(job);
        verify(worldModuleRepository).save(module);
        verify(worldRepository).save(world);
    }

    private World createWorld(Long worldId, Long userId, WorldStatus status, int version) {
        World world = new World();
        world.setId(worldId);
        world.setStatus(status);
        world.setVersion(version);
        world.setPublishedAt(null);
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
        module.setLastEditedBy(world.getUser().getId());
        module.setLastEditedAt(LocalDateTime.now());
        return module;
    }

    private WorldGenerationJob createJob(World world, String moduleKey) {
        WorldGenerationJob job = new WorldGenerationJob();
        job.setWorld(world);
        job.setModuleKey(moduleKey);
        job.setSequence(1);
        job.setJobType(WorldGenerationJobType.MODULE_FINAL);
        job.setStatus(WorldGenerationJobStatus.WAITING);
        job.setAttempts(0);
        return job;
    }
}

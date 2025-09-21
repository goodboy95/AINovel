package com.example.ainovel.service.world;

import com.example.ainovel.dto.world.WorldGenerationStatusResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class WorldGenerationWorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorldGenerationWorkflowService.class);
    private static final Set<WorldGenerationJobStatus> INCOMPLETE_STATUSES = EnumSet.of(
            WorldGenerationJobStatus.WAITING,
            WorldGenerationJobStatus.RUNNING
    );

    private final WorldGenerationJobRepository jobRepository;
    private final WorldRepository worldRepository;
    private final WorldModuleRepository worldModuleRepository;
    private final WorldPromptTemplateService templateService;
    private final WorldPromptContextBuilder contextBuilder;
    private final WorldModuleDefinitionRegistry definitionRegistry;
    private final AiService aiService;
    private final SettingsService settingsService;
    private final long maxErrorLength;

    public WorldGenerationWorkflowService(WorldGenerationJobRepository jobRepository,
                                          WorldRepository worldRepository,
                                          WorldModuleRepository worldModuleRepository,
                                          WorldPromptTemplateService templateService,
                                          WorldPromptContextBuilder contextBuilder,
                                          WorldModuleDefinitionRegistry definitionRegistry,
                                          AiService aiService,
                                          SettingsService settingsService,
                                          @Value("${world.generation.max-error-length:400}") long maxErrorLength) {
        this.jobRepository = jobRepository;
        this.worldRepository = worldRepository;
        this.worldModuleRepository = worldModuleRepository;
        this.templateService = templateService;
        this.contextBuilder = contextBuilder;
        this.definitionRegistry = definitionRegistry;
        this.aiService = aiService;
        this.settingsService = settingsService;
        this.maxErrorLength = maxErrorLength;
    }

    @Transactional
    public void initializeJobs(World world, List<WorldModule> modulesToGenerate) {
        jobRepository.deleteByWorldId(world.getId());
        if (CollectionUtils.isEmpty(modulesToGenerate)) {
            return;
        }
        int sequence = 1;
        List<WorldGenerationJob> jobs = new ArrayList<>();
        for (WorldModule module : modulesToGenerate) {
            WorldGenerationJob job = new WorldGenerationJob();
            job.setWorld(world);
            job.setModuleKey(module.getModuleKey());
            job.setJobType(WorldGenerationJobType.MODULE_FINAL);
            job.setStatus(WorldGenerationJobStatus.WAITING);
            job.setSequence(sequence++);
            job.setAttempts(0);
            jobs.add(job);
        }
        jobRepository.saveAll(jobs);
    }

    @Transactional(readOnly = true)
    public WorldGenerationStatusResponse getStatus(Long worldId, Long userId) {
        World world = worldRepository.findByIdAndUserId(worldId, userId)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "世界不存在或无权访问"));
        List<WorldGenerationJob> jobs = jobRepository.findByWorldIdOrderBySequenceAsc(worldId);
        List<WorldGenerationStatusResponse.JobStatus> queue = new ArrayList<>();
        for (WorldGenerationJob job : jobs) {
            WorldGenerationStatusResponse.JobStatus jobStatus = new WorldGenerationStatusResponse.JobStatus()
                    .setModuleKey(job.getModuleKey())
                    .setModuleLabel(definitionRegistry.resolveLabel(job.getModuleKey()))
                    .setStatus(job.getStatus())
                    .setAttempts(job.getAttempts())
                    .setStartedAt(job.getStartedAt())
                    .setFinishedAt(job.getFinishedAt())
                    .setError(job.getLastError());
            queue.add(jobStatus);
        }
        return new WorldGenerationStatusResponse()
                .setWorldId(world.getId())
                .setStatus(world.getStatus())
                .setVersion(world.getVersion())
                .setQueue(queue);
    }

    @Transactional
    public void retryModule(Long worldId, String moduleKey, Long userId) {
        World world = worldRepository.findByIdAndUserId(worldId, userId)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "世界不存在或无权访问"));
        WorldGenerationJob job = jobRepository.findByWorldIdAndModuleKey(worldId, moduleKey)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "未找到对应的生成任务"));
        if (job.getStatus() != WorldGenerationJobStatus.FAILED) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "仅允许重试失败的任务");
        }
        job.setStatus(WorldGenerationJobStatus.WAITING);
        job.setLastError(null);
        job.setStartedAt(null);
        job.setFinishedAt(null);
        jobRepository.save(job);

        WorldModule module = worldModuleRepository.findByWorldIdAndModuleKey(worldId, moduleKey)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "模块不存在"));
        module.setStatus(WorldModuleStatus.AWAITING_GENERATION);
        module.setLastEditedAt(LocalDateTime.now());
        module.setLastEditedBy(userId);
        worldModuleRepository.save(module);
        if (world.getStatus() != WorldStatus.GENERATING) {
            world.setStatus(WorldStatus.GENERATING);
            worldRepository.save(world);
        }
    }

    @Scheduled(fixedDelayString = "${world.generation.poll-interval-ms:3000}")
    @Transactional
    public void processQueue() {
        Optional<WorldGenerationJob> jobOpt = jobRepository.fetchNextJobForUpdate();
        if (jobOpt.isEmpty()) {
            return;
        }
        WorldGenerationJob job = jobOpt.get();
        World world = job.getWorld();
        if (world == null) {
            log.warn("Job {} missing world reference", job.getId());
            job.setStatus(WorldGenerationJobStatus.FAILED);
            job.setLastError("缺少世界信息");
            job.setFinishedAt(LocalDateTime.now());
            jobRepository.save(job);
            return;
        }
        try {
            executeJob(job, world);
        } catch (Exception ex) {
            log.error("Failed to process world generation job {}", job.getId(), ex);
            job.setStatus(WorldGenerationJobStatus.FAILED);
            job.setFinishedAt(LocalDateTime.now());
            job.setLastError(truncateError(ex.getMessage()));
            jobRepository.save(job);
            WorldModule module = worldModuleRepository.findByWorldIdAndModuleKey(world.getId(), job.getModuleKey())
                    .orElse(null);
            if (module != null) {
                module.setStatus(WorldModuleStatus.FAILED);
                worldModuleRepository.save(module);
            }
        }
    }

    private void executeJob(WorldGenerationJob job, World world) {
        job.setStatus(WorldGenerationJobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        job.setAttempts((job.getAttempts() == null ? 0 : job.getAttempts()) + 1);
        job.setLastError(null);
        jobRepository.save(job);

        List<WorldModule> modules = worldModuleRepository.findByWorldId(world.getId());
        WorldModule module = modules.stream()
                .filter(m -> job.getModuleKey().equals(m.getModuleKey()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "模块不存在"));

        Long ownerId = world.getUser() != null ? world.getUser().getId()
                : worldRepository.findById(world.getId())
                .map(entity -> entity.getUser() != null ? entity.getUser().getId() : null)
                .orElse(null);
        if (ownerId == null) {
            throw new IllegalStateException("无法确定世界所属用户");
        }
        Map<String, Object> context = contextBuilder.buildModuleContext(world, module, modules);
        String prompt = templateService.renderFinalTemplate(job.getModuleKey(), context, ownerId);
        AiCredentials credentials = resolveCredentials(ownerId);
        String result = aiService.generate(prompt, credentials.apiKey(), credentials.baseUrl(), credentials.model());
        if (!StringUtils.hasText(result)) {
            throw new IllegalStateException("AI 返回空内容");
        }
        String trimmed = result.trim();
        module.setFullContent(trimmed);
        module.setFullContentUpdatedAt(LocalDateTime.now());
        module.setStatus(WorldModuleStatus.COMPLETED);
        worldModuleRepository.save(module);

        job.setStatus(WorldGenerationJobStatus.SUCCEEDED);
        job.setFinishedAt(LocalDateTime.now());
        jobRepository.save(job);

        finalizeWorldIfNeeded(world);
    }

    private void finalizeWorldIfNeeded(World world) {
        long remaining = jobRepository.countByWorldIdAndStatusIn(world.getId(), INCOMPLETE_STATUSES);
        if (remaining > 0) {
            return;
        }
        world.setStatus(WorldStatus.ACTIVE);
        world.setVersion((world.getVersion() == null ? 0 : world.getVersion()) + 1);
        world.setPublishedAt(LocalDateTime.now());
        worldRepository.save(world);
    }

    private String truncateError(String message) {
        if (!StringUtils.hasText(message)) {
            return "任务执行失败";
        }
        if (message.length() <= maxErrorLength) {
            return message;
        }
        return message.substring(0, (int) maxErrorLength) + "…";
    }

    private AiCredentials resolveCredentials(Long userId) {
        try {
            String apiKey = settingsService.getDecryptedApiKeyByUserId(userId);
            String baseUrl = null;
            String model = null;
            try {
                baseUrl = settingsService.getBaseUrlByUserId(userId);
            } catch (Exception ignored) {
            }
            try {
                model = settingsService.getModelNameByUserId(userId);
            } catch (Exception ignored) {
            }
            return new AiCredentials(apiKey, baseUrl, model);
        } catch (Exception e) {
            throw new IllegalStateException("缺少 AI 凭证", e);
        }
    }

    private record AiCredentials(String apiKey, String baseUrl, String model) {
    }
}

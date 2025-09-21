package com.example.ainovel.service.world;

import com.example.ainovel.dto.world.WorldFieldRefineRequest;
import com.example.ainovel.exception.ResourceNotFoundException;
import com.example.ainovel.model.world.World;
import com.example.ainovel.model.world.WorldModule;
import com.example.ainovel.model.world.WorldStatus;
import com.example.ainovel.repository.WorldModuleRepository;
import com.example.ainovel.repository.WorldRepository;
import com.example.ainovel.service.AiService;
import com.example.ainovel.service.SettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class WorldFieldRefineService {

    private final WorldRepository worldRepository;
    private final WorldModuleRepository worldModuleRepository;
    private final WorldPromptTemplateService templateService;
    private final WorldPromptContextBuilder contextBuilder;
    private final AiService aiService;
    private final SettingsService settingsService;

    public WorldFieldRefineService(WorldRepository worldRepository,
                                   WorldModuleRepository worldModuleRepository,
                                   WorldPromptTemplateService templateService,
                                   WorldPromptContextBuilder contextBuilder,
                                   AiService aiService,
                                   SettingsService settingsService) {
        this.worldRepository = worldRepository;
        this.worldModuleRepository = worldModuleRepository;
        this.templateService = templateService;
        this.contextBuilder = contextBuilder;
        this.aiService = aiService;
        this.settingsService = settingsService;
    }

    public String refineField(Long worldId,
                              String moduleKey,
                              String fieldKey,
                              Long userId,
                              WorldFieldRefineRequest request) {
        World world = loadWorld(worldId, userId);
        if (world.getStatus() == WorldStatus.GENERATING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "世界正在生成中，无法调用字段优化");
        }
        WorldModule module = worldModuleRepository.findByWorldIdAndModuleKey(worldId, moduleKey)
                .orElseThrow(() -> new ResourceNotFoundException("指定模块不存在"));
        List<WorldModule> modules = worldModuleRepository.findByWorldId(worldId);
        String originalText = request == null ? null : request.getText();
        if (!StringUtils.hasText(originalText)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "待优化文本不能为空");
        }
        String focusNote = templateService.resolveFocusNote(moduleKey, fieldKey);
        Map<String, Object> context = contextBuilder.buildFieldRefineContext(world, module, fieldKey, focusNote,
                originalText, request.getInstruction(), modules);
        String prompt = templateService.renderFieldRefineTemplate(context);
        AiCredentials credentials = resolveCredentials(userId);
        String refined = aiService.generate(prompt, credentials.apiKey(), credentials.baseUrl(), credentials.model());
        if (!StringUtils.hasText(refined)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 返回内容为空");
        }
        return refined.trim();
    }

    private World loadWorld(Long worldId, Long userId) {
        return worldRepository.findByIdAndUserId(worldId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("世界不存在或无权访问"));
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
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "请先在用户设置中配置有效的 AI Key", e);
        }
    }

    private record AiCredentials(String apiKey, String baseUrl, String model) {
    }
}

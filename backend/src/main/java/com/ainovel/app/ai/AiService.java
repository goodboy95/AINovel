package com.ainovel.app.ai;

import com.ainovel.app.ai.dto.*;
import com.ainovel.app.ai.model.ModelConfigEntity;
import com.ainovel.app.ai.repo.ModelConfigRepository;
import com.ainovel.app.economy.EconomyService;
import com.ainovel.app.settings.SettingsService;
import com.ainovel.app.settings.model.SystemSettings;
import com.ainovel.app.settings.repo.SystemSettingsRepository;
import com.ainovel.app.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AiService {
    @Autowired
    private ModelConfigRepository modelConfigRepository;
    @Autowired
    private SystemSettingsRepository systemSettingsRepository;
    @Autowired
    private SettingsService settingsService;
    @Autowired
    private EconomyService economyService;
    @Autowired
    private OpenAiCompatClient openAiCompatClient;
    @Autowired
    private ObjectMapper objectMapper;

    public List<AiModelDto> listModels() {
        List<ModelConfigEntity> enabled = modelConfigRepository.findByEnabledTrueOrderByDisplayNameAsc();
        if (enabled.isEmpty()) {
            enabled = modelConfigRepository.findAll();
        }
        return enabled.stream().map(this::toDto).toList();
    }

    public AiChatResponse chat(User user, AiChatRequest request) {
        SystemSettings settings = systemSettingsRepository.findByUser(user).orElseGet(() -> {
            settingsService.getSettings(user);
            return systemSettingsRepository.findByUser(user).orElseThrow();
        });
        var global = settingsService.getGlobalSettings();

        ModelConfigEntity model = resolveModel(request.modelId()).orElse(null);
        String modelName = model != null ? model.getName()
                : (settings.getModelName() != null && !settings.getModelName().isBlank() ? settings.getModelName() : global.getLlmModelName());
        double inMult = model != null ? model.getInputMultiplier() : 1.0;
        double outMult = model != null ? model.getOutputMultiplier() : 1.0;

        String baseUrl = settings.getBaseUrl() != null && !settings.getBaseUrl().isBlank() ? settings.getBaseUrl() : global.getLlmBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.openai.com/v1";
        String apiKey = settings.getApiKeyEncrypted();
        if (apiKey == null || apiKey.isBlank()) apiKey = global.getLlmApiKeyEncrypted();
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("未配置 API Key，请联系管理员在后台配置，或前往设置页配置");
        }
        if (modelName == null || modelName.isBlank()) modelName = "gpt-4o";

        List<Map<String, Object>> messages = new ArrayList<>();
        if (request.context() != null) {
            messages.add(Map.of(
                    "role", "system",
                    "content", "你是一个专业的中文小说写作助手。以下是当前上下文(JSON)：\n" + safeJson(request.context())
            ));
        } else {
            messages.add(Map.of("role", "system", "content", "你是一个专业的中文小说写作助手。"));
        }
        if (request.messages() != null) {
            for (AiChatRequest.Message m : request.messages()) {
                if (m == null) continue;
                if (m.role() == null || m.content() == null) continue;
                messages.add(Map.of("role", m.role(), "content", m.content()));
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", modelName);
        payload.put("messages", messages);
        payload.put("temperature", 0.7);

        OpenAiCompatClient.ChatResult result = openAiCompatClient.chatCompletions(baseUrl, apiKey, payload);
        int promptTokens = result.promptTokens() != null ? result.promptTokens() : estimateTokens(messages);
        int completionTokens = result.completionTokens() != null ? result.completionTokens() : Math.max(1, result.content().length() / 4);

        double cost = (promptTokens * inMult + completionTokens * outMult) / 100000.0;
        cost = round4(cost);
        economyService.deduct(user, cost, "Model: " + modelName + ", In: " + promptTokens + ", Out: " + completionTokens);

        return new AiChatResponse("assistant", result.content(), new AiUsageDto(promptTokens, completionTokens, cost), user.getCredits());
    }

    public AiRefineResponse refine(User user, AiRefineRequest request) {
        String instruction = request.instruction() == null ? "" : request.instruction();
        AiChatRequest chatRequest = new AiChatRequest(
                List.of(new AiChatRequest.Message("user", "请根据以下指令润色文本。\n\n指令:\n" + instruction + "\n\n文本:\n" + request.text())),
                request.modelId(),
                null
        );
        AiChatResponse resp = chat(user, chatRequest);
        return new AiRefineResponse(resp.content(), resp.usage(), resp.remainingCredits());
    }

    private Optional<ModelConfigEntity> resolveModel(String modelId) {
        if (modelId == null || modelId.isBlank()) return Optional.empty();
        try {
            return modelConfigRepository.findById(UUID.fromString(modelId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private AiModelDto toDto(ModelConfigEntity entity) {
        return new AiModelDto(entity.getId(), entity.getName(), entity.getDisplayName(), entity.getInputMultiplier(), entity.getOutputMultiplier(), entity.getPoolId(), entity.isEnabled());
    }

    private String safeJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    private int estimateTokens(List<Map<String, Object>> messages) {
        int chars = 0;
        for (Map<String, Object> m : messages) {
            Object c = m.get("content");
            if (c != null) chars += c.toString().length();
        }
        return Math.max(1, chars / 4);
    }

    private double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}

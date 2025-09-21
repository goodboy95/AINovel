package com.example.ainovel.service.world;

import com.example.ainovel.model.User;
import com.example.ainovel.model.world.World;
import com.example.ainovel.model.world.WorldModule;
import com.example.ainovel.model.world.WorldModuleStatus;
import com.example.ainovel.model.world.WorldStatus;
import com.example.ainovel.repository.WorldModuleRepository;
import com.example.ainovel.repository.WorldRepository;
import com.example.ainovel.service.AiService;
import com.example.ainovel.service.SettingsService;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinition;
import com.example.ainovel.worldbuilding.definition.WorldModuleDefinitionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldModuleGenerationServiceTest {

    @Mock
    private WorldRepository worldRepository;
    @Mock
    private WorldModuleRepository worldModuleRepository;
    @Mock
    private WorldModuleDefinitionRegistry definitionRegistry;
    @Mock
    private WorldPromptTemplateService templateService;
    @Mock
    private WorldPromptContextBuilder contextBuilder;
    @Mock
    private AiService aiService;
    @Mock
    private SettingsService settingsService;

    private WorldModuleGenerationService service;

    @BeforeEach
    void setUp() {
        service = new WorldModuleGenerationService(worldRepository, worldModuleRepository, definitionRegistry,
                templateService, contextBuilder, aiService, settingsService, new ObjectMapper());
    }

    @Test
    void generateModuleShouldMergeFieldsAndUpdateStatus() {
        World world = new World();
        world.setId(1L);
        world.setStatus(WorldStatus.DRAFT);
        world.setVersion(0);
        User owner = new User();
        owner.setId(99L);
        world.setUser(owner);

        WorldModule module = new WorldModule();
        module.setId(10L);
        module.setModuleKey("cosmos");
        module.setWorld(world);
        module.setStatus(WorldModuleStatus.EMPTY);

        WorldModuleDefinition.FieldDefinition field1 = new WorldModuleDefinition.FieldDefinition(
                "cosmos_structure", "宇宙结构与尺度", true, 10, 0);
        WorldModuleDefinition.FieldDefinition field2 = new WorldModuleDefinition.FieldDefinition(
                "space_time", "时间与空间规则", true, 10, 0);
        WorldModuleDefinition definition = new WorldModuleDefinition("cosmos", "宇宙观与法则", List.of(field1, field2), 1);

        when(worldRepository.findByIdAndUserId(1L, 99L)).thenReturn(Optional.of(world));
        when(worldModuleRepository.findByWorldIdAndModuleKey(1L, "cosmos")).thenReturn(Optional.of(module));
        when(worldModuleRepository.findByWorldId(1L)).thenReturn(List.of(module));
        when(templateService.renderDraftTemplate(eq("cosmos"), any(), eq(99L))).thenReturn("prompt");
        when(definitionRegistry.requireModule("cosmos")).thenReturn(definition);
        when(aiService.generateJson(eq("prompt"), eq("test-key"), any(), any())).thenReturn("{" +
                "\"cosmos_structure\":\"广袤的浮空大陆\"," +
                "\"space_time\":\"昼夜交错的时空循环\"}");
        when(settingsService.getDecryptedApiKeyByUserId(99L)).thenReturn("test-key");
        doThrow(new IllegalStateException("no base"))
                .when(settingsService).getBaseUrlByUserId(99L);
        doThrow(new IllegalStateException("no model"))
                .when(settingsService).getModelNameByUserId(99L);

        WorldModule updated = service.generateModule(1L, "cosmos", 99L);

        assertThat(updated.getFields()).containsEntry("cosmos_structure", "广袤的浮空大陆");
        assertThat(updated.getFields()).containsEntry("space_time", "昼夜交错的时空循环");
        assertThat(updated.getStatus()).isEqualTo(WorldModuleStatus.READY);
        assertThat(updated.getContentHash()).isNotNull();
        verify(worldModuleRepository, times(1)).save(module);
        verify(worldRepository, times(1)).save(world);
    }

    @Test
    void generateModuleShouldFailWhenMissingApiKey() {
        World world = new World();
        world.setId(2L);
        world.setStatus(WorldStatus.DRAFT);
        User owner = new User();
        owner.setId(50L);
        world.setUser(owner);

        WorldModule module = new WorldModule();
        module.setModuleKey("cosmos");
        module.setWorld(world);

        when(worldRepository.findByIdAndUserId(2L, 50L)).thenReturn(Optional.of(world));
        when(worldModuleRepository.findByWorldIdAndModuleKey(2L, "cosmos")).thenReturn(Optional.of(module));
        when(worldModuleRepository.findByWorldId(2L)).thenReturn(List.of(module));
        when(templateService.renderDraftTemplate(eq("cosmos"), any(), eq(50L))).thenReturn("prompt");

        when(settingsService.getDecryptedApiKeyByUserId(50L))
                .thenThrow(new IllegalStateException("missing"));

        assertThatThrownBy(() -> service.generateModule(2L, "cosmos", 50L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.PRECONDITION_FAILED);
    }
}

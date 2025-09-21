package com.example.ainovel.service.world;

import com.example.ainovel.dto.world.WorldFieldRefineRequest;
import com.example.ainovel.model.User;
import com.example.ainovel.model.world.World;
import com.example.ainovel.model.world.WorldModule;
import com.example.ainovel.model.world.WorldStatus;
import com.example.ainovel.repository.WorldModuleRepository;
import com.example.ainovel.repository.WorldRepository;
import com.example.ainovel.service.AiService;
import com.example.ainovel.service.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorldFieldRefineServiceTest {

    @Mock
    private WorldRepository worldRepository;
    @Mock
    private WorldModuleRepository worldModuleRepository;
    @Mock
    private WorldPromptTemplateService templateService;
    @Mock
    private WorldPromptContextBuilder contextBuilder;
    @Mock
    private AiService aiService;
    @Mock
    private SettingsService settingsService;

    private WorldFieldRefineService service;

    @BeforeEach
    void setUp() {
        service = new WorldFieldRefineService(worldRepository, worldModuleRepository, templateService,
                contextBuilder, aiService, settingsService);
    }

    @Test
    void refineFieldShouldUseFocusNoteAndReturnTrimmedResult() {
        World world = new World();
        world.setId(9L);
        world.setStatus(WorldStatus.DRAFT);
        User owner = new User();
        owner.setId(88L);
        world.setUser(owner);

        WorldModule module = new WorldModule();
        module.setId(20L);
        module.setWorld(world);
        module.setModuleKey("cosmos");

        when(worldRepository.findByIdAndUserId(9L, 88L)).thenReturn(Optional.of(world));
        when(worldModuleRepository.findByWorldIdAndModuleKey(9L, "cosmos")).thenReturn(Optional.of(module));
        when(worldModuleRepository.findByWorldId(9L)).thenReturn(List.of(module));
        when(templateService.resolveFocusNote("cosmos", "cosmos_structure")).thenReturn("聚焦宇宙结构");
        Map<String, Object> context = Map.of("key", "value");
        when(contextBuilder.buildFieldRefineContext(world, module, "cosmos_structure", "聚焦宇宙结构",
                "原始文本", "请更具象", List.of(module))).thenReturn(context);
        when(templateService.renderFieldRefineTemplate(context)).thenReturn("prompt");
        when(settingsService.getDecryptedApiKeyByUserId(88L)).thenReturn("api-key");
        when(aiService.generate("prompt", "api-key", null, null)).thenReturn("  精炼后的文本  ");

        WorldFieldRefineRequest request = new WorldFieldRefineRequest()
                .setText("原始文本")
                .setInstruction("请更具象");

        String result = service.refineField(9L, "cosmos", "cosmos_structure", 88L, request);

        assertThat(result).isEqualTo("精炼后的文本");
        verify(templateService).resolveFocusNote("cosmos", "cosmos_structure");
        verify(contextBuilder).buildFieldRefineContext(eq(world), eq(module), eq("cosmos_structure"),
                eq("聚焦宇宙结构"), eq("原始文本"), eq("请更具象"), eq(List.of(module)));
    }

    @Test
    void refineFieldShouldValidateInputText() {
        World world = new World();
        world.setId(3L);
        world.setStatus(WorldStatus.DRAFT);
        User owner = new User();
        owner.setId(42L);
        world.setUser(owner);

        WorldModule module = new WorldModule();
        module.setWorld(world);
        module.setModuleKey("cosmos");

        when(worldRepository.findByIdAndUserId(3L, 42L)).thenReturn(Optional.of(world));
        when(worldModuleRepository.findByWorldIdAndModuleKey(3L, "cosmos")).thenReturn(Optional.of(module));
        when(worldModuleRepository.findByWorldId(3L)).thenReturn(List.of(module));

        WorldFieldRefineRequest request = new WorldFieldRefineRequest().setText("   ");

        assertThatThrownBy(() -> service.refineField(3L, "cosmos", "cosmos_structure", 42L, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

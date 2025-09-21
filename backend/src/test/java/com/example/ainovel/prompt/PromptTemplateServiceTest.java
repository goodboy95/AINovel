package com.example.ainovel.prompt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.ainovel.dto.prompt.PromptTemplatesUpdateRequest;
import com.example.ainovel.dto.prompt.RefinePromptTemplatesUpdateRequest;
import com.example.ainovel.model.prompt.PromptSettings;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceTest {

    @Mock
    private PromptTemplateRepository promptTemplateRepository;

    private PromptTemplateService promptTemplateService;

    @BeforeEach
    void setUp() {
        PromptDefaults defaults = new PromptDefaults();
        defaults.setStoryCreation("default story");
        defaults.setOutlineChapter("default outline");
        defaults.setManuscriptSection("default manuscript");
        RefinePromptDefaults refineDefaults = new RefinePromptDefaults();
        refineDefaults.setWithInstruction("default with");
        refineDefaults.setWithoutInstruction("default without");
        defaults.setRefine(refineDefaults);

        promptTemplateService = new PromptTemplateService(
                promptTemplateRepository,
                defaults,
                new TemplateEngine(new ObjectMapper()),
                new PromptTemplateMetadataProvider());
    }

    @Test
    void saveTemplatesNormalizesLineEndings() {
        when(promptTemplateRepository.findByUserId(1L)).thenReturn(Optional.empty());

        PromptTemplatesUpdateRequest request = new PromptTemplatesUpdateRequest()
                .setStoryCreation("line1\r\nline2\rline3\nline4")
                .setOutlineChapter("outline\r\nsecond")
                .setManuscriptSection("section\ronly")
                .setRefine(new RefinePromptTemplatesUpdateRequest()
                        .setWithInstruction("with\r\nvalue")
                        .setWithoutInstruction("without\rvalue"));

        promptTemplateService.saveTemplates(1L, request);

        ArgumentCaptor<PromptSettings> settingsCaptor = ArgumentCaptor.forClass(PromptSettings.class);
        verify(promptTemplateRepository).save(eq(1L), settingsCaptor.capture());

        PromptSettings savedSettings = settingsCaptor.getValue();
        assertThat(savedSettings.getStoryCreation()).isEqualTo("line1\nline2\nline3\nline4");
        assertThat(savedSettings.getOutlineChapter()).isEqualTo("outline\nsecond");
        assertThat(savedSettings.getManuscriptSection()).isEqualTo("section\nonly");
        assertThat(savedSettings.getRefine()).isNotNull();
        assertThat(savedSettings.getRefine().getWithInstruction()).isEqualTo("with\nvalue");
        assertThat(savedSettings.getRefine().getWithoutInstruction()).isEqualTo("without\nvalue");
    }
}

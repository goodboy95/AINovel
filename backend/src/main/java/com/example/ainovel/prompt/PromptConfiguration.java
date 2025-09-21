package com.example.ainovel.prompt;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

@Configuration
public class PromptConfiguration {

    @Bean
    public PromptDefaults promptDefaults() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        ClassPathResource resource = new ClassPathResource("prompts/default-prompts.yaml");
        try (InputStream inputStream = resource.getInputStream()) {
            PromptDefaults defaults = mapper.readValue(inputStream, PromptDefaults.class);
            defaults.validate();
            return defaults;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load default prompt templates", e);
        }
    }
}

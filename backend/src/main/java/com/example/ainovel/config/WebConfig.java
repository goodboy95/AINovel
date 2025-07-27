package com.example.ainovel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configures web-related beans and settings for the application.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configures resource handlers to serve static files from the classpath.
     * @param registry The resource handler registry.
     */
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    /**
     * Configures view controllers to forward all non-API, non-file requests to the single-page application's entry point.
     * This is essential for client-side routing in SPAs.
     * @param registry The view controller registry.
     */
   @Override
   public void addViewControllers(@NonNull ViewControllerRegistry registry) {
       registry.addViewController("/{path:[^\\.]*}")
               .setViewName("forward:/index.html");
       registry.addViewController("/**/{path:[^\\.]*}")
               .setViewName("forward:/index.html");
   }

    /**
     * Creates a customized ObjectMapper bean to handle JSON serialization and deserialization.
     * This configuration includes support for Java 8 Date and Time API (JSR-310).
     * @param builder The builder used to create the ObjectMapper.
     * @return A configured ObjectMapper instance.
     */
    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new Hibernate6Module());
        return objectMapper;
    }
}

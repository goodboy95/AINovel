package com.example.ainovel.service.material;

import java.util.Optional;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.ainovel.dto.material.StructuredMaterial;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 调用 LLM 完成素材结构化解析。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialExtractionService {

    private static final String SYSTEM_PROMPT = """
        你是一名小说素材知识库的整理助手。请阅读用户提供的素材正文，
        按照 JSON Schema 输出结构化信息。缺失的字段使用 null。仅输出 JSON，
        不要添加多余文字。
        {
          "type": "string (character/place/item/event/lore/style/fragment/idea)",
          "title": "string",
          "aliases": ["string"],
          "summary": "string",
          "tags": ["string"],
          "character": {
            "name": "string",
            "age": "string",
            "traits": ["string"],
            "relations": [{"to": "string", "relation": "string"}],
            "backstory": "string"
          },
          "place": {"name": "string", "era": "string", "geo": "string"},
          "event": {"time": "string", "participants": ["string"], "outcome": "string"}
        }
        """;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public Optional<StructuredMaterial> extract(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return Optional.empty();
        }
        try {
            String content = chatClient
                .prompt()
                .system(SYSTEM_PROMPT)
                .user(rawText)

                .call()
                .content();
            if (!StringUtils.hasText(content)) {
                return Optional.empty();
            }
            StructuredMaterial structured = objectMapper.readValue(content, StructuredMaterial.class);
            return Optional.ofNullable(structured);
        } catch (Exception ex) {
            log.warn("调用 LLM 解析素材失败：{}", ex.getMessage());
            return Optional.empty();
        }
    }
}

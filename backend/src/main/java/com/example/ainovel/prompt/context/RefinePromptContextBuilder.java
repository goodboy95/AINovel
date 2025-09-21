package com.example.ainovel.prompt.context;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.ainovel.dto.RefineRequest;

@Component
public class RefinePromptContextBuilder {

    public Map<String, Object> build(RefineRequest request) {
        Map<String, Object> root = new LinkedHashMap<>();
        String contextType = safeTrim(request.getContextType());
        String note = contextType == null ? "" : String.format("这是一个关于“%s”的文本。\n", contextType);

        root.put("text", request.getText());
        root.put("instruction", request.getInstruction());
        root.put("contextType", contextType == null ? "" : contextType);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("note", note);
        root.put("context", context);

        root.put("request", Map.of("raw", request));
        return root;
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

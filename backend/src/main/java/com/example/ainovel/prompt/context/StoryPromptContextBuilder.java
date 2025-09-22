package com.example.ainovel.prompt.context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.ainovel.dto.ConceptionRequest;
import com.example.ainovel.service.world.WorkspaceWorldContext;

@Component
public class StoryPromptContextBuilder {

    public Map<String, Object> build(ConceptionRequest request, WorkspaceWorldContext worldContext) {
        Map<String, Object> root = new LinkedHashMap<>();
        String idea = safeTrim(request.getIdea());
        String genre = safeTrim(request.getGenre());
        String tone = safeTrim(request.getTone());

        List<String> tags = request.getTags() == null ? List.of() : request.getTags().stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new))
                .stream()
                .collect(Collectors.toList());

        StringBuilder summaryBuilder = new StringBuilder();
        Map<String, String> lines = new LinkedHashMap<>();

        if (idea != null) {
            String line = "核心想法：" + idea + "\n";
            summaryBuilder.append(line);
            lines.put("idea", line);
        } else {
            lines.put("idea", "");
        }
        if (genre != null) {
            String line = "类型：" + genre + "\n";
            summaryBuilder.append(line);
            lines.put("genre", line);
        } else {
            lines.put("genre", "");
        }
        if (tone != null) {
            String line = "基调：" + tone + "\n";
            summaryBuilder.append(line);
            lines.put("tone", line);
        } else {
            lines.put("tone", "");
        }
        if (!tags.isEmpty()) {
            String line = "标签：" + String.join("，", tags) + "\n";
            summaryBuilder.append(line);
            lines.put("tags", line);
        } else {
            lines.put("tags", "");
        }

        if (summaryBuilder.length() == 0) {
            summaryBuilder.append("核心想法：请根据用户提供的信息生成故事。\n");
        }
        summaryBuilder.append("\n");

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("summary", summaryBuilder.toString());
        context.put("lines", lines);

        root.put("idea", idea == null ? "" : idea);
        root.put("type", genre == null ? "" : genre);
        root.put("genre", genre == null ? "" : genre);
        root.put("tone", tone == null ? "" : tone);
        root.put("tags", tags);
        root.put("tag", tags);
        root.put("context", context);
        root.put("request", Map.of("raw", request));
        Map<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("world", worldContext);
        root.put("workspace", workspace);
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

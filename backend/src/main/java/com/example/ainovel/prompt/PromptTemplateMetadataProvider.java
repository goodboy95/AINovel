package com.example.ainovel.prompt;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.ainovel.dto.prompt.PromptFunctionMetadataDto;
import com.example.ainovel.dto.prompt.PromptTemplateMetadataResponse;
import com.example.ainovel.dto.prompt.PromptTypeMetadataDto;
import com.example.ainovel.dto.prompt.PromptVariableMetadataDto;

@Component
public class PromptTemplateMetadataProvider {

    public PromptTemplateMetadataResponse getMetadata() {
        return new PromptTemplateMetadataResponse()
                .setTemplates(List.of(
                        new PromptTypeMetadataDto(
                                "storyCreation",
                                "故事构思",
                                List.of(
                                        new PromptVariableMetadataDto("idea", "string", "用户填写的核心想法。"),
                                        new PromptVariableMetadataDto("type", "string", "`genre` 的别名，便于在模板中书写。"),
                                        new PromptVariableMetadataDto("genre", "string", "故事类型。"),
                                        new PromptVariableMetadataDto("tone", "string", "故事基调。"),
                                        new PromptVariableMetadataDto("tags", "string[]", "标签数组，可搭配 `[index]` 或 `[*]` 使用。"),
                                        new PromptVariableMetadataDto("tag", "string[]", "`tags` 的别名，作用相同。"),
                                        new PromptVariableMetadataDto("context.summary", "string", "自动汇总的上下文块，包含想法、类型、基调与标签。"),
                                        new PromptVariableMetadataDto("context.lines.idea", "string", "当提供想法时为 `核心想法：...\\n`，否则为空。"),
                                        new PromptVariableMetadataDto("context.lines.genre", "string", "类型对应的单行提示文本。"),
                                        new PromptVariableMetadataDto("context.lines.tone", "string", "基调对应的单行提示文本。"),
                                        new PromptVariableMetadataDto("context.lines.tags", "string", "标签对应的单行提示文本。"),
                                        new PromptVariableMetadataDto("request.raw", "object", "原始的 `ConceptionRequest` 对象。")
                                )
                        ),
                        new PromptTypeMetadataDto(
                                "outlineChapter",
                                "大纲章节",
                                List.of(
                                        new PromptVariableMetadataDto("story.title", "string", "故事标题。"),
                                        new PromptVariableMetadataDto("story.synopsis", "string", "故事简介。"),
                                        new PromptVariableMetadataDto("story.genre", "string", "故事类型。"),
                                        new PromptVariableMetadataDto("story.tone", "string", "故事基调。"),
                                        new PromptVariableMetadataDto("story.storyArc", "string", "长线剧情走向。"),
                                        new PromptVariableMetadataDto("outline.title", "string", "大纲名称。"),
                                        new PromptVariableMetadataDto("outline.pointOfView", "string", "叙事视角，可为空。"),
                                        new PromptVariableMetadataDto("chapter.number", "number", "当前章节编号。"),
                                        new PromptVariableMetadataDto("chapter.sectionsPerChapter", "number", "预设节数。"),
                                        new PromptVariableMetadataDto("chapter.wordsPerSection", "number", "预估每节字数。"),
                                        new PromptVariableMetadataDto("chapter.previousSynopsis", "string", "上一章梗概，首章为“无，这是第一章。”"),
                                        new PromptVariableMetadataDto("characters.list", "object[]", "故事主要角色列表（含简介/详情/关系）。"),
                                        new PromptVariableMetadataDto("characters.summary", "string", "预格式化的角色条目文本。"),
                                        new PromptVariableMetadataDto("characters.names", "string[]", "角色姓名数组。"),
                                        new PromptVariableMetadataDto("request.raw", "object", "原始的 `GenerateChapterRequest` 对象。")
                                )
                        ),
                        new PromptTypeMetadataDto(
                                "manuscriptSection",
                                "正文创作",
                                List.of(
                                        new PromptVariableMetadataDto("story.title", "string", "故事标题。"),
                                        new PromptVariableMetadataDto("story.genre", "string", "故事类型。"),
                                        new PromptVariableMetadataDto("story.tone", "string", "故事基调。"),
                                        new PromptVariableMetadataDto("story.synopsis", "string", "故事简介。"),
                                        new PromptVariableMetadataDto("outline.pointOfView", "string", "叙事视角，默认为第三人称有限视角。"),
                                        new PromptVariableMetadataDto("characters.all", "object[]", "全部角色档案。"),
                                        new PromptVariableMetadataDto("characters.allSummary", "string", "预格式化的角色档案文本。"),
                                        new PromptVariableMetadataDto("characters.present", "string[]", "当前场景出场人物姓名数组。"),
                                        new PromptVariableMetadataDto("scene.number", "number", "当前小节编号。"),
                                        new PromptVariableMetadataDto("scene.totalInChapter", "number", "本章总小节数。"),
                                        new PromptVariableMetadataDto("scene.synopsis", "string", "本节梗概。"),
                                        new PromptVariableMetadataDto("scene.expectedWords", "number", "期望字数。"),
                                        new PromptVariableMetadataDto("scene.coreCharacters", "object[]", "核心人物状态卡数据。"),
                                        new PromptVariableMetadataDto("scene.coreCharacterSummary", "string", "核心人物状态卡文本。"),
                                        new PromptVariableMetadataDto("scene.temporaryCharacters", "object[]", "临时人物数组。"),
                                        new PromptVariableMetadataDto("scene.temporaryCharacterSummary", "string", "临时人物文本。"),
                                        new PromptVariableMetadataDto("scene.presentCharacters", "string", "本节出场人物名称字符串。"),
                                        new PromptVariableMetadataDto("previousSection.content", "string", "上一节正文原文。"),
                                        new PromptVariableMetadataDto("previousChapter.scenes", "object[]", "上一章全部场景大纲。"),
                                        new PromptVariableMetadataDto("previousChapter.summary", "string", "上一章大纲摘要。"),
                                        new PromptVariableMetadataDto("currentChapter.scenes", "object[]", "本章全部场景大纲。"),
                                        new PromptVariableMetadataDto("currentChapter.summary", "string", "本章大纲摘要。"),
                                        new PromptVariableMetadataDto("chapter.number", "number", "当前章号。"),
                                        new PromptVariableMetadataDto("chapter.total", "number", "总章节数。"),
                                        new PromptVariableMetadataDto("chapter.title", "string", "当前章标题。"),
                                        new PromptVariableMetadataDto("log.latestByCharacter", "map", "角色 -> 最近成长日志摘要。")
                                )
                        ),
                        new PromptTypeMetadataDto(
                                "refine",
                                "文本润色",
                                List.of(
                                        new PromptVariableMetadataDto("text", "string", "原始文本。"),
                                        new PromptVariableMetadataDto("instruction", "string", "优化指令（仅在带指令模板中使用）。"),
                                        new PromptVariableMetadataDto("contextType", "string", "文本类型描述，例如“角色介绍”。"),
                                        new PromptVariableMetadataDto("context.note", "string", "基于 `contextType` 生成的提示语。"),
                                        new PromptVariableMetadataDto("request.raw", "object", "原始的 `RefineRequest` 对象。")
                                )
                        )
                ))
                .setFunctions(List.of(
                        new PromptFunctionMetadataDto("default(value)", "当前值为空字符串时返回备用值。", "${genre|default(\"未指定类型\")}"),
                        new PromptFunctionMetadataDto("join(separator)", "将数组或列表按分隔符拼接。", "${tags[*]|join(\"，\")}"),
                        new PromptFunctionMetadataDto("upper()", "将字符串转为大写。", "${tone|upper()}"),
                        new PromptFunctionMetadataDto("lower()", "将字符串转为小写。", "${contextType|lower()}"),
                        new PromptFunctionMetadataDto("trim()", "去除首尾空白。", "${idea|trim()}"),
                        new PromptFunctionMetadataDto("json()", "序列化为 JSON 字符串，便于调试或日志记录。", "${scene.coreCharacters|json()}")
                ))
                .setSyntaxTips(List.of(
                        "使用 `${变量}` 插入上下文，支持嵌套访问，例如 `${story.title}`。",
                        "列表支持 `[索引]` 与 `[*]` 语法；`[*]` 默认使用顿号连接，可配合 `|join()` 自定义分隔符。",
                        "可以链式调用函数，如 `${tags[*]|join(\"，\")|upper()}`。"
                ))
                .setExamples(List.of(
                        "${context.lines.tags|default(\"标签：暂无\")}",
                        "${characters.present[*]|join(\"、\")}",
                        "${previousSection.content|trim()}"
                ));
    }
}

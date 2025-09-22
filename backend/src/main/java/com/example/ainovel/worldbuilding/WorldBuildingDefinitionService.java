package com.example.ainovel.worldbuilding;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.ainovel.dto.world.WorldBasicInfoDefinitionDto;
import com.example.ainovel.dto.world.WorldBuildingDefinitionResponse;
import com.example.ainovel.dto.world.WorldFieldDefinitionDto;
import com.example.ainovel.dto.world.WorldFieldRefineTemplateDto;
import com.example.ainovel.dto.world.WorldModuleDefinitionDto;
import com.example.ainovel.dto.world.WorldPromptContextDefinitionDto;

@Service
public class WorldBuildingDefinitionService {

    public WorldBuildingDefinitionResponse getDefinitions() {
        return new WorldBuildingDefinitionResponse()
                .setBasicInfo(buildBasicInfo())
                .setModules(List.of(
                        buildCosmosModule(),
                        buildGeographyModule(),
                        buildSocietyModule(),
                        buildHistoryModule(),
                        buildCultureModule(),
                        buildEconomyModule(),
                        buildFactionsModule()))
                .setFieldRefineTemplate(buildFieldRefineTemplate())
                .setPromptContext(buildPromptContextDefinition());
    }

    private WorldBasicInfoDefinitionDto buildBasicInfo() {
        return new WorldBasicInfoDefinitionDto()
                .setDescription("世界基础信息（顶栏）")
                .setFields(List.of(
                        new WorldFieldDefinitionDto()
                                .setKey("name")
                                .setLabel("世界名称")
                                .setDescription("世界的正式名称，必填。")
                                .setTooltip("请输入世界的正式名称或最常用称呼，可使用中文或英文，建议不超过 30 字。")
                                .setValidation("单行文本，必填，1-30 字符。")
                                .setRecommendedLength("1-30 字符"),
                        new WorldFieldDefinitionDto()
                                .setKey("tagline")
                                .setLabel("一句话概述")
                                .setDescription("用一句话描述世界的核心卖点。")
                                .setTooltip("用一句话概括这个世界的核心概念，例如‘魔法等于记忆’或‘漂浮大陆上的赛博城邦’。")
                                .setValidation("单行文本，必填，20-60 字。")
                                .setRecommendedLength("20-60 字"),
                        new WorldFieldDefinitionDto()
                                .setKey("themes")
                                .setLabel("主题标签")
                                .setDescription("3-5 个标签描述风格或主题。")
                                .setTooltip("输入 3-5 个标签，例如‘黑暗奇幻’、‘蒸汽朋克’。用于提示词上下文。")
                                .setValidation("标签输入，至少 1 个，最多 5 个。")
                                .setRecommendedLength("3-5 个标签"),
                        new WorldFieldDefinitionDto()
                                .setKey("creativeIntent")
                                .setLabel("创作意图")
                                .setDescription("记录创作目标、灵感或限制。")
                                .setTooltip("说明你希望在这个世界中探索的主题、灵感来源或需要遵守的禁忌。")
                                .setValidation("多行文本，必填，建议不少于 150 字。")
                                .setRecommendedLength("≥150 字"),
                        new WorldFieldDefinitionDto()
                                .setKey("notes")
                                .setLabel("开发者备注")
                                .setDescription("可选，记录协作说明。")
                                .setTooltip("（可选）补充团队协作约定、参考资料链接等信息。")
                                .setValidation("多行文本，可为空，建议不超过 300 字。")
                                .setRecommendedLength("≤300 字")));
    }

    private WorldModuleDefinitionDto buildCosmosModule() {
        return new WorldModuleDefinitionDto()
                .setKey("cosmos")
                .setLabel("宇宙观与法则")
                .setDescription("解释世界的整体结构与运作法则。")
                .setFields(List.of(
                        new WorldFieldDefinitionDto()
                                .setKey("cosmos_structure")
                                .setLabel("宇宙结构与尺度")
                                .setDescription("描述世界的宏观形态、层级与维度。")
                                .setTooltip("说明世界的整体形态（行星、浮空层、巨兽背脊等）以及是否存在多层宇宙。")
                                .setValidation("必填，建议 200-400 字。")
                                .setRecommendedLength("200-400 字")
                                .setAiFocus("突出层级结构、空间形态与视觉符号。"),
                        new WorldFieldDefinitionDto()
                                .setKey("space_time")
                                .setLabel("时间与空间规则")
                                .setDescription("解释时间流逝、空间边界与异常。")
                                .setTooltip("时间是否线性？能否穿越？空间有无裂隙、镜像、边界墙等特殊现象。")
                                .setValidation("必填，建议 180-350 字。")
                                .setRecommendedLength("180-350 字")
                                .setAiFocus("说明时间流速、边界、可否穿越及代价。"),
                        new WorldFieldDefinitionDto()
                                .setKey("energy_system")
                                .setLabel("能量/魔法来源与运作")
                                .setDescription("说明力量来源、使用方式、代价。")
                                .setTooltip("力量来自何处？谁能使用？施展需要什么步骤或媒介？付出什么代价？")
                                .setValidation("必填，建议 220-400 字。")
                                .setRecommendedLength("220-400 字")
                                .setAiFocus("明确来源、使用者、代价三要素。"),
                        new WorldFieldDefinitionDto()
                                .setKey("technology_magic_balance")
                                .setLabel("科技与超自然的关系")
                                .setDescription("描述科技与超自然力量的互动。")
                                .setTooltip("科技水平如何？与魔法或超自然力量是融合、对立还是互补？")
                                .setValidation("必填，建议 180-300 字。")
                                .setRecommendedLength("180-300 字")
                                .setAiFocus("比较科技与超自然的共存或冲突。"),
                        new WorldFieldDefinitionDto()
                                .setKey("constraints")
                                .setLabel("法则限制与禁忌")
                                .setDescription("列举世界运作的硬性限制。")
                                .setTooltip("列出世界设定中不能被打破的规则、禁忌或副作用，帮助保持逻辑一致。")
                                .setValidation("必填，建议 150-250 字。")
                                .setRecommendedLength("150-250 字")
                                .setAiFocus("列出不可违背的规则，并说明违反后果。")))
                .setAiGenerationTemplate("""
                        你是一名资深世界观设定顾问，请围绕「${world.name}」的“${module.label}”模块补全全部字段。

                        【已知基础信息】
                        - 世界概述：${world.tagline}
                        - 主题标签：${world.themes[*]|join(" / ")}
                        - 创作意图：${world.creativeIntent}
                        ${relatedModules[*]|map(m -> "- ${m.label}摘要：${m.summary}")|join("\n")}

                        【当前字段草稿】
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        【写作要求】
                        1. 输出必须是合法 JSON，对象键分别为 ${helper.fieldDefinitions[*].key|join(", ")}。
                        2. 如果字段已有内容，请在生成时充分参考原值 ${module.fields} 并在必要时润色、补足，而不是完全覆盖设定。
                        3. 语言需具体、具有画面感，避免泛泛而谈。
                        4. 每个字段保持 180-400 字，必要时可使用有序列表表达规则。

                        请直接返回 JSON 对象，无需额外解释。
                        """)
                .setFinalTemplate("""
                        你是世界设定档案的撰稿人，请将「${world.name}」的“${module.label}”模块整理为一篇结构化说明，用于向作者与 AI 解释世界的基础法则。

                        【输入素材】
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        【撰写要求】
                        1. 以第一层标题“${module.label}”开篇，后续使用段落/列表组织，不要使用 Markdown 标题符号（#）。
                        2. 先给出 2-3 句概述，再按主题拆分段落（例如宇宙结构、时间规则、能量体系、科技关系、限制）。
                        3. 每个段落需包含明确的规则或示例，突出本世界的独特性。
                        4. 结尾追加“创作提示”段，提醒在小说创作中需要注意的设定边界。
                        5. 最终文字约 500-700 字，语言正式且便于引用。
                        """);
    }

    private WorldModuleDefinitionDto buildGeographyModule() {
        return new WorldModuleDefinitionDto()
                .setKey("geography")
                .setLabel("地理与生态")
                .setDescription("描述世界的空间格局、生态与资源。")
                .setFields(List.of(
                        new WorldFieldDefinitionDto()
                                .setKey("world_map_overview")
                                .setLabel("世界格局概览")
                                .setDescription("概述大陆、海洋、天空层等整体格局。")
                                .setTooltip("描述世界的整体版图、主要大陆或层级结构，可附带方位与比例感。")
                                .setValidation("必填，建议 200-400 字。")
                                .setRecommendedLength("200-400 字")
                                .setAiFocus("交代整体地图与方向感。"),
                        new WorldFieldDefinitionDto()
                                .setKey("signature_regions")
                                .setLabel("代表性区域与地标")
                                .setDescription("罗列 3-5 个标志性地点及特征。")
                                .setTooltip("列出最具代表性的区域或地标，每个附带特色、重要性与常见冲突。")
                                .setValidation("必填，建议列点呈现，每条 2-3 句。")
                                .setRecommendedLength("列点，单条 2-3 句")
                                .setAiFocus("为每个地点提供特色、势力、剧情钩子。"),
                        new WorldFieldDefinitionDto()
                                .setKey("climate_cycles")
                                .setLabel("气候与季节循环")
                                .setDescription("描述气候带、季节循环及异常气象。")
                                .setTooltip("说明不同地区的气候特点，是否存在特殊季节或极端天气。")
                                .setValidation("必填，建议 180-320 字。")
                                .setRecommendedLength("180-320 字")
                                .setAiFocus("说明气候规律、异常天气及其成因。"),
                        new WorldFieldDefinitionDto()
                                .setKey("sapient_species")
                                .setLabel("智慧种族与分布")
                                .setDescription("列出主要智慧种族及居住区域。")
                                .setTooltip("列举人类/非人种族，说明体貌、习俗、聚居地与势力范围。")
                                .setValidation("必填，建议 220-360 字。")
                                .setRecommendedLength("220-360 字")
                                .setAiFocus("展示种族与地域的关系与摩擦点。"),
                        new WorldFieldDefinitionDto()
                                .setKey("ecosystems_resources")
                                .setLabel("生态系统与关键资源")
                                .setDescription("描述生态链、特有生物与资源分布。")
                                .setTooltip("说明食物链、独特动植物、矿产或能量资源，以及资源争夺。")
                                .setValidation("必填，建议 220-360 字。")
                                .setRecommendedLength("220-360 字")
                                .setAiFocus("突出资源稀缺性、生态链闭环与争夺。")))
                .setAiGenerationTemplate("""
                        你是一名世界地理设定专家，请为「${world.name}」的“${module.label}”模块生成详细内容。

                        【基础背景】
                        - 世界概述：${world.tagline}
                        - 主题标签：${world.themes[*]|join(" / ")}
                        ${relatedModules[*]|map(m -> "- ${m.label}摘要：${m.summary}")|join("\n")}

                        【当前字段草稿】
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        【写作要求】
                        1. 输出 JSON，键为 ${helper.fieldDefinitions[*].key|join(", ")}。
                        2. 若已有字段值，请基于原内容润色和扩展，保持既有设定。
                        3. 每个字段需包含具体地名、生物或资源，避免泛泛的描述。
                        4. “代表性区域与地标”字段建议输出有序列表（1-5），其余字段输出段落文本。
                        """)
                .setFinalTemplate("""
                        请基于以下素材撰写「${world.name}」的《地理与生态总览》文档，长度约 500-700 字。

                        素材清单：
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        写作要求：
                        1. 首段总结世界地理格局与生态主题。
                        2. 按“总体格局 > 区域亮点 > 气候循环 > 种族分布 > 资源生态”顺序组织内容。
                        3. 在描述资源时指出潜在冲突或剧情线索。
                        4. 结尾提供 2-3 条创作建议，例如旅行描写、环境冲突的利用方式。
                        5. 不要使用 Markdown 标题。
                        """);
    }

    private WorldModuleDefinitionDto buildSocietyModule() {
        return new WorldModuleDefinitionDto()
                .setKey("society")
                .setLabel("社会与政治")
                .setDescription("刻画权力结构、法律秩序与社会面貌。")
                .setFields(List.of(
                        new WorldFieldDefinitionDto()
                                .setKey("political_landscape")
                                .setLabel("政体与势力分布")
                                .setDescription("描述主要政体、势力、统治结构。")
                                .setTooltip("列出主要国家、联盟或组织，以及各自的统治方式与势力范围。")
                                .setValidation("必填，建议 200-360 字。")
                                .setRecommendedLength("200-360 字")
                                .setAiFocus("明确势力、统治方式、影响范围。"),
                        new WorldFieldDefinitionDto()
                                .setKey("law_order")
                                .setLabel("法律体系与社会秩序")
                                .setDescription("说明法律制定、执行机构与治安。")
                                .setTooltip("谁制定法律？执法机构怎样运作？社会治安如何保障或被破坏？")
                                .setValidation("必填，建议 180-320 字。")
                                .setRecommendedLength("180-320 字")
                                .setAiFocus("突出法律与民众生活的关系。"),
                        new WorldFieldDefinitionDto()
                                .setKey("social_structure")
                                .setLabel("社会阶层与民生结构")
                                .setDescription("描述阶级划分、职业、日常生活。")
                                .setTooltip("阶层如何划分？职业分布与社会流动性如何？普通人的日常是什么样？")
                                .setValidation("必填，建议 200-350 字。")
                                .setRecommendedLength("200-350 字")
                                .setAiFocus("展现阶层差异与社会流动。"),
                        new WorldFieldDefinitionDto()
                                .setKey("military_conflict")
                                .setLabel("军事力量与冲突态势")
                                .setDescription("说明军备、战略与主要冲突。")
                                .setTooltip("军队规模、装备与特色如何？目前有哪些热点冲突或潜在战争？")
                                .setValidation("必填，建议 200-320 字。")
                                .setRecommendedLength("200-320 字")
                                .setAiFocus("呈现冲突动因与关键战线。"),
                        new WorldFieldDefinitionDto()
                                .setKey("governance_dynamics")
                                .setLabel("权力运作与治理模式")
                                .setDescription("解析幕后权力与政治文化。")
                                .setTooltip("描述权力如何运作：议会、贵族、教会或财阀？有哪些腐败、阴谋或改革？")
                                .setValidation("必填，建议 200-320 字。")
                                .setRecommendedLength("200-320 字")
                                .setAiFocus("揭示权力博弈与制度张力。")))
                .setAiGenerationTemplate("""
                        请根据以下上下文，为「${world.name}」的“${module.label}”模块生成详细的社会与政治设定。

                        世界背景：
                        - 概述：${world.tagline}
                        - 主题：${world.themes[*]|join(" / ")}
                        ${relatedModules[*]|map(m -> "- ${m.label}摘要：${m.summary}")|join("\n")}

                        【当前字段草稿】
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        要求：
                        1. 输出 JSON，对象包含 ${helper.fieldDefinitions[*].key|join(", ")}。
                        2. 每个字段需包含结构（机构/阶层）、关键人物或组织、当前矛盾。
                        3. “军事力量与冲突态势”应包含至少一个正在进行或潜在的冲突场景。
                        4. 注意与其他模块（地理、历史、文化）的设定保持一致。
                        """)
                .setFinalTemplate("""
                        撰写《${world.name} 社会与政治蓝图》，目标读者为编剧与 AI 文本生成器。

                        输入：
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        结构建议：
                        1. 导语：概括政治基调与社会核心矛盾。
                        2. 政体结构：介绍主要政体与势力，列出代表人物或机构。
                        3. 社会面貌：描述阶层、职业、民生状态。
                        4. 秩序与冲突：说明法律、治安、军事实力与当前冲突。
                        5. 权力动态：分析潜在改革、阴谋或革命火种。
                        6. 创作提示：给出 2-3 条剧情应用建议。

                        篇幅：600-750 字，避免使用 Markdown 标题。
                        """);
    }

    private WorldModuleDefinitionDto buildHistoryModule() {
        return new WorldModuleDefinitionDto()
                .setKey("history")
                .setLabel("历史与传说")
                .setDescription("梳理世界的起源、纪元、事件与谜团。")
                .setFields(List.of(
                        new WorldFieldDefinitionDto()
                                .setKey("creation_myth")
                                .setLabel("创世神话与起源")
                                .setDescription("讲述世界诞生与奠基事件。")
                                .setTooltip("描述世界如何诞生，是否由神祇、科学实验或自然演化而成。")
                                .setValidation("必填，建议 180-320 字。")
                                .setRecommendedLength("180-320 字")
                                .setAiFocus("讲述起源故事，并与现世呼应。"),
                        new WorldFieldDefinitionDto()
                                .setKey("timeline_epochs")
                                .setLabel("纪元与年代划分")
                                .setDescription("列出主要纪元、时代与特点。")
                                .setTooltip("列出 3-5 个关键纪元/时代，标注起止、标志性事件与社会变化。")
                                .setValidation("必填，可使用分段或列表。")
                                .setRecommendedLength("列点或分段")
                                .setAiFocus("按时间顺序列出时代与变化。"),
                        new WorldFieldDefinitionDto()
                                .setKey("turning_points")
                                .setLabel("关键历史事件")
                                .setDescription("选取影响深远的事件。")
                                .setTooltip("挑选 3-5 个改变世界格局的事件，说明起因、经过、结果。")
                                .setValidation("必填，每事件 2-3 句。")
                                .setRecommendedLength("单条 2-3 句")
                                .setAiFocus("强调因果链与后续影响。"),
                        new WorldFieldDefinitionDto()
                                .setKey("legendary_figures")
                                .setLabel("传奇人物与传承")
                                .setDescription("描述历史人物、英雄或恶棍。")
                                .setTooltip("介绍历史上的英雄、反派或文化符号，他们的遗产如何影响当下。")
                                .setValidation("必填，建议 180-300 字。")
                                .setRecommendedLength("180-300 字")
                                .setAiFocus("展示人物事迹与文化影响。"),
                        new WorldFieldDefinitionDto()
                                .setKey("prophecies")
                                .setLabel("预言、禁忌与未解之谜")
                                .setDescription("整理仍影响当下的悬念或预言。")
                                .setTooltip("列出仍影响当下的预言、禁忌、谜团，说明可信度与现状。")
                                .setValidation("必填，建议 150-280 字。")
                                .setRecommendedLength("150-280 字")
                                .setAiFocus("呈现谜团、可信度、现状影响。")))
                .setAiGenerationTemplate("""
                        作为历史顾问，请补充「${world.name}」世界的历史与传说。

                        背景摘要：
                        - 世界概述：${world.tagline}
                        ${relatedModules[*]|map(m -> "- ${m.label}摘要：${m.summary}")|join("\n")}

                        【当前字段草稿】
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        任务：输出 JSON，字段包括 ${helper.fieldDefinitions[*].key|join(", ")}。
                        要求：
                        1. 使用叙事化语言，突出事件因果与人物动机。
                        2. “纪元与年代划分”可使用编号列表，包含时间跨度与关键变革。
                        3. “预言、禁忌与未解之谜”需指出当下的影响或待解线索。
                        """)
                .setFinalTemplate("""
                        撰写《${world.name} 历史与传说档案》，供世界观资料库引用。

                        素材：
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        要求：
                        1. 以叙事性导语引入世界历史基调。
                        2. 采用“时代纵览 > 重大事件 > 传奇人物 > 未解之谜”结构。
                        3. 强调历史对当前社会、文化与冲突的影响。
                        4. 结尾列出 2 条适合在作品中揭示或反转的历史伏笔。
                        5. 篇幅 500-700 字，不使用 Markdown 标题。
                        """);
    }

    private WorldModuleDefinitionDto buildCultureModule() {
        return new WorldModuleDefinitionDto()
                .setKey("culture")
                .setLabel("文化与日常")
                .setDescription("展现世界的信仰、语言、艺术与价值观。")
                .setFields(List.of(
                        new WorldFieldDefinitionDto()
                                .setKey("religion_faith")
                                .setLabel("宗教体系与信仰实践")
                                .setDescription("描述神祇体系、教义、仪式。")
                                .setTooltip("说明主要宗教或信仰、神祇分工、重要节日与禁忌。")
                                .setValidation("必填，建议 200-320 字。")
                                .setRecommendedLength("200-320 字")
                                .setAiFocus("描绘神祇、仪式、社会影响。"),
                        new WorldFieldDefinitionDto()
                                .setKey("language_communication")
                                .setLabel("语言与交流")
                                .setDescription("介绍主要语言、文字、沟通方式。")
                                .setTooltip("列出主要语言/文字体系，方言或跨文化沟通手段。")
                                .setValidation("必填，建议 180-260 字。")
                                .setRecommendedLength("180-260 字")
                                .setAiFocus("说明语言谱系、方言、沟通方式。"),
                        new WorldFieldDefinitionDto()
                                .setKey("arts_entertainment")
                                .setLabel("艺术、娱乐与媒体")
                                .setDescription("描述艺术形式、娱乐活动、传媒。")
                                .setTooltip("说明大众娱乐、艺术风格、传播渠道（戏剧、战斗竞技、虚拟现实等）。")
                                .setValidation("必填，建议 180-300 字。")
                                .setRecommendedLength("180-300 字")
                                .setAiFocus("展示文化生活与情绪色彩。"),
                        new WorldFieldDefinitionDto()
                                .setKey("customs_rituals")
                                .setLabel("社会习俗与仪式")
                                .setDescription("说明婚丧嫁娶、节庆与礼仪。")
                                .setTooltip("列出重要仪式、传统礼节，以及它们的象征意义。")
                                .setValidation("必填，建议 180-300 字。")
                                .setRecommendedLength("180-300 字")
                                .setAiFocus("描述仪式流程、象征意义、差异。"),
                        new WorldFieldDefinitionDto()
                                .setKey("values_morality")
                                .setLabel("价值观与伦理争议")
                                .setDescription("总结主流价值与道德冲突。")
                                .setTooltip("描述社会普遍崇尚什么？有哪些道德议题或禁区？")
                                .setValidation("必填，建议 200-320 字。")
                                .setRecommendedLength("200-320 字")
                                .setAiFocus("提出主流价值与对立观点。")))
                .setAiGenerationTemplate("""
                        请以文化学者视角，为「${world.name}」生成“${module.label}”模块的全部字段。

                        世界背景：
                        - 概述：${world.tagline}
                        - 主题标签：${world.themes[*]|join(" / ")}
                        ${relatedModules[*]|map(m -> "- ${m.label}摘要：${m.summary}")|join("\n")}

                        【当前字段草稿】
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        要求：
                        1. 返回 JSON，对象包含 ${helper.fieldDefinitions[*].key|join(", ")}。
                        2. 强调文化如何回应历史事件、社会结构和地理环境。
                        3. 至少提供 2 个具体节庆、仪式或娱乐例子。
                        4. “价值观与伦理争议”需呈现对立观点与矛盾点。
                        """)
                .setFinalTemplate("""
                        撰写《${world.name} 文化与日常生活档案》，长度约 550-700 字。

                        素材：
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        结构建议：
                        1. 导语：概括文化基调与日常生活特色。
                        2. 信仰与语言：描述宗教体系与语言差异。
                        3. 艺术与娱乐：呈现典型艺术形式、娱乐方式。
                        4. 习俗与礼仪：列出关键仪式与社会意义。
                        5. 价值观冲突：分析道德分歧及其引发的社会议题。
                        6. 创作提示：提供 2 条文化细节在故事中的应用建议。

                        输出为连续段落，不使用 Markdown 标题。
                        """);
    }

    private WorldModuleDefinitionDto buildEconomyModule() {
        return new WorldModuleDefinitionDto()
                .setKey("economy")
                .setLabel("经济与科技")
                .setDescription("描绘经济体系、产业网络与科技水平。")
                .setFields(List.of(
                        new WorldFieldDefinitionDto()
                                .setKey("economic_system")
                                .setLabel("经济体系与货币")
                                .setDescription("描述经济模式、货币与财富流动。")
                                .setTooltip("说明主要经济模式（农业、贸易、能源等）与货币体系或交换方式。")
                                .setValidation("必填，建议 200-320 字。")
                                .setRecommendedLength("200-320 字")
                                .setAiFocus("明确经济模式、货币、财富流动。"),
                        new WorldFieldDefinitionDto()
                                .setKey("industry_trade")
                                .setLabel("产业与贸易网络")
                                .setDescription("列出关键产业、贸易路线与伙伴。")
                                .setTooltip("列举支柱产业、重要贸易路线、合作或竞争势力。")
                                .setValidation("必填，建议 200-320 字。")
                                .setRecommendedLength("200-320 字")
                                .setAiFocus("描述供应链、贸易伙伴、冲突。"),
                        new WorldFieldDefinitionDto()
                                .setKey("technology_level")
                                .setLabel("科技/魔法应用水平")
                                .setDescription("描述科技（含魔法科技）的发展程度。")
                                .setTooltip("说明整体科技阶段，以及在通信、医疗、军事等方面的特点。")
                                .setValidation("必填，建议 200-320 字。")
                                .setRecommendedLength("200-320 字")
                                .setAiFocus("说明科技层级、领域差异。"),
                        new WorldFieldDefinitionDto()
                                .setKey("infrastructure")
                                .setLabel("交通、通讯与基础设施")
                                .setDescription("说明交通方式、通讯网络、公共设施。")
                                .setTooltip("描述主要交通工具、通讯手段（魔法/科技）和公共基础设施。")
                                .setValidation("必填，建议 200-300 字。")
                                .setRecommendedLength("200-300 字")
                                .setAiFocus("展示基础设施覆盖与缺口。"),
                        new WorldFieldDefinitionDto()
                                .setKey("daily_life_tech")
                                .setLabel("日常生活技术与民生")
                                .setDescription("描述民众日常使用的技术或魔法。")
                                .setTooltip("举例说明普通人在衣食住行、医疗等方面使用的技术或便利。")
                                .setValidation("必填，建议 180-280 字。")
                                .setRecommendedLength("180-280 字")
                                .setAiFocus("强调普通人如何使用技术。")))
                .setAiGenerationTemplate("""
                        请以世界经济分析师的视角，为「${world.name}」补全“${module.label}”模块。

                        背景：
                        - 世界概述：${world.tagline}
                        - 主题：${world.themes[*]|join(" / ")}
                        ${relatedModules[*]|map(m -> "- ${m.label}摘要：${m.summary}")|join("\n")}

                        【当前字段草稿】
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        要求：
                        1. 输出 JSON，键为 ${helper.fieldDefinitions[*].key|join(", ")}。
                        2. 描述经济结构时关联资源、社会、历史模块中的设定。
                        3. 在“产业与贸易网络”中至少提供一个贸易冲突或合作案例。
                        4. “科技/魔法应用水平”需区分高端与民用差异。
                        """)
                .setFinalTemplate("""
                        撰写《${world.name} 经济与科技白皮书》摘要版，篇幅约 600 字。

                        素材：
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        结构建议：
                        1. 总览：经济体系、货币与整体繁荣度。
                        2. 核心产业：描述主要产业、贸易网络与关键利益相关者。
                        3. 科技/魔法应用：区分军用、民用、黑市等层级。
                        4. 基础设施：阐述交通、通讯、能源的运行模式。
                        5. 民生体验：说明普通人如何接触并受益/受限于技术。
                        6. 创作提示：提供经济冲突、科技伦理的剧情切入点。

                        输出为段落文本。
                        """);
    }

    private WorldModuleDefinitionDto buildFactionsModule() {
        return new WorldModuleDefinitionDto()
                .setKey("factions")
                .setLabel("势力与剧情钩子")
                .setDescription("盘点世界中的关键势力、冲突与剧情线索。")
                .setFields(List.of(
                        new WorldFieldDefinitionDto()
                                .setKey("faction_overview")
                                .setLabel("主导势力与组织")
                                .setDescription("列出 3-5 个核心势力及目标。")
                                .setTooltip("介绍掌握话语权的势力或组织，说明核心人物、目标与资源。")
                                .setValidation("必填，建议 200-320 字。")
                                .setRecommendedLength("200-320 字")
                                .setAiFocus("突出势力目标与资源优势。"),
                        new WorldFieldDefinitionDto()
                                .setKey("species_relations")
                                .setLabel("种族关系与联盟")
                                .setDescription("概述种族间的合作、矛盾与盟约。")
                                .setTooltip("说明主要种族之间的联盟、仇恨、贸易或互补关系。")
                                .setValidation("必填，建议 200-320 字。")
                                .setRecommendedLength("200-320 字")
                                .setAiFocus("强调关系网与张力。"),
                        new WorldFieldDefinitionDto()
                                .setKey("current_conflicts")
                                .setLabel("当前冲突与危机")
                                .setDescription("描述正在进行的冲突、危机或阴谋。")
                                .setTooltip("列出 2-3 个当下的冲突（战争、瘟疫、政治危机），说明牵涉势力。")
                                .setValidation("必填，建议 180-300 字。")
                                .setRecommendedLength("180-300 字")
                                .setAiFocus("呈现冲突动因与利害关系。"),
                        new WorldFieldDefinitionDto()
                                .setKey("key_locations")
                                .setLabel("关键地点与剧情场景")
                                .setDescription("点名对剧情至关重要的地点。")
                                .setTooltip("列出可能成为剧情舞台的地点，解释其重要性与隐藏秘密。")
                                .setValidation("必填，建议 180-280 字。")
                                .setRecommendedLength("180-280 字")
                                .setAiFocus("明确地点作用、隐秘要素。"),
                        new WorldFieldDefinitionDto()
                                .setKey("story_hooks")
                                .setLabel("故事钩子与潜在剧情线")
                                .setDescription("提供可直接用于创作的剧情钩子。")
                                .setTooltip("输出 3-5 条故事钩子，指出涉及的角色、地点、冲突与潜在转折。")
                                .setValidation("必填，建议使用列点形式。")
                                .setRecommendedLength("列点 3-5 条")
                                .setAiFocus("给出可执行、含冲突的剧情起点。")))
                .setAiGenerationTemplate("""
                        请以剧本统筹的角色，为「${world.name}」撰写“${module.label}”模块内容。

                        参考背景：
                        - 世界概述：${world.tagline}
                        ${relatedModules[*]|map(m -> "- ${m.label}摘要：${m.summary}")|join("\n")}

                        【当前字段草稿】
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        要求：
                        1. 输出 JSON，键为 ${helper.fieldDefinitions[*].key|join(", ")}。
                        2. 每个势力/故事钩子需明确“目标”“阻力”“可能后果”。
                        3. “关键地点”需说明它与势力或冲突的关联。
                        4. 语气偏叙事，便于直接转化为剧情大纲。
                        """)
                .setFinalTemplate("""
                        撰写《${world.name} 剧情钩子总览》，以供工作台引用。

                        素材：
                        ${helper.fieldDefinitions[*]|map(m -> "- ${m.label}：${module.fields[m.key]}")|join("\n")}

                        写作要求：
                        1. 以“势力棋局”导语开启，强调世界当前的紧张局势。
                        2. 将内容组织为：势力图谱 → 种族关系 → 当前冲突 → 关键地点 → 故事钩子。
                        3. 每个故事钩子至少包含：触发事件、关键冲突、潜在结局。
                        4. 结尾提示如何根据钩子衍生主线与支线。
                        5. 篇幅 600-750 字，段落化呈现。
                        """);
    }

    private WorldFieldRefineTemplateDto buildFieldRefineTemplate() {
        return new WorldFieldRefineTemplateDto()
                .setDescription("世界构建字段级 AI 优化提示词骨架")
                .setTemplate("""
                        你是一名严谨的世界构建编辑，负责完善「${world.name}」世界中“${module.label}”模块的【${fieldLabel}】字段。

                        【当前世界背景】
                        - 核心概述：${world.tagline}
                        - 主题标签：${world.themes[*]|join(" / ")}
                        ${relatedModules[*]|map(m -> "- ${m.label}摘要：${m.summary}")|join("\n")}

                        【优化目标】
                        ${focusNote}
                        - 保持条理清晰，可使用短句或项目符号。
                        - 尊重已知规则，避免自相矛盾。

                        请在 180-400 字内重写下方内容，只返回优化后的正文，无需任何解释或 Markdown 标题。

                        原始文本：
                        '''
                        ${originalText}
                        '''
                        """)
                .setUsageNotes(List.of(
                        "调用时需提供 fieldLabel、focusNote、originalText。",
                        "focusNote 由字段定义提供，指导 AI 聚焦重点。"));
    }

    private WorldPromptContextDefinitionDto buildPromptContextDefinition() {
        return new WorldPromptContextDefinitionDto()
                .setDescription("世界构建提示词上下文结构")
                .setExample("""
                        {
                          "world": {
                            "id": 123,
                            "name": "永昼群星",
                            "tagline": "白昼永不落的多层天空都市",
                            "themes": ["高幻想", "阴谋", "成长"],
                            "genre": "高幻想",
                            "tone": "史诗",
                            "creativeIntent": "我想探索……",
                            "notes": "团队共识……"
                          },
                          "module": {
                            "key": "cosmos",
                            "label": "宇宙观与法则",
                            "fields": {
                              "cosmos_structure": "……",
                              "space_time": "……"
                            },
                            "emptyFields": ["cosmos_structure", "space_time"],
                            "dirtyFields": ["cosmos_structure"],
                            "previousFullContent": "……"
                          },
                          "relatedModules": [
                            {
                              "key": "geography",
                              "label": "地理与生态",
                              "status": "READY",
                              "summary": "北大陆被浮空珊瑚森林覆盖……"
                            }
                          ],
                          "helper": {
                            "fieldDefinitions": [
                              {"key": "cosmos_structure", "label": "宇宙结构与尺度", "recommendedLength": "200-400 字"},
                              {"key": "space_time", "label": "时间与空间规则", "recommendedLength": "200-350 字"}
                            ],
                            "markdownAllowed": true
                          }
                        }
                        """)
                .setNotes(List.of(
                        "`relatedModules` 按模块顺序传入，仅包含已填写或已有完整信息的模块摘要。",
                        "`helper.fieldDefinitions` 提供当前模块字段的 key、名称与推荐字数，便于模板遍历。",
                        "提示词模板可访问 ${world.*}、${module.*}、${relatedModules[*].summary} 等变量，并支持管道函数。"));
    }
}

# 提示词配置化功能设计文档

## 1. 背景与目标
现有系统在以下场景中通过硬编码的方式构建 AI 提示词：

- **故事创作（Story Conception）**：由 `AbstractAiService.buildConceptionPrompt` 在服务端拼接多段中文提示词。
- **大纲章节创作（Outline Chapter Generation）**：由 `OutlineService.buildChapterPrompt` 构造带有 Markdown 结构的长提示词。
- **小说正文创作（Manuscript Generation）**：由 `ManuscriptService.buildGenerationPrompt` 在运行时生成长文本提示词。
- **AI 文本优化（Refine）**：`AbstractAiService.buildRefinePrompt` 包含「带优化指令」与「纯润色」两套模板。

上述提示词均写死在 Java 代码中，用户无法按需调整；提示词中的上下文信息亦通过字符串拼接完成，后续维护成本高。本次需求希望：

1. 将上述提示词全部抽离到可配置的模板文件中，并允许按用户覆盖默认值；
2. 支持在模板中书写 `${...}` 形式的插值表达式，自动注入故事、角色、上下文等动态数据；
3. 在「设置」页面提供可视化配置界面和帮助文档，让不同用户独立管理自己的提示词。

## 2. 现状梳理
### 2.1 后端提示词位置
- `AbstractAiService.buildConceptionPrompt(...)`：拼接故事构思 JSON 模版。
- `OutlineService.buildChapterPrompt(...)`：构建章节级大纲提示词，包含故事核心信息、角色设定、写作要求等段落。
- `ManuscriptService.buildGenerationPrompt(...)`：针对具体小节正文，提供角色档案、上一节原文、写作规则等指令。
- `AbstractAiService.buildRefinePrompt(...)`：根据是否提供 instruction 选择不同句式的润色提示。

### 2.2 用户设置存储
- `user_settings` 表目前包含 `custom_prompt` 文本字段，但尚未被前后端使用。
- `SettingsDto`、`UserSetting` 实体均持有 `customPrompt` 字段，但 UI 未暴露配置入口。

### 2.3 前端设置界面
- `WorkbenchHeader` 中点击「设置」会打开 `UserSettingsModal` 弹窗，可配置 Base URL、模型名称、API Key。
- 存在未接入路由的 `Settings.tsx` 页面，功能与弹窗重复。
- 无提示词配置入口，也没有提示词编写帮助文档。

## 3. 功能需求概述
1. **模板配置**：故事构思、大纲章节、正文创作、AI 优化（带 / 不带指令）共 5 份模板可自定义；若用户未配置，则回退到系统默认模板。
2. **插值语法**：模板中支持 `${变量}`、`${变量[索引]}`、`${变量[*]}` 等表达式，并提供常用函数（如 `|default(...)`、`|join(...)`）。
3. **默认模板管理**：默认模板存于配置文件（YAML/JSON），随应用部署，后端加载缓存；不得再硬编码在 Java 方法中。
4. **用户隔离**：提示词定制存于用户维度，互不影响；前端设置页展示当前生效模板（用户自定义或默认值）。
5. **提示词帮助**：新增「提示词编写帮助」页面，列出全部支持的变量与语法示例，并从设置页提供跳转入口。

## 4. 默认提示词配置方案
### 4.1 配置文件结构
- 新增 `backend/src/main/resources/prompts/default-prompts.yaml`（UTF-8，支持多行文本）。
- 结构示例：
  ```yaml
  storyCreation: |
    ...默认故事构思模板...
  outlineChapter: |
    ...默认大纲模板...
  manuscriptSection: |
    ...默认正文模板...
  refine:
    withInstruction: |
      ...默认带指令模板...
    withoutInstruction: |
      ...默认无指令模板...
  ```
- 启动时通过 `@ConfigurationProperties(prefix = "prompts")` 或 `YamlPropertiesFactoryBean` 读取，加载成不可变对象缓存于内存。

### 4.2 默认模板文本
以下内容需完整迁移到配置文件（保留当前语言风格与结构，仅将动态字段替换为插值变量）。

#### 4.2.1 故事构思模板（`storyCreation`）
```text
你是一个世界级的小说家。请根据以下用户输入生成一个详细的故事构思：
${context.summary}
请严格按照以下JSON格式返回：
{
  "storyCard": {
    "title": "故事标题",
    "synopsis": "(要求：详细、丰富的故事梗概，长度不少于400字)",
    "worldview": "(要求：基于梗概生成详细的世界观设定)"
  },
  "characterCards": [
    {
      "name": "角色姓名",
      "synopsis": "角色简介（年龄、性别、外貌、性格等）",
      "details": "角色的详细背景故事和设定",
      "relationships": "角色与其他主要角色的关系"
    }
  ]
}
characterCards要基于梗概生成3-5个主要角色描述。只返回JSON对象，不要包含任何额外的解释或markdown格式。
```

#### 4.2.2 大纲章节模板（`outlineChapter`）
```text
你是一位洞悉读者心理、擅长制造“爽点”与“泪点”的顶尖网络小说家。现在，请你以合作者的身份，为我的故事设计接下来的一章。我希望这一章不仅是情节的推进，更是情感的积累和爆发。

# 故事核心信息
- **故事简介:** ${story.synopsis}
- **核心主题与基调:** ${story.genre} / ${story.tone}
- **故事长期走向:** ${story.storyArc}

# 主要角色设定
${characters.summary}

# 上下文回顾
- **上一章梗概:** ${chapter.previousSynopsis}

# 本章创作任务 (第 ${chapter.number} 章)
- **预设节数:** ${chapter.sectionsPerChapter}
- **预估每节字数:** ${chapter.wordsPerSection}

# 你的创作目标与自由度
1.  **情节设计:** 请构思一章充满“钩子”的情节。思考：这一章的结尾，最能让读者好奇地想读下一章的悬念是什么？中间是否可以安排一个小的“情绪爆点”或“情节反转”？
2.  **人物弧光:** 思考核心人物在本章的经历，他们的内心会产生怎样的变化？他们的信念是会更坚定，还是会受到挑战？
3.  **伏笔与回收:** 如果有机会，可以埋下一些与长线剧情相关的伏笔。如果前文有伏笔，思考本章是否是回收它的好时机。
4.  **创作建议 (重要):** 在满足核心要求的前提下，你完全可以提出更有创意的想法。例如，你认为某个临时人物的设定稍微调整一下会更有戏剧性，或者某个情节有更好的表现方式，请大胆地在你的设计中体现出来，并用 `[创作建议]` 标签标注。
5.  **拒绝平庸:** 请极力避免机械地推进剧情。每一节都应该有其独特的作用，或是塑造人物，或是铺垫情绪，或是揭示信息。
6.  **关键节点呈现:** 对于每节大纲，不应当写一篇长简介，而是应该写出本节的多个关键故事节点（按照每节的预定长度来决定关键故事节点个数，可考虑平均每200-400字一个关键节点），每个关键故事节点只有两三句话。
7.  **语言风格:** 应当减少比喻、排比等修辞手法的使用，仅在认为确实有必要的情况下才少量使用；同时减少套路化的写作格式和剧情走向，允许在一定程度上自由发挥。

# 输出格式
请严格以JSON格式返回。根对象应包含 "title", "synopsis" 和一个 "scenes" 数组。
每个 scene 对象必须包含:
- "sceneNumber": (number) 序号。
- "synopsis": (string) 详细、生动、充满画面感的故事梗概，字数不少于200字。
- "presentCharacters": (string[]) 核心出场人物姓名列表。
- "sceneCharacters": (object[]) 一个数组，用结构化的“人物卡”描述每位核心人物在本节中的状态，字段必须包含:
  - "characterName": (string) 角色姓名。
  - "status": (string) 角色在本节的生理或环境状态。
  - "thought": (string) 角色在本节的主要想法或心理活动。
  - "action": (string) 角色在本节的关键行动。
- "temporaryCharacters": (object[]) 一个对象数组，用于描写本节新出现的或需要详细刻画的临时人物。如果不需要，则返回空数组[]。每个对象必须包含所有字段: "name", "summary", "details", "relationships", "status", "thought", "action"。
```

#### 4.2.3 正文创作模板（`manuscriptSection`）
```text
你是一位资深的中文小说家，请使用简体中文进行创作。你的文风细腻而有张力，擅长通过细节与内心戏推动剧情。

【故事核心设定】
- 类型/基调: ${story.genre} / ${story.tone}
- 叙事视角: ${outline.pointOfView}
- 故事简介: ${story.synopsis}

【全部角色档案】
${characters.allSummary}

【上一章大纲回顾】
${previousChapter.summary}

【上一节正文（原文）】
${previousSection.content}

【本章完整大纲】
${currentChapter.summary}

【当前创作位置】
- 章节: 第 ${chapter.number}/${chapter.total} 章
- 小节: 第 ${scene.number}/${scene.totalInChapter} 节

【本节创作蓝图】
- 梗概: ${scene.synopsis}
- 核心出场人物: ${scene.presentCharacters}
- 核心人物状态卡:
${scene.coreCharacterSummary}
- 临时出场人物:
${scene.temporaryCharacterSummary}

【写作规则】
1. 钩子与悬念: 开篇30-80字设置强钩子；本节结尾制造悬念或情绪余韵。
2. 伏笔与回收: 合理埋设或回收伏笔，贴合剧情逻辑，避免突兀。
3. 人物弧光: 通过对话与行动自然呈现人物内心变化，拒绝直白说教。
4. 节奏与张力: 结合当前进度（第 ${chapter.number}/${chapter.total} 章，第 ${scene.number}/${scene.totalInChapter} 节）控制信息密度与冲突烈度。
5. 细节与画面: 强化感官细节、空间调度与象征性意象，避免模板化。
6. 忠于大纲，高于大纲: 不改变核心走向与设定，可进行合理艺术加工使剧情更佳。
7. 风格统一: 始终保持故事既定基调与叙事视角。
8. 输出要求: 直接输出本节“正文”，约 ${scene.expectedWords} 字；不要输出标题、注释或总结。
9. 语节制: 减少比喻、排比等修辞手法，仅在确有必要时少量使用。
10. 剧情新鲜: 避免套路化的表达和桥段，保持情节的惊喜度与原创性。
11. 自然质感: 让叙述与对话贴近人物的真实语感，避免机械化陈述。
12. 段落饱满: 每个自然段尽量写满多句内容，避免“一句话一个段落”的碎片化写法。

开始创作。
```

#### 4.2.4 AI 优化模板（`refine.withInstruction` 与 `refine.withoutInstruction`）
```text
// 带 instruction
你是一个专业的编辑。请根据我的修改意见，优化以下文本。${context.note}请只返回优化后的文本内容，不要包含任何解释性文字或Markdown格式。

原始文本:
"${text}"

我的意见:
"${instruction}"

// 无 instruction
你是一个专业的编辑。请优化以下文本，使其更生动、更具吸引力。${context.note}请只返回优化后的文本内容，不要包含任何解释性文字或Markdown格式。

原始文本:
"${text}"
```
> 其中 `${context.note}` 表示“这是一个关于……的文本”提示语，若 `contextType` 为空则替换为空字符串。

### 4.3 配置加载策略
- 应用启动时读取默认模板，封装为 `PromptDefaults`（不可变对象）。
- 通过 Spring `@Bean` 暴露单例；`PromptTemplateService` 注入后即可访问。
- 若配置文件缺失或字段为空，启动失败并抛出描述性异常，避免线上运行时出现空模板。
- 提供 `--spring.config.additional-location` 支持部署侧覆盖默认 YAML。

## 5. 用户级提示词存储模型
### 5.1 数据库调整
- **新列**：在 `user_settings` 表新增 `prompt_settings` (LONGTEXT)；迁移脚本示例：
  ```sql
  ALTER TABLE `user_settings`
    ADD COLUMN `prompt_settings` longtext NULL AFTER `custom_prompt`;
  ```
- `custom_prompt` 字段保留（兼容旧数据），后续可在清理任务中迁移 / 删除。
- `prompt_settings` 保存 JSON，结构与默认配置对应：
  ```json
  {
    "storyCreation": "...",
    "outlineChapter": "...",
    "manuscriptSection": "...",
    "refine": {
      "withInstruction": "...",
      "withoutInstruction": "..."
    }
  }
  ```
- 用户未保存任何模板时，该字段为 `NULL`。

### 5.2 实体与 DTO
- 新建 `PromptSettings` Java Bean：
  ```java
  public class PromptSettings {
      private String storyCreation;
      private String outlineChapter;
      private String manuscriptSection;
      private RefinePromptSettings refine;
  }
  public class RefinePromptSettings {
      private String withInstruction;
      private String withoutInstruction;
  }
  ```
- 在 `UserSetting` 实体中引入 `@Convert(converter = PromptSettingsAttributeConverter.class)` 将 JSON <-> 对象互转。
- `SettingsDto` 去掉旧 `customPrompt` 字段，新增 `PromptSettingsDto promptSettings`。
- 新增 `PromptTemplateDto`（前后端交互使用），包含：
  ```json
  {
    "storyCreation": {"content": "...", "isDefault": true},
    "outlineChapter": {"content": "...", "isDefault": false},
    ...
  }
  ```
  `isDefault` 标志用于前端显示“当前是否使用系统默认模板”。

### 5.3 Fallback 规则
1. 读取用户设置时：若 `prompt_settings` 为空，则直接返回默认模板，`isDefault=true`。
2. 若某个子模板缺失（例如只保存了 `refine.withInstruction`），其余字段回退默认值。
3. 保存时仅覆盖用户提供的字段，未传字段保持原值；若某字段置空字符串则视为用户自定义为空（不会回退）。
4. 提供「恢复默认」接口：清除指定模板的用户值（置为 `null`），下次读取自动回退。

## 6. 模板插值语法规范
### 6.1 表达式语法
- 基础格式：`${expression}`。
- `expression` 由「路径」+ 可选管道函数组成：`path | func1(arg) | func2`。
- 路径：使用 `.` 访问对象属性，`[index]` 访问列表元素，`[*]` 表示整列；示例：
  - `${idea}`
  - `${tags[0]}`
  - `${tags[*]}`
  - `${characters[1].name}`
  - `${scene.coreCharacters[0].status}`
- `[index]` 超界、空值、空列表 => 返回空字符串。
- `[*]` 默认以顿号 `、` 连接；可通过 `|join("，")` 自定义分隔。

### 6.2 内置函数
| 函数 | 作用 | 使用示例 |
| --- | --- | --- |
| `default(value)` | 若前面表达式结果为空字符串，则返回 `value` | `${genre|default("未指定类型")}` |
| `join(separator)` | 将列表（或 `[*]` 结果）使用 `separator` 连接 | `${tags|join(", ")}` |
| `upper()` | 转为大写（仅对字符串有效） | `${type|upper()}` |
| `lower()` | 转为小写 | `${contextType|lower()}` |
| `json()` | 将对象序列化为 JSON 字符串（用于调试/记录） | `${scene.characters|json()}` |
| `trim()` | 去除首尾空白 | `${idea|trim()}` |
> 管道函数按顺序执行，返回值会成为下一个函数的输入。

### 6.3 错误处理与转义
- 模板解析采用正则扫描，遇到未知函数或非法语法时：记录日志并抛出 `PromptTemplateException`，由业务方捕获并回退默认模板，同时提示用户模板存在语法错误。
- 模板中若需要输出 `${` 字面量，可写成 `$${`（引擎在渲染前先替换为单个 `$`）。
- 对于 JSON 模板，模板引擎不额外转义引号；用户需自行确保生成的字符串符合 JSON 格式。

### 6.4 可用变量总表
以下表格将用于帮助页展示，后台亦会通过 `GET /api/v1/prompt-templates/metadata` 返回统一元数据。

#### 6.4.1 故事构思模板（`storyCreation`）
| 变量 | 类型 | 说明 |
| --- | --- | --- |
| `idea` | string | 用户填写的“一句话想法”。 |
| `type` | string | `genre` 的别名，方便书写 `${type}`。 |
| `genre` | string | 故事类型。 |
| `tone` | string | 故事基调。 |
| `tags` / `tag` | string[] | 标签数组，支持 `${tags[0]}`、`${tag[*]}`。 |
| `context.summary` | string | 预构建的上下文块（含回退文案，末尾带换行）。 |
| `context.lines.idea` | string | 若 `idea` 有值，则为 `"核心想法：...\n"`，否则为空串。 |
| `context.lines.genre` | string | 若类型有值则返回对应行。 |
| `context.lines.tone` | string | 同上。 |
| `context.lines.tags` | string | 若标签非空，格式如 `"标签：奇幻、冒险\n"`。 |
| `request.raw` | object | 原始 `ConceptionRequest` 对象，供高级用法 `${request.raw.genre}`。 |

#### 6.4.2 大纲章节模板（`outlineChapter`）
| 变量 | 类型 | 说明 |
| --- | --- | --- |
| `story.title` | string | 故事标题。 |
| `story.synopsis` | string | 故事简介。 |
| `story.genre` | string | 类型。 |
| `story.tone` | string | 基调。 |
| `story.storyArc` | string | 长线走向。 |
| `outline.title` | string | 大纲名称。 |
| `outline.pointOfView` | string | 叙事视角（可为空）。 |
| `chapter.number` | number | 当前章节号。 |
| `chapter.sectionsPerChapter` | number | 预设节数。 |
| `chapter.wordsPerSection` | number | 预估字数。 |
| `chapter.previousSynopsis` | string | 上一章梗概，首章时为“无，这是第一章。”。 |
| `characters.list` | object[] | 故事主角数组 `{ id, name, synopsis, details, relationships }`。 |
| `characters.summary` | string | 预格式化的角色 bullet 列表（与现有 `characterProfiles` 一致）。 |
| `characters.names` | string[] | 角色姓名列表，便于 `${characters.names[*]}`。 |
| `request.raw` | object | `GenerateChapterRequest` 数据。 |

#### 6.4.3 正文创作模板（`manuscriptSection`）
| 变量 | 类型 | 说明 |
| --- | --- | --- |
| `story.title` | string | 故事标题。 |
| `story.genre` | string | 类型。 |
| `story.tone` | string | 基调。 |
| `story.synopsis` | string | 故事简介。 |
| `outline.pointOfView` | string | 叙事视角，若为空默认“第三人称有限视角”。 |
| `characters.all` | object[] | 全部角色卡 `{ id, name, synopsis, details, relationships }`。 |
| `characters.allSummary` | string | 预格式化角色档案（对应 `formatCharacters` 输出）。 |
| `characters.present` | string[] | 当前场景出场人物姓名。 |
| `scene.number` | number | 当前小节编号。 |
| `scene.totalInChapter` | number | 本章总小节数。 |
| `scene.synopsis` | string | 本节梗概。 |
| `scene.expectedWords` | number | 预计字数（默认 1200）。 |
| `scene.coreCharacters` | object[] | `SceneCharacter` 列表 `{ characterName, status, thought, action }`。 |
| `scene.coreCharacterSummary` | string | 结构化人物状态卡文本。 |
| `scene.temporaryCharacters` | object[] | 临时角色数组。 |
| `scene.temporaryCharacterSummary` | string | 临时角色格式化文本。 |
| `scene.presentCharacters` | string | 出场人物字符串（逗号分隔）。 |
| `previousSection.content` | string | 上一节正文原文（若无则返回“无”）。 |
| `previousChapter.scenes` | object[] | 上一章大纲场景列表。 |
| `previousChapter.summary` | string | 上一章大纲格式化文本（若无则“无”）。 |
| `currentChapter.scenes` | object[] | 当前章全部场景大纲。 |
| `currentChapter.summary` | string | 当前章大纲格式化文本。 |
| `chapter.number` | number | 当前章号。 |
| `chapter.total` | number | 总章节数。 |
| `chapter.title` | string | 当前章标题。 |
| `log.latestByCharacter` | map | 角色 -> 最近成长日志摘要，供高阶模板使用。 |

#### 6.4.4 AI 优化模板
| 变量 | 类型 | 说明 |
| --- | --- | --- |
| `text` | string | 原始文本。 |
| `instruction` | string | 用户的优化指令（仅在 `withInstruction` 中有意义）。 |
| `contextType` | string | 文本类型，例如“角色介绍”。 |
| `context.note` | string | 若 `contextType` 非空，则为 `这是一个关于“${contextType}”的文本。\n`，否则为空。 |
| `request.raw` | object | `RefineRequest` 原始数据。 |

## 7. 模板渲染服务设计
### 7.1 `PromptTemplateService`
职责：
1. 读取默认模板；
2. 合并用户自定义模板；
3. 提供渲染接口，支持指定用户 / 默认模式。

核心方法：
- `PromptTemplates getEffectiveTemplates(Long userId)`：返回用户模板与 `isDefault` 信息。
- `void saveTemplates(Long userId, PromptTemplatesUpdateRequest req)`：持久化用户自定义值。
- `String render(PromptType type, Long userId, Map<String, Object> context)`：渲染并返回最终提示词。
- `String renderWithFallback(PromptType type, Map<String, Object> context)`：用于系统任务或匿名请求（如未登录）。

内部组件：
- `PromptTemplateRepository`：封装 `UserSetting` 的 JSON 字段读写。
- `TemplateEngine`：负责解析 `${...}`、执行函数、异常处理。
- `PromptContextFactory`：按类型构建 context map，保证字段命名与帮助文档一致。

### 7.2 上下文构建器
为每种 PromptType 提供专用 builder：
- `StoryPromptContextBuilder`：整合 `ConceptionRequest`，产出 `idea`、`genre`、`context.lines.*` 等字段。
- `OutlinePromptContextBuilder`：传入 `StoryCard`、`OutlineCard`、`GenerateChapterRequest`，封装角色列表、格式化字符串。
- `ManuscriptPromptContextBuilder`：收集 `OutlineScene`、历史章节、角色日志等，调用现有 `format*` 方法，避免重复代码。
- `RefinePromptContextBuilder`：按需构造 `context.note` 与原文、指令。

### 7.3 线程安全与性能
- 默认模板及函数注册在应用启动时初始化为不可变对象；
- 用户模板读取使用 `@Cacheable(value = "promptTemplates", key = "#userId")` 可选缓存；更新时通过 `@CacheEvict` 清理；
- 模板渲染过程中仅使用局部变量，无共享可变状态，确保线程安全。

## 8. 后端接口与业务流程改造
### 8.1 提示词管理接口
新增 `PromptTemplateController`：
- `GET /api/v1/prompt-templates`：返回 `PromptTemplatesResponse`（含 `content`、`isDefault`）。
- `PUT /api/v1/prompt-templates`：接收 `PromptTemplatesUpdateRequest`（允许部分字段），保存用户自定义模板。
- `POST /api/v1/prompt-templates/reset`：可选，支持按键（如 `storyCreation`）恢复默认。
- `GET /api/v1/prompt-templates/metadata`：返回第 6 章表格（用于前端帮助页）。

所有接口需要认证，基于 `@AuthenticationPrincipal` 获取用户 ID。

### 8.2 `SettingsController` 调整
- 保留原有 API Key / Base URL / Model 保存逻辑；
- `SettingsDto` 不再返回 `customPrompt`；
- 设置页前端在加载时并行请求 `GET /settings` 与 `GET /prompt-templates`。

### 8.3 业务调用链接入
- `AbstractAiService.generateConception`：改为 `promptTemplateService.render(PromptType.STORY_CONCEPTION, userId, context)`；`userId` 通过调用链传递。
- `OutlineService.generateChapterOutline`：调用 `render(PromptType.OUTLINE_CHAPTER, userId, context)`。
- `ManuscriptService.generateSection`（或相关方法）：同理调用 `MANUSCRIPT_SECTION` 模板。
- `AbstractAiService.refineText`：根据是否存在 instruction，选择 `REFINE_WITH_INSTRUCTION` 或 `REFINE_WITHOUT_INSTRUCTION`。
- 所有调用处需要补充用户 ID（若现有方法未携带，需从关联实体中获取 `userId`）。

### 8.4 回退与错误处理
- 渲染失败（语法错误、未知变量）时：
  1. 记录包含用户 ID、模板类型、错误原因的 warn 日志；
  2. 向调用方抛出业务异常，提示“当前提示词存在语法错误，已回退为系统默认模板”；
  3. 自动尝试默认模板渲染，若默认模板异常则终止流程。
- 提供后台运维脚本排查用户模板（可通过导出 JSON）。

## 9. 前端改造
### 9.1 路由与导航
- 在 `App.tsx` 中新增受保护路由：`/settings`（主设置页）、`/settings/prompt-guide`（提示词帮助页）。
- `WorkbenchHeader` 点击「设置」时使用 `useNavigate` 跳转到 `/settings`，取消旧弹窗；`UserSettingsModal` 将被移除或仅作为组件库保留但不再使用。

### 9.2 设置页信息架构
- `Settings.tsx` 改造为包含 `Tabs` 的全屏页面：
  1. **模型设置**：沿用现有表单（Base URL、模型、API Key）。
  2. **提示词配置**：
     - 展示 5 个文本域（故事构思、大纲章节、正文创作、AI 优化-带指令、AI 优化-无指令）。
     - 每个文本域下方显示“当前状态：默认 / 自定义”。
     - 提供“恢复默认”按钮（调用后端 reset 接口）。
     - 顶部展示说明文字与「查看提示词编写帮助」链接。
- 表单保存：
  - 提示词区域单独维护表单 state，点击「保存提示词」时向 `/prompt-templates` 发 PUT 请求。
  - 保存成功后刷新本地 `isDefault` 状态。

### 9.3 表单交互
- 加载阶段先调用 `GET /prompt-templates`，填充文本域；`isDefault` 字段用于禁用“恢复默认”按钮。
- 输入框支持自动高度、行号显示（可使用 `Input.TextArea` + `showCount`）。
- 保存前进行基本校验：非空提示、JSON 模板高亮（可选集成 Monaco / CodeMirror，短期内先保留文本域）。
- 在模板编辑区域提供「插值表达式提示」浮层，列出常用表达式（从 `metadata` 接口获取）。

### 9.4 提示词编写帮助页
- 新建 `PromptHelpPage.tsx`：
  - 展示插值语法说明、管道函数表、变量总表（使用 `metadata` 接口数据渲染表格）。
  - 提供示例片段（如 `${tags|join("，")}` 的使用效果）。
  - 支持复制默认模板按钮（方便用户恢复）。
- 设置页顶部添加链接 `<Link to="/settings/prompt-guide">`，并在帮助页提供返回设置的按钮。

### 9.5 异常提示
- 当后端返回模板语法错误信息时，在设置页以 `Alert` 展示，并保持用户输入不丢失。
- 保存成功后使用 `message.success`；恢复默认后提示“已恢复系统默认模板”。

## 10. 多用户隔离与权限控制
- 所有提示词 API 基于当前登录用户，后端通过 `userId` 过滤。
- `PromptTemplateService` 保存时将用户 ID 作为唯一键写入 `user_settings.prompt_settings`。
- 任何跨用户访问将触发 403（依赖现有 Spring Security 配置）。

## 11. 数据迁移与兼容性
1. 执行 SQL 迁移新增 `prompt_settings` 字段；
2. 若历史版本曾使用 `custom_prompt` 存储单个模板，可提供一次性脚本迁移至 `prompt_settings.storyCreation`；
3. 启动时若读取到旧 `custom_prompt` 但 `prompt_settings` 为空，可自动填充至故事构思模板并记录日志。

## 12. 测试计划
- **单元测试**：
  - TemplateEngine 表达式解析（含 `[*]`、函数链、错误分支）。
  - PromptTemplateService 合并逻辑（覆盖部分字段、自定义+默认混合）。
  - 上下文构建器字段正确性（断言关键字段不为空、场景数据匹配）。
- **集成测试**：
  - `/prompt-templates` GET/PUT/RESET 权限与数据正确性。
  - 故事 / 大纲 / 正文 / 优化接口调用自定义模板生效（可通过注入自定义模板，断言最终调用的 prompt 字符串包含特定片段）。
- **前端测试**：
  - Settings 页面交互（加载、编辑、保存、恢复默认）。
  - 帮助页路由与内容展示。
- **回归测试**：
  - 未自定义模板用户生成流程保持原行为；
  - 多用户并发修改模板互不影响。

## 13. 开发步骤
1. 新增默认模板配置文件及读取类；
2. 搭建模板插值引擎与上下文构建器，编写单元测试；
3. 扩展数据库字段、实体、Service；
4. 实现 `PromptTemplateController`、`PromptTemplateService`；
5. 重构故事 / 大纲 / 正文 / 优化业务逻辑，接入模板渲染；
6. 调整设置页前端、移除旧弹窗、接入新 API；
7. 编写提示词帮助页；
8. 完成端到端测试与文档更新。

---
本设计文档覆盖了模板配置、插值语法、后端服务、前端交互及多用户隔离等关键事项。如对插值变量或默认模板格式有补充需求，可在确认后再行调整。

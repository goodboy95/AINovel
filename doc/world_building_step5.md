# 世界构建 Step 5 —— 提示词配置与编写帮助

## 1. 目标

1. 将用户设置页面中的“提示词配置”改名为“工作台提示词配置”。
2. 新增“世界构建提示词配置”入口，允许用户自定义模块自动生成与完整信息生成提示词。
3. 支持重置为系统默认值、展示当前是否使用默认模板。
4. 提供“世界构建提示词编写帮助”页面/文档，说明可用变量、函数、示例。
5. 后端建立独立的世界构建提示词存储与渲染服务，避免与工作台模板互相干扰。

## 2. 后端设计

### 2.1 数据表 `world_prompt_settings`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | BIGINT PK |
| `user_id` | BIGINT UNIQUE | 用户 ID。 |
| `module_templates` | JSON | 模块自动生成模板 Map（key: 模块 key, value: string）。 |
| `final_templates` | JSON | 正式创建完整信息模板 Map。 |
| `field_refine_template` | TEXT | 可选，自定义字段优化骨架（默认使用 Step 1 版本）。 |
| `created_at` | DATETIME |
| `updated_at` | DATETIME |

> 若用户未配置某模块，使用默认模板；JSON 中仅保存差异部分。

### 2.2 默认模板资源

- 新建资源文件 `resources/prompts/world-defaults.yaml`：

```yaml
draftTemplates:
  cosmos: |
    # 与 Step 1 的“模块级 AI 自动生成提示词（默认）”一致
    # （请粘贴 Step 1 中 cosmos 模块的完整提示词原文）
  geography: |
    # （请粘贴 Step 1 中 geography 模块的完整提示词原文）
  # 依次覆盖其余模块：society/history/culture/economy/factions
finalTemplates:
  cosmos: |
    # 与 Step 1 的“完整信息生成提示词（默认）”一致
    # （请粘贴对应原文）
  # 依次覆盖其余模块
fieldRefineTemplate: |
  # Step 1 的字段级 AI 优化骨架全文
focusNotes:
  cosmos:
    cosmos_structure: "突出层级结构、空间形态与视觉符号。"
    space_time: "解释时间流速、边界、可否穿越及代价。"
    energy_system: "明确来源、使用条件、施法流程与代价。"
    technology_magic_balance: "比较科技与超自然的相互作用与冲突点。"
    constraints: "列出不可违背的规则，并说明违反后果。"
  # 其余模块同样列出所有字段 focusNote
```

- `focusNotes` 供 `WorldPromptTemplateService` 在渲染字段优化提示词时注入。

### 2.3 服务与控制器

新增 `WorldPromptTemplateService`：

```java
public class WorldPromptTemplateService {
    WorldPromptDefaults defaults; // 解析 YAML
    WorldPromptTemplateRepository repository;
    TemplateEngine templateEngine;

    public String renderDraftTemplate(String moduleKey, Map<String, Object> context, Long userId);
    public String renderFinalTemplate(String moduleKey, Map<String, Object> context, Long userId);
    public String renderFieldRefineTemplate(String moduleKey, String fieldKey, Map<String, Object> context, Long userId);

    public WorldPromptTemplatesResponse getEffectiveTemplates(Long userId);
    public void saveTemplates(Long userId, WorldPromptTemplatesUpdateRequest request);
    public void resetTemplates(Long userId, List<String> keys);
    public WorldPromptTemplateMetadataResponse getMetadata();
}
```

API：
- `GET /api/v1/world-prompts` —— 获取有效模板（默认 + 用户覆盖）。
- `PUT /api/v1/world-prompts` —— 保存用户模板。
- `POST /api/v1/world-prompts/reset` —— 按 key 重置。
- `GET /api/v1/world-prompts/metadata` —— 返回模块、字段列表、变量说明（供配置页与帮助页使用）。

DTO 结构：

```json
{
  "modules": {
    "cosmos": { "content": "...", "isDefault": false },
    "geography": { "content": "...", "isDefault": true },
    ...
  },
  "final": {
    "cosmos": { "content": "...", "isDefault": false },
    ...
  },
  "fieldRefine": { "content": "...", "isDefault": true }
}
```

### 2.4 模板元数据

`WorldPromptTemplateMetadataResponse` 内容：
- `modules`: 每个模块的 key、label、字段列表、推荐长度。
- `variables`: 表示上下文变量（详见 3.1）。
- `functions`: 可用的模板函数（复用现有 `join`, `upper`, `default`, `json` 等）。
- `examples`: 样例提示词片段。

### 2.5 安全与校验

- 保存模板时限制长度（如 ≤ 6000 字）。
- 对模板中注入的变量做运行期校验，若渲染出错回退默认模板并记录日志。

## 3. 世界构建提示词编写帮助

### 3.1 可用变量

| 变量 | 类型 | 描述 |
| --- | --- | --- |
| `${world.name}` | string | 世界名称。 |
| `${world.tagline}` | string | 一句话概述。 |
| `${world.themes}` / `${world.themes[*]}` | string[] | 主题标签。 |
| `${world.creativeIntent}` | string | 创作意图。 |
| `${module.key}` / `${module.label}` | string | 当前模块信息。 |
| `${module.fields.FIELD_KEY}` | string | 当前模块的字段原文。 |
| `${module.emptyFields}` | string[] | 当前仍为空的字段（模块自动生成时使用）。 |
| `${module.dirtyFields}` | string[] | 自上次发布以来被修改的字段。 |
| `${module.previousFullContent}` | string | 上一次完整信息（若存在）。 |
| `${relatedModules[*].label}` | string | 其他模块的名称。 |
| `${relatedModules[*].summary}` | string | 其他模块的摘要（完整信息或字段拼接）。 |
| `${helper.fieldDefinitions[*].label}` | string | 字段中文名称。 |
| `${helper.fieldDefinitions[*].recommendedLength}` | string | 推荐字数。 |

### 3.2 追加函数

沿用工作台函数并新增：
- `headline()`：将字符串转为醒目标题（首字母大写，移除多余空格）。
- `truncate(n)`：截断字符串并添加省略号。

### 3.3 模板撰写建议

1. **输出格式**：
   - 模块自动生成模板必须返回合法 JSON，对象的 key 与模块字段完全对应。
   - 正式创建模板产出 Markdown 兼容的纯文本（无 `#` 标题）。
2. **字数控制**：
   - 可以使用 `${helper.fieldDefinitions[*].recommendedLength}` 提醒模型写作长度。
3. **引用其他模块**：
   - 使用 `${relatedModules[*]|map(m -> "- " + m.label + "摘要：" + m.summary)|join("\n")}` 将其他模块的摘要拼接为列表。
4. **条件语句**：模板引擎支持 `default`/`trim` 等函数，可在缺失值时给出替代文本。
5. **调试**：
   - 使用 `${module|json()}` 输出上下文，以检查结构（仅测试时使用，正式模板需删除）。

### 3.4 示例片段

```
世界背景：
- 核心概念：${world.tagline}
- 主题标签：${world.themes[*]|join(" / ")}
${relatedModules[*]|map(m -> "- " + m.label + "摘要：" + m.summary)|join("\n")}

请严格输出以下 JSON：
{
  "cosmos_structure": "……",
  "space_time": "……"
}
```

### 3.5 帮助页面呈现

- 新建组件 `WorldPromptHelpPage`，在用户设置内链接到 `/settings/world-prompts/help`。
- 页面结构：
  1. 概述 + 注意事项。
  2. 变量表格（按模块/通用分类）。
  3. 模板函数说明。
  4. 示例片段（模块自动生成、完整信息、字段优化）。
  5. 常见问题（如“如何引用其他模块？”、“如何防止 JSON 解析失败？”）。

## 4. 前端设置页面改造

### 4.1 导航结构

- 将现有 `Settings` 页面拆分为两个 Tab：
  - `工作台提示词配置`（沿用现有逻辑）。
  - `世界构建提示词配置`。
- 在用户设置主菜单中保持“AI 模型设置”与“提示词设置”分区。

### 4.2 世界构建提示词配置 UI

```
Tabs: [模块自动生成, 正式创建, 字段优化]
└─ 模块自动生成（默认 Tab）
   ├─ Select[模块]（cosmos / geography / ...）
   ├─ Alert[提示：必须输出 JSON]
   ├─ TextArea[模板内容]
   ├─ Switch[恢复默认]
   ├─ Button[保存当前模块]
   └─ Link[查看编写帮助]
└─ 正式创建
   ├─ 同上，提示输出段落文本
└─ 字段优化
   ├─ TextArea（整体骨架，可选）
   └─ 提示：focusNote 仍由系统维护
```

- UI 参考现有 `Settings.tsx` 的结构，复用 `Form` + `Tabs`。
- 保存逻辑：
  - 切换模块时自动保存当前编辑内容或提示是否丢弃。
  - `保存` 按钮调用 `PUT /world-prompts`，参数示例：

```json
{
  "modules": {
    "cosmos": "..."
  }
}
```

- `恢复默认`：调用 `/world-prompts/reset`，并刷新内容。

### 4.3 提示词帮助入口

- 在每个 Tab 顶部放置 `QuestionCircleOutlined` 图标 + “编写帮助”链接。
- 链接跳转至 `/settings/world-prompts/help`。
- 帮助页面支持目录导航（`Anchor` 组件）。

## 5. 默认提示词落地

- 后端启动时，将 `world-defaults.yaml` 载入内存。
- 新增 `WorldPromptDefaults` Bean，提供 `getDraftTemplate(moduleKey)` 等方法。
- 在数据库迁移脚本中插入默认模板副本（可选，用于审计），或在 README 中说明默认模板来源。
- `WorldPromptTemplateService.getEffectiveTemplates` 返回 `isDefault` 字段，供前端展示“已使用默认模板”标签。

## 6. 与其他步骤的衔接

- Step 1 的默认提示词文本即本步骤的默认模板内容，两者需保持一致。建议将文本集中维护于 `world-defaults.yaml`，供 Step 1 文档引用（通过脚本或人工同步）。
- Step 3 的 AI 调用应改为使用 `WorldPromptTemplateService`，优先读取用户自定义模板。
- Step 6 的工作台提示词扩展需读取 `WorldPromptTemplateMetadataResponse` 中的变量说明，以生成帮助文档的一致内容。

## 7. 测试与验收

1. **后端单元测试**：模板保存/重置、渲染用户模板、回退默认模板。
2. **前端测试**：
   - 切换模块时的保存逻辑。
   - `isDefault` 标签是否正确显示。
   - 帮助页面内容可展开、变量表格渲染正确。
3. **集成测试**：修改模板后调用模块生成/正式创建，确认使用最新模板。

完成本阶段后，提示词配置体系与帮助文档齐备，为 Step 6 的工作台集成提供上下文变量说明与配置能力。

# Safe Output 最终答辩配图 Image Prompts

## 使用说明

本文档用于把最终答辩 PPT 大纲转化为后续 image 生图可用的提示词。所有提示词统一采用商务科技风：浅色背景、专业、清晰、适合竞赛答辩，并贴合 Safe Output 当前白底业务后台风格。

使用建议：

- 默认画幅使用 `16:9`，适合作为 PPT 页面主视觉、章节图或架构插图。
- 图片中不要依赖大量可读小字，关键标题和说明建议后续在 PPT 中手动叠加。
- 不生成真实手机号、身份证号、银行卡号、邮箱、完整日志或原始 response。
- 敏感内容只使用类型标签，例如 `MOBILE`、`ID_CARD`、`EMAIL`、`PASSWORD`。
- 所有报告、Dashboard、规则建议相关画面只表达聚合指标和脱敏 evidence，不展示敏感原文。

统一负面提示词：

```text
真实手机号，真实身份证号，真实银行卡号，真实邮箱，完整日志内容，原始 response，敏感原文，密集小字，无法阅读的代码墙，暗黑驾驶舱，大面积霓虹渐变，赛博朋克风，卡通风，夸张 3D 角色，真实人物肖像，杂乱背景，低清晰度，水印，logo 侵权
```

## 01. 封面：Safe Output 项目定位

用途：PPT 第 1 页封面，明确项目名称、赛道、问题和一句话价值。

中文提示词：

```text
为竞赛答辩 PPT 生成一张商务科技风封面图，主题是 Safe Output，一个面向 Java 8 / Spring Boot 2.x 存量系统的输出侧敏感数据治理 starter。画面中心是一个干净的企业级 Java 应用模块，外部有三条输出通道分别指向 Response、Log4j2、Manual Masking，并被一个半透明安全护盾统一保护。右侧以简洁图形表达聚合报告和治理 Dashboard。整体为浅色背景，蓝色、青色、绿色作为语义色，风格专业、克制、清晰，适合 16:9 PPT 首页。不要出现真实敏感数据，只出现 MOBILE、ID_CARD、EMAIL、PASSWORD 等类型标签。文字尽量少，项目标题和副标题留给 PPT 后期叠加。
```

画面元素：

- 中心：Java 业务系统模块。
- 输出通道：Response、Log4j2、Manual Masking。
- 右侧：Report、Dashboard、Rule Suggestions。
- 视觉符号：安全护盾、聚合指标、轻量连线。

风格约束：

- 商务科技风，浅色背景。
- 蓝、青、绿为主，少量橙色用于风险提示。
- 不要暗黑驾驶舱，不要真实个人信息。

建议比例：`16:9`

## 02. 问题背景：老系统敏感输出风险

用途：PPT 第 2 页，说明敏感信息不只在数据库里，也会通过多条输出路径离开应用。

中文提示词：

```text
生成一张浅色商务信息图，表现 Java 8 / Spring Boot 2.x 老业务系统中的敏感输出风险。画面左侧是一个标注为 Legacy Java App 的企业应用框，右侧分出四条输出路径：API Response、Application Logs、Export Reports、Downstream Calls。每条路径旁用小标签表示 MOBILE、ID_CARD、EMAIL、ADDRESS、PASSWORD 等敏感类型。路径末端有风险提示图标，但不要制造恐怖氛围。整体布局清晰，适合竞赛答辩 PPT，风格专业、克制、现代。不要出现真实数据，只用字段类型标签和抽象图标。
```

画面元素：

- 左侧：存量 Java 系统。
- 右侧：接口响应、日志、报告、下游调用四条路径。
- 风险提示：人工遗漏、分散脱敏、日志盲区、报告泄露源。

风格约束：

- 类似企业咨询信息图。
- 风险用橙色点到为止。
- 避免恐吓式安全海报。

建议比例：`16:9`

## 03. 产品方案：输出侧治理闭环

用途：PPT 第 3 页，展示 Safe Output 的 Response、Log、Manual、Report 闭环。

中文提示词：

```text
生成一张商务科技风流程图，展示 Safe Output 输出侧敏感数据治理闭环。画面从左到右依次是 Business App、safe-output-spring-boot-starter、ResponseBodyAdvice、Log4j2 %safeOutputMsg、SafeOutputMaskService、MaskMetricsCollector、JSON Aggregate Report、Governance Dashboard、Rule Suggestions。用清晰的箭头连接，形成从业务输出到脱敏、统计、报告、规则建议的闭环。底部用三个简洁图标表达 fail-open、no raw sensitive data、manual review for suggestions。浅色背景，蓝色和青色为主，绿色表示安全聚合，橙色表示风险。不要出现大量文字，保留空间给 PPT 后期叠加说明。
```

画面元素：

- 主链路：业务系统 -> starter -> 三类脱敏入口 -> 聚合统计 -> 报告/Dashboard。
- 底部边界：fail-open、不保存敏感原文、建议人工复核。

风格约束：

- 结构优先，图形清晰。
- 可读性高，避免复杂网状连线。

建议比例：`16:9`

## 04. 低侵入接入：Starter + YAML + 注解 + Ignore

用途：PPT 第 4 页，突出老系统低成本接入方式。

中文提示词：

```text
生成一张浅色企业级技术方案图，主题是低侵入接入 Safe Output。画面中心是一个老业务系统应用，周围四个整洁模块分别代表 Maven Starter Dependency、YAML Rules、@Desensitize Annotation、Ignore Policy。每个模块用抽象代码卡片形式表达，但不要生成大段真实代码。底部用一条简洁流程表示：import starter -> configure rules -> run application -> masked response and logs。整体是商务科技风，白底、细边框、蓝青绿色语义色，适合 PPT 技术方案页。不要出现真实敏感样本。
```

画面元素：

- 四个接入入口：starter、YAML、注解、ignore。
- 老系统应用作为中心。
- 输出结果：masked response、masked logs、risk metrics。

风格约束：

- 类似产品架构说明页。
- 代码卡片只展示抽象符号，不展示敏感值。

建议比例：`16:9`

## 05. 架构拆分：Maven 多模块边界

用途：PPT 第 5 页，展示模块职责清晰、Demo 和 Dashboard 不污染核心链路。

中文提示词：

```text
生成一张商务科技风 Maven 多模块架构图，展示 Safe Output 的模块边界。画面采用分层布局：底层是 safe-output-core，包含 rules、strategies、object masking；中间是 safe-output-spring-boot-starter、safe-output-log4j2、safe-output-report；右侧是 optional safe-output-dashboard-spring-boot-starter；最外侧是 safe-output-demo 作为样板应用。用清晰的依赖箭头表示 starter 组合 core、log4j2、report，dashboard 是可选附加包，demo 只用于展示和验证。浅色背景，专业技术架构图风格，蓝色表示核心能力，绿色表示治理视图，灰色表示 demo 样板。不要出现复杂小字。
```

画面元素：

- `core` 为底座。
- `starter`、`log4j2`、`report` 为中层。
- `dashboard starter` 为可选附加包。
- `demo` 为外部样板应用。

风格约束：

- 清晰模块边界。
- 避免过多类名和源码细节。

建议比例：`16:9`

## 06. 安全边界：不保存敏感原文与 Fail-open

用途：PPT 第 6 页，强调安全类组件的红线和治理边界。

中文提示词：

```text
生成一张商务科技风安全边界图，主题是 Safe Output 的安全设计。画面中心是一个安全治理组件，左侧输入为 Response、Log Message、Manual Input，右侧输出为 Masked Output、Aggregate Metrics、Risk Profile、Rule Suggestion。组件内部有一个明确标识的 No Raw Sensitive Data Vault，表示不会保存原始敏感数据；旁边有 fail-open fallback path，表示异常时不阻断业务主链路。画面底部以简洁图标表达 no full logs stored、no raw response stored、suggestions disabled by default、dashboard optional and protected by user。整体浅色、专业、可信，风险提示使用少量橙色。
```

画面元素：

- 输入：Response、Log、Manual。
- 输出：脱敏结果、聚合指标、风险画像、候选建议。
- 安全红线：不存敏感原文、fail-open、建议默认关闭、Dashboard 默认关闭。

风格约束：

- 可信、克制，不做夸张安全攻击场景。
- 不出现真实数据样本。

建议比例：`16:9`

## 07. AI 研发过程：PRD 到 Memory 的闭环

用途：PPT 第 7 页，说明 AI 作为研发主力的工程化过程。

中文提示词：

```text
生成一张商务科技风 AI 研发流程图，展示 Safe Output 的 AI 原生研发闭环。画面从左到右依次是 Requirement Clarification、PRD、Local Markdown Issues、Implementation、Tests、Docs、.codex-memory Handoff。每个节点用简洁文件夹或文档图标表示，AI agent 以抽象发光节点或协作助手符号贯穿流程，但不要使用真实人物。流程底部展示四类证据：PRD files、Issue tracker、Test cases、Memory documents。整体是专业研发管理图，浅色背景，蓝青绿色调，适合竞赛答辩说明 AI 如何持续参与工程交付。
```

画面元素：

- 主流程：需求澄清 -> PRD -> Issue -> 实现 -> 测试 -> 文档 -> Memory。
- 证据链：PRD、issues、tests、memory。

风格约束：

- 抽象 AI 协作，不用机器人卡通形象。
- 不要暗黑、科幻过度。

建议比例：`16:9`

## 08. 质量验证：模块级与联调覆盖矩阵

用途：PPT 第 8 页，展示质量验证不是只停留在单元测试。

中文提示词：

```text
生成一张浅色商务测试验证矩阵图，展示 Safe Output 的质量验证结构。画面横向是模块：core、starter、log4j2、report、dashboard starter、demo；纵向是验证类型：unit tests、MockMvc integration、random port HTTP tests、security boundary checks、report safety checks。用勾选标记和浅色网格表示覆盖关系。右侧有一个小的 CI terminal card 显示 mvn test BUILD SUCCESS，但不要写过多日志。底部用图标强调 fail-open verified、API ignore risk counted、no raw sensitive data in reports。整体专业、清晰、白底、蓝绿色语义色。
```

画面元素：

- 测试矩阵。
- `mvn test BUILD SUCCESS` 简洁终端卡片。
- 安全边界验收图标。

风格约束：

- 矩阵可视化，不要真实长日志。
- 避免展示 `mvn verify` 通过，因为当前材料中 verify 曾因 Windows 临时目录清理失败。

建议比例：`16:9`

## 09. Demo 路径：端到端业务样板系统

用途：PPT 第 9 页，展示 Demo 演示路线，服务现场答辩。

中文提示词：

```text
生成一张商务科技风 Demo 演示路径图，展示 Safe Output 端到端演示路线。画面像一个产品旅程地图，依次经过 Workbench Overview、Customer Profile、Raw API Ignore Risk、Log Scenario、Masking Lab、Governance Dashboard、Historical Report。每个站点用简洁浏览器窗口缩略图表示，界面风格为白底企业后台，细边框、蓝青绿色图表。Raw API Ignore 站点只用 Demo Mock Data 和 Risk Count 标签表达，不展示任何明文值。Dashboard 站点展示聚合图表和规则建议，不展示原始日志。整体适合 PPT 讲解 3 到 5 分钟演示路径。
```

画面元素：

- 演示路径：总览 -> 客户详情 -> ignore 风险 -> 日志 -> 实验室 -> Dashboard -> 历史报告。
- 浏览器缩略图风格。
- 聚合数据与脱敏结果作为视觉重点。

风格约束：

- 贴合现有白底业务后台。
- 不展示真实敏感字段值。

建议比例：`16:9`

## 10. 成熟度与后续方向：可演示、可集成验证、可扩展

用途：PPT 第 10 页收尾，说明当前成熟度和后续方向。

中文提示词：

```text
生成一张商务科技风路线图，主题是 Safe Output 当前成熟度与后续扩展方向。画面左侧是 Current MVP/R2 Ready，包含 Response Masking、Log4j2 Masking、Manual Masking、Aggregate Report、Optional Dashboard、Demo Workbench 六个已完成能力节点。右侧是 Next Extensions，包含 AI Risk Summary、AI Configuration Suggestions、Suggestion Review Flow、More Legacy System Templates、Log Length Strategy。中间用稳健的演进箭头连接，底部用小字区域表达 current boundary：Java 8 / Spring Boot 2.x verified, no raw sensitive data stored, dashboard optional. 整体浅色、专业、克制，适合答辩结尾页。
```

画面元素：

- 左侧：当前成熟能力。
- 右侧：后续扩展方向。
- 底部：关键边界。

风格约束：

- 路线图简洁，不要承诺未实现能力已经完成。
- 后续方向用轻量虚线或淡色节点表达。

建议比例：`16:9`


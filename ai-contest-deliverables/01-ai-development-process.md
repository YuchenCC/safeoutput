# Safe Output AI 研发过程说明

## 1. 文档定位

本文用于说明 Safe Output 的 AI 研发过程，而不是重复介绍最终组件能力。项目代码能力总述见 `ai-contest-deliverables/00-submission-overview.md`；本文重点回答：

- AI 如何参与需求澄清、PRD 编写、WBS 拆解和 issue 化。
- AI 如何在 Java 8 / Spring Boot 2.x / Log4j2 / 报告安全边界下推进实现。
- AI 如何通过测试、文档和 `.codex-memory/` 交接记忆保持多轮迭代一致。
- 哪些材料是研发过程证据，哪些材料是赛后整理总结。

本文不编造聊天记录、提交记录、人力节省数值或未运行的测试结果。所有阶段描述均以本仓库内 PRD、issue、验收清单、项目记忆和现有交付文档为依据。

## 2. AI 参与方式总览

Safe Output 的 AI 参与方式不是“让模型一次性生成完整项目”，而是围绕可追踪工程流程持续推进：

| 环节 | AI 参与方式 | 主要产物 | 可追踪证据 |
|---|---|---|---|
| 需求澄清 | 把用户目标拆成定位、范围、非目标、安全边界和验收口径 | PRD、澄清说明、变更记录 | `doc/prd/safe-output-*.md` |
| WBS 与 issue 化 | 把 PRD 拆成可独立验收的纵向切片 | 本地 Markdown issue | `.scratch/safe-output-mvp/*.md` |
| 实现辅助 | 根据 issue、PRD、上下文文档定位模块并完成最小切片 | core、starter、log4j2、report、demo、dashboard starter | `safe-output/` |
| 测试补齐 | 根据新增能力补单元测试或集成测试，保持可回归 | 各模块 `src/test/java` | `safe-output/**/src/test/java` |
| 文档同步 | 更新 README、接入说明、设计边界和参赛材料 | README、组件接入手册、交付物 | `safe-output/README.md`、`doc/`、`ai-contest-deliverables/` |
| 交接记忆 | 把跨轮次状态、模块地图、调用链和边界沉淀给后续 AI | `.codex-memory/00-04` | `.codex-memory/*.md` |

这个过程吸收了社区中围绕 agent skills 的最佳实践：把大型工程任务拆成小而可组合的技能和工作流，而不是依赖一个巨型提示词。Matt Pocock 的 `mattpocock/skills` 将这类实践概括为：先通过提问对齐目标，再用共享领域语言、issue tracker、triage labels、测试反馈和架构复盘约束 agent 行为。Safe Output 没有把这些社区 skill 作为项目运行依赖，但在工作流设计上采用了相同原则：先对齐、再拆解、再实现、再验证、最后沉淀记忆。

## 3. 章节实现方法：Skill、工作流与提示词

本节说明 Safe Output 过程材料中每一类章节是如何由 AI 辅助实现的。这里的 “skill” 指可复用的 agent 工作流，不表示项目代码依赖了第三方 skill 包。

### 3.1 需求澄清章节

需求澄清类材料包括：

- `doc/prd/safe-output-requirements-clarifications.md`
- `doc/prd/safe-output-requirements-change-log.md`
- 各轮 PRD 的 Problem Statement、Solution、Out of Scope

采用的工作流类似 `grill-with-docs` / `grill-me`：

1. 先不写代码，要求 AI 读取已有 PRD、`CONTEXT.md` 和项目约束。
2. 让 AI 追问会影响范围、边界、验收和安全策略的问题。
3. 把确认后的结论沉淀为澄清条目和变更记录。
4. 后续实现时用这些结论反向约束代码，不允许临时放宽边界。

典型提示词模板：

```text
请先不要实现代码。基于当前 PRD 和 CONTEXT.md，列出会影响范围、
安全边界、验收标准和模块职责的关键澄清问题。只问无法从现有文档
推断的问题，并给出推荐默认结论。
```

在 Safe Output 中，这一流程形成了多个关键结论：Response 只处理普通 JSON 对象结构；Log4j2 不引入完整 JSON Parser；报告不保存敏感原文；接口级 ignore 返回明文但必须进入风险统计；Java 8 / Spring Boot 2.x 是兼容性基线。

### 3.2 PRD 章节

PRD 类材料包括：

- `doc/prd/safe-output-mvp-prd.md`
- `doc/prd/safe-output-r2-prd.md`
- `doc/prd/safe-output-r25-prd.md`
- `doc/prd/safe-output-r26-dashboard-prd.md`
- `doc/prd/safe-output-r3-prd.md`

采用的工作流类似 `to-prd`：

1. 把口头目标或上一轮反馈整理成 Problem Statement。
2. 明确 Solution，但同时写清楚 Out of Scope。
3. 用 User Stories 描述使用者为什么需要该能力。
4. 用 Implementation Decisions 限定技术方案和模块边界。
5. 用 Testing Decisions 提前规定验收方式。

典型提示词模板：

```text
请把已确认需求整理成 PRD。结构必须包含 Problem Statement、
Solution、User Stories、Implementation Decisions、Testing Decisions
和 Out of Scope。不要写实现代码，不要把未确认能力写成已实现能力。
```

Safe Output 的 PRD 不是一次性冻结，而是按轮次演进：MVP 先证明低侵入 starter、Response 和 Log4j2；R2 增强 String 类型标签、主动脱敏和治理报告；R2.5 把 Demo 改造成真实接入样板系统；R2.6 抽离可选 Dashboard starter；R3 聚焦日志长度策略增强。

### 3.3 Issue 拆解章节

Issue 拆解材料位于 `.scratch/safe-output-mvp/`。这些 issue 使用本地 Markdown 作为 issue tracker，符合 `docs/agents/issue-tracker.md` 中的约定。

采用的工作流类似 `to-issues`：

1. 先读取 PRD 的 WBS、Implementation Decisions 和 Testing Decisions。
2. 按纵向切片拆分，而不是按技术层水平拆分。
3. 每个 issue 必须能独立描述 “What to build”。
4. 每个 issue 必须包含 acceptance criteria 和 test requirements。
5. 通过 `blocked_by` 串联依赖，避免 AI 乱序实现。

典型提示词模板：

```text
请把这份 PRD 拆成本地 Markdown issue。每个 issue 必须是纵向可验收切片，
包含 What to build、Acceptance criteria、Test requirements 和 blocked_by。
不要生成无法独立验证的大包 issue。
```

例如 MVP 的 `0001-0020` 覆盖 Maven 多模块、核心策略、规则匹配、Response、Log4j2、报告、Demo 和打包验证；R2 的 `0021-0039` 覆盖姓名策略、身份证分层识别、String type、自定义策略、主动脱敏、风险画像和日志规则建议；R2.6 的 `0055-0065` 覆盖 Dashboard starter、POST API、报告安全读取、上传临时查看和脱敏实验室。

### 3.4 Triage 与状态推进章节

本项目的 issue 使用 `category`、`state`、`source`、`wbs`、`blocked_by` 等元数据管理状态。状态口径由 `docs/agents/triage-labels.md` 约束。

采用的工作流类似 `triage`：

1. 新 issue 默认进入 `ready-for-agent` 或可执行状态。
2. 存在依赖时通过 `blocked_by` 表达顺序。
3. 完成后记录 completion notes 或勾选 acceptance criteria。
4. 无法由 AI 独立判断时标记为需要人工确认，而不是编造结论。

典型提示词模板：

```text
请按项目 triage 标签检查这些 issue：确认 source 是否明确、blocked_by 是否合理、
acceptance criteria 是否可验证。不要开始实现，只输出需要补充或可以进入 agent
执行的 issue 列表。
```

这个过程让 AI 的工作从“自由生成”变成“按 issue 状态机执行”。`.scratch/safe-output-mvp/ACCEPTANCE_CHECKLIST.md` 后续又把 issue 验收结果汇总成可复查清单。

### 3.5 实现章节

实现阶段采用小切片执行方式，接近 `do-work` 类工程工作流：

1. 先读取 `.codex-memory/`、PRD、issue 和相关模块源码。
2. 根据 issue 只修改当前切片所需模块。
3. 优先沿用现有模型：`MaskStrategy`、`MaskRuleMatcher`、`MaskMetricsCollector`、`ResponseBodyAdvice`、`PatternConverter`。
4. 不引入新依赖解决小问题，不突破 Java 8 / Spring Boot 2.x 基线。
5. 新能力必须补测试；安全边界变化必须同步文档和记忆。

典型提示词模板：

```text
请实现当前 issue。开始前必须读取 .codex-memory 和相关源码；
实现时只做当前 issue 的最小闭环；新增脱敏能力必须补测试；
不得保存敏感原文，不得改变 fail-open 边界。
```

这种模式体现在项目模块边界上：`safe-output-core` 承载规则和策略，`safe-output-spring-boot-starter` 承载自动装配和 Response 接入，`safe-output-log4j2` 承载日志脱敏，`safe-output-report` 承载聚合报告，Dashboard 作为可选 starter 抽离，不混入 demo mock 业务。

### 3.6 TDD、诊断与回归章节

测试和诊断阶段参考 `tdd` 与 `diagnose` 工作流：

1. 对新增能力先明确外部可观察行为。
2. 高风险链路补单元测试或集成测试。
3. Bug 或边界问题按“复现 -> 定位 -> 修复 -> 回归”推进。
4. 测试只验证可观察行为，避免过度绑定私有实现。

典型提示词模板：

```text
请为这个能力补测试。先列出用户可观察行为，再写最小测试覆盖；
不要只测试 mock 或内部私有方法；测试必须覆盖安全边界和 fail-open 行为。
```

Safe Output 的测试覆盖分布在各模块：

- core：策略、规则匹配、注解解析、对象递归、策略注册。
- log4j2：PatternConverter、JSON-like/key-value、regex fallback 和边界。
- report：聚合模型、风险等级、JSON 快照、保留数量和失败处理。
- starter：属性绑定、自动装配、Response advice、API ignore、Log4j2 bridge。
- dashboard starter：默认关闭、POST API、报告安全读取、临时上传和实验室。
- demo：Response、Log、Manual、Report 和业务工作台集成场景。

### 3.7 架构复盘章节

架构复盘类材料包括：

- `CONTEXT.md`
- `.codex-memory/01-module-map.md`
- `.codex-memory/02-core-flow-map.md`
- `.codex-memory/03-decision-and-boundary.md`
- `ai-contest-deliverables/issues/0004-architecture-decisions.md`

采用的工作流类似 `improve-codebase-architecture` 和 `grill-with-docs`：

1. 先建立共享领域语言，避免 AI 在 “规则”“策略”“类型”“ignore”“报告” 等词上漂移。
2. 对每轮实现后形成的模块职责做复盘。
3. 把核心调用链写成文字地图，供后续 agent 快速定位。
4. 把安全边界写成禁止项和设计决策，避免后续迭代反复推翻。

典型提示词模板：

```text
请基于当前代码和 CONTEXT.md 复盘模块边界。输出每个模块职责、
关键入口、核心调用链、已知风险和后续不建议大改的位置。
不要提出无关重构，只记录会影响后续 agent 实现判断的信息。
```

这类文档直接降低了后续 AI 误改风险。例如 `.codex-memory/03-decision-and-boundary.md` 明确：不引入 fastjson，不做粗暴全局正则，报告不保存敏感原文，Dashboard 默认关闭且不提供权限系统。

### 3.8 文档同步与交接记忆章节

交接记忆位于 `.codex-memory/`，是 Safe Output 多轮 AI 协作的关键机制：

- `00-project-current-state.md`：当前能力、未实现能力、风险和成熟度。
- `01-module-map.md`：模块职责、核心类、入口和依赖关系。
- `02-core-flow-map.md`：Response、Log、配置、统计、Dashboard、ignore、异常兜底调用链。
- `03-decision-and-boundary.md`：架构决策、安全边界和扩展边界。
- `04-next-round-handoff.md`：下一轮适合切入的位置、风险和必读文件。

采用的工作流类似 agent memory / handoff：

1. 每轮实现完成后检查是否改变模块职责、核心调用链、Demo 能力、运行命令或测试命令。
2. 如有变化，同步更新对应 memory 文件。
3. 后续编码前先读 memory，避免只凭当前 prompt 猜测历史决策。

典型提示词模板：

```text
请对照本轮改动检查 .codex-memory 是否需要更新。只有当模块职责、
核心调用链、设计边界、运行命令、测试命令、Demo 能力或交接信息变化时才修改。
更新时只写可验证事实，不写计划口号。
```

这也是安全类项目中最重要的 AI 约束之一：让模型每轮都从真实项目状态出发，而不是从通用脱敏组件想象出发。

## 4. 阶段推进时间线

| 阶段 | 目标 | AI 主要参与活动 | 过程证据 |
|---|---|---|---|
| MVP | 建立 Java 8 / Spring Boot 2.x 低侵入 starter，覆盖 Response、Log4j2、基础报告和 Demo | 编写 MVP PRD、拆 WBS、生成 `0001-0020` issue、辅助实现多模块和测试 | `doc/prd/safe-output-mvp-prd.md`、`.scratch/safe-output-mvp/0001-0020*.md` |
| R2 | 增强真实业务适配和治理能力 | 澄清 String type、自定义策略、主动脱敏、Response 风险画像、Log 规则建议；拆 `0021-0039` issue | `doc/prd/safe-output-r2-prd.md`、`.scratch/safe-output-mvp/0021-0039*.md` |
| R2.5 | 把 Demo 从功能控制台升级为真实接入样板系统 | 设计客户、订单、支付、工单、账户 mock 业务域；重构前端工作台和报告中心 | `doc/prd/safe-output-r25-prd.md`、`.scratch/safe-output-mvp/0047-0054*.md` |
| R2.6 | 抽离通用治理 Dashboard 为可选 starter | 明确 Demo 与 Dashboard 边界；设计默认关闭、POST API、报告安全读取和临时上传 | `doc/prd/safe-output-r26-dashboard-prd.md`、`.scratch/safe-output-mvp/0055-0065*.md` |
| R3 | 继续收敛日志长度策略和后续展示增强 | 编写 R3 PRD，记录下一轮日志 `maxMessageLength` / `max-scan-length` 策略和交接重点 | `doc/prd/safe-output-r3-prd.md`、`.codex-memory/04-next-round-handoff.md` |

## 5. 过程证据链

Safe Output 的过程证据分为四类：

| 证据类型 | 文件 | 证明内容 |
|---|---|---|
| PRD 与需求演进 | `doc/prd/safe-output-*.md` | 每一轮为什么做、做什么、不做什么、怎么验收 |
| 本地 issue | `.scratch/safe-output-mvp/*.md` | AI 如何把 PRD 拆成可执行、可验收、可依赖排序的工作项 |
| 验收与测试记录 | `.scratch/safe-output-mvp/ACCEPTANCE_CHECKLIST.md`、各模块测试目录 | issue 验收标准如何被测试和人工核查承接 |
| 项目记忆 | `.codex-memory/*.md` | 跨轮次上下文、模块边界、调用链、安全决策和下一轮交接 |

参赛交付材料属于赛后整理总结：

- `ai-contest-deliverables/00-submission-overview.md`：面向评审的项目总述。
- `ai-contest-deliverables/01-ai-development-process.md`：本文，面向评审的 AI 研发过程说明。
- `ai-contest-deliverables/issues/*.md`：参赛材料自身的整理任务清单。

也就是说，PRD、issue、测试和 memory 是研发过程证据；`ai-contest-deliverables/` 是基于证据整理出的参赛叙事材料。

## 6. 安全类项目中的 AI 约束

Safe Output 是安全治理类组件，因此 AI 参与研发时不能只追求“看起来能跑”。项目通过文档、issue、测试和记忆持续约束以下边界：

- 不保存敏感原文、完整 response 或完整日志。
- Response 脱敏异常必须 fail-open，不影响业务接口。
- 日志只做轻量 JSON-like / key-value 识别和有限 regex fallback，不引入 fastjson。
- 银行卡不做无上下文全局兜底。
- 接口级 ignore 可以返回明文，但必须进入风险统计。
- Dashboard 是可选附加包，默认关闭，不提供权限、审计、数据库或公网防护。
- Java 8 / Spring Boot 2.x 是主要兼容性基线，不使用 Boot 3 专属自动装配作为唯一入口。
- Log 规则建议只输出候选配置，默认关闭，需要人工复核，不自动改写 YAML。

这些约束不是只写在 README 里，而是重复出现在 PRD、澄清说明、issue 验收标准、测试和 `.codex-memory/03-decision-and-boundary.md` 中。这样做的目的，是让后续 AI 在实现新需求时也能优先看到边界，而不是为了演示效果临时扩大能力。

## 7. 赛后整理与真实过程边界

本文是赛后整理材料，因此只做归纳，不把整理过程包装成不存在的事实：

- 不声称项目真实安装或运行了 `mattpocock/skills`，只说明方法论上参考了小型可组合 skill、共享领域语言、issue tracker、triage、TDD 和架构复盘等社区实践。
- 不补写虚构聊天记录。提示词只作为模板片段，说明当时使用的工作方式。
- 不补写虚构 commit 历史。项目过程以 PRD、issue、测试和 memory 为准。
- 不补写人力节省比例。参赛价值体现在过程可追踪、交付完整和边界可验证，而不是不可验证的效率数字。
- 不把未实现能力写成已实现能力。Boot 3、集中式治理平台、权限系统、数据库落库、规则自动采纳等仍是边界外能力。

## 8. 可复查清单

评审或后续维护者可以按下面路径复查本文结论：

- 项目总述：`ai-contest-deliverables/00-submission-overview.md`
- 本文来源 issue：`ai-contest-deliverables/issues/0002-ai-development-process.md`
- 需求演进：`doc/prd/safe-output-mvp-prd.md`、`doc/prd/safe-output-r2-prd.md`、`doc/prd/safe-output-r25-prd.md`、`doc/prd/safe-output-r26-dashboard-prd.md`、`doc/prd/safe-output-r3-prd.md`
- 需求澄清与变更：`doc/prd/safe-output-requirements-clarifications.md`、`doc/prd/safe-output-requirements-change-log.md`
- 本地 issue：`.scratch/safe-output-mvp/`
- 验收清单：`.scratch/safe-output-mvp/ACCEPTANCE_CHECKLIST.md`
- 项目领域语言：`CONTEXT.md`
- AI 交接记忆：`.codex-memory/00-project-current-state.md` 至 `.codex-memory/04-next-round-handoff.md`
- 组件现状说明：`safe-output/README.md`

社区方法论参考：

- `mattpocock/skills`：https://github.com/mattpocock/skills
- `setup-matt-pocock-skills` 工作流说明：https://github.com/mattpocock/skills/blob/main/skills/engineering/setup-matt-pocock-skills/SKILL.md

# Safe Output 需求演进说明

## 1. 文档定位

本文整理 Safe Output 从 MVP 到 R3 的需求演进路径，说明项目如何在 AI 协作下从“低侵入输出侧脱敏 starter”逐步扩展为包含 Response、Log4j2、主动脱敏、聚合报告、规则建议、Demo 样板系统和可选 Dashboard 的完整治理闭环。

本文只描述可追溯的需求变化，不把历史设想直接等同为当前能力。当前实现现状以 `.codex-memory/00-project-current-state.md` 和相关代码为准；历史需求来源以 `doc/prd/safe-output-*.md`、需求变更记录和澄清说明为准。

## 2. 演进总览

| 阶段 | 主要来源 | 需求目标 | 核心变化 |
|---|---|---|---|
| MVP | `safe-output-mvp-prd.md` | 建立 Java 8 / Spring Boot 2.x 低侵入脱敏 starter | 覆盖 Response、Log4j2、配置规则、注解、ignore、基础统计报告和 Demo |
| R2 | `safe-output-r2-prd.md` | 从基础脱敏升级到可扩展和可治理 | String type、自定义策略、主动脱敏、Response 风险画像、Log 规则建议 |
| R2.5 | `safe-output-r25-prd.md` | 把 Demo 从功能验证控制台升级为接入样板系统 | 增加业务域、工作台、实验室、报告展示和真实 Log4j2 聚合路径 |
| R2.5 补充 | `safe-output-r25-supplemental-prd.md` | 固化 Demo 实现后的最终口径 | 白底后台风格、工作台总览、日志只读聚合、固定两轮实验室 |
| R2.6 | `safe-output-r26-dashboard-prd.md` | 抽离通用治理 Dashboard | 新增可选 `safe-output-dashboard-spring-boot-starter`，默认关闭，API 全 POST |
| R3 | `safe-output-r3-prd.md` | 强化日志长度策略并保留展示增强方向 | 日志超长整条跳过与前缀扫描窗口两种策略；Demo 看板作为待排方向 |

## 3. MVP：低侵入输出侧脱敏 starter

MVP 的核心目标是证明 Safe Output 能作为一个 Java 8 / Spring Boot 2.x starter 被老项目低成本接入。业务系统通过引入 `safe-output-spring-boot-starter`、增加少量 YAML 配置和 Log4j2 pattern 配置，即可覆盖接口 response 与日志输出两个高风险输出场景。

MVP 的主要需求包括：

- 提供 Maven 多模块工程和对外 starter Jar，内部聚合 core、log4j2、report 等模块。
- 通过 Spring MVC `ResponseBodyAdvice` 在 JSON 序列化前处理 Bean、Map、Collection、Array 和嵌套对象。
- 支持常见内置类型：`MOBILE`、`ID_CARD`、`BANK_CARD`、`EMAIL`、`CHINESE_NAME`、`ADDRESS`、`PASSWORD`、`DEFAULT`。
- 支持 YAML 配置规则、默认规则库、字段注解 `@Desensitize`、字段级 ignore 和接口级 ignore。
- 支持 Log4j2 `%safeOutputMsg` 日志脱敏，采用轻量 JSON-like/key-value 识别和有限 regex fallback。
- 提供基础统计报告和 Response 接口风险统计，报告只保存聚合信息。
- 提供 Demo 用于验证 response、log、ignore 和基础统计报告。

MVP 的边界同样明确：不做完整 DLP 平台，不做数据库落库、配置中心热更新、权限动态脱敏、MyBatis 自动脱敏、Logback 实现、完整 JSON Parser 日志解析，也不保存敏感原文、完整 response 或完整日志。

## 4. R2：从基础脱敏到治理辅助

R2 的需求来源于 MVP 编码、验证和试点后的反馈。它没有改变“低侵入输出侧脱敏组件”的定位，但把组件从“能脱敏”推进到“可扩展、可主动复用、可生成治理建议”。

MVP 到 R2 的核心变化包括：

- 脱敏类型标签从枚举强绑定调整为 String，配置、注解、策略注册、上下文、结果和统计报告都能贯穿自定义 type。
- 允许业务方通过自定义 `MaskStrategy` 扩展脱敏策略；未知 type 不阻断启动，当前口径为 `warn + DEFAULT fallback` 并进入统计。
- 姓名策略从传统 2 到 4 位中文姓名扩展为更通用的首尾保留策略，但不扩大 `name` 等歧义字段的默认识别范围。
- 身份证识别改为分层策略：明确上下文命中时优先脱敏，日志无上下文兜底时再做轻量校验。
- Log 场景复用 `rules.keys` 做 key-value 精准匹配，补足姓名、地址和自定义字段缺少稳定全局格式的问题。
- 新增主动脱敏服务 `SafeOutputMaskService`，支持指定 type、对象规则脱敏和强扫描三种模式，并计入 `MANUAL` 场景统计。
- 报告从基础运行指标升级为治理辅助能力，包括 Response 风险画像、性能画像、风险原因、治理建议和 Log 规则建议。
- Log 规则建议基于 regex fallback 附近 key 聚合生成候选规则和 YAML 片段，默认不自动生效，需要人工复核。
- Demo 增加主动脱敏、幂等验证、风险画像、性能画像、Log 建议和配置片段查看能力。

R2 继续坚持安全边界：不保存原始 response、完整日志、敏感值或单次字段明细；Agent 只作为后续异步报告或配置建议预留，不参与在线脱敏链路，不自动写配置。

## 5. R2.5：Demo 从工具集合变为接入样板系统

R2.5 的目标不是新增更多核心脱敏算法，而是把已有能力通过一个真实业务系统样板讲清楚。此前 Demo 更像功能 API 集合，字段以 `bean`、`map`、`list` 等技术形态为主。R2.5 将 Demo 重构为“已经接入 Safe Output 的业务系统”。

R2.5 的主要新增需求包括：

- 建立客户、订单、支付、工单、账户等 mock 业务域，覆盖所有内置脱敏类型。
- 通过业务列表、详情和嵌套对象展示 Response 自动脱敏，而不是前端手写脱敏效果。
- 通过 `/{id}/raw` 明文查看接口演示 API ignore；该接口可返回明文，但必须进入风险统计。
- 工作台总览展示默认规则、YAML 配置、注解、字段 ignore 和 API ignore 的接入方式。
- 脱敏实验室提供指定 type、对象脱敏、强扫描，并展示连续两轮结果、幂等性和耗时。
- 日志场景只读展示 JSON-like、key=value、regex fallback 三类聚合结果和规则建议。
- 报告能力展示实时聚合与历史 JSON 报告快照，历史报告读取必须限制在报告目录内。
- 不引入后端 PDF 生成依赖，报告 JSON 仍是权威产物，页面展示和浏览器打印只是派生能力。

R2.5 的边界是 Demo 业务化，不是把核心组件改造成治理平台。它不新增数据库、权限系统、后端 PDF、原始日志读取、规则自动采纳，也不改变 Response fail-open 和日志 fallback 边界。

## 6. R2.5 补充：按实际实现收敛展示口径

R2.5 补充 PRD 用于校准原始设想与最终实现差异，防止后续把已经收敛的设计误判为缺陷。

最终口径包括：

- 前端采用白底业务后台风格，不恢复深色驾驶舱、大屏模板或大面积渐变装饰。
- 默认入口和导航围绕治理 Dashboard、工作台、脱敏实验室、日志场景组织。
- 工作台分组包含总览、客户档案、订单履约、支付核验、工单处理、账户安全。
- 接入说明收敛为工作台总览内容，不再作为独立一级菜单。
- 日志场景页不提供专用触发按钮，真实日志采集来源于业务工作台接口和脱敏实验室接口。
- 脱敏实验室固定连续执行两轮，不接收前端 iterations 输入。
- 历史报告能力收敛在 Dashboard 的历史报告 Tab，不直接展示 JSON 原文。

这一补充说明了一个重要演进特征：需求不是一次性生成后机械执行，而是在实现过程中根据展示路径、模块边界和安全边界继续收敛。

## 7. R2.6：Dashboard 从 Demo 抽离为可选附加包

R2.5 后，Demo 同时承担模拟业务系统、接入样板和治理 Dashboard 三类职责。R2.6 的需求是把通用治理 Dashboard 从 Demo 中抽离为可选 starter，让真实接入应用可以显式启用本地治理页面，而不需要复制 Demo 代码。

R2.6 的主要需求包括：

- 新增 `safe-output-dashboard-spring-boot-starter`，不依赖 `safe-output-demo`。
- Dashboard 默认关闭，仅在 Spring MVC Servlet Web 应用且配置 `safe-output.dashboard.enabled=true` 时装配。
- 默认入口为 `/safe-output/dashboard/index.html`，默认路径前缀可配置。
- 静态页面和资源使用 GET，所有 Dashboard 后端 API 使用 POST。
- 复用当前应用真实的 `MaskMetricsCollector`、报告目录、Response 风险画像、Log 规则建议和 `SafeOutputMaskService`。
- 提供实时概览、接口风险、日志建议、历史报告、上传报告临时查看和通用脱敏实验室。
- 不包含 Demo 业务域、小眼睛明文查看、客户订单等 mock 数据。
- 不内置登录、权限、审计、多租户、数据库或公网防护；接入方必须自行保护入口。
- 上传报告只在请求内解析，不写入报告目录，不进入历史列表。

R2.6 的核心价值是模块边界收敛：Demo 回归模拟接入方，Dashboard 成为可复用但默认关闭的附加能力。

## 8. R3：日志长度策略增强与后续展示方向

R3 当前聚焦 Log4j2 日志脱敏长度策略增强。此前日志使用 `maxMessageLength` 控制整条 message 处理上限，超过阈值后整条 fail-open 返回原文。该策略性能可控，但对于敏感字段通常出现在日志前半段的系统，安全收益不足。

R3 提出两种策略：

- 整条日志长度限制模式：保留 R2 行为，超过 `maxMessageLength` 时整条日志不处理，保持升级兼容。
- 前缀扫描窗口模式：新增 `max-scan-length` 语义，只扫描前 N 个字符，尾部原样拼回，最终日志不截断。

R3 的边界包括：

- 默认行为优先兼容 R2，避免升级后日志行为突变。
- `max-scan-length` 只限制扫描范围，不表示输出截断。
- key-value 片段或 regex 候选值跨越窗口边界时保守处理。
- 不引入完整 JSON Parser，不新增银行卡无上下文 fallback，不保存原始日志或敏感命中值。
- 该能力只影响日志场景，不牵连 Response 和主动脱敏配置语义。

R3 PRD 中的 Demo 竞赛展示看板仍属于后续展示增强方向；当前不应把它误写为已经全部实现的核心能力。

## 9. 需求演进结论

Safe Output 的需求演进体现了三条主线：

第一，能力范围从输出侧自动脱敏扩展到主动复用和治理闭环。MVP 解决 response 与日志输出脱敏；R2 增加主动脱敏、风险画像和规则建议；后续版本通过 Demo 与 Dashboard 展示治理结果。

第二，扩展性从内置枚举收敛为 String type 与自定义策略。R2 的 String type 改造让配置、注解、策略、日志、报告和 Demo 能贯穿业务自定义脱敏类型，是从试点组件走向通用组件的关键变化。

第三，展示形态从功能验证走向可复用模块。R2.5 强化 Demo 的业务语境，R2.6 又把 Dashboard 从 Demo 抽离，避免通用治理能力和 mock 业务系统混在一起。

贯穿所有阶段的稳定边界没有变化：Java 8 / Spring Boot 2.x 兼容优先；Response 脱敏异常 fail-open；日志只做轻量 JSON-like/key-value 识别；报告、Dashboard 和规则建议不保存敏感原文；规则建议默认人工复核后采纳。

## 10. 来源索引

- `doc/prd/safe-output-mvp-prd.md`
- `doc/prd/safe-output-r2-prd.md`
- `doc/prd/safe-output-r25-prd.md`
- `doc/prd/safe-output-r25-supplemental-prd.md`
- `doc/prd/safe-output-r26-dashboard-prd.md`
- `doc/prd/safe-output-r3-prd.md`
- `doc/prd/safe-output-requirements-change-log.md`
- `doc/prd/safe-output-requirements-clarifications.md`
- `.codex-memory/00-project-current-state.md`
- `.codex-memory/03-decision-and-boundary.md`

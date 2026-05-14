# Safe Output 需求澄清说明

本文档用于记录 Safe Output MVP 的关键需求、边界和变更纪要。主需求以 [Safe Output MVP 主需求文档](./safe-output-mvp-prd.md) 为准；需求迭代以 [需求变更记录](./safe-output-requirements-change-log.md) 为准。

## 1. 使用规则

1. 本文档记录“为什么这样定”和“边界怎么解释”，不承载完整功能范围。
2. 改变交付范围的内容必须同步到需求变更记录。
3. 影响后续实现的澄清应关联 WBS。
4. 若澄清结论与主 PRD 冲突，应先更新变更记录，再修订主 PRD。

## 2. 澄清状态

| 状态 | 含义 |
|---|---|
| Confirmed | 已确认，默认执行 |
| Pending | 待确认 |
| Superseded | 已被新结论替代 |

## 3. 关键澄清结论

| 编号 | 澄清主题 | 结论 | 状态 | 影响范围 |
|---|---|---|---|---|
| C01 | MVP 主目标 | 优先证明传统 Spring Boot 2.x 老项目低侵入接入，并覆盖 response 与 Log4j2 日志两个输出场景。 | Confirmed | Response、Log4j2、Demo |
| C02 | 默认启用策略 | 推荐默认总开关开启，但 response 和 log 场景开关可独立关闭；Demo 展示默认开启。 | Confirmed | 配置、Starter、Demo |
| C03 | Response 处理边界 | 只处理普通 JSON 响应中的 Bean、Map、Collection、Array；跳过 String、byte[]、文件下载、流式响应和不可安全遍历对象。 | Confirmed | Response 脱敏 |
| C04 | 日志处理边界 | 不做完整 JSON 解析；Log4j2 使用 PatternConverter，采用 JSON-like key-value 识别和正则兜底。 | Confirmed | Log4j2 |
| C05 | 模糊字段 | `name`、`id`、`code`、`no` 不进入默认强规则，只能通过 path rule、key rule 或注解显式配置。 | Confirmed | 规则匹配 |
| C06 | 规则优先级 | 固定为接口级 ignore > 字段级 ignore > 注解规则 > path rule > key rule > default rule > regex fallback。 | Confirmed | 规则匹配、测试 |
| C07 | 接口级 ignore 统计 | 接口级 ignore 后仍进入接口风险统计，但不执行 response 脱敏；报告中标记 ignored。 | Confirmed | Response、统计报告 |
| C08 | 统计报告数据 | 禁止保存敏感原文、完整 response、完整日志；只保存聚合指标、类型、计数、接口标识和风险等级。 | Confirmed | 统计报告、安全 |
| C09 | 统计存储 | 不需要数据库；采用内存聚合 + 定时写入本地 JSON 快照。 | Confirmed | 统计报告 |
| C10 | 身份证识别 | 必须做严格大陆身份证校验，包括地区码、出生日期、校验位和合理年份。 | Superseded | 策略、正则兜底 |
| C11 | 银行卡兜底 | 不进入无上下文全局正则；仅在字段名、路径、注解或明确类型命中时脱敏银行卡。 | Confirmed | 策略、日志 |
| C12 | 异常策略 | fail-open，记录失败指标，返回原业务结果或原日志内容。 | Confirmed | 全局稳定性 |
| C13 | 兼容性基线 | JDK8 + Spring Boot 2.x，自动装配使用 `META-INF/spring.factories`。 | Confirmed | 工程、Starter |
| C14 | 后续扩展 | Logback、WebFlux、RPC/Dubbo、MyBatis、配置中心热更新、动态权限、治理平台只预留边界，不进入 MVP。 | Confirmed | 范围控制 |
| C15 | 验收方式 | 以 Demo 可运行、测试通过、接入文档完整、response/log/ignore/统计报告可演示为验收主线。 | Confirmed | 验收 |

## 4. R2 关键澄清结论

R2 需求以 [Safe Output 数据脱敏组件第二轮需求说明书 PRD](./safe-output-r2-prd.md) 为准。若 R2 与 MVP 澄清冲突，R2 结论覆盖 MVP 历史结论。

| 编号 | 澄清主题 | 结论 | 状态 | 影响范围 |
|---|---|---|---|---|
| R2-C01 | 姓名脱敏策略 | 姓名策略调整为通用首尾保留：空值和空字符串返回原值，长度 1 脱敏为 `maskChar`，长度 2 保留首字符，长度大于等于 3 保留首尾字符；该调整只改变算法，不扩大 `name` 等歧义字段的默认识别范围。 | Confirmed | 策略、规则匹配、Demo |
| R2-C02 | 身份证识别分层 | 明确上下文命中 `ID_CARD` 时优先脱敏，不强制完整合法性认证；日志无上下文 Regex fallback 才执行格式、生日、年份和可配置校验位等轻量校验；行政区划校验不默认启用。 | Confirmed | 策略、日志、主动脱敏 |
| R2-C03 | 自定义类型扩展 | 对外脱敏类型标签统一使用 String，配置、注解、策略注册、上下文、结果和统计报告均需贯穿自定义 type；内置 `MaskType` 或 `MaskTypes` 仅作为标准类型来源和推荐常量。 | Confirmed | Core、Starter、配置、统计 |
| R2-C04 | Unknown type 策略 | 未注册 type 不阻断应用启动；运行期命中未知 type 时默认 `warn + skip`，不回退到 `DEFAULT`，并纳入 unknown type 统计。 | Confirmed | 策略注册、规则匹配、统计 |
| R2-C05 | Log key-value 规则匹配 | Log 场景复用 `rules.keys` 建立 key -> type 映射，支持轻量 `key=value`、`key: value` 和常见引号形式；`rules.paths` 暂不作为日志文本匹配依据，字段级 `ignore.keys` 对日志 key-value 匹配生效。 | Confirmed | Log4j2、配置、ignore |
| R2-C06 | 主动脱敏服务 | 新增 Spring Bean 形式的主动脱敏服务，支持指定 type、对象规则和强扫描三种模式；不把数据库、MyBatis、缓存、MQ 或文件导出自动拦截纳入 R2。 | Confirmed | Core、Starter、Demo |
| R2-C07 | Manual 场景统计 | 主动调用统一计入 `MANUAL` 场景基础统计，但不默认计入 Response 接口风险统计。 | Confirmed | 统计报告、主动脱敏 |
| R2-C08 | 统计治理增强 | Response 统计升级为风险画像、治理建议和性能画像；Log 分析异步生成规则建议和配置片段。所有分析只基于聚合摘要，不保存敏感原文、完整 response、完整日志或单次字段明细。 | Confirmed | 统计报告、治理建议、安全 |
| R2-C09 | Agent 边界 | Agent 只作为异步报告或配置建议的后续扩展预留，不参与在线脱敏链路，不自动修改运行配置，所有建议需接入方人工确认。 | Confirmed | 报告、配置建议、后续扩展 |
| R2-C10 | Demo R2 验证范围 | R2 Demo 优先提供可调用、可验证、可复现、可截图的接口或轻量页面；完整竞赛展示看板归入 R3，不进入 R2 必须交付。 | Confirmed | Demo、验收、R3 |

## 5. 默认执行决策

如无新的变更记录覆盖，以下决策作为后续概要设计和实现输入：

1. MVP 名称沿用“通用 Java 数据脱敏组件”，工程模块使用 `safe-output-*`。
2. Response 脱敏为 MVP 主链路，Log4j2 日志脱敏为第二主链路。
3. 所有场景必须支持全局开关和场景开关。
4. 统计报告仅保存聚合指标，不保存敏感原文。
5. Demo 必须覆盖即插即用、response 脱敏、日志脱敏、字段级 ignore、接口级 ignore、统计报告。
6. 实施文档只保留与交付、验收、架构边界直接相关的信息，不展开过程沟通和工具提示词。

## 6. 关键边界纪要

### 6.1 Response 边界

Response 脱敏是 MVP 主链路，但不是全局输出包装器。它只覆盖标准 Spring MVC response 写出链路中的普通 JSON 对象结构，不处理文件、流、二进制、非 Spring MVC 输出和不可安全遍历对象。

### 6.2 Log4j2 边界

日志脱敏通过 Log4j2 2.x PatternConverter 接入。MVP 不追求完整 JSON 解析，不引入 fastjson 作为强依赖，不承诺覆盖未接入 PatternConverter 的日志格式。

### 6.3 Ignore 边界

Ignore 是脱敏豁免，不是安全权限或审计绕过。接口级 ignore 只跳过 response 脱敏，不自动放开 log 脱敏，并且仍进入接口风险统计。

### 6.4 统计报告边界

统计报告只保存聚合指标和风险统计，不保存敏感原文、完整 response、完整日志或单次请求明细。文件快照由定时任务异步写入，不在请求链路同步刷盘。

### 6.5 正则兜底边界

Regex fallback 是最后兜底能力，不是默认主识别规则。Response 场景优先依赖 ignore、注解、path、key 和默认规则；日志场景可以使用手机号、邮箱和按 R2 分层策略处理的大陆身份证正则兜底。银行卡默认不做无上下文全局兜底。

### 6.6 R2 报告安全边界

R2 的风险画像、性能画像、Log 规则建议和配置片段都只能基于聚合摘要生成。报告允许保存 type、key、接口维度、耗时、风险等级、治理建议、unknown type、nearbyKey、suggestedType、hitCount、confidence、evidence 和配置片段；禁止保存脱敏前原始值、脱敏后完整值、完整 response、完整日志、单次请求敏感字段明细或可反推出敏感原文的大段上下文。

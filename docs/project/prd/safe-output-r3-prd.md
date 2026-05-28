# Safe Output 数据脱敏组件第三轮需求说明书 PRD

版本：v0.5 / R3
基准版本：R2 PRD v0.4
适用范围：Java 通用数据脱敏组件第三轮迭代
技术基线：JDK8 + Spring Boot 2.x + Log4j2 2.x
交付形态：`safe-output-spring-boot-starter` Java Starter 包

---

## 1. 文档目标

本 PRD 基于 Safe Output R2 版本后的使用反馈整理，作为第三轮需求开发、概要设计、详细设计、Issue 拆分和 AI Coding 的输入文档。

第三轮继续保持组件定位：

> Safe Output 是面向传统 Java 服务的低侵入式输出侧敏感数据脱敏组件，优先覆盖接口 Response、Log4j2 日志打印和宿主系统主动调用三个场景。

本 PRD 当前聚焦 R3 日志脱敏长度策略增强需求：在兼顾性能上限和安全收益的前提下，让接入方可以选择继续使用当前超长日志整条跳过策略，或启用只扫描前缀窗口的策略。

---

## 2. R3 需求总览

| 编号 | 需求名称 | 优先级 | 类型 | 第三轮是否实施 |
|---|---|---:|---|---|
| R3-01 | Log4j2 日志脱敏长度策略可配置切换 | P0 | 日志增强 | 是 |
| R3-02 | Demo 竞赛展示看板 | P1 | Demo 增强 | 待排期 |

---

## 3. Problem Statement

当前日志脱敏使用 `maxMessageLength` 控制整条日志处理成本：当日志消息长度超过阈值时，整条 message 直接 fail-open 返回原文，不做 key-value 解析，也不做 regex fallback。这个行为性能可控、实现简单，但安全性偏弱。

在真实业务日志中，敏感字段通常出现在日志前半段，例如请求摘要、用户标识、手机号、邮箱或证件号。即使整条日志很长，接入方也希望 Safe Output 至少能在可控长度范围内完成脱敏，而不是因为尾部堆栈、报文或调试信息导致整条日志完全跳过。

同时，不同业务对性能和日志完整性的取舍不同：

1. 部分系统更重视吞吐和稳定性，希望继续使用当前超长日志整条跳过策略。
2. 部分系统更重视安全兜底，希望对超长日志的前 `N` 个字符执行脱敏扫描，并保留尾部原文用于排障。
3. 接入方需要能通过配置明确选择 `maxMessageLength` 或 `max-scan-length` 语义，避免单一配置项承担两种不同含义。

---

## 4. Solution

第三轮为日志脱敏提供两种长度处理方案，并允许接入方通过配置选择。

方案 A：整条日志长度限制模式。

该方案保持当前语义：`maxMessageLength` 是整条 message 的处理上限。日志长度不超过阈值时，按现有 key-value / JSON-like 规则和 regex fallback 完整处理；日志长度超过阈值时，整条日志 fail-open 返回原文，不截断、不扫描。

方案 B：前缀扫描窗口模式。

新增 `max-scan-length` 配置，表示最多只扫描日志前 `N` 个字符。日志输出仍保留完整 message，不截断返回内容；脱敏逻辑只对前缀窗口内的内容执行 key-value / JSON-like 规则和 regex fallback，窗口之后的尾部原样拼回。

推荐设计原则：

1. `maxMessageLength` 保留为兼容当前行为的整条处理上限。
2. `max-scan-length` 表示扫描窗口长度，不表示日志输出截断长度。
3. 两种方案必须有明确配置语义，不允许同一配置在不同版本中静默改变含义。
4. 默认行为优先兼容 R2，避免接入方升级后日志脱敏行为突变。
5. 新方案必须继续保持 fail-open，任何解析或脱敏异常都返回原日志消息或当前可安全处理的结果。

---

## 5. User Stories

1. As a Java 服务接入方, I want 继续使用 `maxMessageLength` 控制超长日志整条跳过, so that 我可以保持现有性能边界和升级兼容性。
2. As a Java 服务接入方, I want 启用 `max-scan-length` 只扫描日志前缀, so that 超长日志开头的手机号、邮箱、身份证等敏感值仍有机会被脱敏。
3. As a Java 服务接入方, I want 日志输出内容不被截断, so that 线上排障仍能看到完整日志上下文。
4. As a 安全负责人, I want 超长日志不再只能整条 fail-open, so that 长日志中的常见敏感字段暴露风险可以降低。
5. As a 性能负责人, I want 扫描长度有明确上限, so that 正则 fallback 不会因为超长日志带来不可控 CPU 消耗。
6. As a 平台维护者, I want `maxMessageLength` 与 `max-scan-length` 语义清晰区分, so that 接入文档和配置排查不会混淆。
7. As a 老项目接入方, I want 默认行为兼容 R2, so that 升级 starter 后无需立即调整配置。
8. As a 安全负责人, I want 前缀扫描模式在敏感值跨越扫描边界时保守处理, so that 不完整匹配不会制造错误脱敏结果。
9. As a 开发者, I want key-value 规则和 regex fallback 在扫描窗口内沿用现有顺序, so that 新策略不会改变已有脱敏规则优先级。
10. As a 开发者, I want `maxValueLength` 继续限制单个 value 或 regex 命中值, so that 单值级成本和误伤边界仍可控。
11. As a Demo 使用者, I want 能看到两种长度策略的示例配置和输出差异, so that 我可以理解不同策略的安全和性能取舍。
12. As a 测试维护者, I want 两种模式都有明确集成测试, so that 后续重构不会破坏日志脱敏长度边界。
13. As a 文档读者, I want 集成指南说明 `max-scan-length` 不截断输出, so that 我不会误以为日志内容会被 Safe Output 裁剪。
14. As a 运维人员, I want 超长日志处理策略能从配置中直接判断, so that 线上问题排查时无需阅读源码。
15. As a 组件维护者, I want 该能力只影响日志脱敏场景, so that Response 脱敏和主动脱敏不会被日志长度策略牵连。

---

## 6. Implementation Decisions

- 修改日志脱敏模块，抽象日志消息长度处理策略，避免在主脱敏流程中散落长度判断。
- 保留当前 `maxMessageLength` 语义作为兼容模式：超过阈值时整条日志不处理。
- 新增 `max-scan-length` 配置，用于前缀扫描窗口模式：只扫描前 `N` 个字符，尾部原样拼回。
- 新增策略选择配置，接入方可明确选择使用整条长度限制模式或前缀扫描窗口模式。具体配置命名在概要设计阶段确定，但必须避免和现有 `maxMessageLength` 语义混淆。
- 当启用前缀扫描窗口模式时，不应截断最终输出日志。扫描窗口外的尾部不做解析、不做 regex fallback、不做脱敏，直接拼接回输出。
- 当 key-value 片段跨越扫描窗口边界时，应保守处理，不对不完整片段进行脱敏替换。
- regex fallback 仍只在扫描窗口内执行，并继续固定覆盖手机号、邮箱和合法大陆身份证号；银行卡号不进入日志 regex fallback。
- `maxValueLength` 继续作为单个 value 或 regex fallback 候选值的长度上限，不改为截断处理。
- Spring Boot starter 需要绑定新配置，并通过 Log4j2 runtime bridge 传递给真实日志 converter。
- 无 Spring runtime bridge、直接使用 `%safeOutputMsg{...}` 的场景，也需要支持等价的 pattern option。
- 文档必须同步说明两种方案的适用场景、默认行为、兼容性和安全边界。
- 报告不保存超长日志原文，不因该需求引入敏感样本持久化。

---

## 7. Testing Decisions

- 测试只验证外部可观察行为，不直接断言私有正则、内部循环或具体实现分支。
- 日志模块单元测试需要覆盖当前兼容模式：未超过 `maxMessageLength` 时正常脱敏，超过时整条原样返回。
- 日志模块单元测试需要覆盖前缀扫描模式：前缀窗口内敏感值被脱敏，窗口外尾部保持原文，最终日志长度和尾部内容不被截断。
- 日志模块单元测试需要覆盖扫描边界：key-value 片段或 regex 候选值跨越窗口边界时不产生破坏性替换。
- starter 自动装配测试需要覆盖新配置绑定和 runtime bridge 传递。
- Log4j2 converter 测试需要覆盖无 Spring runtime bridge 时 pattern option 生效。
- Demo 或 starter 集成测试需要提供至少一个可复现样例，展示两种模式的输出差异。
- 回归测试需要覆盖 `maxValueLength` 仍按单值跳过处理，不被 `max-scan-length` 改写语义。

---

## 8. Out of Scope

- 不引入完整 JSON Parser。
- 不对 Response 脱敏增加全局字符串扫描。
- 不改变主动脱敏 `strong-scan` 的配置语义。
- 不支持对日志尾部继续异步扫描。
- 不截断最终输出日志。
- 不新增银行卡号的日志无上下文 regex fallback。
- 不保存原始日志、完整 message 或敏感命中值到报告。
- 不改变 `MaskRuleMatcher` 的规则优先级。

---

## 9. Further Notes

- 默认行为建议保持 R2 兼容，即未显式启用前缀扫描窗口模式时，仍使用 `maxMessageLength` 整条跳过语义。
- `max-scan-length` 更适合安全要求更高、且敏感字段通常位于日志前部的业务系统。
- `maxMessageLength` 更适合极端重视日志链路性能稳定、且能接受超长日志 fail-open 风险的业务系统。
- 后续概要设计需要明确当两个配置同时存在时的优先级和非法配置处理方式。
- 接入指南需要用示例说明：前缀扫描模式只限制扫描范围，不改变最终日志内容。

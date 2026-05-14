# Safe Output 需求变更记录

本文档用于管理 Safe Output MVP 的需求迭代、范围调整和后续工作下发。主需求以 [Safe Output MVP 主需求文档](./safe-output-mvp-prd.md) 为准；关键需求决策以 [需求澄清说明](./safe-output-requirements-clarifications.md) 为准。

## 1. 使用规则

1. 所有需求变更先记录在本文档，再决定是否同步修改主 PRD。
2. 已确认并影响交付范围的变更，必须补充影响分析和后续工作项。
3. 只影响解释口径、不改变范围的内容，优先记录到澄清说明文档。
4. 已进入实现的变更应关联 issue、WBS 或里程碑。
5. 变更记录不删除历史结论；废弃项通过状态标记。

## 2. 状态定义

| 状态 | 含义 |
|---|---|
| Proposed | 已提出，尚未确认 |
| Accepted | 已确认，需要进入主 PRD 或后续 issue |
| Rejected | 已拒绝，不进入范围 |
| Superseded | 已被后续变更替代 |
| Implemented | 已完成实现或文档同步 |

## 3. 变更记录模板

```md
### CR-YYYYMMDD-NN 变更标题

- 状态：
- 来源：
- 关联文档：
- 关联 WBS：
- 关联 issue：
- 变更摘要：
- 变更原因：
- 范围影响：
- 验收影响：
- 后续工作：
```

## 4. 当前基线

### CR-20260511-01 建立 v0.4 MVP 需求基线

- 状态：Accepted
- 来源：`doc/java_data_desensitization_mvp_scope_and_wbs_v0.3.md`
- 关联文档：[Safe Output MVP 主需求文档](./safe-output-mvp-prd.md)
- 关联 WBS：WBS-00 至 WBS-12
- 变更摘要：将原始综合 PRD 拆分为主 PRD、需求变更记录、需求澄清说明三份文档。
- 变更原因：原始文档同时承载需求范围、版本沿革、WBS、澄清计划和后续决策，不利于后续 issue 拆分、迭代管理和关键变更纪要维护。
- 范围影响：不改变 MVP 实现范围，只调整需求管理结构。
- 验收影响：后续 issue 拆分以主 PRD 的 WBS 和验收标准为准。
- 后续工作：后续新增或调整需求时，先在本文档记录，再同步主 PRD 或澄清说明。

### CR-20260511-02 明确最终交付物为 Spring Boot 2.x Starter Jar

- 状态：Implemented
- 来源：用户要求“最终的交付物应该是个 java 包，可以供 springboot2.x 系统直接引用接入，同时更新 WBS”
- 关联文档：[Safe Output MVP 主需求文档](./safe-output-mvp-prd.md)
- 关联 WBS：WBS-00、WBS-03、WBS-04、WBS-07、WBS-08、WBS-10、WBS-12、WBS-13
- 变更摘要：将 MVP 对外交付物明确为 `safe-output-spring-boot-starter` Maven Jar，业务系统通过 Spring Boot 2.x starter 直接引用接入；新增 `WBS-13 Starter 打包与发布验证`。
- 变更原因：原 PRD 虽然包含 starter 模块，但交付物章节仍以多模块源码为主，容易让后续 issue 偏向源码工程实现，而不是可被业务系统直接接入的 Java 包。
- 范围影响：不新增脱敏能力范围，但新增 starter 打包、安装、依赖树、自动装配和 Demo 外部引用验证要求。
- 验收影响：MVP 成功标准新增“能生成 `safe-output-spring-boot-starter` Jar”和“Spring Boot 2.x 业务系统只引用 starter 即可完成自动装配”。
- 后续工作：从主 PRD 拆 issue 时，需要为 WBS-13 单独创建打包与引用验证 issue；Demo issue 必须禁止直接依赖内部模块绕过 starter。

### CR-20260514-01 建立 R2 第二轮需求基线

- 状态：Accepted
- 来源：[Safe Output 数据脱敏组件第二轮需求说明书 PRD](./safe-output-r2-prd.md)
- 关联文档：[Safe Output 数据脱敏组件第二轮需求说明书 PRD](./safe-output-r2-prd.md)、[需求澄清说明](./safe-output-requirements-clarifications.md)
- 关联 WBS：待按 R2 PRD 拆分
- 关联 issue：待拆分
- 变更摘要：将 R2-01 至 R2-08 作为 MVP 完成后的第二轮实施范围，覆盖姓名策略、身份证识别、自定义 String type、Log key-value 规则匹配、主动脱敏服务、统计治理增强和 Demo 可验证性增强；R3-01 仅作为第三轮展示看板预留。
- 变更原因：MVP 试点后暴露出真实姓名格式适配不足、自定义 type 无法贯穿、日志语义字段覆盖不足、业务代码无法复用统一脱敏能力、统计报告治理价值不足等问题。
- 范围影响：R2 从 response 与 Log4j2 自动输出侧脱敏，扩展到宿主系统主动调用和治理辅助报告；仍保持 JDK8、Spring Boot 2.x、Log4j2 2.x 和 starter Jar 交付基线。
- 验收影响：第二轮验收需覆盖 R2 新增能力、Demo 调用接口、报告安全边界和自定义 type 贯穿链路；不能只沿用 MVP response/log/ignore 基础验收。
- 后续工作：按 R2-01 至 R2-08 拆分本地 Markdown issue，标记 `ready-for-agent`，并按 R2 澄清结论更新概要设计和测试范围。

### CR-20260514-02 调整身份证识别为 R2 分层策略

- 状态：Accepted
- 来源：[Safe Output 数据脱敏组件第二轮需求说明书 PRD](./safe-output-r2-prd.md) R2-02
- 关联文档：[需求澄清说明](./safe-output-requirements-clarifications.md)
- 关联 WBS：待拆分
- 关联 issue：待拆分
- 变更摘要：废弃 MVP 中“所有身份证识别必须严格校验地区码、出生日期、校验位和合理年份”的统一口径；R2 改为明确上下文命中即脱敏，日志无上下文兜底才执行轻量校验，行政区划校验不默认启用。
- 变更原因：组件定位是输出侧脱敏，不是身份证真伪认证系统；在字段、注解、path、Log key-value 或主动 type 已明确为身份证时，应优先避免明文泄露。
- 范围影响：身份证策略需要支持上下文感知的处理分支，并预留 `checksum-check-enabled`、`region-check-enabled` 等配置。
- 验收影响：Response 或主动调用中格式不完全合法但上下文明确信息为 `ID_CARD` 时仍应脱敏；日志孤立疑似身份证字符串需经过轻量校验后再脱敏。
- 后续工作：拆分策略实现、日志兜底测试、配置绑定测试和回归测试，避免将普通 18 位流水号在无上下文场景中过度误脱敏。

### CR-20260514-03 放开自定义脱敏类型并统一 String type 标签

- 状态：Accepted
- 来源：[Safe Output 数据脱敏组件第二轮需求说明书 PRD](./safe-output-r2-prd.md) R2-03、R2-04
- 关联文档：[需求澄清说明](./safe-output-requirements-clarifications.md)
- 关联 WBS：待拆分
- 关联 issue：待拆分
- 变更摘要：配置规则、注解、策略接口、策略注册表、脱敏上下文、脱敏结果和统计报告中的脱敏类型标签统一改为 String；内置 `MaskType` 或 `MaskTypes` 仅作为标准类型来源和推荐常量。
- 变更原因：当前 `MaskType` 枚举强绑定会导致配置中出现自定义 type 时启动失败，也阻断自定义策略从配置、注解到统计报告的全链路贯穿。
- 范围影响：Core、Starter 配置绑定、注解 API、策略注册、规则匹配、日志脱敏、统计报告和 Demo 示例都需要适配 String type。
- 验收影响：`type: mobileM`、`@Desensitize(type = "mobileM")` 和自定义 `MaskStrategy.type() = "mobileM"` 均应可用；未注册 type 默认 `warn + skip`，并进入 unknown type 统计。
- 后续工作：优先拆分类型模型重构 issue，再拆分配置绑定、注解兼容、统计报告和自定义策略 Demo issue。

### CR-20260514-04 增强 Log key-value 规则匹配和异步规则发现

- 状态：Accepted
- 来源：[Safe Output 数据脱敏组件第二轮需求说明书 PRD](./safe-output-r2-prd.md) R2-05、R2-07
- 关联文档：[需求澄清说明](./safe-output-requirements-clarifications.md)
- 关联 WBS：待拆分
- 关联 issue：待拆分
- 变更摘要：Log 场景复用 `rules.keys` 做 key-value 精准匹配，支持常见 `key=value`、`key: value` 和引号形式；同时基于 regex fallback 的 nearbyKey 聚合，异步生成规则建议和 application.yml 配置片段。
- 变更原因：姓名、地址和自定义敏感字段缺少稳定全局格式，仅靠通用正则无法覆盖；老系统真实字段命名需要从日志运行摘要中反向发现。
- 范围影响：Log4j2 脱敏链路需要初始化 key -> type 映射、Pattern 缓存、长度限制和规则 key 数量限制；报告模块需要新增 Log 规则建议摘要。
- 验收影响：已配置 key 能在日志 key-value 中脱敏，`ignore.keys` 对日志字段生效；规则建议默认不自动写配置、不自动生效，报告不包含完整日志或敏感原文。
- 后续工作：拆分 Log 精准匹配、性能保护、nearbyKey 聚合、建议报告导出和 Demo 查看接口 issue。

### CR-20260514-05 新增主动脱敏服务和 MANUAL 场景统计

- 状态：Accepted
- 来源：[Safe Output 数据脱敏组件第二轮需求说明书 PRD](./safe-output-r2-prd.md) R2-06
- 关联文档：[需求澄清说明](./safe-output-requirements-clarifications.md)
- 关联 WBS：待拆分
- 关联 issue：待拆分
- 变更摘要：新增可注入的 `SafeOutputMaskService`，支持指定 type、对象规则和强扫描三种主动调用模式；主动调用统一进入 `MANUAL` 场景统计。
- 变更原因：宿主系统在 DAO 查询结果处理、写入缓存前处理、业务代码局部展示、消息发送前处理等场景需要复用统一策略，避免重复实现和规则不一致。
- 范围影响：需要抽取或复用现有策略注册、对象递归、日志强扫描、上下文和统计能力；但不实现数据库、MyBatis、缓存、MQ、文件导出的自动拦截。
- 验收影响：业务代码可注入服务并调用 `mask`、`maskObject`、`maskStrong`；对象规则模式默认不做全量 value 正则扫描，强扫描必须由业务方显式调用。
- 后续工作：拆分服务接口、默认实现、强扫描选项、MANUAL 统计和 Demo 主动脱敏接口 issue。

### CR-20260514-06 将统计报告升级为治理辅助能力

- 状态：Accepted
- 来源：[Safe Output 数据脱敏组件第二轮需求说明书 PRD](./safe-output-r2-prd.md) R2-07
- 关联文档：[需求澄清说明](./safe-output-requirements-clarifications.md)
- 关联 WBS：待拆分
- 关联 issue：待拆分
- 变更摘要：Response 统计从基础运行指标升级为风险画像、风险原因、治理建议和性能画像；Log 分析异步输出规则补充建议和配置片段。
- 变更原因：仅有脱敏次数、类型分布和基础耗时不足以支撑试点治理和竞赛展示，需要从运行摘要中解释接口风险、性能风险和规则补齐方向。
- 范围影响：报告模块需要新增 Response 风险分析器、性能阈值、topRiskApis、responseRiskSummary、performanceWarnings、logRuleSuggestions 和配置片段输出。
- 验收影响：报告必须展示风险摘要、Top 风险接口、风险原因、治理建议、性能画像和 Log 规则建议；所有报告仍禁止保存敏感原文、完整 response、完整日志和单次字段明细。
- 后续工作：拆分 Response 风险聚合、规则型分析器、Log 建议分析、报告 JSON 导出和安全边界测试 issue。

### CR-20260514-07 增强 R2 Demo 可验证性并预留 R3 看板

- 状态：Accepted
- 来源：[Safe Output 数据脱敏组件第二轮需求说明书 PRD](./safe-output-r2-prd.md) R2-08、R3-01
- 关联文档：[Safe Output 数据脱敏组件第二轮需求说明书 PRD](./safe-output-r2-prd.md)
- 关联 WBS：待拆分
- 关联 issue：待拆分
- 变更摘要：R2 Demo 新增主动脱敏、幂等性、Response 风险画像、Response 性能画像、Log 规则建议和配置片段查看能力；完整前端竞赛展示看板作为 R3 需求预留。
- 变更原因：R2 新增能力需要可调用、可验证、可复现、可截图的验收入口；竞赛展示看板需要更多前端与图表工作，不应阻塞 R2 核心能力交付。
- 范围影响：Demo 需要补充接口或轻量页面、示例 DTO、自定义 `mobileM` 策略示例和报告查看接口。
- 验收影响：Demo 必须能验证指定 type、对象规则、强扫描、二次脱敏幂等性、MANUAL 统计和报告安全边界；不要求完成 R3 五页看板。
- 后续工作：拆分 Demo API、样例数据、报告查看和 R3 看板待办 issue。

## 5. 已纳入基线的版本沿革

### 5.1 v0.4 调整

1. 删减 WBS 中的过程性沟通内容，不再在每个工作包中展开工具规则生成说明。
2. 将 WBS 从“工具执行规划”收敛为“需求范围和可交付工作包”。
3. 新增阶段性目标和里程碑，明确 MVP 从基础骨架到演示验收的推进节奏。
4. 新增整体需求澄清计划，按推荐结论一次性确认关键需求边界。

### 5.2 v0.3 边界

1. 日志脱敏 MVP 优先支持 Log4j2 2.x，不再以 Logback 作为第一优先级。
2. 日志模块设计为可扩展日志适配层，后续预留兼容 Logback 的扩展点。
3. 统计报告不再只做内存聚合，需要增加定时写入本地 JSON 文件机制，避免应用重启后统计完全丢失。
4. 定时文件写入只输出聚合指标快照，不保存敏感原文、不保存完整 response、不保存完整日志。
5. 统计文件写入采用定时任务方式，不在请求链路同步刷盘。

### 5.3 v0.2 边界

1. 模糊字段处理策略：`name`、`id`、`code`、`no` 不进入默认强脱敏规则。
2. 支持字段级 ignore、接口级 ignore、包级 ignore 的脱敏豁免机制。
3. 规则优先级：接口级 ignore > 字段级 ignore > 注解规则 > path rule > key rule > default rule > regex fallback。
4. 日志场景不引入 fastjson，采用轻量 JSON-like key-value 识别 + 正则兜底。
5. 全局正则兜底默认支持手机号、邮箱、严格大陆身份证。
6. 银行卡不做无上下文全局正则兜底。
7. Spring Boot 2.x 自动装配使用 `spring.factories`，不得只适配 Spring Boot 3.x 自动装配机制。
8. 接口风险统计 MVP 只做 response 场景。
9. 统计报告不保存敏感原文、不保存完整 response、不保存完整日志。
10. 接口级 ignore 的接口不执行 response 脱敏，但仍进入接口风险统计。

## 6. 后续变更待办池

以下能力当前不进入 MVP，如后续要推进，应先新增变更记录，再拆 issue：

1. Logback 日志脱敏。
2. WebFlux 支持。
3. RPC/Dubbo Filter 支持。
4. MyBatis 查询结果脱敏。
5. 配置中心热更新。
6. 基于用户权限或角色的动态脱敏。
7. 治理平台与可视化后台。
8. Prometheus、Actuator 指标集成。
9. 分布式统计聚合。
10. Log 场景接口归因。
11. 业务文件导出内容脱敏。
12. 消息队列脱敏。
13. FastJson 深度集成。

# Safe Output MVP 主需求文档

本文档由 `doc/java_data_desensitization_mvp_scope_and_wbs_v0.3.md` 拆分而来，作为后续 issue 拆分的主入口。版本沿革和关键澄清分别维护在：

- [需求变更记录](./safe-output-requirements-change-log.md)
- [需求澄清说明](./safe-output-requirements-clarifications.md)

## 1. 项目定位

Safe Output 是面向传统 Java 服务的低侵入式输出侧敏感数据脱敏组件。

MVP 基于 JDK8 + Spring Boot 2.x，支持传统老项目通过 starter 和配置文件实现即插即用的数据脱敏能力，优先覆盖接口 response 和 Log4j2 2.x 日志打印两个核心输出场景，并在日志适配层预留后续兼容 Logback 的扩展空间。

## 2. MVP 核心价值

1. 老项目即插即用。
2. 配置优先，注解可选。
3. 不要求大规模修改业务代码。
4. 同时覆盖接口显示和日志打印两个高风险场景。
5. 支持脱敏豁免机制，避免粗暴全局脱敏影响业务正确性。
6. 支持接口风险统计，便于识别敏感数据暴露接口。
7. 有基础统计报告，可支撑竞赛汇报和试点验证。
8. 架构边界清晰，便于后续分模块实现和持续迭代。

## 3. 业务目标

1. 支持试点 Spring Boot 老项目快速接入。
2. 降低接口 response 和日志打印中的敏感数据泄露风险。
3. 减少业务代码中手工脱敏改造工作量。
4. 统一常见敏感数据的脱敏规则。
5. 支持必要的脱敏豁免，避免影响必须展示敏感明文的业务接口。
6. 支持对触发脱敏的接口进行统计，为接口安全风险分级提供基础数据。
7. 为竞赛汇报提供可演示、可说明、可量化的成果。
8. 为后续版本扩展权限动态脱敏、MyBatis 脱敏、配置中心热更新、治理平台等能力预留架构空间。

## 4. 技术目标

1. 兼容 JDK8。
2. 兼容 Spring Boot 2.x。
3. 提供 Spring Boot Starter 自动装配。
4. Spring Boot 2.x 自动装配使用 `META-INF/spring.factories`。
5. 不使用 Spring Boot 3.x 专属自动装配机制作为唯一入口。
6. 不强制覆盖宿主系统 Spring Boot 版本。
7. 支持 `ResponseBodyAdvice` 方式进行接口 response 脱敏。
8. MVP 优先支持 Log4j2 2.x 日志输出脱敏。
9. 日志脱敏模块预留后续兼容 Logback 的适配扩展点。
10. 支持 YAML 配置化规则。
11. 支持注解精确脱敏。
12. 支持字段级和接口级脱敏豁免。
13. 支持常见敏感数据类型的内置脱敏策略。
14. 支持对象、Map、Collection、Array 等常见返回结构。
15. 支持基础统计指标采集和报告输出。
16. 支持 response 场景接口风险统计。

## 5. 目标用户与场景

### 5.1 目标用户

1. 传统 Java 服务端项目开发人员。
2. Spring Boot 项目维护人员。
3. 系统安全治理人员。
4. 架构组或公共组件维护人员。
5. 竞赛评审或试点项目验收人员。

### 5.2 核心使用场景

1. 老项目引入 starter 和少量 YAML 配置后，原接口 response 自动脱敏。
2. 接口返回对象中的手机号、身份证、银行卡、邮箱、姓名等字段按规则脱敏。
3. 业务日志通过 Log4j2 2.x PatternConverter 输出脱敏后的 message。
4. 新开发 DTO 或重点对象可以通过 `@Desensitize` 注解精确指定脱敏类型。
5. 商品名、角色名、机构名等误脱敏风险字段可以通过字段级 ignore 豁免。
6. 查询完整手机号、实名信息确认、内部客服查询等接口可以通过接口级 ignore 豁免 response 脱敏。
7. 竞赛演示可展示接入前后 response、log、ignore、统计报告的对比。

## 6. 功能范围总览

| 编号 | 功能模块 | 功能名称 | 优先级 | MVP |
|---|---|---|---|---|
| M01 | 核心能力 | 脱敏类型与策略模型 | P0 | 是 |
| M02 | 核心能力 | 内置常见脱敏策略 | P0 | 是 |
| M03 | 核心能力 | 策略注册与查找 | P0 | 是 |
| M04 | 配置能力 | YAML 配置绑定 | P0 | 是 |
| M05 | 配置能力 | 默认规则库 | P0 | 是 |
| M06 | 配置能力 | ignore 配置 | P0 | 是 |
| M07 | 规则能力 | 字段名 key 匹配 | P0 | 是 |
| M08 | 规则能力 | 字段路径 path 匹配 | P0 | 是 |
| M09 | 规则能力 | 模糊字段处理策略 | P0 | 是 |
| M10 | 规则能力 | 注解识别 | P0 | 是 |
| M11 | 豁免能力 | 字段级 ignore | P0 | 是 |
| M12 | 豁免能力 | 接口级 ignore | P0 | 是 |
| M13 | Response 场景 | ResponseBodyAdvice 拦截 | P0 | 是 |
| M14 | Response 场景 | Bean 脱敏 | P0 | 是 |
| M15 | Response 场景 | Map 脱敏 | P0 | 是 |
| M16 | Response 场景 | Collection/Array 脱敏 | P0 | 是 |
| M17 | Log 场景 | Log4j2 PatternConverter | P0 | 是 |
| M18 | Log 场景 | JSON-like key-value 日志脱敏 | P0 | 是 |
| M19 | Log 场景 | 正则兜底日志脱敏 | P1 | 是 |
| M20 | 正则能力 | 严格大陆身份证校验 | P0 | 是 |
| M21 | 统计报告 | 内存聚合指标采集 | P1 | 是 |
| M22 | 统计报告 | 基础报告输出 | P1 | 是 |
| M23 | 统计报告 | Response 接口风险统计 | P1 | 是 |
| M24 | Demo | 示例项目 | P0 | 是 |
| M25 | 测试 | 单元测试和集成测试 | P0 | 是 |
| M26 | 文档 | 接入说明和演示说明 | P1 | 是 |

## 7. 详细需求

### 7.1 核心脱敏模型

提供脱敏组件的基础领域模型，包括 `MaskType`、`MaskScene`、`MaskContext`、`MaskResult`、`MaskStrategy` 和 `MaskStrategyRegistry`。

验收标准：

1. 支持通过 `MaskType` 查找对应策略。
2. 支持调用统一策略接口完成脱敏。
3. 支持空值、空字符串、短字符串安全处理。
4. 支持未知类型安全跳过或返回原值。
5. 单元测试覆盖核心策略调用链。

### 7.2 内置脱敏策略

MVP 内置常见敏感数据脱敏策略，满足试点项目和竞赛演示需要。

| 类型 | 示例原文 | 示例脱敏结果 | 优先级 |
|---|---|---|---|
| MOBILE | 13812345678 | 138****5678 | P0 |
| ID_CARD | 350102199001011234 | 350102********1234 | P0 |
| BANK_CARD | 6222021234567890123 | 622202*********0123 | P0 |
| EMAIL | zhangsan@example.com | zha****@example.com | P1 |
| CHINESE_NAME | 张三 / 王小明 | 张* / 王** | P1 |
| ADDRESS | 福建省福州市鼓楼区xxx | 福建省福州市**** | P1 |
| PASSWORD | abc123456 | ******** | P1 |
| DEFAULT | 任意字符串 | 按默认规则处理 | P1 |

验收标准：

1. 每种内置策略均有单元测试。
2. 对空值和异常格式不抛出业务异常。
3. 脱敏结果符合默认规则预期。
4. 支持后续新增策略而不修改主流程。

### 7.3 Spring Boot 自动装配

提供 `safe-output-spring-boot-starter`，使业务系统引入依赖后自动注册配置属性、脱敏服务、response 脱敏 Advice、规则匹配器和默认策略。

约束：

1. 必须兼容 JDK8。
2. 必须兼容 Spring Boot 2.x。
3. 使用 `META-INF/spring.factories` 注册自动装配类。
4. 不使用 Spring Boot 3.x 专属 API 作为唯一入口。
5. 支持用户自定义策略 Bean 注入。
6. 支持 `safe-output.enabled=false` 关闭能力。

验收标准：

1. Demo 项目引入 starter 后可以自动装配。
2. 不需要手动声明核心 Bean。
3. 设置 `safe-output.enabled=false` 后 response 脱敏不生效。
4. 启动过程无额外强制依赖异常。
5. 自动装配文件使用 `spring.factories` 生效。

### 7.4 配置文件规则

通过 `application.yml` 配置敏感字段规则，支持老项目低侵入接入。

配置能力包括：

1. 全局开关。
2. response 场景开关。
3. log 场景开关。
4. `maskChar`。
5. `maxDepth`。
6. `maxCollectionSize`。
7. `rules`，支持 `name`、`keys`、`paths`、`type`、`enabled`。
8. 默认规则库。
9. 用户配置覆盖或补充默认规则。
10. `ignore` 配置。
11. `report` 配置。

验收标准：

1. `application.yml` 配置可以正确绑定。
2. 配置字段名命中后能正确脱敏。
3. 未配置字段但命中默认规则时能正确脱敏。
4. 关闭某条规则后不再生效。
5. 关闭某个场景后该场景不再脱敏。
6. ignore 配置命中后优先豁免。

### 7.5 模糊字段处理策略

`name`、`id`、`code`、`no`、`number`、`title`、`account`、`userName` 等字段默认不进入强脱敏规则。模糊字段只能通过注解、明确 key、明确 path 或 ignore 显式处理。

默认强规则优先覆盖语义明确字段，例如 `mobile`、`phone`、`telephone`、`idCard`、`certNo`、`identityNo`、`bankCard`、`cardNo`、`email`、`password`、`secret`、`token`。

验收标准：

1. 默认规则中不因字段名为 `name` 就直接脱敏。
2. 配置 `realName` 后可以按姓名脱敏。
3. 配置 `user.name` path 后可以按姓名脱敏。
4. 配置 `product.name` ignore 后不会脱敏。
5. 单元测试覆盖模糊字段和明确字段差异。

### 7.6 脱敏豁免机制

MVP 支持字段级 ignore 和接口级 ignore，用于解决局部字段误脱敏、业务必须返回敏感明文、包或类型不应递归扫描、即插即用后的兼容性控制。

字段级 ignore：

1. P0 支持 `ignore.keys`。
2. P0 支持 `ignore.paths`。
3. P1 可支持 `ignore.packages`。
4. P1 可支持 `ignore.classes`。

接口级 ignore：

1. 支持 `method + path` 配置。
2. 支持 Ant 风格 path pattern。
3. 支持 scenes 配置。
4. 默认只作用于 response 场景。
5. response ignore 不自动放开 log 脱敏。
6. 命中后本次 response 不执行脱敏。
7. 命中的接口仍进入接口风险统计。
8. 报告中标记 `ignored=true` 和 `ignoreReason`。

规则优先级固定为：

```text
接口级 ignore > 字段级 ignore > 注解规则 > path rule > key rule > default rule > regex fallback
```

验收标准：

1. 命中 `ignore.keys` 的字段不脱敏。
2. 命中 `ignore.paths` 的字段不脱敏。
3. 命中 `ignore.apis` 的接口 response 不脱敏。
4. 命中接口级 ignore 后，日志脱敏不受影响。
5. 命中接口级 ignore 的接口仍进入统计报告。
6. 报告中可以看到 ignored 接口的 hitCount、reason 和 riskLevel。

### 7.7 注解脱敏

提供 `@Desensitize` 注解，支持字段级精确脱敏。注解至少支持指定 type，可选支持指定 scene。注解优先级高于配置字段名匹配，并支持字段注解缓存。

验收标准：

1. 字段添加注解后，即使字段名不在配置规则中，也能按注解类型脱敏。
2. 注解和配置同时存在时，优先使用注解。
3. 无注解字段可继续通过配置规则匹配。
4. 单元测试覆盖注解识别逻辑。

### 7.8 Response 接口返回脱敏

通过 Spring MVC `ResponseBodyAdvice` 在接口响应写出前统一脱敏。

支持对象范围：

1. Java Bean。
2. Map。
3. Collection。
4. List。
5. Array。
6. 嵌套对象。
7. 统一响应结构。
8. `ResponseEntity` body。

默认跳过：

1. 文件流。
2. 图片。
3. 二进制响应。
4. Spring 内部对象。
5. Servlet 原生对象。
6. 基础类型和日期类型。
7. 配置中指定忽略的包或类型。

验收标准：

1. Controller 返回 Java Bean 时敏感字段自动脱敏。
2. Controller 返回 Map 时敏感 key 自动脱敏。
3. Controller 返回 List 时内部元素自动脱敏。
4. 嵌套对象中的敏感字段自动脱敏。
5. 超过最大深度时安全停止。
6. 出现脱敏异常时不影响接口正常返回。
7. 关闭 response 场景后不再脱敏。
8. 命中接口级 ignore 后 response 不脱敏。
9. 命中接口级 ignore 后接口仍进入风险统计。

### 7.9 Log4j2 日志脱敏

MVP 优先支持 Log4j2 2.x，通过 PatternConverter 在日志输出前对最终 message 脱敏。日志脱敏模块保持适配层边界，后续可在同一套 core 策略与 log masker 基础上增加 Logback 适配。

日志识别策略：

1. 轻量 JSON-like key-value 识别。
2. value 内轻量正则扫描。
3. 整条 message 正则兜底。

边界：

1. 不引入 fastjson、Jackson 或其他 JSON Parser 作为强依赖。
2. 银行卡不做无上下文全局正则兜底。
3. 日志脱敏异常不得影响日志输出。
4. Log4j2 插件初始化失败时，不得影响 response 脱敏能力。

验收标准：

1. 日志中 `"mobile":"13812345678"` 可脱敏。
2. 日志中直接出现 `13812345678` 可按手机号规则脱敏。
3. 日志中合法大陆身份证可通过严格校验后脱敏。
4. 普通 18 位流水号如果不符合身份证校验规则，不应被当作身份证脱敏。
5. 日志中邮箱可脱敏。
6. 日志中无上下文银行卡号默认不做全局兜底脱敏。
7. 关闭 log 场景后不再脱敏。
8. 未配置 Log4j2 PatternConverter 时，不影响应用启动和 response 脱敏。
9. 日志脱敏实现不得依赖 Logback API。

### 7.10 正则兜底边界

正则兜底用于日志场景补充脱敏，不作为 response 场景主要识别方式。

默认支持：

1. 手机号。
2. 邮箱。
3. 严格大陆身份证。

大陆身份证正则兜底必须做格式、出生日期、年份范围、校验位和可选行政区划基础校验。

银行卡不做无上下文全局正则兜底，仅在字段名、路径、注解、明确上下文或用户显式配置开启时脱敏。

Response 场景默认不对所有字符串 value 做全局正则扫描，避免误伤订单号、流水号、业务编号。

### 7.11 自定义脱敏策略

允许业务系统通过实现 `MaskStrategy` 扩展自定义脱敏逻辑。

验收标准：

1. Demo 中可增加一个自定义策略并成功生效。
2. 自定义策略不会破坏内置策略。
3. 策略注册失败时有清晰日志。

### 7.12 统计报告与存储边界

MVP 只做运行指标类统计，不做明细审计类统计。运行时以内存聚合为主，按固定周期将聚合快照导出到本地 JSON 文件，避免应用重启后统计结果完全丢失。

运行指标包括：

1. 脱敏总次数。
2. response 脱敏次数。
3. log 脱敏次数。
4. 按脱敏类型统计次数。
5. 按接口统计脱敏次数。
6. 脱敏失败次数。
7. 平均耗时。
8. 最大耗时。
9. 正则兜底命中次数。
10. 接口风险等级。
11. 最近一次文件快照写入时间。
12. 文件快照写入失败次数。

禁止保存：

1. 脱敏前原始值。
2. 脱敏后完整值。
3. 完整 response。
4. 完整日志内容。
5. 单次请求的敏感字段明细。
6. 脱敏前后完整对比。

存储方式：

```text
内存聚合统计 + 定时写入本地 JSON 文件快照 + 可选日志输出 + 不落明细数据
```

MVP 不支持数据库落库、Redis 存储、远程接口上报、Prometheus 指标、治理平台实时上报。

验收标准：

1. response 脱敏后统计次数增加。
2. log 脱敏后统计次数增加。
3. 按类型统计结果正确。
4. 接口维度统计结果正确。
5. 可以输出基础报告。
6. 报告不包含敏感原文。
7. 报告不包含完整 response 或完整日志。
8. 统计模块异常不影响主流程。
9. 超过接口统计容量后可归入 overflow。
10. 开启文件导出后，可以按配置间隔生成 JSON 报告文件。
11. 应用重启后，历史报告文件仍可用于查看最近一次统计快照。
12. 文件写入失败不会影响接口返回、日志输出和脱敏主流程。

### 7.13 Response 接口风险统计

MVP 只在 response 场景中进行接口风险统计，不做 log 与接口的关联统计。

统计维度：

1. HTTP method。
2. URI 或 mapping pattern。
3. Controller class。
4. Controller method。
5. 脱敏字段数量。
6. 脱敏类型分布。
7. 规则命中次数。
8. 本次脱敏耗时。
9. 总命中次数。
10. 风险等级。
11. 是否接口级 ignore。
12. ignore reason。

风险等级建议：

| 触发情况 | 风险等级 |
|---|---|
| 命中 PASSWORD / SECRET / TOKEN | CRITICAL |
| 命中 ID_CARD / BANK_CARD | HIGH |
| 单接口单次脱敏字段数 >= 5 | HIGH |
| 命中 MOBILE / EMAIL / CHINESE_NAME | MEDIUM |
| 少量普通敏感字段 | LOW |
| 接口级 ignore 且 reason 表示业务明文展示 | HIGH 或 IGNORED_HIGH |

MVP 不做 log 场景接口归因、MDC 绑定 request path、异步线程日志接口归因、traceId 维度聚合、分布式接口风险统计。

### 7.14 Demo 示例项目

Demo 应覆盖：

1. starter 引入示例。
2. `application.yml` 配置示例。
3. `log4j2.xml` 配置示例。
4. UserDTO 示例。
5. OrderDTO 嵌套对象示例。
6. Map 返回示例。
7. List 返回示例。
8. 注解脱敏示例。
9. 字段级 ignore 示例。
10. 接口级 ignore 示例。
11. 日志打印脱敏示例。
12. 严格大陆身份证日志兜底示例。
13. 接口风险统计报告示例。
14. 基础统计报告示例。

验收标准：

1. Demo 可本地启动。
2. 可访问接口看到 response 脱敏效果。
3. 可查看控制台日志脱敏效果。
4. 可查看字段级 ignore 效果。
5. 可查看接口级 ignore 效果。
6. 可查看 response 接口风险统计。
7. 可查看基础统计报告。
8. Demo 说明文档清晰。

## 8. 非功能需求

### 8.1 兼容性

1. 必须兼容 JDK8。
2. 必须兼容 Spring Boot 2.x。
3. Spring Boot 2.x 自动装配必须支持 `spring.factories`。
4. 不使用 Java 9+ API。
5. 不强制依赖 Spring Security、MyBatis、配置中心。
6. MVP 优先支持 Log4j2 2.x。
7. 后续预留兼容 Logback 的日志适配扩展点。
8. 日志处理不强制引入 fastjson 或其他 JSON Parser。

### 8.2 性能

1. 普通 response 对象脱敏应控制在可接受耗时内。
2. 支持 Class 元信息缓存、字段规则缓存、正则 Pattern 缓存。
3. 支持最大递归深度、最大集合处理数量、日志最大 message 长度、日志 value 最大扫描长度限制。
4. 不对明显不可处理对象做深度扫描。
5. 统计采用内存聚合，文件报告由定时任务异步写入。

### 8.3 稳定性

1. 脱敏失败不影响业务接口返回。
2. 日志脱敏失败不影响日志输出。
3. 统计失败不影响脱敏主流程。
4. 配置错误应有提示，但不导致应用不可启动，除非是关键配置严重错误。
5. 对循环引用、超大集合和高频接口统计有保护。

### 8.4 安全

1. 统计报告不得保存脱敏前原始值。
2. 统计报告不得保存完整 response。
3. 统计报告不得保存完整日志。
4. 接口级 ignore 需要记录 reason。
5. 正则兜底不应对银行卡做无上下文全局脱敏。
6. 严格大陆身份证必须通过合法性校验后才脱敏。

### 8.5 可扩展性

1. 脱敏策略可扩展。
2. 规则匹配方式可扩展。
3. 输出场景可扩展。
4. ignore 类型可扩展。
5. 报告输出方式可扩展。
6. 未来可扩展权限动态脱敏、MyBatis、Logback、FastJson、Prometheus、Actuator、治理平台等能力。

## 9. MVP 边界

MVP 第一版不支持：

1. 基于用户权限的动态脱敏。
2. 根据角色显示明文。
3. MyBatis 查询结果脱敏。
4. 数据库存储加密。
5. 配置中心热更新。
6. 分布式规则管理。
7. 分布式统计平台。
8. Logback 日志脱敏。
9. FastJson 深度集成。
10. 业务文件导出内容脱敏。
11. 消息队列脱敏。
12. System.out 全局拦截。
13. 第三方 SDK 内部日志完全覆盖。
14. 全链路 DLP 平台能力。
15. Log 场景接口归因统计。
16. 每次脱敏明细落库。
17. 保存敏感原文。

## 10. 推荐项目结构

```text
safe-output
├── pom.xml
├── docs
│   ├── 01-feasibility-analysis.md
│   ├── 02-mvp-scope.md
│   ├── 03-architecture-design.md
│   └── 04-ai-coding-guide.md
├── safe-output-core
├── safe-output-spring-boot-starter
├── safe-output-log4j2
├── safe-output-report
└── safe-output-demo
```

## 11. Issue 拆分用 WBS

### 11.1 WBS 总览

| WBS 编号 | 工作包名称 | 对应模块 | 优先级 | 主要目标 |
|---|---|---|---|---|
| WBS-00 | 项目骨架与工程约束 | Root | P0 | 建立 Maven 多模块和 JDK8 约束 |
| WBS-01 | 核心领域模型 | core | P0 | 定义 MaskType、MaskScene、上下文、结果对象 |
| WBS-02 | 内置脱敏策略 | core | P0 | 实现手机号、身份证、银行卡等策略 |
| WBS-03 | 策略注册与扩展机制 | core/starter | P0 | 支持内置和自定义策略注册 |
| WBS-04 | 配置属性模型 | starter | P0 | 实现 YAML 配置绑定、ignore、report 配置 |
| WBS-05 | 规则匹配能力 | starter | P0 | 支持字段名、路径、注解、默认规则、ignore 优先级 |
| WBS-06 | 对象递归脱敏引擎 | starter | P0 | 支持 Bean、Map、Collection、Array |
| WBS-07 | Response 脱敏接入 | starter | P0 | 实现 ResponseBodyAdvice、接口级 ignore、接口统计 |
| WBS-08 | Log4j2 日志脱敏 | log4j2 | P0 | 实现 Log4j2 PatternConverter、JSON-like、正则边界 |
| WBS-09 | 统计指标与报告 | report | P1 | 实现内存聚合、定时文件快照、接口风险统计、报告输出 |
| WBS-10 | Demo 示例项目 | demo | P0 | 演示即插即用、ignore、统计报告 |
| WBS-11 | 测试体系 | all | P0 | 单元测试、集成测试、Demo 验证 |
| WBS-12 | 接入文档与演示材料 | docs | P1 | 编写接入说明和竞赛演示说明 |

### 11.2 WBS 拆分规则

后续从本文档拆 issue 时，建议每个 WBS 至少拆成一个纵向可验收 issue。若工作包过大，再按“核心接口、默认实现、配置集成、测试验收”拆成多个子 issue。

每个 issue 应包含：

1. 背景和目标。
2. 范围内事项。
3. 不做范围。
4. 依赖关系。
5. 验收标准。
6. 测试要求。

### 11.3 WBS 工作包说明

#### WBS-00 项目骨架与工程约束

目标：建立 Maven 多模块工程骨架，明确 JDK8、Spring Boot 2.x、依赖边界和基础编码约束。

输出物：根 `pom.xml`、各子模块 `pom.xml`、基础目录结构、初始 README、`META-INF/spring.factories` 示例。

不做范围：不实现具体脱敏逻辑、response 拦截、日志脱敏。

#### WBS-01 核心领域模型

目标：定义脱敏组件的核心领域模型，为所有场景提供统一抽象。

输出物：`MaskType`、`MaskScene`、`MaskContext`、`MaskResult`、`MaskStrategy`、基础单元测试。

不做范围：不实现 Spring Boot 自动装配、YAML 配置读取、对象递归、日志处理。

#### WBS-02 内置脱敏策略

目标：实现 MVP 内置敏感数据脱敏策略。

输出物：手机号、身份证、银行卡、邮箱、中文姓名、地址、密码、默认脱敏策略，严格大陆身份证校验工具，策略单元测试。

不做范围：不做字段识别、规则匹配、日志正则识别、权限判断。

#### WBS-03 策略注册与扩展机制

目标：实现策略注册、查找和扩展机制，支持内置策略和用户自定义策略。

输出物：`MaskStrategyRegistry`、Spring 集成注册逻辑、自定义策略示例、单元测试。

不做范围：不做权限动态策略、远程规则加载、配置中心热更新。

#### WBS-04 配置属性模型

目标：实现 `application.yml` 配置绑定，为即插即用、规则控制、ignore、统计报告提供配置基础。

输出物：`SafeOutputProperties`、规则配置类、ignore 配置类、report 配置类、配置绑定测试、YAML 示例。

不做范围：不做配置中心、运行时热更新、复杂规则表达式、远程配置拉取。

#### WBS-05 规则匹配能力

目标：实现敏感字段识别、规则匹配、模糊字段控制和 ignore 优先级能力。

输出物：`RuleMatcher`、`SensitiveFieldResolver`、`IgnoreMatcher`、`ApiIgnoreMatcher`、默认规则定义、模糊字段清单、注解解析逻辑、单元测试。

不做范围：不做权限判断、机器学习识别、数据库字段扫描、复杂表达式规则。

#### WBS-06 对象递归脱敏引擎

目标：实现 response 场景下的对象递归脱敏能力。

输出物：`ObjectMasker`、`BeanMasker`、`MapMasker`、`CollectionMasker`、`ArrayMasker`、单元测试。

不做范围：不处理日志字符串、数据库结果集、文件流或二进制对象，不做 response 场景全局正则扫描。

#### WBS-07 Response 脱敏接入

目标：通过 Spring MVC `ResponseBodyAdvice` 接入 response 脱敏能力，并实现接口级 ignore 和接口风险统计。

输出物：ResponseBodyAdvice 实现、自动装配配置、接口级 ignore 集成测试、接口统计集成测试、Demo Controller 示例。

不做范围：不实现 WebFlux、RPC/Dubbo Filter、Servlet Filter 全局响应包装、文件下载 response、log 接口归因。

#### WBS-08 Log4j2 日志脱敏

目标：实现 Log4j2 2.x 日志脱敏能力，覆盖 MVP 的脱敏打印场景，并为后续 Logback 适配预留边界。

输出物：`DesensitizeMessagePatternConverter`、`LogMasker`、`JsonLikeLogMasker`、`RegexLogMasker`、`MainlandIdCardDetector`、`log4j2.xml` 示例、日志脱敏测试。

不做范围：不实现 Logback、System.out 拦截、第三方 SDK 内部日志全覆盖、复杂自然语言敏感信息识别、log 与接口关联统计，不引入 JSON Parser 作为强依赖。

#### WBS-09 统计指标与报告

目标：实现基础脱敏统计、接口风险统计和报告输出能力。

输出物：`MaskMetricsCollector`、`MaskMetrics`、`ApiMaskMetrics`、`ApiRiskLevel`、`MaskReport`、`MaskReportExporter`、`ScheduledFileReportExporter`、报告 JSON 文件示例、单元测试。

不做范围：不做分布式聚合、数据库落库、Redis、Prometheus、可视化后台、强制 Actuator，不保存敏感原文、完整 response 或日志。

#### WBS-10 Demo 示例项目

目标：提供可运行 Demo，展示 MVP 即插即用能力和核心场景。

输出物：Demo 项目代码、Demo 配置文件、Demo 接口说明、Demo 演示步骤。

不做范围：不接入真实数据库、真实业务系统、前端页面、复杂登录权限。

#### WBS-11 测试体系

目标：建立 MVP 的基本测试体系，保证核心能力可验证。

输出物：单元测试代码、集成测试代码、测试数据、手工验收清单。

不做范围：不做大规模性能压测平台、安全扫描平台、全链路自动化测试。

#### WBS-12 接入文档与演示材料

目标：编写 MVP 接入说明和竞赛演示说明。

输出物：README、快速接入文档、配置说明、ignore 使用说明、Log4j2 接入说明、统计报告说明、Demo 演示步骤、MVP 验收清单、竞赛演示脚本初稿。

不做范围：不写完整竞赛 PPT、详细设计文档、后续版本完整方案。

## 12. 阶段性目标与里程碑

| 阶段 | 覆盖 WBS | 阶段目标 | 里程碑产物 | 通过标准 |
|---|---|---|---|---|
| P0 基础骨架阶段 | WBS-00、WBS-01 | 建立工程结构和核心领域模型 | 多模块 Maven 工程、核心枚举、上下文、结果对象 | JDK8 编译通过，核心模型单测通过 |
| P1 核心策略阶段 | WBS-02、WBS-03 | 完成内置策略和策略注册扩展 | 内置策略、策略注册器、自定义策略接口 | 手机号、邮箱、身份证、银行卡等策略单测通过 |
| P2 规则配置阶段 | WBS-04、WBS-05 | 完成 YAML 配置、默认规则、ignore 和优先级 | 配置属性模型、规则匹配器、默认规则库 | key、path、注解、ignore 优先级测试通过 |
| P3 Response 脱敏阶段 | WBS-06、WBS-07 | 完成对象递归脱敏和接口 response 接入 | 对象脱敏引擎、ResponseBodyAdvice、接口级 ignore | Demo 接口无需改 Controller 即可脱敏，接口级 ignore 生效 |
| P4 Log4j2 日志阶段 | WBS-08 | 完成 Log4j2 2.x 日志输出脱敏 | PatternConverter、JSON-like 识别、正则兜底 | Demo 日志中手机号、邮箱、身份证可脱敏，不引入 fastjson |
| P5 统计报告阶段 | WBS-09 | 完成基础统计、接口风险统计和本地文件快照 | 内存聚合指标、接口风险统计、JSON 报告文件 | 定时文件写入可配置，不保存敏感原文，不影响主流程 |
| P6 演示验收阶段 | WBS-10、WBS-11、WBS-12 | 完成 Demo、测试体系和接入文档 | Demo 项目、测试报告、接入说明、验收清单 | 可完整演示即插即用、response、log、ignore、统计报告 |

## 13. MVP 交付物

代码交付物：

1. `safe-output-core`
2. `safe-output-spring-boot-starter`
3. `safe-output-log4j2`
4. `safe-output-report`
5. `safe-output-demo`

文档交付物：

1. 可行性分析报告。
2. MVP 需求范围说明书。
3. 概要设计说明书。
4. 快速接入文档。
5. 配置说明文档。
6. ignore 使用说明。
7. Log4j2 接入说明。
8. 统计报告说明。
9. Demo 演示说明。
10. 验收清单。

测试交付物：

1. 策略单元测试。
2. 严格身份证校验测试。
3. 规则匹配测试。
4. ignore 测试。
5. 配置绑定测试。
6. Response 集成测试。
7. Log4j2 日志脱敏测试。
8. 统计报告测试。
9. Demo 验证清单。

## 14. MVP 成功标准

1. 能在 JDK8 + Spring Boot 2.x Demo 中运行。
2. 能通过 starter 完成自动装配。
3. 能通过 YAML 配置完成老项目字段脱敏。
4. 能在不修改 Controller 的情况下实现 response 脱敏。
5. 能通过 Log4j2 2.x 配置实现日志脱敏。
6. 能支持常见敏感数据类型。
7. 能处理模糊字段误脱敏风险。
8. 能支持字段级 ignore。
9. 能支持接口级 response ignore。
10. 能区分 response 和 log 两个场景。
11. 能输出基础统计报告。
12. 能按配置定时写入本地 JSON 统计报告文件。
13. 能输出 response 接口风险统计。
14. 统计报告不保存敏感原文。
15. 能演示即插即用价值。
16. 代码结构适合后续分模块继续迭代。

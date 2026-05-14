# Safe Output 项目代码概览

## 项目定位

面向 Spring Boot 2.x 的 Java 8 starter，在不改 Controller 业务代码的前提下，对 HTTP 响应体和 Log4j2 日志做敏感信息脱敏，并输出聚合统计报告。

- **groupId**: `com.safeoutput`
- **版本**: `0.1.0-SNAPSHOT`
- **Java 兼容性**: Java 8（通过 animal-sniffer 强制校验）
- **Spring Boot 版本**: `2.7.18`
- **测试框架**: JUnit Jupiter `5.10.2`

## 模块结构

```
safe-output/
├── pom.xml                          # 父 POM (reactor)
├── README.md                        # 项目总览
├── CODING_STANDARDS.md              # 编码规范
├── config/checkstyle/checkstyle.xml # Checkstyle 配置
├── doc/core.md                      # 核心领域文档
├── safe-output-core/                # 核心模块（无 Spring 依赖）
├── safe-output-log4j2/              # Log4j2 适配模块
├── safe-output-report/              # 报告与统计模块
├── safe-output-spring-boot-starter/ # Spring Boot Starter（对外入口）
└── safe-output-demo/                # Demo 示例应用
```

**模块依赖关系**:

```
safe-output-demo
  └── safe-output-spring-boot-starter
        ├── safe-output-core
        ├── safe-output-log4j2 ──> safe-output-core
        └── safe-output-report ──> safe-output-core
```

## 代码规模

| 模块 | 主代码文件 | 主代码行数 | 测试文件 | 测试行数 |
|---|---|---|---|---|
| safe-output-core | 32 | 2,347 | 7 | 1,042 |
| safe-output-log4j2 | 2 | 407 | 2 | 303 |
| safe-output-report | 15 | 1,194 | 2 | 325 |
| safe-output-spring-boot-starter | 6 | 782 | 9 | 838 |
| safe-output-demo | 5 | 377 | 1 | 162 |
| **合计** | **60** | **5,107** | **21** | **2,670** |

## 各模块详解

### safe-output-core — 核心引擎模块

**职责**: 定义脱敏领域模型、规则匹配、内置脱敏策略、注解解析和响应对象递归脱敏。不依赖 Spring。

**包名**: `com.safeoutput.core`

**核心接口**:

| 类 | 类型 | 职责 |
|---|---|---|
| `SafeOutputMaskService` | 接口 | 主动脱敏服务入口，提供 4 个方法：`mask(value, type)`, `maskObject(value)`, `maskStrong(value)`, `maskObjectStrong(value)` |
| `MaskStrategy` | 接口 | 脱敏策略扩展点，定义 `type()` 和 `mask(rawValue, context)` |
| `MaskEventRecorder` | 接口 | 脱敏事件记录钩子，`recordMask(scene, type, elapsedNanos)` |
| `ResponseRiskRecorder` | 接口 | 响应风险事件记录钩子，`record(ResponseRiskEvent)` |
| `UnknownTypeRecorder` | 接口 | 未知类型记录钩子，`recordUnknownType(type, scene)` |
| `LogRuleSuggestionCollector` | 接口 | 日志规则建议采集钩子，`record(event)` + `snapshotSuggestions()` |

**核心实现类**:

| 类 | 职责 |
|---|---|
| `DefaultSafeOutputMaskService` | `SafeOutputMaskService` 默认实现，组合 `MaskStrategyRegistry` + `ObjectMasker` + `StrongTextMasker` + `StrongObjectMasker` |
| `MaskStrategyRegistry` | 策略注册中心，管理内置策略和自定义策略，支持 `find(type)` 查找 |
| `BuiltInMaskStrategies` | 8 种内置脱敏策略: MOBILE, ID_CARD, BANK_CARD, EMAIL, CHINESE_NAME, ADDRESS, PASSWORD, DEFAULT |
| `MaskRuleMatcher` | 规则匹配器，优先级：API Ignore > 字段 Ignore > `@Desensitize` 注解 > 配置 Rule > 默认 Rule > Regex fallback |
| `ObjectMasker` | 递归对象脱敏引擎，遍历 Bean/Map/Collection/Array，含循环引用保护和深度/大小限制 |
| `StrongTextMasker` | 强扫描文本脱敏器，处理 key-value 片段 + regex fallback（手机号/邮箱/身份证/银行卡） |
| `StrongObjectMasker` | 强扫描对象脱敏器，对对象中所有字符串执行 `StrongTextMasker` |
| `SensitiveFieldResolver` | 字段敏感度解析器，带 `ConcurrentHashMap` 缓存，解析 `@Desensitize` 注解 |
| `InMemoryLogRuleSuggestionCollector` | 内存日志规则建议收集器，按 `key:type` 聚合命中次数和时间 |

**核心模型类**:

| 类 | 说明 |
|---|---|
| `MaskType` | 枚举: UNKNOWN, MOBILE, EMAIL, ID_CARD, BANK_CARD, CHINESE_NAME, ADDRESS, PASSWORD, DEFAULT |
| `MaskTypes` | String 类型常量（核心贯穿类型标签）：`unknown`, `mobile`, `email`, `id_card`, `bank_card`, `chinese_name`, `address`, `password`, `default` |
| `MaskScene` | 枚举: UNKNOWN, RESPONSE, LOG, MANUAL, REPORT |
| `RuleSource` | 枚举: API_IGNORE, FIELD_IGNORE, ANNOTATION, CONFIGURED, DEFAULT, REGEX_FALLBACK |
| `RuleAction` | 枚举: MASK, IGNORE |
| `MaskRule` | 规则定义: name, keys, paths, type, enabled, source |
| `MaskRuleRequest` | 规则匹配请求: key, path, apiIgnored, annotationType, regexFallbackType |
| `RuleMatch` | 规则匹配结果: maskType, ruleName, source, action |
| `MaskContext` | 脱敏上下文: maskType, scene, path, fieldName, rawValue |
| `MaskResult` | 脱敏结果: context, value, masked |
| `MaskingResult` | 对象脱敏结果: value, maskTypeCounts, maskedFieldCount |
| `ObjectMaskerOptions` | 对象脱敏选项: maxDepth(默认8), maxCollectionSize(默认1000) |
| `Desensitize` | 字段注解 `@Desensitize(type = "...")` |
| `MainlandIdCards` | 大陆身份证校验工具：格式 + 生日 + 校验位 |
| `ResponseRiskEvent` | 响应风险事件: method, path, apiKey, ignored, failed, maskedFieldCount, maskTypeCounts, elapsedNanos |
| `LogRuleSuggestionEvent` | 日志规则建议事件: key, type, evidence, seenTimeMillis |
| `LogRuleSuggestionMetric` | 日志规则建议聚合指标: key, type, hitCount, firstSeenTimeMillis, lastSeenTimeMillis, evidence |

**内置脱敏策略具体规则**:

- MOBILE: `1[3-9]\d{9}` 匹配后 `138****8000`
- ID_CARD: 18 位，含格式/生日/校验位验证，`110105********002X`
- BANK_CARD: 12-19 位纯数字，`622202****90123`
- EMAIL: `foo****@example.com`
- CHINESE_NAME: 首尾保留中间 `*`，如 `张*` / `张*三`
- ADDRESS: 中文开头保留前6字符 + `****`
- PASSWORD: 统一替换为 `********`
- DEFAULT: 保留首尾各2字符 + `****`

### safe-output-log4j2 — Log4j2 适配模块

**职责**: 提供 `%safeOutputMsg` / `%safeOutputMessage` Log4j2 PatternConverter，在日志消息输出前执行 key-value 脱敏和可选 regex fallback。

**包名**: `com.safeoutput.log4j2`

**依赖**: `safe-output-core` + `log4j-core`(optional)

**核心类**:

| 类 | 职责 |
|---|---|
| `SafeOutputMessagePatternConverter` | Log4j2 `LogEventPatternConverter` 实现，注册为 `%safeOutputMsg` / `%safeOutputMessage`，解析选项 (enabled, regexFallback, maxMessageLength, maxValueLength, idCardCheckCode, keyValueRuleEnabled, maxRuleKeys) |
| `SafeOutputLogMessageMasker` | 日志消息脱敏引擎，两阶段处理：1) key-value 规则匹配 2) regex fallback（手机号/邮箱/身份证）。regex fallback 命中后采集 nearbyKey 规则线索 |

**日志脱敏选项**:

- `enabled`: 启用/禁用（默认 true）
- `keyValueRuleEnabled`: key-value 规则（默认 true）
- `regexFallback`: 正则兜底（默认 true）
- `idCardCheckCode`: 身份证校验位检查（默认 true）
- `maxMessageLength`: 超长消息跳过（默认 5000）
- `maxValueLength`: 超长值跳过（默认 300）
- `maxRuleKeys`: 参与匹配的字段名上限（默认 128）

### safe-output-report — 报告与统计模块

**职责**: 聚合脱敏指标、生成接口风险画像、性能画像、Log 规则建议，导出本地 JSON 报告快照。

**包名**: `com.safeoutput.report`

**依赖**: `safe-output-core`

**核心类**:

| 类 | 职责 |
|---|---|
| `MaskMetricsCollector` | 核心指标聚合器，同时实现 `ResponseRiskRecorder`、`UnknownTypeRecorder`、`MaskEventRecorder`、`LogRuleSuggestionCollector` 四个接口 |
| `MaskReport` | 报告快照数据模型 |
| `MaskReportExporter` | 定时/手动导出 JSON 报告到本地文件，支持 `retainFiles` 保留策略 |
| `MaskReportExportOptions` | 报告导出配置: directory, filePrefix, intervalMillis, retainFiles |
| `ApiMaskMetrics` | 接口维度指标: method, path, hitCount, ignored, failureCount, maskedFieldCount, 耗时, slowMaskCount, maskTypeCounts, riskLevel |
| `ApiRiskLevel` | 枚举: LOW, MEDIUM, HIGH, CRITICAL, IGNORED_HIGH |
| `ResponseRiskAnalyzer` | 响应风险分析器，按风险评分排序接口 |
| `ResponseRiskAnalysis` | 响应风险分析结果: summary + topRiskApis + ignoredRiskApis |
| `ResponseRiskSummary` | 风险概要: apiCount, highRiskApiCount, ignoredApiCount, slowApiCount |
| `ResponseRiskApiProfile` | 接口风险画像: riskScore(0-100), riskLevel, riskReasons, governanceAdvice, performanceProfile, maskTypeCounts |
| `PerformanceProfile` | 性能画像: averageElapsedNanos, maxElapsedNanos, slowMaskCount, warnings |
| `LogRuleSuggestionAnalyzer` | Log 规则建议分析器，将 fallback 线索转换为规则建议和 YAML 片段 |
| `LogRuleSuggestion` | 规则建议: key, suggestedType, hitCount, confidence(LOW/MEDIUM/HIGH), evidence, effectScopes, autoApply(默认 false) |
| `LogRuleSuggestionReport` | 规则建议报告: suggestions + configSnippet |
| `LogRuleSuggestionConfidence` | 枚举: LOW, MEDIUM, HIGH |

**风险评分规则**:

- PASSWORD 类型: +45 分
- ID_CARD/BANK_CARD: +35 分
- 高字段数 (>=5): +20 分
- 高频次 (>=10): +15 分
- IGNORED 响应: +30 分
- >=80: CRITICAL, >=50: HIGH, >0: MEDIUM, 0: LOW

### safe-output-spring-boot-starter — Spring Boot Starter

**职责**: 面向业务系统的唯一接入入口，通过 `spring.factories` 自动装配核心 Bean 和 MVC 响应脱敏。

**包名**: `com.safeoutput.spring.boot.autoconfigure`

**依赖**: safe-output-core + safe-output-log4j2 + safe-output-report + spring-boot-autoconfigure(optional) + spring-webmvc(optional)

**spring.factories 注册**:

```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
  com.safeoutput.spring.boot.autoconfigure.SafeOutputAutoConfiguration,\
  com.safeoutput.spring.boot.autoconfigure.SafeOutputMvcAutoConfiguration
```

**核心类**:

| 类 | 职责 |
|---|---|
| `SafeOutputAutoConfiguration` | 自动装配入口，创建核心 Bean |
| `SafeOutputMvcAutoConfiguration` | MVC 自动装配，注册 `SafeOutputResponseBodyAdvice` |
| `SafeOutputResponseBodyAdvice` | `@ControllerAdvice`，在 JSON 序列化前拦截响应体执行脱敏 |
| `SafeOutputProperties` | `@ConfigurationProperties(prefix = "safe-output")`，嵌套配置类 |
| `ApiIgnoreMatcher` | API 忽略匹配器，使用 `AntPathMatcher` 匹配路径 |
| `ApiIgnoreMatch` | API 忽略匹配结果 |

**SafeOutputProperties 配置项**:

- `safe-output.enabled` (默认 true)
- `safe-output.mask-char` (默认 "*")
- `safe-output.max-depth` (默认 8)
- `safe-output.max-collection-size` (默认 1000)
- `safe-output.response.enabled` (默认 true)
- `safe-output.log.framework` (默认 LOG4J2)
- `safe-output.log.key-value-rule-enabled` (默认 true)
- `safe-output.log.max-rule-keys` (默认 128)
- `safe-output.manual.strong-scan.types` (自定义强扫描类型)
- `safe-output.strategy.unknown-type-policy` (默认 SKIP)
- `safe-output.rules[].name/keys/paths/type/enabled`
- `safe-output.ignore.keys/paths/apis`
- `safe-output.report.enabled/directory/file-prefix/interval-millis/retain-files`

### safe-output-demo — 示例应用

**职责**: Spring Boot 2.x 端到端 Demo，演示 response 脱敏、Log4j2 日志脱敏和报告导出。

**包名**: `com.safeoutput.demo`

**核心类**:

| 类 | 职责 |
|---|---|
| `DemoApplication` | 启动类，注册自定义 `mobileM` 策略 Bean |
| `DemoResponseController` | 响应脱敏演示: `/demo/bean`, `/demo/map`, `/demo/list`, `/demo/nested`, `/demo/ignored` |
| `DemoLogController` | 日志脱敏演示: `/demo/logs` |
| `DemoManualMaskController` | 主动脱敏演示: `/demo/mask/by-type`, `/demo/mask/object`, `/demo/mask/strong` |
| `DemoReportController` | 报告演示: `/demo/report/snapshot`, `/demo/report/export`, `/demo/report/response-risk`, `/demo/report/log-suggestions` |

## 测试覆盖

| 模块 | 测试类 | 覆盖范围 |
|---|---|---|
| core (7个) | `MaskContractTest`, `BuiltInMaskStrategiesTest`, `MaskStrategyRegistryTest`, `MaskRuleMatcherTest`, `SensitiveFieldResolverTest`, `ObjectMaskerTest`, `SafeOutputMaskServiceTest` | 核心契约、内置策略、策略注册、规则匹配、注解解析、递归对象脱敏、主动脱敏服务 |
| log4j2 (2个) | `SafeOutputMessagePatternConverterTest`, `SafeOutputLogMessageMaskerTest` | Converter 发现/选项、key-value 脱敏/regex fallback/边界 |
| report (2个) | `MaskReportExporterTest`, `MaskMetricsCollectorTest` | JSON 导出/文件保留/写入失败、指标聚合/overflow/风险等级 |
| starter (9个) | `SpringFactoriesTest`, `ProjectSkeletonTest`, `SafeOutputAutoConfigurationTest`, `SafeOutputPropertiesBindingTest`, `SafeOutputMaskServiceAutoConfigurationTest`, `SafeOutputRuleMatcherAutoConfigurationTest`, `SafeOutputReportAutoConfigurationTest`, `SafeOutputLog4j2StarterIntegrationTest`, `SafeOutputResponseBodyAdviceIntegrationTest` | spring.factories/骨架、属性绑定、自动装配各 Bean、Log4j2 集成、ResponseAdvice 集成 |
| demo (1个) | `DemoResponseIntegrationTest` | Spring Boot 2.x 端到端 |

## 关键设计决策

1. **fail-open 原则**: 所有脱敏路径都捕获 RuntimeException 并返回原始值，确保安全组件异常不会放大为业务故障。

2. **String 类型标签贯穿**: 使用 `MaskTypes` 定义的 String 常量（如 `"mobile"`, `"id_card"`）而非 `MaskType` 枚举贯穿规则、策略、上下文、结果和统计链路，支持业务自定义类型。

3. **规则优先级固定**: API Ignore > 字段 Ignore > `@Desensitize` 注解 > 配置 Rule > 默认 Rule > Regex fallback，不支持自定义优先级。

4. **无上下文不做银行卡兜底**: 日志 regex fallback 只覆盖手机号、邮箱、身份证，不做银行卡全局兜底，避免误伤流水号。

5. **报告不保存敏感原文**: 报告快照只保存聚合指标和脱敏后的 evidence，不保存敏感字段原始值、完整 response 或完整日志。

6. **Java 8 强制校验**: 通过 `animal-sniffer-maven-plugin` 和 `maven-enforcer-plugin` 确保 API 兼容 Java 8。

7. **模块边界清晰**: core 不依赖 Spring；Log4j2 代码不进入 core；报告持久化和调度逻辑不进入 core 脱敏策略代码。

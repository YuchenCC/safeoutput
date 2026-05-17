# 核心流程地图

## Response 脱敏完整调用链

`SafeOutputResponseBodyAdvice.supports` -> `beforeBodyWrite` -> `matchApiIgnore` 或 `maskDataPath` / `objectMasker.maskWithResult` -> `ObjectMasker.maskValue` 递归 Bean/Map/List/数组 -> `SensitiveFieldResolver.resolve` / `MaskRuleMatcher.decide` -> `MaskStrategyRegistry.find` -> `MaskStrategy.apply` -> `recordRisk` -> 输出脱敏后的 body。风险点：Bean 原地修改；`body-data-path` 反射找不到字段时 fail-open 返回原 body。

## 日志脱敏完整调用链

Log4j2 PatternLayout `%safeOutputMsg{...}` -> `SafeOutputMessagePatternConverter.newInstance` -> 优先从 `SafeOutputLog4j2Runtime` 读取 starter 注册的 `MaskRuleMatcher` / `MaskStrategyRegistry` / log 选项，无 Spring 注册时回退默认规则 -> `SafeOutputLogMessageMasker.mask` -> key-value/JSON-like 正则匹配 -> `keyValueMatches` 查规则 -> 策略脱敏 -> `maskFallback` 扫 mobile/email/idCard -> 输出日志 message。风险点：runtime bridge 是进程级静态配置，适合单应用上下文；starter 中 `safe-output.rules.default-enabled=false` 会让默认 key 不进入日志 key-value 匹配；超过 `maxMessageLength` 的日志整条 fail-open。

## 配置加载与规则匹配链路

`SafeOutputProperties` 绑定 `safe-output.rules[]` / `ignore.*`，`SafeOutputAutoConfiguration.maskRuleMatcher` 从 Environment 读取 `safe-output.rules.default-enabled` -> 构造 `MaskRule.configured` -> `MaskRuleMatcher.builder` -> `decide` 固定优先级 -> 输出 `RuleMatch` 或 empty。风险点：配置规则目前低于字段注解；path 只支持精确等值匹配；默认规则开关只影响内置默认规则，不影响配置规则和注解。

## 注解解析链路

字段上 `@Desensitize(type=...)` -> `ObjectMasker.maskBean` -> `SensitiveFieldResolver.resolve(field, path)` -> 字段元数据缓存 -> `MaskRuleRequest.annotationType` -> `MaskRuleMatcher.decide` -> 输出注解类型 `RuleMatch`。风险点：只支持字段注解，不支持 getter、类级或方法级注解。

## 脱敏策略选择链路

`RuleMatch.maskType` -> `MaskStrategyRegistry.find(type)` -> 内置策略或 Spring 注入的自定义 `MaskStrategy` -> `MaskContext` 携带 scene/path/field/rawValue -> `MaskResult` -> 计数。风险点：未知 type 默认 warn + skip，不回退 `DEFAULT`。

## 统计指标采集与报告生成链路

`ObjectMasker.applyStrategy` / `DefaultSafeOutputMaskService` -> `MaskEventRecorder.recordMask` -> `MaskMetricsCollector` 聚合总量、场景、类型、耗时 -> `SafeOutputResponseBodyAdvice.recordRisk` -> `ResponseRiskRecorder.record` -> API 维度指标 -> `MaskReportExporter.exportNow` -> `MaskReport.snapshot` + `ResponseRiskAnalyzer` + `LogRuleSuggestionAnalyzer` -> 本地 JSON 文件。风险点：内存聚合，无持久化；接口维度超过上限进入 overflow。

## ignore 生效链路

字段级：`safe-output.ignore.keys/paths` -> `MaskRuleMatcher.matchFieldIgnore` -> `RuleAction.IGNORE` -> `ObjectMasker` 不脱敏该字段。接口级：`safe-output.ignore.apis` -> `ApiIgnoreMatcher.match(method,path,RESPONSE)` -> `beforeBodyWrite` 直接返回原 body -> `recordRisk(ignored=true)`。风险点：字段 path 是精确匹配；API ignore 缺少 path/pattern 不会扩大成全局豁免。

## 异常兜底链路

Response：`beforeBodyWrite` 捕获 `RuntimeException` -> 记录 failed 风险事件 -> 返回原 body。对象递归：反射/策略异常 -> 当前分支 fail-open。日志：converter 初始化或 mask 异常 -> 输出原 message。报告：导出异常 -> `recordFailure` + warning。风险点：fail-open 符合不影响业务，但异常时可能输出明文，应依赖报告 failure 指标发现。

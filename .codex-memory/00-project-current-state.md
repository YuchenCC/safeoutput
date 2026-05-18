# 当前项目状态

## 项目目标

Safe Output 是面向 Spring Boot 2.x / Java 8 老项目的通用数据脱敏 starter，目标是在尽量少改业务代码的前提下，对 Response、Log4j2 日志和主动调用场景做敏感信息脱敏，并输出不含敏感原文的聚合统计报告。

## 第一、二轮完成范围

- 第一轮：Maven 多模块骨架、core 策略与规则、Spring Boot starter 自动装配、Response 脱敏、Log4j2 接入、Demo 和基础测试。
- 第二轮：String 类型标签、自定义策略、主动脱敏、Response 风险画像、性能画像、Log 规则建议、报告扩展和 Demo 前端控制台。

## 已实现能力

- Spring Boot 2.x 自动装配：`safe-output-spring-boot-starter/src/main/resources/META-INF/spring.factories` 注册 `SafeOutputAutoConfiguration`、`SafeOutputMvcAutoConfiguration`。
- 注解模式脱敏：`@Desensitize(type=...)`，由 `SensitiveFieldResolver` 解析字段注解。
- 配置模式脱敏：`safe-output.rules[]` 绑定到 `SafeOutputProperties`，转换为 `MaskRule` 后进入 `MaskRuleMatcher`；`safe-output.rules.default-enabled=false` 可在自动装配时关闭默认规则库。
- 内置默认字段规则库集中在 `DefaultMaskRules`，供 `MaskRuleMatcher` 使用；文档默认规则表应以该类为准。
- 内置策略：`MOBILE`、`ID_CARD`、`BANK_CARD`、`EMAIL`、`CHINESE_NAME`、`ADDRESS`、`PASSWORD`、`DEFAULT`。
- 对象递归脱敏：`ObjectMasker` 支持 Bean、Map、Collection、数组，带最大深度、集合上限、循环引用保护。
- Response 返回值脱敏：`SafeOutputResponseBodyAdvice` 在 JSON 序列化前处理，可配置 `response.body-data-path`。
- ignore：字段级 `ignore.keys` / `ignore.paths`；接口级 `ignore.apis` 命中后明文返回但记录风险事件。
- 日志接入：Log4j2 `%safeOutputMsg`，实现类 `SafeOutputMessagePatternConverter`。
- JSON-like 日志轻量识别：`SafeOutputLogMessageMasker` 用 key-value 正则处理 `"key":"value"`、`key=value` 等片段。
- Spring Boot starter 会把 `safe-output.rules[].keys`、`safe-output.ignore.keys`、自定义 `MaskStrategy`、默认规则开关、`safe-output.log.*` 选项和 report collector 桥接给真实 `%safeOutputMsg`。
- 统计采集、风险分析、报告输出：`MaskMetricsCollector`、`ResponseRiskAnalyzer`、`MaskReportExporter`；开启 report 后真实 Log4j2 脱敏会记录 `LOG` 计数和 fallback 规则线索。
- 报告导出、Demo Dashboard 和 Demo Log 规则建议接口会复用 `safe-output.rules[].keys` 过滤已配置 key，避免重复治理建议；过滤只影响报告建议，不影响 Log4j2 在线脱敏和聚合计数。
- Demo：R2.5 主入口已升级为“业务系统敏感数据治理驾驶舱”，覆盖业务工作台、接入说明、脱敏实验室、日志场景和报告中心；旧 Response/Log/Manual/Report 兼容接口仍保留，但不再作为默认首屏。
- R2.5 Demo 业务域：客户、订单、支付、工单、账户 mock 数据源与业务服务，业务接口覆盖 Bean、Map、Collection、嵌套对象，并覆盖 `MOBILE`、`ID_CARD`、`BANK_CARD`、`EMAIL`、`CHINESE_NAME`、`ADDRESS`、`PASSWORD`、`DEFAULT`。
- R2.5 Demo 前端：`safe-output-demo/src/main/resources/static/index.html` 只保留壳层，页面拆到 `static/css/app.css`、`static/js/api.js`、`static/js/views/*` 和 `static/js/components/*`；默认路由为 `#workbench`，继续使用本地 `vendor/` 资源。
- R2.5 报告中心：Demo 可导出、列出、读取配置报告目录内的 JSON 报告，并基于 JSON 聚合字段派生单报告 dashboard；读取限制在 `safe-output.report.directory` 且只允许 `file-prefix-*.json`，不读取任意文件。
- 测试：core、starter、log4j2、report、demo 均有单元或集成测试。

## 未实现或未确认能力

- 未确认支持 Spring Boot 3.x：当前以 Boot 2.7.18、`spring.factories`、`javax.servlet` 测试为主。
- 未实现强 JSON Parser 日志解析：设计上只做轻量 JSON-like 识别。
- 未实现报告持久化数据库或可视化后端，只输出本地 JSON 快照和 Demo 聚合接口。
- 未实现自动采纳配置建议，`LogRuleSuggestionAnalyzer` 生成的候选规则默认 `enabled: false`。
- 未确认支持跨应用上下文并发隔离的 Log4j2 runtime bridge；当前按单应用 Spring Boot 进程使用。

## 已知风险

- `ObjectMasker` 对 Bean 是原地修改字段；如果调用方复用响应对象实例，需要注意副作用。
- `MaskRuleMatcher.decide` 当前确认优先级是 API ignore / 字段 ignore > 注解 > 配置/默认规则 > regex fallback；后续改动需同步测试该优先级边界。
- 日志和强扫描允许对 `MOBILE` / `ID_CARD` / `EMAIL` 做无上下文 regex fallback；除手机、身份证、邮箱外，不做无上下文全局兜底。
- 日志 `%safeOutputMsg` 在 starter 场景会消费 Spring `safe-output.rules[]`、默认规则开关、自定义策略和 report collector；无 Spring runtime bridge 时回退到内置默认规则。
- 报告 JSON 使用手写序列化，字段较稳定但不是通用 JSON 序列化框架。

## 成熟度判断

当前代码达到可演示、可集成验证的 MVP/R2 成熟度：核心链路清晰、fail-open 边界明确、测试覆盖主要场景。仍不宜视为生产级全场景脱敏网关，第三轮应优先补齐配置一致性、报告/画像可解释性和 Demo 展示质量。

## 第三轮适合扩展方向

- 强化 Demo 竞赛展示看板和交互式验证；R2.5 已完成主路径重构，后续可继续做浏览器人工视觉验收和截图级 polish。
- 增强 Response 风险画像、接口治理建议和性能分析。
- 增强 Log 规则建议的配置生成、人工确认流和采纳状态。
- 预留 Agent 摘要接口，但继续保持报告不保存敏感原文。

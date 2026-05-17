# 第三轮交接

## 当前最稳定的基础能力

- core 规则匹配、内置策略、自定义策略注册、对象递归脱敏。
- starter 自动装配和 ResponseBodyAdvice 接入。
- starter 到 Log4j2 `%safeOutputMsg` 的 runtime bridge，可复用 Spring 配置规则、自定义策略、日志选项、日志脱敏计数和日志规则建议采集。
- 默认规则库总开关：`safe-output.rules.default-enabled=false` 可关闭内置默认字段规则，配置规则和注解仍生效。
- report 聚合指标、Response 风险画像、真实 Log4j2 `LOG` 计数和 fallback 规则线索、本地 JSON 快照。
- demo 端到端测试覆盖 Response、Log、Manual、Report。

## 当前最适合扩展的模块

- `safe-output-report`：风险画像、统计图表数据、Agent 摘要和配置建议都应从聚合模型扩展。
- `safe-output-demo`：适合增强展示看板、交互式验证和接入指南。
- `safe-output-spring-boot-starter`：适合补齐配置一致性和暴露更多可插拔 Bean。

## 当前不建议大改的模块

- `safe-output-core/ObjectMasker`：递归、fail-open、统计和规则耦合较集中，除非先补测试。
- `safe-output-log4j2/SafeOutputLogMessageMasker`：正则边界容易引入误伤，改动前先明确 fallback 策略。
- 父 POM Java 8 / Boot 2.x 基线：第三轮若无明确需求，不建议升级。

## 第三轮切入建议

- 增强 Demo：从 `safe-output-demo/src/main/resources/static/index.html`、`DemoReportController`、`DemoManualMaskController` 切入。
- 增强风险画像：从 `ResponseRiskAnalyzer`、`ApiMaskMetrics`、`ResponseRiskApiProfile`、`MaskReportExporter.toJson` 切入。
- 增强统计图表：优先扩展 `DemoReportController.dashboard` 返回结构，再更新 `static/index.html`。
- 增强 Agent 分析摘要：预留 `MaskReport` / `ResponseRiskAnalysis` 到摘要 DTO 的纯函数接口；输入只用聚合指标，不传原始 response/log。
- 增强配置建议生成：从 `LogRuleSuggestionAnalyzer` 扩展，保留 `enabled:false` 和人工确认；可增加建议来源、影响范围、置信度原因。
- Log4j2 report bridge 已补齐：`/demo/logs` 可产生真实 `LOG` 计数和 `phoneNo` / `certNum` / `mailAddr` fallback 规则线索，后续不要再用 Demo controller 手动 seed 日志建议。

## 编码前必须阅读

- `.codex-memory/00-project-current-state.md`
- `.codex-memory/01-module-map.md`
- `.codex-memory/02-core-flow-map.md`
- `.codex-memory/03-decision-and-boundary.md`
- `safe-output/README.md`
- `safe-output/safe-output-core/src/main/java/com/safeoutput/core/MaskRuleMatcher.java`
- `safe-output/safe-output-core/src/main/java/com/safeoutput/core/ObjectMasker.java`
- `safe-output/safe-output-spring-boot-starter/src/main/java/com/safeoutput/spring/boot/autoconfigure/SafeOutputResponseBodyAdvice.java`
- `safe-output/safe-output-report/src/main/java/com/safeoutput/report/MaskMetricsCollector.java`

## 测试前必须运行

- 快速验证：`cd safe-output && mvn test`
- 指定 demo：`cd safe-output && mvn -pl safe-output-demo -am test`
- 指定 starter：`cd safe-output && mvn -pl safe-output-spring-boot-starter -am test`
- 发布前完整验证：`cd safe-output && mvn verify`


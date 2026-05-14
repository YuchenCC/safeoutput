# safe-output-report

`safe-output-report` 是 Safe Output 的聚合统计和本地报告模块。它只保存聚合指标，不保存敏感原文、完整响应或完整日志。

## 职责

- 通过 `MaskMetricsCollector` 聚合脱敏次数、场景、String 类型标签、失败次数和耗时。
- 通过 `ResponseRiskRecorder` 接收响应接口风险事件。
- 通过 `ApiMaskMetrics` 维护接口级命中、Ignore、失败、字段数量、耗时、慢脱敏次数和风险等级。
- 通过 `ResponseRiskAnalyzer` 在报告快照阶段生成接口风险画像和性能画像。
- 通过 `LogRuleSuggestionAnalyzer` 将 Log fallback 线索转换为人工确认的规则建议和 YAML 片段。
- 通过 `MaskReportExporter` 定时或手动导出本地 JSON 报告快照。

## 报告内容

报告快照包含：

- 总脱敏次数、响应脱敏次数、日志脱敏次数、主动脱敏次数。
- 失败次数、平均耗时、最大耗时。
- 按 String 类型标签聚合的计数，内置类型使用 `MaskTypes` 标准值。
- 按未知 String 类型标签聚合的计数，用于发现配置了但未注册策略的 type。
- 接口维度的稳定接口标识、命中次数、Ignore 状态、Ignore 原因、失败次数、脱敏字段数量、平均耗时、最大耗时、慢脱敏次数和风险等级。
- `responseRiskSummary`、`topRiskApis`、`ignoredRiskApis`，其中敏感风险原因和性能告警分开展示。
- `logRuleSuggestions` 和 `configSnippet`，建议默认 `autoApply=false`，只为中高置信度线索生成候选配置片段。

报告快照不包含：

- 敏感字段原始值。
- 完整 response。
- 完整日志 message。

## 保留策略

`MaskReportExporter` 会按文件名前缀和 `.json` 后缀筛选报告文件，并保留最新 `retainFiles` 个快照。导出失败会增加失败计数并记录 warning，不影响业务脱敏流程。

## 本模块验证

在 `safe-output/` 根目录执行：

```sh
mvn -pl safe-output-report test
```

测试覆盖指标聚合、接口 overflow、风险等级、JSON 导出、文件保留和写入失败。

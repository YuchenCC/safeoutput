# Safe Output Report 模块梳理与使用指南

本文档基于当前代码整理 `safe-output-report` 报告模块的能力边界、数据流、输出字段和接入方式。报告模块只处理聚合指标和脱敏后的 evidence，不保存敏感原文、完整 response 或完整日志。

## 模块定位

`safe-output-report` 是 Safe Output 的统计与报告模块，位于：

```text
safe-output/safe-output-report/
```

它不是业务方通常直接依赖的入口。业务系统引入 `safe-output-spring-boot-starter` 后，在开启 `safe-output.report.enabled=true` 时，starter 会自动创建：

- `MaskMetricsCollector`：内存聚合指标采集器。
- `MaskReportExporter`：定时和手动本地 JSON 报告导出器。

模块职责包括：

- 聚合 response、log、manual 三类脱敏场景的命中次数；开启报告后，starter 会把 response、log 和 manual 脱敏事件写入 `MaskMetricsCollector`。
- 按 String 类型标签统计脱敏类型分布。
- 统计未知类型标签，用于发现配置错误或策略未注册。
- 聚合接口维度 response 风险事件。
- 生成 response 风险画像、性能画像和治理建议。
- 汇总 log regex fallback 规则线索，生成候选配置片段；开启报告后，真实 Log4j2 `%safeOutputMsg` fallback 线索会进入 `MaskMetricsCollector`。
- 导出本地 JSON 快照并按数量保留最新报告文件。

## 核心类

| 类 | 职责 |
|---|---|
| `MaskMetricsCollector` | 内存聚合采集器，同时实现 `MaskEventRecorder`、`ResponseRiskRecorder`、`UnknownTypeRecorder`、`LogRuleSuggestionCollector` |
| `MaskReport` | 当前聚合快照模型 |
| `ApiMaskMetrics` | 单个接口的命中、ignore、失败、字段数量、耗时和类型分布 |
| `ResponseRiskAnalyzer` | 基于接口聚合指标生成风险评分、风险等级、原因和建议 |
| `ResponseRiskAnalysis` | response 风险画像结果 |
| `ResponseRiskApiProfile` | 单个接口的风险画像 |
| `PerformanceProfile` | 接口脱敏性能画像 |
| `LogRuleSuggestionAnalyzer` | 将日志 fallback 线索转换为规则建议和 YAML 片段 |
| `LogRuleSuggestionReport` | 日志规则建议报告 |
| `MaskReportExporter` | 定时或手动导出 JSON 文件 |
| `MaskReportJsonWriter` | 手写 JSON 序列化，只输出聚合字段 |
| `MaskReportExportOptions` | 导出目录、文件前缀、间隔和保留数量 |

## 数据来源

### 1. 通用脱敏事件

成功脱敏后，core 通过 `MaskEventRecorder.recordMask(scene, type, elapsedNanos)` 记录：

- `scene`：`RESPONSE`、`LOG`、`MANUAL`。
- `type`：标准化后的 String 类型标签，例如 `mobile`、`email`、`id_card`。
- `elapsedNanos`：单次策略执行耗时。

当前自动来源：

- Response 对象递归脱敏：`ObjectMasker` 记录 `RESPONSE`。
- Log4j2 `%safeOutputMsg` 成功脱敏 key-value 或 regex fallback 值后记录 `LOG`。
- 主动脱敏：`DefaultSafeOutputMaskService` 和 `StrongTextMasker` 记录 `MANUAL`。

补充说明：`logCount` 的统计单位是成功脱敏的日志值次数，不是日志行数。一行日志中多个字段或 fallback 值被脱敏时会累计多次；超长 message fail-open 或日志脱敏禁用时不会记录成功计数。

### 2. Response 接口风险事件

`SafeOutputResponseBodyAdvice` 在每次 response 脱敏后记录 `ResponseRiskEvent`，包括：

- HTTP method。
- 原始 path。
- 稳定接口 key。
- 是否命中 API ignore。
- ignore reason。
- 是否 fail-open。
- 本次脱敏字段数量。
- 本次类型分布。
- response 脱敏总耗时。

稳定接口 key 优先使用 Spring MVC 的 `BEST_MATCHING_PATTERN_ATTRIBUTE`，例如 `/customers/{id}`。拿不到 MVC pattern 时，会轻量归一化数字段和 UUID 段，避免高基数原始 URL 撑爆接口维度。

### 3. 未知类型事件

当规则命中但没有注册对应 `MaskStrategy` 时，core 会回退到 `DEFAULT` 策略，同时通过 `UnknownTypeRecorder` 记录未知 type 计数。报告通过 `unknownTypeCounts` 暴露这些配置问题。

### 4. Log 规则建议线索

日志模块的 `SafeOutputLogMessageMasker` 支持在 regex fallback 命中后，从命中值前方提取 nearby key 并写入 `LogRuleSuggestionCollector`。线索只保存：

- key。
- suggested type。
- 命中次数。
- 首次和末次出现时间。
- 脱敏 evidence，格式类似 `key=<type>`。

不会保存命中的日志原文、完整日志 message 或敏感值。

开启 `safe-output.report.enabled=true` 后，starter 会把 `MaskMetricsCollector` 作为 `LogRuleSuggestionCollector` 传入 Log4j2 runtime。真实 `%safeOutputMsg` 的 regex fallback 命中未配置 nearby key 时，会自动写入脱敏 evidence；业务方也可以自行调用 `MaskMetricsCollector.record(LogRuleSuggestionEvent)` 补充线索。

## 监控数据的内存存储

当前监控数据以进程内聚合方式存储，核心入口是 `MaskMetricsCollector`。starter 在 `safe-output.report.enabled=true` 时创建单例 `MaskMetricsCollector(1000)`，并把它同时注入到 response、manual、log 和报告导出链路：

- 作为 `MaskEventRecorder`：接收 response、log、manual 场景的成功脱敏事件。
- 作为 `ResponseRiskRecorder`：接收 `SafeOutputResponseBodyAdvice` 生成的接口风险事件。
- 作为 `UnknownTypeRecorder`：接收未知类型标签计数。
- 作为 `LogRuleSuggestionCollector`：接收日志 regex fallback 规则建议线索。

### 1. 全局计数存储

`MaskMetricsCollector` 内部用普通字段保存全局计数，并通过 `synchronized` 方法串行写入：

| 内存字段 | 含义 |
|---|---|
| `totalCount` | 成功脱敏值总次数 |
| `responseCount` | response 场景成功脱敏值次数 |
| `logCount` | log 场景成功脱敏值次数，不是日志行数 |
| `manualCount` | 主动脱敏场景成功脱敏值次数 |
| `failureCount` | 报告导出失败次数 |
| `totalElapsedNanos` | 所有成功脱敏事件耗时总和 |
| `maxElapsedNanos` | 单次成功脱敏最大耗时 |

这些字段只保存聚合数字，不保存 raw value、response body 或日志 message。

### 2. 类型计数存储

类型分布存储在两个 `LinkedHashMap<String, Long>` 中：

- `maskTypeCounts`：按标准化后的 String type 统计成功脱敏次数。
- `unknownTypeCounts`：按标准化后的 String type 统计未知类型次数。

写入前统一通过 `MaskTypes.normalize(type)` 归一化。未知类型事件只记录类型标签和次数；实际脱敏链路会使用 `DEFAULT` 策略兜底，但报告仍保留原未知 type 计数，方便排查配置拼写错误或策略未注册。

### 3. 接口维度存储

Response 接口风险指标存储在：

```java
Map<String, ApiMaskMetrics> apiMetrics
```

key 由 `method + " " + apiPath` 组成。`apiPath` 优先使用 `ResponseRiskEvent.apiKey`，也就是 Spring MVC `BEST_MATCHING_PATTERN_ATTRIBUTE`；拿不到时由 `SafeOutputResponseBodyAdvice` 对数字路径段和 UUID 路径段做轻量归一化。

每个 `ApiMaskMetrics` 只保存接口聚合信息：

- `hitCount`
- `ignored`
- `ignoreReason`
- `failureCount`
- `maskedFieldCount`
- `totalElapsedNanos`
- `maxElapsedNanos`
- `slowMaskCount`
- `maskTypeCounts`

starter 当前固定使用 `MaskMetricsCollector(1000)`。当接口维度超过上限后，新增接口不再继续扩张 `apiMetrics`，而是聚合到：

```text
OVERFLOW __overflow__
```

这能避免高基数 path 导致内存无限增长，但 overflow 会丢失新增接口的单独维度。

### 4. 日志规则建议存储

日志 regex fallback 线索由 `MaskMetricsCollector` 内部的 `InMemoryLogRuleSuggestionCollector` 保存。其内部也是 `LinkedHashMap`，聚合 key 为：

```text
normalizedKey + ":" + normalizedType
```

每条建议线索的可变指标只包含：

- `key`
- `type`
- `hitCount`
- `firstSeenTimeMillis`
- `lastSeenTimeMillis`
- `evidence`

其中 `evidence` 在日志模块中被写成 `key=<type>` 形态，不包含命中的敏感值。

### 5. 快照读取语义

`MaskMetricsCollector.snapshot()` 会把当前内存聚合转成 `MaskReport`：

- 全局数字按当前值复制。
- `maskTypeCounts` 和 `unknownTypeCounts` 复制为不可变 `LinkedHashMap`。
- `apiMetrics` 复制为不可变 List，但 List 内部的 `ApiMaskMetrics` 对象仍来自当前聚合对象。
- `ResponseRiskAnalysis` 不在线存储，而是在 `MaskReport.getResponseRiskAnalysis()` 被调用时基于 `apiMetrics` 即时计算。

因此当前快照是“当前进程内聚合视图”，不是事件明细，也不能用于还原任何敏感原文。

## 监控数据的持久化存储

当前持久化只支持本地 JSON 快照文件，由 `MaskReportExporter` 完成；没有数据库、没有外部存储适配器、没有事件级追加日志，也没有从历史快照恢复内存计数的机制。

### 1. 自动定时导出

starter 在 `safe-output.report.enabled=true` 时创建 `MaskReportExporter`，并立即调用 `start()`。`start()` 会启动单线程守护调度器：

```text
safe-output-report-exporter
```

调度方式是 `scheduleWithFixedDelay`，初始延迟和固定延迟都使用 `intervalMillis`。也就是说应用启动后不会立即写第一份报告，而是在第一个间隔后导出。

### 2. 手动导出

业务代码或 Demo 可以注入 `MaskReportExporter` 并调用：

```java
Path path = exporter.exportNow();
```

Demo 的 `GET /demo/report/export` 就是手动触发该方法。导出失败时返回 `null`，同时调用 `collector.recordFailure()` 增加 `failureCount` 并记录 warning，不向业务链路抛出异常。

### 3. 文件写入内容

每次 `exportNow()` 会执行以下步骤：

1. `Files.createDirectories(options.getDirectory())` 创建报告目录。
2. 调用 `collector.snapshotSuggestions()` 读取日志规则建议线索。
3. 调用 `LogRuleSuggestionAnalyzer.analyze(..., emptyList())` 生成规则建议报告。
4. 调用 `collector.snapshot()` 读取当前聚合快照。
5. 使用 `MaskReportJsonWriter` 手写 JSON。
6. 通过 `Files.write(..., UTF_8)` 一次性写入本地文件。
7. 调用 `retainNewestFiles()` 清理旧快照。

JSON 文件是某个时间点的完整聚合快照，而不是从上次导出到本次导出的增量。

### 4. 文件命名和保留

文件名格式为：

```text
{filePrefix}-{yyyyMMddHHmmssSSS}-{sequence}.json
```

其中 `sequence` 是当前 `MaskReportExporter` 实例内的 `AtomicLong` 递增序号。旧文件清理逻辑只扫描同一目录下满足以下条件的文件：

- 文件名以 `{filePrefix}-` 开头。
- 文件名以 `.json` 结尾。

然后按文件名字典序排序，删除超出 `retainFiles` 数量的最旧文件。`retainFiles` 在 `MaskReportExportOptions` 中最小归一为 1。

### 5. 重启后的行为

应用重启后：

- `MaskMetricsCollector` 会重新创建，内存计数从 0 开始。
- 已导出的 JSON 文件仍保留在配置目录中，受后续 `retainFiles` 清理影响。
- 当前代码不会读取历史 JSON 文件恢复计数。
- `sequence` 从新 exporter 实例重新开始递增，但文件名包含毫秒时间戳，通常不会覆盖历史文件。

因此当前持久化能力更适合审计快照和离线查看，不是实时查询数据库，也不是长期完整指标仓库。

## 当前支持的报告功能

### 1. 总量与场景统计

`MaskReport` 输出：

- `totalCount`：总脱敏次数。
- `responseCount`：response 场景脱敏次数。
- `logCount`：log 场景成功脱敏值次数。
- `manualCount`：主动脱敏场景次数。
- `failureCount`：报告导出等失败次数。
- `averageElapsedNanos`：所有脱敏事件平均耗时。
- `maxElapsedNanos`：最大耗时。

### 2. 类型分布

`maskTypeCounts` 按标准化 String type 聚合。内置类型来自 `MaskTypes`，例如：

- `mobile`
- `id_card`
- `bank_card`
- `email`
- `chinese_name`
- `address`
- `password`
- `default`

自定义类型也会按标准化后的 String 标签进入统计。

### 3. 未知类型统计

`unknownTypeCounts` 记录规则引用了但没有注册策略的类型。当前运行时会 fail-open 兜底为 `DEFAULT` 脱敏，但报告保留未知 type 计数，便于定位配置拼写错误或漏注册策略。

### 4. 接口维度指标

`apiMetrics` 按 method + stable api key 聚合，每个接口包含：

- `method`
- `path`
- `hitCount`
- `ignored`
- `ignoreReason`
- `failureCount`
- `maskedFieldCount`
- `averageElapsedNanos`
- `maxElapsedNanos`
- `slowMaskCount`
- `riskLevel`
- `maskTypeCounts`

默认 starter 创建的 `MaskMetricsCollector` 最多维护 1000 个接口维度。超过上限后，新增接口会聚合到：

```text
OVERFLOW __overflow__
```

### 5. Response 风险画像

`ResponseRiskAnalyzer` 基于 `apiMetrics` 生成：

- `responseRiskSummary`
- `topRiskApis`
- `ignoredRiskApis`

风险等级：

| 等级 | 含义 |
|---|---|
| `LOW` | 有低风险或暂无明显敏感类型聚合 |
| `MEDIUM` | 命中手机号、邮箱、中文姓名等 |
| `HIGH` | 命中身份证、银行卡，或字段数量较高 |
| `CRITICAL` | 风险评分达到临界值，常见于 password 等高敏类型叠加 |
| `IGNORED_HIGH` | 接口显式 ignore，明文返回但仍进入高风险豁免统计 |

当前风险原因包括：

- `PASSWORD`
- `ID_CARD`
- `BANK_CARD`
- `HIGH_FIELD_COUNT`
- `HIGH_FREQUENCY`
- `IGNORED_RESPONSE`

当前评分规则：

- `PASSWORD`：+45。
- `ID_CARD`：+35。
- `BANK_CARD`：+35。
- 脱敏字段数大于等于 5：+20。
- 接口命中次数大于等于 10：+15。
- API ignore：+30，且等级固定为 `IGNORED_HIGH`。
- 分数大于等于 80 为 `CRITICAL`，大于等于 50 为 `HIGH`，大于 0 为 `MEDIUM`。

### 6. 性能画像

每个接口有 `performanceProfile`：

- `averageElapsedNanos`
- `maxElapsedNanos`
- `slowMaskCount`
- `warnings`

当前慢脱敏阈值为 50ms，即 `elapsedNanos >= 50000000` 时 `slowMaskCount` 增加，并输出 `SLOW_MASKING` warning。

### 7. Log 规则建议

`LogRuleSuggestionAnalyzer` 将日志 fallback 线索转换为建议：

- `key`
- `suggestedType`
- `hitCount`
- `confidence`
- `evidence`
- `effectScopes`
- `autoApply`

置信度规则：

| 命中次数 | 置信度 |
|---|---|
| 1 | `LOW` |
| 2 到 4 | `MEDIUM` |
| 大于等于 5 | `HIGH` |

`configSnippet` 只为 `MEDIUM` 和 `HIGH` 置信度生成候选配置，且默认：

```yaml
enabled: false
```

这意味着报告只提出建议，不自动改配置、不自动启用规则。

### 8. 本地 JSON 快照导出

`MaskReportExporter.exportNow()` 会：

1. 创建报告目录。
2. 从 `MaskMetricsCollector` 获取当前快照。
3. 生成日志规则建议。
4. 写入 JSON 文件。
5. 按 `retainFiles` 删除旧文件。

文件名格式：

```text
{filePrefix}-{yyyyMMddHHmmssSSS}-{sequence}.json
```

定时导出由 `start()` 启动，使用守护线程 `safe-output-report-exporter`，按 `intervalMillis` 固定延迟执行。

## Starter 接入方式

业务系统通常只引入 starter：

```xml
<dependency>
  <groupId>com.safeoutput</groupId>
  <artifactId>safe-output-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

开启报告：

```yaml
safe-output:
  report:
    enabled: true
    directory: ./safe-output-reports
    file-prefix: safe-output-report
    interval-millis: 60000
    retain-files: 10
```

配置说明：

| 配置 | 默认值 | 当前行为 |
|---|---:|---|
| `safe-output.report.enabled` | `false` | 为 `true` 时创建 `MaskMetricsCollector` 和 `MaskReportExporter` |
| `safe-output.report.directory` | `./safe-output-reports` | JSON 快照输出目录 |
| `safe-output.report.file-prefix` | `safe-output-report` | 快照文件名前缀 |
| `safe-output.report.interval-millis` | `60000` | 定时导出间隔，最小会归一为 1 |
| `safe-output.report.retain-files` | `10` | 保留最新文件数，最小会归一为 1 |
| `safe-output.report.include-api-metrics` | `true` | 当前已绑定配置，但 JSON writer 尚未按该开关裁剪字段 |
| `safe-output.report.include-field-path` | `true` | 当前已绑定配置，报告实际不输出字段路径 |
| `safe-output.report.include-raw-value` | `false` | 当前已绑定配置，报告仍不会输出敏感原文 |

注意：报告默认关闭。未开启时，starter 不会创建 `MaskMetricsCollector`，因此 response 风险画像、报告导出和 Demo 报告接口都依赖 `safe-output.report.enabled=true`。

## Demo 使用指南

Demo 配置位于：

```text
safe-output/safe-output-demo/src/main/resources/application.yml
```

当前 demo 已开启报告：

```yaml
safe-output:
  report:
    enabled: true
    directory: target/safe-output-demo-reports
    file-prefix: demo-report
    interval-millis: 600000
```

启动 demo：

```sh
cd safe-output
mvn -pl safe-output-demo -am spring-boot:run
```

浏览器入口：

```text
http://localhost:8080/index.html
```

可先调用几个 response、log、manual 示例接口产生数据，再查看报告接口。

## Demo 报告接口

### 1. 查看当前聚合快照

```http
GET /demo/report/snapshot
```

返回 `MaskReport`。包含总量、场景计数、类型分布、未知类型统计和 `apiMetrics`。

### 2. 手动导出报告文件

```http
GET /demo/report/export
```

返回：

```json
{"path":"target/safe-output-demo-reports/demo-report-...json"}
```

如果导出失败，`path` 为空字符串，`failureCount` 增加，业务接口不受影响。

### 3. Dashboard 聚合数据

```http
GET /demo/report/dashboard
```

返回面向前端看板的简化聚合结构：

- `totalCount`
- `responseCount`
- `logCount`
- `manualCount`
- `highRiskApiCount`
- `suggestionCount`
- `averageElapsedNanos`
- `maskTypeCounts`
- `topRiskApis`
- `sceneTrend`

### 4. Response 风险画像

```http
GET /demo/report/response-risk
```

返回：

- `responseRiskSummary`
- `topRiskApis`
- `ignoredRiskApis`

### 5. Log 规则建议

```http
GET /demo/report/log-suggestions
```

返回：

- `logRuleSuggestions`
- `configSnippet`

Demo 中可先调用 `GET /demo/logs` 产生真实 Log4j2 fallback 线索，再通过该接口查看建议能力。接口只展示脱敏后的 evidence，不写入示例线索。

## JSON 快照字段

导出的 JSON 顶层字段当前为：

```json
{
  "totalCount": 0,
  "responseCount": 0,
  "logCount": 0,
  "manualCount": 0,
  "failureCount": 0,
  "averageElapsedNanos": 0,
  "maxElapsedNanos": 0,
  "maskTypeCounts": {},
  "unknownTypeCounts": {},
  "apiMetrics": [],
  "responseRiskSummary": {},
  "topRiskApis": [],
  "ignoredRiskApis": [],
  "logRuleSuggestions": [],
  "configSnippet": ""
}
```

`apiMetrics` 元素结构：

```json
{
  "method": "GET",
  "path": "/api/path",
  "hitCount": 1,
  "ignored": false,
  "ignoreReason": null,
  "failureCount": 0,
  "maskedFieldCount": 1,
  "averageElapsedNanos": 0,
  "maxElapsedNanos": 0,
  "slowMaskCount": 0,
  "riskLevel": "MEDIUM",
  "maskTypeCounts": {}
}
```

`topRiskApis` 和 `ignoredRiskApis` 元素结构：

```json
{
  "method": "GET",
  "path": "/api/path",
  "ignored": false,
  "ignoreReason": null,
  "riskScore": 0,
  "riskLevel": "LOW",
  "riskReasons": [],
  "governanceAdvice": [],
  "performanceProfile": {
    "averageElapsedNanos": 0,
    "maxElapsedNanos": 0,
    "slowMaskCount": 0,
    "warnings": []
  },
  "maskTypeCounts": {}
}
```

`logRuleSuggestions` 元素结构：

```json
{
  "key": "fieldKey",
  "suggestedType": "mobile",
  "hitCount": 2,
  "confidence": "MEDIUM",
  "evidence": "fieldKey=<mobile>",
  "effectScopes": ["RESPONSE", "LOG", "MANUAL_OBJECT"],
  "autoApply": false
}
```

## 典型使用流程

### 业务接入

1. 引入 `safe-output-spring-boot-starter`。
2. 配置 response、log、manual 的脱敏规则。
3. 开启 `safe-output.report.enabled=true`。
4. 配置报告目录、文件前缀、导出间隔和保留数量。
5. 运行应用，让 response、log 或 manual 场景产生脱敏事件。
6. 定期查看导出的 JSON 快照，或在业务系统中注入 `MaskMetricsCollector` 读取 `snapshot()`。

### 手动导出

如果业务系统需要主动导出，可注入：

```java
private final MaskReportExporter exporter;
```

调用：

```java
Path path = exporter.exportNow();
```

`exportNow()` 失败时返回 `null`，同时增加失败计数并记录 warning，不会抛出异常影响业务流程。

### 程序内读取快照

可注入：

```java
private final MaskMetricsCollector metricsCollector;
```

读取：

```java
MaskReport report = metricsCollector.snapshot();
ResponseRiskAnalysis analysis = report.getResponseRiskAnalysis();
```

快照对象只包含聚合指标，不能从中恢复原始 response、日志或敏感值。

## 安全边界

当前代码明确遵守以下边界：

- 报告不保存敏感原文。
- 报告不保存完整 response。
- 报告不保存完整日志 message。
- 日志规则建议 evidence 只保存 `key=<type>` 形态。
- API ignore 返回明文，但必须进入风险统计，并标记为 `IGNORED_HIGH`。
- 报告导出失败只增加 `failureCount` 并记录 warning，不影响业务脱敏或接口返回。
- 接口维度有上限，超过后进入 overflow，避免高基数路径导致内存无限增长。
- Response 风险画像只基于聚合后的类型计数、字段数、频率、ignore 和耗时，不读取原始响应内容。

## 当前限制与注意事项

- 报告是进程内内存聚合，没有数据库持久化；应用重启后内存计数会清空。
- 导出的 JSON 是快照文件，不是实时查询存储。
- `MaskReportJsonWriter` 是手写 JSON 序列化，不是通用 JSON 框架。
- `include-api-metrics`、`include-field-path`、`include-raw-value` 当前只是属性绑定，导出逻辑尚未基于这些开关裁剪或扩展字段。
- starter 默认 `MaskMetricsCollector(1000)`，接口维度上限当前不可通过配置调整。
- `LogRuleSuggestionAnalyzer.analyze(metrics, configuredKeys)` 支持传入已配置 key 过滤建议，但当前 `MaskReportExporter` 调用时传入空列表。
- Log4j2 runtime bridge 是进程级静态配置，适合单应用 Spring Boot 进程；多应用上下文并发隔离仍不是当前目标。
- 风险评分是当前内置启发式规则，不代表合规结论；应作为治理线索使用。
- 主动脱敏计入 `MANUAL` 场景总量，但默认不进入 Response 接口风险统计。

## 验证命令

报告模块单测：

```sh
cd safe-output
mvn -pl safe-output-report test
```

starter 自动装配相关测试：

```sh
cd safe-output
mvn -pl safe-output-spring-boot-starter -am test
```

Demo 报告接口端到端测试：

```sh
cd safe-output
mvn -pl safe-output-demo -am test
```

快速全量测试：

```sh
cd safe-output
mvn test
```

## 源码阅读入口

建议按以下顺序阅读：

1. `safe-output/safe-output-report/src/main/java/com/safeoutput/report/MaskMetricsCollector.java`
2. `safe-output/safe-output-report/src/main/java/com/safeoutput/report/MaskReport.java`
3. `safe-output/safe-output-report/src/main/java/com/safeoutput/report/ApiMaskMetrics.java`
4. `safe-output/safe-output-report/src/main/java/com/safeoutput/report/ResponseRiskAnalyzer.java`
5. `safe-output/safe-output-report/src/main/java/com/safeoutput/report/LogRuleSuggestionAnalyzer.java`
6. `safe-output/safe-output-report/src/main/java/com/safeoutput/report/MaskReportExporter.java`
7. `safe-output/safe-output-spring-boot-starter/src/main/java/com/safeoutput/spring/boot/autoconfigure/SafeOutputAutoConfiguration.java`
8. `safe-output/safe-output-spring-boot-starter/src/main/java/com/safeoutput/spring/boot/autoconfigure/SafeOutputResponseBodyAdvice.java`
9. `safe-output/safe-output-demo/src/main/java/com/safeoutput/demo/DemoReportController.java`

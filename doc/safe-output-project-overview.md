# Safe Output 项目代码概览

本文档是 Safe Output 当前代码实现的总览，面向维护者和二次开发者。业务接入步骤以 `safe-output/doc/component-integration-guide.md` 为准；报告模块细节以 `doc/safe-output-report-module-guide.md` 为准。

## 1. 项目定位

Safe Output 是面向 Java 8 / Spring Boot 2.x 遗留服务的输出侧脱敏 starter。它在少改业务代码的前提下处理三类场景：

- Response 脱敏：通过 Spring MVC `ResponseBodyAdvice` 在 JSON 序列化前处理返回值。
- Log4j2 日志脱敏：通过 `%safeOutputMsg` / `%safeOutputMessage` pattern converter 处理日志 message。
- 主动脱敏：通过 `SafeOutputMaskService` 显式处理字符串、对象或强扫描文本。

报告能力只保存聚合指标、风险画像、性能画像和脱敏后的 evidence，不保存敏感原文、完整 response 或完整日志。

## 2. 工程结构

```text
safe-output/
├── pom.xml
├── README.md
├── doc/
│   ├── README.md
│   ├── component-integration-guide.md
│   └── core.md
├── safe-output-core/
├── safe-output-log4j2/
├── safe-output-report/
├── safe-output-spring-boot-starter/
└── safe-output-demo/
```

根目录 `doc/` 保存项目级说明、PRD 和报告模块深挖文档；`safe-output/doc/` 保存随组件源码发布的接入和核心原理文档。

模块依赖关系：

```text
safe-output-demo
  └── safe-output-spring-boot-starter
        ├── safe-output-core
        ├── safe-output-log4j2 ──> safe-output-core
        └── safe-output-report ──> safe-output-core
```

## 3. 代码规模

| 模块 | 主代码文件 | 主代码行数 | 测试文件 | 测试行数 |
|---|---:|---:|---:|---:|
| `safe-output-core` | 33 | 2,485 | 7 | 1,166 |
| `safe-output-log4j2` | 3 | 631 | 2 | 315 |
| `safe-output-report` | 16 | 1,217 | 2 | 349 |
| `safe-output-spring-boot-starter` | 8 | 929 | 9 | 1,234 |
| `safe-output-demo` | 5 | 390 | 1 | 270 |
| **合计** | **65** | **5,652** | **21** | **3,334** |

## 4. 核心模块

`safe-output-core` 不依赖 Spring，负责类型标签、规则、策略、对象递归和主动脱敏。

| 类 | 定位 |
|---|---|
| `MaskTypes` / `MaskType` | String 类型标签和兼容枚举。运行链路以 String type 为准。 |
| `DefaultMaskRules` | 内置默认字段规则库，是文档默认规则表的代码锚点。 |
| `MaskRuleMatcher` | 规则裁决模块，固定执行 ignore、注解、配置 Rule、默认 Rule、regex fallback 优先级。 |
| `MaskStrategyRegistry` | 策略注册中心，合并内置策略和业务自定义 `MaskStrategy`。 |
| `ObjectMasker` | 对 Bean、Map、Collection、数组递归脱敏，带深度、集合上限和循环引用保护。 |
| `SensitiveFieldResolver` | 解析字段级 `@Desensitize(type = "...")` 注解并缓存字段元数据。 |
| `DefaultSafeOutputMaskService` | 主动脱敏服务默认实现。 |
| `StrongTextMasker` / `StrongObjectMasker` | 主动强扫描文本和对象中的字符串。 |

默认字段规则由 `DefaultMaskRules.all()` 统一维护：

| 默认 Rule | 字段名 | 类型标签 |
|---|---|---|
| `default.mobile` | `mobile`, `phone`, `telephone`, `tel`, `userMobile` | `mobile` |
| `default.id-card` | `idCard`, `certNo`, `identityNo`, `certificateNo` | `id_card` |
| `default.bank-card` | `bankCard`, `cardNo`, `bankNo` | `bank_card` |
| `default.email` | `email`, `mail` | `email` |
| `default.password` | `password`, `secret`, `token` | `password` |

`name`、`id`、`code`、`no`、`address` 等歧义字段不会默认脱敏，需要显式 Rule 或字段注解。

## 5. Log4j2 模块

`safe-output-log4j2` 提供 `%safeOutputMsg` / `%safeOutputMessage`。

| 类 | 定位 |
|---|---|
| `SafeOutputMessagePatternConverter` | Log4j2 converter，解析 pattern 局部选项并按 runtime 版本缓存 masker。 |
| `SafeOutputLogMessageMasker` | 两阶段日志脱敏：先处理 key-value / JSON-like，再按配置执行有限 regex fallback。 |
| `SafeOutputLog4j2Runtime` | starter 到 Log4j2 converter 的进程级 runtime bridge。 |

通过 starter 接入时，最终运行值来自 `safe-output.log.*`，pattern 局部选项主要用于无 Spring runtime bridge 的直接 log4j2 模块场景。XML 中 `enabled=false` 仍可直接关闭当前 pattern。

日志 regex fallback 只覆盖手机号、邮箱、合法大陆身份证号；银行卡号不做无字段名上下文兜底。超过 `max-message-length` 的日志整条 fail-open 返回原 message。

## 6. Report 模块

`safe-output-report` 聚合指标并导出本地 JSON 快照。

| 类 | 定位 |
|---|---|
| `MaskMetricsCollector` | 进程内聚合采集器，同时实现 mask event、response risk、unknown type、log suggestion 四类 recorder。 |
| `MaskReport` | 当前聚合快照模型。 |
| `MaskReportJsonWriter` | 手写 JSON writer，只输出聚合字段。 |
| `MaskReportExporter` | 定时和手动导出 JSON，负责文件保留策略。 |
| `ResponseRiskAnalyzer` | 生成接口风险等级、风险原因、治理建议和性能画像。 |
| `LogRuleSuggestionAnalyzer` | 将日志 fallback nearby key 线索转成候选 `safe-output.rules[]` 配置。 |

报告默认关闭。开启 `safe-output.report.enabled=true` 后，starter 创建 `MaskMetricsCollector` 和 `MaskReportExporter`，并把 collector 注入 Response、Manual、Log4j2 runtime bridge。

## 7. Starter 模块

`safe-output-spring-boot-starter` 是业务系统唯一推荐依赖入口。

| 类 | 定位 |
|---|---|
| `SafeOutputAutoConfiguration` | 创建策略注册、规则 matcher、对象脱敏、主动脱敏、Log4j2 runtime 注册和报告 Bean。 |
| `SafeOutputMvcAutoConfiguration` | 在 Spring MVC 存在时注册 `SafeOutputResponseBodyAdvice`。 |
| `SafeOutputResponseBodyAdvice` | Response 脱敏入口，支持 API ignore、`body-data-path` 和风险统计。 |
| `SafeOutputProperties` | `safe-output.*` 配置绑定。 |
| `ApiIgnoreMatcher` | 接口级 ignore 匹配。 |
| `SafeOutputConfiguredKeys` | 从配置 Rule 提取已配置日志 key，供报告建议过滤复用。 |

自动装配仍使用 Spring Boot 2.x `spring.factories`，未迁移 Boot 3 `AutoConfiguration.imports`。

## 8. Demo 模块

`safe-output-demo` 是端到端示例应用，只直接依赖 starter。

| 类 | 定位 |
|---|---|
| `DemoApplication` | 启动类，并注册 `mobileM` 自定义策略示例。 |
| `DemoResponseController` | Response 脱敏、嵌套对象、Map/List、API ignore 示例。 |
| `DemoLogController` | 真实 Log4j2 `%safeOutputMsg` 日志脱敏示例。 |
| `DemoManualMaskController` | 主动脱敏、对象脱敏、强扫描示例。 |
| `DemoReportController` | 报告快照、手动导出、风险画像、日志规则建议接口。 |

浏览器入口为 `http://localhost:8080/index.html`。

## 9. 关键调用链

Response：

```text
SafeOutputResponseBodyAdvice.beforeBodyWrite
  -> ApiIgnoreMatcher
  -> body-data-path 提取或整 body 处理
  -> ObjectMasker.maskWithResult
  -> SensitiveFieldResolver / MaskRuleMatcher.decide
  -> MaskStrategyRegistry.find
  -> MaskEventRecorder + ResponseRiskRecorder
```

Log4j2：

```text
%safeOutputMsg
  -> SafeOutputMessagePatternConverter
  -> SafeOutputLog4j2Runtime.createMasker
  -> SafeOutputLogMessageMasker.mask
  -> key-value Rule 脱敏
  -> 可选 regex fallback
  -> LOG 计数和 fallback 规则线索
```

主动脱敏：

```text
SafeOutputMaskService
  -> mask(value,type) / maskObject / maskStrong / maskObjectStrong
  -> MaskStrategyRegistry / ObjectMasker / StrongTextMasker
  -> MANUAL 场景计数
```

## 10. 设计边界

- Response、对象递归、日志、报告导出都坚持 fail-open，异常不影响业务接口或日志输出。
- `rules[].paths` 和 `ignore.paths` 使用 Safe Output 递归路径，不是完整 JSONPath；只支持精确匹配和 `[*]` 数字下标段通配。
- 字段 ignore 和 API ignore 高于注解、配置 Rule、默认 Rule 和 fallback。
- 未知 type 当前行为是告警、记录 unknown type 聚合指标，并使用 `DEFAULT` 策略兜底脱敏。
- Log4j2 只做轻量 key-value / JSON-like 识别，不引入 JSON parser。
- 统计和报告不保存敏感原文。

## 11. 验证入口

```sh
cd safe-output
mvn test
mvn verify
mvn -pl safe-output-demo -am test
mvn -pl safe-output-spring-boot-starter -am test
```

当前本轮重构已运行：

```sh
mvn -pl safe-output-core test
```

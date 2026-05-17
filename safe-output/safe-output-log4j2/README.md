# safe-output-log4j2

`safe-output-log4j2` 是 Safe Output 的 Log4j2 适配模块。它提供 `%safeOutputMsg` / `%safeOutputMessage` pattern converter，在日志消息输出前执行 key-value 脱敏和可选 regex fallback。

## 职责

- 通过 `SafeOutputMessagePatternConverter` 接入 Log4j2 `PatternLayout`。
- 通过 `SafeOutputLog4j2Runtime` 接收 starter 注册的 Spring 规则、策略和日志选项。
- 通过 `SafeOutputLogMessageMasker` 处理 JSON-like 和 key-value 日志片段。
- key-value 脱敏复用 `rules.keys` 到类型标签的映射，支持内置和自定义策略。
- 对没有字段名上下文的手机号、邮箱、合法大陆身份证号执行可选 regex fallback。
- regex fallback 命中后可采集 nearbyKey 规则线索，只保存 key、type、次数、时间和脱敏后的 evidence。
- 通过 `maxMessageLength` 和 `maxValueLength` 控制日志脱敏成本和误伤范围。

## 使用方式

在业务应用的 `log4j2.xml` 中配置：

```xml
<PatternLayout pattern="%d{HH:mm:ss.SSS} %-5level %logger{36} - %safeOutputMsg{maxMessageLength=5000,maxValueLength=300}%n"/>
```

可用选项：

- `enabled`: 是否启用日志脱敏，默认 `true`。
- `keyValueRuleEnabled`: 是否启用 key-value 规则脱敏，默认 `true`。
- `regexFallback`: 是否启用无字段名上下文的兜底正则，默认 `true`。
- `idCardCheckCode`: regex fallback 识别孤立身份证号时是否校验末位校验码，默认 `true`；日期格式和年份范围始终校验。
- `maxMessageLength`: 超过该长度的整条日志不处理，默认 `5000`。
- `maxValueLength`: 超过该长度的单个值不处理，默认 `300`。
- `maxRuleKeys`: 参与日志 key-value 匹配的字段名上限，默认 `128`；超限时跳过 key-value 规则。

通过 `safe-output-spring-boot-starter` 使用时，starter 会在 Spring 启动后注册 `safe-output.rules[].keys`、`safe-output.ignore.keys`、自定义 `MaskStrategy` Bean 和 `safe-output.log.*` 选项；无 Spring 注册时，converter 使用本模块默认规则和 pattern 选项。

## 边界

- key-value 规则依赖字段名，例如 `mobile=13800138000` 或 `"email":"foo@example.com"`。
- key-value 支持 `key=value`、`key: value`、`key = value`、`key : value`，key 和 value 可使用单引号、双引号或不带引号。
- `ignore.keys` 命中时优先跳过日志 key-value 脱敏；`rules.paths` 不作为日志文本匹配依据。path 只用于 Response/主动对象递归链路，其中 `$` 表示被脱敏对象根节点，`[*]` 表示任意数字下标段，不是完整 JSONPath。
- 未超过 `maxMessageLength` 的日志会遍历处理多个 key-value；超过该限制时整条日志 fail-open 返回原文。
- key-value 规则在 `SafeOutputLogMessageMasker` 初始化时构建字段名缓存；单条日志处理不动态拼接或编译规则集合。
- regex fallback 只覆盖手机号、邮箱和通过轻量格式、日期、年份及可选校验位检查的大陆身份证号。
- fallback 规则线索不会保存命中值或完整日志；已配置 `rules.keys` 的 key 不重复生成线索。
- 无上下文银行卡号不做全局兜底，避免误伤普通流水号。
- converter 或脱敏过程异常时返回原日志消息，保持 fail-open。

## 本模块验证

在 `safe-output/` 根目录执行：

```sh
mvn -pl safe-output-log4j2 test
```

测试覆盖 converter 发现、配置选项、开关、key-value 脱敏、regex fallback 和误伤边界。

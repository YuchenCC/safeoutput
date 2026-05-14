# safe-output-log4j2

`safe-output-log4j2` 是 Safe Output 的 Log4j2 适配模块。它提供 `%safeOutputMsg` / `%safeOutputMessage` pattern converter，在日志消息输出前执行 key-value 脱敏和可选 regex fallback。

## 职责

- 通过 `SafeOutputMessagePatternConverter` 接入 Log4j2 `PatternLayout`。
- 通过 `SafeOutputLogMessageMasker` 处理 JSON-like 和 key-value 日志片段。
- 对没有字段名上下文的手机号、邮箱、合法大陆身份证号执行可选 regex fallback。
- 通过 `maxMessageLength` 和 `maxValueLength` 控制日志脱敏成本和误伤范围。

## 使用方式

在业务应用的 `log4j2.xml` 中配置：

```xml
<PatternLayout pattern="%d{HH:mm:ss.SSS} %-5level %logger{36} - %safeOutputMsg{maxMessageLength=5000,maxValueLength=300}%n"/>
```

可用选项：

- `enabled`: 是否启用日志脱敏，默认 `true`。
- `regexFallback`: 是否启用无字段名上下文的兜底正则，默认 `true`。
- `idCardCheckCode`: regex fallback 识别孤立身份证号时是否校验末位校验码，默认 `true`；日期格式和年份范围始终校验。
- `maxMessageLength`: 超过该长度的整条日志不处理，默认 `5000`。
- `maxValueLength`: 超过该长度的单个值不处理，默认 `300`。

## 边界

- key-value 规则依赖字段名，例如 `mobile=13800138000` 或 `"email":"foo@example.com"`。
- regex fallback 只覆盖手机号、邮箱和通过轻量格式、日期、年份及可选校验位检查的大陆身份证号。
- 无上下文银行卡号不做全局兜底，避免误伤普通流水号。
- converter 或脱敏过程异常时返回原日志消息，保持 fail-open。

## 本模块验证

在 `safe-output/` 根目录执行：

```sh
mvn -pl safe-output-log4j2 test
```

测试覆盖 converter 发现、配置选项、开关、key-value 脱敏、regex fallback 和误伤边界。

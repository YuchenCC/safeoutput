# Safe Output

Safe Output 是面向 Spring Boot 2.x 的 Java 8 starter，用于在不改 Controller 业务代码的前提下，对 response 和 Log4j2 日志做敏感信息脱敏，并输出聚合统计报告。

R2 扩展了 String 类型标签、自定义策略、主动脱敏、Response 风险画像、性能画像和 Log 规则建议。R3 竞赛展示看板不属于本轮实现范围。

## 模块

- `safe-output-core`: 内部模块，核心模型、脱敏策略、规则匹配和对象递归脱敏。
- `safe-output-log4j2`: 内部模块，Log4j2 `PatternConverter` 和日志 key-value/regex 脱敏。
- `safe-output-report`: 内部模块，指标聚合和本地 JSON 报告快照。
- `safe-output-spring-boot-starter`: 对外入口，业务系统只需要直接引用这个 starter。
- `safe-output-demo`: Spring Boot 2.x demo，演示 response、Log4j2 和 report 场景。

## 引用方式

完整业务接入步骤见 [组件接入手册](doc/component-integration-guide.md)。

Maven:

```xml
<dependency>
  <groupId>com.safeoutput</groupId>
  <artifactId>safe-output-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Gradle:

```groovy
implementation "com.safeoutput:safe-output-spring-boot-starter:0.1.0-SNAPSHOT"
```

本地验证安装:

```sh
mvn install
```

业务系统不需要手动声明 `safe-output-core`、`safe-output-log4j2` 或 `safe-output-report`；这些由 starter 聚合。

## 最小接入

Spring Boot 2.x 应用引入 starter 后，`spring.factories` 会自动装配 response 脱敏、规则匹配、日志适配和报告能力。Controller 不需要调用 Safe Output API:

```java
@RestController
class CustomerController {
    @GetMapping("/customers/current")
    CustomerResponse current() {
        return new CustomerResponse("张三", "13800138000");
    }
}
```

常见字段名如 `mobile`、`phone`、`email`、`idCard`、`bankCard`、`password` 会按默认规则脱敏。

类型识别使用 String 类型标签贯穿规则、策略和统计链路。内置类型可使用 `MaskTypes` 常量；业务自定义类型只需要提供同名 `MaskStrategy` Bean，并在 `rules[].type` 或 `@Desensitize(type = "...")` 中引用。未知 type 当前默认行为是 `warn + DEFAULT fallback`：记录 warning 和未知类型聚合统计，并使用 `DEFAULT` 策略兜底脱敏。

## 配置示例

```yaml
safe-output:
  enabled: true
  response:
    enabled: true
  rules:
    - name: customerMobile
      keys:
        - customerMobile
      paths:
        - $.customer.mobile
      type: MOBILE
      enabled: true
  ignore:
    keys:
      - plainNote
    paths:
      - $.items[*].title
    apis:
      - method: GET
        path: /demo/ignored
        reason: demo plaintext endpoint
  report:
    enabled: true
    directory: target/safe-output-demo-reports
    file-prefix: demo-report
    interval-millis: 600000
    retain-files: 10
```

## 注解和 Ignore

字段注解用于覆盖默认规则:

```java
public class CustomerResponse {
    @Desensitize(type = MaskType.CHINESE_NAME)
    private String name;
}
```

字段级 ignore 通过 `safe-output.ignore.keys` 或 `safe-output.ignore.paths` 配置。接口级 ignore 通过 `safe-output.ignore.apis` 配置；命中后 response 明文返回，但会记录 ignored 风险统计。

## Log4j2

业务系统使用 Log4j2 时，在 `log4j2.xml` 中使用 `%safeOutputMsg`:

```xml
<PatternLayout pattern="%d{HH:mm:ss.SSS} %-5level %logger{36} - %safeOutputMsg{maxMessageLength=5000,maxValueLength=300}%n"/>
```

日志脱敏支持 JSON-like 和 key-value 片段，例如 `"mobile":"13800138000"`、`email=foo@example.com`。整条 message fallback 会识别手机号、邮箱和严格合法的大陆身份证；普通 18 位流水号和无上下文银行卡号不会全局兜底脱敏。

通过 starter 启动时，`%safeOutputMsg` 会复用 Spring 绑定出的 `safe-output.rules[].keys`、`safe-output.ignore.keys`、自定义 `MaskStrategy` Bean 和 `safe-output.log.*` 选项。未超过 `safe-output.log.max-message-length` 的日志会处理所有匹配的 key-value；超过长度限制时整条日志 fail-open 返回原文。

regex fallback 命中后可提取 nearbyKey 规则线索。线索和报告只保存 key、type、次数、时间和脱敏后的 evidence，不保存敏感原文或完整日志。生成的规则建议默认 `autoApply=false`，需要接入方人工确认。

无 Spring 环境直接使用 log4j2 模块时，`%safeOutputMsg` 支持 `enabled`、`regexFallback`、`maxMessageLength`、`maxValueLength` 等 pattern 选项:

```xml
<PatternLayout pattern="%safeOutputMsg{regexFallback=false,maxMessageLength=5000,maxValueLength=300}%n"/>
```

关闭日志脱敏:

```xml
<PatternLayout pattern="%safeOutputMsg{enabled=false}%n"/>
```

## 报告

启用 `safe-output.report.enabled=true` 后，starter 会创建 `MaskMetricsCollector` 和定时 `MaskReportExporter`。报告只包含聚合指标、接口风险等级、ignored 统计、失败次数、耗时、Response 风险画像、性能画像和 Log 规则建议，不保存敏感原文、完整 response 或完整日志。

主动脱敏调用计入 `MANUAL` 场景统计，用于评估显式调用量和类型分布；它不默认进入 Response 接口风险统计。Response 风险统计只聚合响应场景的稳定接口标识、脱敏字段数量、类型分布、耗时、ignore 和失败状态。

demo 也提供手动导出接口:

```text
GET /demo/report/export
```

当前内存中的聚合指标可通过 `GET /demo/report/snapshot` 以 JSON 返回（与写入磁盘的快照字段一致，便于在浏览器中查看）。启动 demo 后可在浏览器打开 `http://localhost:8080/safe-output-playground.html`，一键调用各类脱敏示例接口并刷新或导出报告。

R2 新增 Demo 验证路径包括 `POST /demo/mask/by-type`、`POST /demo/mask/object`、`POST /demo/mask/strong`、`GET /demo/report/response-risk` 和 `GET /demo/report/log-suggestions`。

## Demo 验证

运行 demo 集成测试:

```sh
mvn -pl safe-output-demo -am test
```

完整验证和本地安装:

```sh
mvn verify
mvn install
```

## 验收清单

- Response: Bean、Map、List、嵌套对象可脱敏；注解、字段 ignore、接口 ignore 生效。
- Log: Log4j2 `%safeOutputMsg` 可发现；key-value、JSON-like、regex fallback 和误伤边界生效。
- 策略: 默认规则、用户规则、规则优先级、String type、自定义策略注册和 Java 8 兼容性生效。
- 主动脱敏: 指定 type、对象规则和强扫描可通过服务与 Demo 接口验证，MANUAL 场景统计生效。
- 统计: mask 次数、类型标签、接口维度、ignored 风险、失败次数、字段数、平均/最大耗时可聚合。
- 报告: 本地 JSON 快照、Response 风险画像、性能画像、Log 规则建议、保留数量、失败 fail-open、不包含敏感原文。
- Starter: `spring.factories` 自动装配、starter jar 可 `mvn install`、demo 只直接引用 starter。

## 测试覆盖

- `safe-output-core`: 核心契约、内置策略、规则匹配、注解解析、递归对象脱敏、策略注册。
- `safe-output-log4j2`: PatternConverter 发现、开关、JSON-like/key-value、regex fallback 和边界。
- `safe-output-report`: 聚合模型、风险等级、overflow、JSON 快照、保留数量、写入失败。
- `safe-output-spring-boot-starter`: 属性绑定、自动装配、response advice、API ignore、Log4j2 starter 引用。
- `safe-output-demo`: Spring Boot 2.x response、log、report 端到端场景。

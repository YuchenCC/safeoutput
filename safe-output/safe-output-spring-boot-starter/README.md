# safe-output-spring-boot-starter

`safe-output-spring-boot-starter` 是 Safe Output 面向业务系统的主要接入入口。业务应用通常只需要引用这个模块，不需要直接引用内部模块。

## 职责

- 通过 `spring.factories` 注册 Spring Boot 2.x 自动装配。
- 创建核心 Bean：`MaskRuleMatcher`、`MaskStrategyRegistry`、`SensitiveFieldResolver`、`ObjectMasker`、`SafeOutputMaskService`。
- 在 Spring MVC 环境中注册 `SafeOutputResponseBodyAdvice`，于 JSON 序列化前处理响应体。
- Response 脱敏完成后记录轻量风险事件，使用 MVC pattern 或归一化路径作为稳定接口标识。
- 根据配置启用 `MaskMetricsCollector` 和 `MaskReportExporter`。
- 将配置 Rule、字段 Ignore、API Ignore 和报告配置绑定到 `SafeOutputProperties`。
- `rules[].type` 使用 String 类型标签绑定，支持内置类型和业务自定义类型；策略查找会做 trim 和大小写归一化。
- 未知 type 会记录 warning，并使用 `DEFAULT` 策略兜底脱敏。
- 自定义 `MaskStrategy` 只需作为 Spring Bean 暴露，并返回业务自定义 `type()`，即可被配置 Rule、`@Desensitize` 和统计链路识别。
- Log4j2 `%safeOutputMsg` 会复用 Spring 绑定的 `rules[].keys`、`ignore.keys`、自定义 `MaskStrategy` 和 `safe-output.log.*` 选项。
- `safe-output.log.key-value-rule-enabled` 控制日志 key-value 规则脱敏，`safe-output.log.max-rule-keys` 控制参与日志匹配的字段名数量上限，`safe-output.log.max-message-length` 控制整条日志处理上限。

## 最小接入

```xml
<dependency>
  <groupId>com.safeoutput</groupId>
  <artifactId>safe-output-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

启用后，常见字段名会自动按默认 Rule 脱敏；业务代码无需显式调用 Safe Output API。

需要主动脱敏明确类型的字符串时，可注入 `SafeOutputMaskService` 并调用 `mask(value, type)`；该入口只根据类型标签查找策略，不做字段规则匹配或 regex 扫描。需要复用响应对象规则时，可调用 `maskObject(value)` 递归处理 Bean、Map、Collection 和数组。需要显式强扫描文本或对象字符串时，可调用 `maskStrong(text)` 或 `maskObjectStrong(value)`；未配置 `manual.strong-scan.types` 时默认 fallback 类型为 `MOBILE`、`EMAIL`、`ID_CARD`，配置后以清单为准。

## 常用配置

```yaml
safe-output:
  enabled: true
  response:
    enabled: true
  log:
    key-value-rule-enabled: true
    max-message-length: 5000
    max-rule-keys: 128
    regex-fallback:
      enabled: true
  manual:
    strong-scan:
      types:
        - MOBILE
        - EMAIL
        - ID_CARD
        - BANK_CARD
  rules:
    - name: realName
      keys:
        - realName
      type: CHINESE_NAME
    - name: customMobile
      keys:
        - mobileM
      paths:
        - $.items[*].mobileM
      type: mobileM
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
    directory: target/safe-output-reports
```

`rules[].paths` 和 `ignore.paths` 使用 Safe Output 递归路径，不是完整 JSONPath。`$` 表示本次被脱敏对象的根节点，`.` 表示字段或 Map key 层级，`[0]`、`[1]` 是集合或数组实际数字下标，`[*]` 只表示任意数字下标段。除 `[*]` 外按路径精确匹配，不支持 `**`、条件表达式、字段通配或模糊匹配。

## single-jar profile

`single-jar` profile 会把 `core`、`log4j2`、`report` 和 starter 合并为一个带 `all` classifier 的 jar，但不打入 Spring MVC、Log4j2 等外部依赖。

```sh
mvn package -pl safe-output-spring-boot-starter -am -Psingle-jar -DskipTests
```

## 本模块验证

在 `safe-output/` 根目录执行：

```sh
mvn -pl safe-output-spring-boot-starter -am test
```

测试覆盖属性绑定、自动装配、response advice、API Ignore、报告 Bean 创建和 Log4j2 依赖聚合。

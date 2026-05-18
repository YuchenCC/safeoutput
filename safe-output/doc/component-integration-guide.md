# Safe Output 组件接入手册

本文面向需要在 Spring Boot 2.x 业务系统中接入 Safe Output 的开发者，覆盖核心脱敏组件、Spring Boot starter、Response 脱敏、Log4j2 日志脱敏、主动脱敏、自定义策略和聚合报告配置。

## 1. 环境要求

| 项目 | 要求 |
|---|---|
| JDK | Java 8 |
| Spring Boot | 2.x，当前基线为 2.7.18 |
| 构建工具 | Maven multi-module |
| 自动装配 | `spring.factories` |
| Web 框架 | Spring MVC `ResponseBodyAdvice` |

本地开发或业务系统接入前，先在 Safe Output 工程根目录安装组件：

```sh
cd safe-output
mvn install
```

业务系统只需要直接依赖 starter，不需要单独声明 core、report 等内部模块。

```xml
<dependency>
  <groupId>com.safeoutput</groupId>
  <artifactId>safe-output-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 2. 最小接入

Spring Boot 2.x 应用引入 starter 后，会自动创建规则匹配、策略注册、对象递归脱敏、主动脱敏服务和 Response 脱敏组件。Controller 不需要显式调用 Safe Output。

```java
@RestController
class CustomerController {

    @GetMapping("/customers/current")
    public CustomerResponse current() {
        return new CustomerResponse("张三", "13800138000", "foo@example.com");
    }
}
```

默认规则按字段名精确匹配，当前内置明细如下：

| 默认规则 | 字段名 | 脱敏类型 |
|---|---|---|
| `default.mobile` | `mobile`、`phone`、`telephone`、`tel`、`userMobile` | `MOBILE` |
| `default.id-card` | `idCard`、`certNo`、`identityNo`、`certificateNo` | `ID_CARD` |
| `default.bank-card` | `bankCard`、`cardNo`、`bankNo` | `BANK_CARD` |
| `default.email` | `email`、`mail` | `EMAIL` |
| `default.password` | `password`、`secret`、`token` | `PASSWORD` |

`name`、`id`、`code`、`no`、`address` 等字段不会仅凭字段名默认脱敏，需要使用注解或配置规则明确声明。自定义规则的标准 YAML 配置方式是把 `safe-output.rules` 写成数组：

```yaml
safe-output:
  rules:
    - name: customerMobile
      keys:
        - customerMobile
      type: MOBILE
```

老系统如果存在字段名历史混乱、默认 key 容易误伤的情况，可以关闭默认规则库。关闭后只保留注解规则和 `safe-output.rules[]` 中显式声明的用户规则。

```yaml
safe-output:
  rules:
    default-enabled: false
```

如果在同一个 YAML 文件中既要关闭默认规则库，又要声明自定义规则，不能把 `rules` 同时写成对象和数组。也就是说，下面这种写法不是合法 YAML 结构：

```yaml
safe-output:
  rules:
    default-enabled: false
    - name: customerMobile
      keys:
        - customerMobile
      type: MOBILE
```

此时可以使用带引号的索引 key。`"[0]"` 对应 properties 写法中的 `safe-output.rules[0]`，用于让 Spring Boot 绑定到第 0 条自定义规则：

```yaml
safe-output:
  rules:
    default-enabled: false
    "[0]":
      name: customerMobile
      keys:
        - customerMobile
      type: MOBILE
```

全局开关：

```yaml
safe-output:
  enabled: true
  response:
    enabled: true
```

关闭 `safe-output.enabled` 或 `safe-output.response.enabled` 后，Response 脱敏不会处理返回值。

## 3. Response 脱敏

### 3.1 字段注解

对字段使用 `@Desensitize(type = "...")` 可以明确指定脱敏类型。注解规则优先级高于配置规则。

```java
import com.safeoutput.core.Desensitize;
import com.safeoutput.core.MaskTypes;

public class CustomerResponse {

    @Desensitize(type = MaskTypes.CHINESE_NAME)
    private String realName;

    @Desensitize(type = MaskTypes.MOBILE)
    private String contactNumber;
}
```

内置类型：

| 类型常量 | 配置值 |
|---|---|
| `MaskTypes.MOBILE` | `mobile` |
| `MaskTypes.EMAIL` | `email` |
| `MaskTypes.ID_CARD` | `id_card` |
| `MaskTypes.BANK_CARD` | `bank_card` |
| `MaskTypes.CHINESE_NAME` | `chinese_name` |
| `MaskTypes.ADDRESS` | `address` |
| `MaskTypes.PASSWORD` | `password` |
| `MaskTypes.DEFAULT` | `default` |

配置文件中也可以写大写或短横线形式，例如 `MOBILE`、`ID-CARD`，组件会归一化为标准 String type。`phone` 会归一化为 `mobile`。

### 3.2 配置规则

通过 `safe-output.rules[]` 可以在不改 DTO 的情况下按字段名或路径声明规则。

```yaml
safe-output:
  rules:
    - name: customerMobile
      keys:
        - customerMobile
        - contactNumber
      paths:
        - $.customer.mobile
      type: MOBILE
      enabled: true
    - name: realName
      keys:
        - realName
      type: CHINESE_NAME
```

规则字段说明：

| 配置项 | 说明 |
|---|---|
| `name` | 规则名称，用于识别和排查 |
| `keys` | 按字段名匹配，适合 DTO 字段、Map key |
| `paths` | 按字段路径匹配，支持精确匹配和 `[*]` 数字下标段通配 |
| `type` | 脱敏类型，支持内置类型和自定义 String type |
| `enabled` | 是否启用规则，默认 `true` |

路径格式说明：

- `$` 表示本次被脱敏对象的根节点。
- `.` 表示字段或 Map key 层级。
- `[0]`、`[1]` 是集合或数组遍历时生成的实际数字下标。
- `[*]` 只表示集合或数组任意数字下标段，例如 `$.items[*].title` 可匹配 `$.items[0].title` 和 `$.items[12].title`。
- `paths` 不是完整 JSONPath，不支持 `**`、条件表达式、字段通配或模糊匹配；除 `[*]` 数字下标通配外，其余部分按路径精确匹配。

规则优先级固定为：字段 ignore、注解、配置规则、默认规则。未知 type 会记录 warning，并回退为 `DEFAULT` 策略兜底脱敏。

### 3.3 包装响应体

如果业务响应统一包了一层，例如真正的数据在 `data` 或 `result.data` 中，可以配置 `body-data-path`，只处理指定节点。

```yaml
safe-output:
  response:
    enabled: true
    body-data-path: data
```

嵌套路径示例：

```yaml
safe-output:
  response:
    body-data-path: result.data
```

当路径不存在或读取失败时，Response 链路会 fail-open 返回原始 body，不影响业务接口。

### 3.4 Ignore 配置

字段级 ignore 用于明确允许某些字段保持原样。

```yaml
safe-output:
  ignore:
    keys:
      - plainNote
    paths:
      - $.items[*].title
```

接口级 ignore 用于允许某些接口明文返回。命中后组件不会改写 Response，但会记录 ignored 风险统计。

```yaml
safe-output:
  ignore:
    apis:
      - method: GET
        path: /internal/plain-mobile
        reason: business plaintext lookup
      - pattern: /internal/raw/**
        reason: internal passthrough
```

接口 ignore 字段说明：

| 配置项 | 说明 |
|---|---|
| `method` | HTTP 方法，可选 |
| `path` | 精确路径或 MVC 风格路径 |
| `pattern` | 路径匹配模式 |
| `reason` | 明文豁免原因，会进入风险统计 |
| `scenes` | 生效场景，未配置时按 Response 场景处理 |

## 4. Log4j2 日志脱敏

业务系统使用 Log4j2 时，在 `log4j2.xml` 的 `PatternLayout` 中使用 `%safeOutputMsg` 或 `%safeOutputMessage`，即可在日志消息输出前执行脱敏。

```xml
<Configuration status="WARN">
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <PatternLayout pattern="%d{HH:mm:ss.SSS} %-5level %logger{36} - %safeOutputMsg{maxMessageLength=5000,maxValueLength=300}%n"/>
    </Console>
  </Appenders>
  <Loggers>
    <Root level="INFO">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
```

通过 `safe-output-spring-boot-starter` 接入时，Log4j2 converter 会复用 Spring 中的 `safe-output.rules[].keys`、`safe-output.ignore.keys`、自定义 `MaskStrategy` Bean 和 `safe-output.log.*` 选项。业务系统通常不需要单独依赖 `safe-output-log4j2`。

配置优先级需要注意：Spring Boot 应用启动后，starter 会把 `safe-output.log.*` 注册到 Log4j2 runtime bridge。此时 `%safeOutputMsg{maxMessageLength=5000,maxValueLength=300}` 中的长度、fallback、key-value 等局部选项不再作为最终运行值，最终以 YAML / properties 中的 `safe-output.log.*` 为准。XML 中的局部选项主要用于无 Spring runtime bridge、只直接使用 `safe-output-log4j2` 模块的场景；另外，XML 中 `enabled=false` 仍可在 converter 初始化时直接关闭本 pattern 的脱敏。

日志 key-value 脱敏依赖字段名上下文，例如：

```text
mobile=13800138000
"email":"foo@example.com"
idCard: 11010119900307001X
```

可用配置：

```yaml
safe-output:
  log:
    enabled: true
    key-value-rule-enabled: true
    max-message-length: 5000
    max-value-length: 300
    max-rule-keys: 128
    regex-fallback:
      enabled: false
      id-card-check-code-enabled: true
```

日志配置说明：

| 配置项 | 默认值 | 说明 |
|---|---:|---|
| `log.enabled` | `true` | 是否启用日志脱敏 |
| `log.key-value-rule-enabled` | `true` | 是否启用 key-value 规则脱敏 |
| `log.max-message-length` | `5000` | 超过该长度的整条日志不处理 |
| `log.max-value-length` | `300` | 超过该长度的单个值不处理 |
| `log.max-rule-keys` | `128` | 参与日志 key-value 匹配的字段名上限 |
| `log.regex-fallback.enabled` | `false` | 是否启用无字段名上下文的兜底正则 |
| `log.regex-fallback.id-card-check-code-enabled` | `true` | 识别孤立身份证号时是否校验末位校验码，默认开启以降低普通 18 位编号误伤 |

regex fallback 的实现方式：日志消息会先按 key-value / JSON-like 片段识别字段名并执行规则脱敏；如果开启 `log.regex-fallback.enabled`，第二阶段才对整条消息做有限正则扫描，固定按手机号、邮箱、身份证顺序处理无字段名上下文的敏感值。孤立身份证号会额外经过大陆身份证格式、出生日期、年份范围和可选校验位检查；银行卡号不参与日志 regex fallback。

边界说明：

- key-value 支持 `key=value`、`key: value`、`key = value`、`key : value`，key 和 value 可使用单引号、双引号或不带引号。
- `rules[].keys` 可参与日志 key-value 脱敏，`rules[].paths` 不作为日志文本匹配依据。
- `ignore.keys` 命中时优先跳过对应 key-value 脱敏。
- 超过 `max-message-length` 的日志整条 fail-open 返回原文。
- 超过 `max-value-length` 的单个值不会处理。
- regex fallback 固定覆盖手机号、邮箱、合法大陆身份证号这类边界明确的类型；银行卡号不做无字段名上下文的全局兜底。
- `log.regex-fallback.id-card-check-code-enabled` 默认开启，只影响无字段名上下文的孤立身份证号识别。开启时需要同时通过格式、出生日期、年份范围和末位校验码；关闭后仍会校验格式、出生日期和年份范围，但不再校验第 18 位校验码。
- 带字段名上下文的银行卡号仍可通过 `rules[].keys` 命中 `BANK_CARD` 策略脱敏。
- 带字段名上下文的日志 key-value 脱敏不受该配置影响，例如 `idCard=350102199001011234` 命中 `idCard` 规则后仍会按 `ID_CARD` 脱敏，即使末位校验码不合法。
- converter 初始化或脱敏过程异常时返回原日志消息，避免影响业务日志输出。

无 Spring runtime bridge、只直接使用 log4j2 模块时，也可以在 pattern 中写局部选项；这些局部选项只在未接入 starter runtime bridge 时作为最终配置生效：

```xml
<PatternLayout pattern="%safeOutputMsg{enabled=true,regexFallback=false,maxMessageLength=5000,maxValueLength=300}%n"/>
```

## 5. 主动脱敏

需要在业务代码中主动处理字符串或对象时，可以注入 `SafeOutputMaskService`。

```java
import com.safeoutput.core.MaskTypes;
import com.safeoutput.core.SafeOutputMaskService;

@Service
public class CustomerExportService {

    private final SafeOutputMaskService safeOutputMaskService;

    public CustomerExportService(SafeOutputMaskService safeOutputMaskService) {
        this.safeOutputMaskService = safeOutputMaskService;
    }

    public String maskMobile(String mobile) {
        return safeOutputMaskService.mask(mobile, MaskTypes.MOBILE);
    }
}
```

主动脱敏 API：

| 方法 | 说明 |
|---|---|
| `mask(String value, String type)` | 按指定 type 脱敏单个字符串 |
| `maskObject(Object value)` | 复用对象规则递归处理 Bean、Map、Collection、数组 |
| `maskStrong(String value)` | 对文本执行强扫描，默认覆盖手机号、邮箱、身份证 |
| `maskObjectStrong(Object value)` | 对对象中的字符串执行强扫描 |

强扫描的无字段名上下文 fallback 类型可通过配置指定。未配置时默认扫描 `MOBILE`、`EMAIL`、`ID_CARD`；一旦配置 `types`，则以配置清单为准，不再自动追加默认三类。当前内置 fallback 识别支持 `MOBILE`、`EMAIL`、`ID_CARD`、`BANK_CARD`。

```yaml
safe-output:
  manual:
    strong-scan:
      types:
        - MOBILE
        - EMAIL
        - ID_CARD
        - BANK_CARD
```

主动脱敏调用会进入 `MANUAL` 场景统计，但不会默认进入 Response 接口风险统计。

## 6. 自定义脱敏策略

业务需要新增脱敏类型时，实现 `MaskStrategy` 并注册为 Spring Bean。

```java
import com.safeoutput.core.MaskContext;
import com.safeoutput.core.MaskStrategy;

@Component
public class EmployeeNoMaskStrategy implements MaskStrategy {

    @Override
    public String type() {
        return "employee_no";
    }

    @Override
    public String mask(String rawValue, MaskContext context) {
        if (rawValue == null || rawValue.length() <= 4) {
            return "****";
        }
        return "****" + rawValue.substring(rawValue.length() - 4);
    }
}
```

配置中引用同名 type：

```yaml
safe-output:
  rules:
    - name: employeeNo
      keys:
        - employeeNo
      type: employee_no
```

注解中也可以引用：

```java
public class EmployeeResponse {

    @Desensitize(type = "employee_no")
    private String employeeNo;
}
```

未知 type 的当前策略为 `DEFAULT fallback`：记录 warning 和未知类型统计后，使用 `DEFAULT` 策略兜底脱敏。

## 7. 聚合报告

启用报告后，starter 会创建 `MaskMetricsCollector` 和定时 `MaskReportExporter`，输出本地 JSON 快照。

```yaml
safe-output:
  report:
    enabled: true
    directory: target/safe-output-reports
    file-prefix: safe-output-report
    interval-millis: 60000
    retain-files: 10
```

报告配置说明：

| 配置项 | 默认值 | 说明 |
|---|---:|---|
| `enabled` | `false` | 是否启用报告导出 |
| `directory` | `./safe-output-reports` | 报告输出目录 |
| `file-prefix` | `safe-output-report` | 报告文件名前缀 |
| `interval-millis` | `60000` | 定时导出间隔 |
| `retain-files` | `10` | 保留最新报告数量 |

报告只保存聚合指标、类型分布、接口风险、ignored 次数、失败次数和耗时信息，不应保存敏感原文。

## 8. 通用配置参考

```yaml
safe-output:
  enabled: true
  max-depth: 8
  max-collection-size: 1000
  response:
    enabled: true
    body-data-path: data
  log:
    enabled: true
    key-value-rule-enabled: true
    max-message-length: 5000
    max-value-length: 300
    max-rule-keys: 128
    regex-fallback:
      enabled: false
      id-card-check-code-enabled: true
  manual:
    strong-scan:
      types:
        - BANK_CARD
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
        path: /internal/plain-mobile
        reason: business plaintext lookup
  report:
    enabled: true
    directory: target/safe-output-reports
    file-prefix: safe-output-report
    interval-millis: 60000
    retain-files: 10
```

| 配置项 | 默认值 | 说明 |
|---|---:|---|
| `enabled` | `true` | 总开关 |
| `max-depth` | `8` | 对象递归最大深度 |
| `max-collection-size` | `1000` | 集合或数组最大处理数量 |
| `response.enabled` | `true` | Response 脱敏开关 |
| `response.body-data-path` | 空 | 包装响应体的数据路径 |
| `log.enabled` | `true` | 日志脱敏开关 |
| `log.key-value-rule-enabled` | `true` | 日志 key-value 规则脱敏开关 |
| `log.max-message-length` | `5000` | 日志消息处理长度上限 |
| `log.max-value-length` | `300` | 日志单值处理长度上限 |
| `log.max-rule-keys` | `128` | 日志字段名规则数量上限 |
| `log.regex-fallback.enabled` | `false` | 日志兜底正则开关 |
| `log.regex-fallback.id-card-check-code-enabled` | `true` | 身份证兜底识别是否校验校验位，默认开启，只影响无字段名上下文的孤立身份证号 |
| `manual.strong-scan.types` | 空 | 主动强扫描 fallback 类型清单；为空时默认 `MOBILE`、`EMAIL`、`ID_CARD`，配置后以清单为准 |
| `rules.default-enabled` | `true` | 是否启用内置默认字段规则 |

## 9. 安全边界

- Response 脱敏异常会 fail-open，返回原始业务结果，避免影响接口可用性。
- 对象递归支持 Bean、Map、Collection、数组，并带最大深度、集合上限和循环引用保护。
- Bean 字段脱敏是原地修改；如果业务复用同一个响应对象实例，需要评估副作用。
- 字段 path 按 Safe Output 递归路径匹配，仅支持精确匹配和 `[*]` 数字下标段通配，不做模糊扩大。
- 日志脱敏只做轻量 JSON-like/key-value 识别和有限 fallback，不强制依赖 JSON Parser。
- API ignore 可以返回明文，但必须配置明确 reason，并进入风险统计。
- 统计和报告应只保存聚合信息，不保存原始 response 或敏感字段值。
- 未知 type 默认回退到 `DEFAULT`，同时记录 warning 和未知类型聚合统计，避免配置错误静默丢失。

## 10. 常见问题排查

### 10.1 引入依赖后没有自动脱敏

检查业务应用是否是 Spring Boot 2.x，并确认依赖的是 `safe-output-spring-boot-starter`。如果关闭了 `safe-output.enabled` 或 `safe-output.response.enabled`，Response 不会被处理。

### 10.2 字段没有命中默认规则

默认规则只覆盖第 2 节列出的字段名。对 `name`、`id`、`code`、`no`、`address` 等字段，需要使用 `@Desensitize` 或 `safe-output.rules[]` 显式声明。如果已配置 `safe-output.rules.default-enabled=false`，所有默认字段规则都会关闭。

### 10.3 配置规则没有生效

检查 `rules[].enabled` 是否为 `true`，`keys` 是否与字段名或 Map key 一致，`paths` 是否与实际递归路径一致。集合或数组下标可用 `[*]` 匹配任意数字下标段。还需要确认 `type` 是否存在对应内置策略或自定义 `MaskStrategy`。

### 10.4 `body-data-path` 配置后仍未脱敏

确认路径是否从响应对象根节点开始，并使用点分隔，例如 `data` 或 `result.data`。路径不存在时组件会 fail-open，不会抛出业务异常。

### 10.5 日志没有脱敏

确认业务日志 pattern 中使用了 `%safeOutputMsg` 或 `%safeOutputMessage`，并且 `safe-output.log.enabled=true`。如果日志字段只配置在 `rules[].paths` 中，不会参与日志文本匹配；日志 key-value 脱敏需要配置 `rules[].keys` 或命中默认字段名。若 `safe-output.rules.default-enabled=false`，默认字段名不会参与日志 key-value 脱敏。

### 10.6 日志中孤立手机号、邮箱或身份证没有脱敏

确认是否启用了 `safe-output.log.regex-fallback.enabled=true`。默认推荐依赖字段名上下文处理 key-value 日志，fallback 应只用于边界明确的类型。

孤立身份证号还会额外经过大陆身份证轻量校验：格式、出生日期和年份范围始终校验；`safe-output.log.regex-fallback.id-card-check-code-enabled` 默认值为 `true`，默认还会校验第 18 位校验码。只有在明确需要兼容历史脏数据或测试数据、并能接受更多 18 位编号被识别为身份证时，才建议改为 `false`。

### 10.7 接口返回了明文

检查是否命中了 `safe-output.ignore.apis`。接口级 ignore 命中后会直接返回原 body，但会记录 ignored 风险统计。

### 10.8 报告没有生成

确认 `safe-output.report.enabled=true`，输出目录有写入权限，且应用运行时间超过 `interval-millis`。报告导出失败不会影响业务接口，会记录失败统计。

## 11. Report 模块接入摘要

`safe-output-report` 是统计和报告模块，业务系统通常不需要直接依赖。通过 `safe-output-spring-boot-starter` 接入并开启报告后，starter 会自动创建 `MaskMetricsCollector` 和 `MaskReportExporter`，用于采集脱敏聚合指标并导出本地 JSON 快照。

报告的用途是帮助接入方回答以下问题：

- 当前 Response、Log、Manual 场景分别发生了多少次脱敏。
- 哪些脱敏类型命中最多，是否存在未注册或拼写错误的未知 type。
- 哪些接口返回了高敏类型、字段数量较高、命中频率较高或被 API ignore 豁免。
- Log fallback 发现了哪些疑似可补充为 `safe-output.rules[]` 的字段名。
- 脱敏链路是否存在失败或明显耗时异常。

报告只保存聚合指标、类型标签、接口维度、耗时、失败次数和脱敏后的 evidence，不保存敏感原文、完整 response 或完整日志。

### 11.1 启用报告

业务系统继续只依赖 starter：

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

| 配置项 | 默认值 | 说明 |
|---|---:|---|
| `safe-output.report.enabled` | `false` | 是否启用报告采集和导出 |
| `safe-output.report.directory` | `./safe-output-reports` | JSON 快照输出目录 |
| `safe-output.report.file-prefix` | `safe-output-report` | 快照文件名前缀 |
| `safe-output.report.interval-millis` | `60000` | 定时导出间隔，最小归一为 1 |
| `safe-output.report.retain-files` | `10` | 保留最新报告文件数，最小归一为 1 |
| `safe-output.report.include-api-metrics` | `true` | 当前已绑定配置，导出字段暂不按该开关裁剪 |
| `safe-output.report.include-field-path` | `true` | 当前已绑定配置，报告实际不输出字段路径 |
| `safe-output.report.include-raw-value` | `false` | 当前已绑定配置，报告仍不会输出敏感原文 |

报告默认关闭。未开启时，starter 不会创建报告采集器和导出器，也不会生成 Response 风险画像、Log 规则建议或 JSON 快照。

### 11.2 统计来源和用法

开启报告后，starter 会把同一个 `MaskMetricsCollector` 接入 Response、Log4j2、主动脱敏和报告导出链路。常见统计来源如下：

| 来源 | 场景 | 说明 |
|---|---|---|
| Response 自动脱敏 | `RESPONSE` | `ResponseBodyAdvice` 处理返回值时记录脱敏次数、类型分布、接口风险、ignore 和失败信息 |
| Log4j2 日志脱敏 | `LOG` | `%safeOutputMsg` 成功脱敏 key-value 或 fallback 值后记录日志脱敏次数；`logCount` 统计的是成功脱敏的日志值次数，不是日志行数 |
| 主动脱敏服务 | `MANUAL` | 注入 `SafeOutputMaskService` 调用 `mask`、`maskObject`、`maskStrong` 或 `maskObjectStrong` 时记录主动脱敏次数 |
| 手工补充统计 | 业务自定义 | 业务方可注入 `MaskMetricsCollector`，只补充场景、类型和耗时等聚合指标 |

手工补充统计适合接入方已有独立脱敏入口、异步任务或批处理任务，但又希望统一进入 Safe Output 报告。示例：

```java
private final MaskMetricsCollector metricsCollector;

public void recordManualMaskMetric(long elapsedNanos) {
    metricsCollector.recordMask(MaskScene.MANUAL, "mobile", elapsedNanos);
}
```

手工统计只应传入场景、类型和耗时等聚合信息，不要把原始值、完整响应体、完整日志或敏感样本写入报告链路。

### 11.3 导出和读取报告

启用报告后，`MaskReportExporter` 会按 `safe-output.report.interval-millis` 定时导出 JSON 快照。应用启动后不会立即写第一份报告，而是在第一个间隔后导出。每次导出的文件是当前进程内聚合快照，不是从上次导出到本次导出的增量。

如果业务系统需要主动导出报告，可注入 `MaskReportExporter`：

```java
private final MaskReportExporter exporter;
```

调用：

```java
Path path = exporter.exportNow();
```

`exportNow()` 失败时返回 `null`，同时增加失败计数并记录 warning，不会抛出异常影响业务流程。

如果需要在程序内读取当前聚合快照，可注入 `MaskMetricsCollector`：

```java
private final MaskMetricsCollector metricsCollector;
```

读取：

```java
MaskReport report = metricsCollector.snapshot();
ResponseRiskAnalysis analysis = report.getResponseRiskAnalysis();
```

快照对象只包含聚合指标，不能从中恢复原始 response、日志或敏感值。

### 11.4 报告可提供的信息

| 信息 | 说明 | 常见用途 |
|---|---|---|
| 总量和场景分布 | `totalCount`、`responseCount`、`logCount`、`manualCount` | 判断接入后实际覆盖了哪些脱敏场景 |
| 类型分布 | `maskTypeCounts` | 观察手机号、邮箱、身份证、银行卡等类型的命中情况 |
| 未知类型统计 | `unknownTypeCounts` | 发现配置拼写错误或自定义 `MaskStrategy` 漏注册 |
| 接口维度指标 | method、path、命中次数、脱敏字段数、耗时、失败次数、类型分布 | 定位高频、高敏或耗时异常接口 |
| Response 风险画像 | 风险等级、风险原因、治理建议、ignored 高风险接口 | 排查 API ignore、password、身份证、银行卡、高字段量等治理线索 |
| Log 规则建议 | 候选 key、建议 type、命中次数、置信度、脱敏 evidence、候选 YAML 片段 | 将日志 fallback 线索沉淀为显式 `safe-output.rules[]` 配置 |
| 导出状态 | `failureCount` 和本地 JSON 快照文件 | 发现报告导出失败，保留审计快照 |

Log 规则建议默认只提出候选配置，不自动改配置、不自动启用规则。建议片段中的 evidence 只使用 `key=<type>` 形态，不包含命中的日志原文或敏感值。

### 11.5 使用注意事项

- 报告是进程内内存聚合，没有数据库持久化；应用重启后内存计数会从 0 开始。
- 导出的 JSON 是快照文件，不是实时查询存储，也不是长期完整指标仓库。
- 接口维度有上限，超过后会进入 overflow 聚合，避免高基数路径导致内存无限增长。
- API ignore 可以明文返回，但仍会进入风险统计，并在风险画像中标记为高风险豁免。
- `include-api-metrics`、`include-field-path`、`include-raw-value` 当前已绑定配置；报告仍不会输出敏感原文。
- 风险评分是内置启发式治理线索，不代表合规结论。

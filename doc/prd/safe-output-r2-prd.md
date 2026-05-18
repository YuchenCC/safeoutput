# Safe Output 数据脱敏组件第二轮需求说明书 PRD

版本：v0.4 / R2  
基准版本：MVP PRD v0.3  
适用范围：Java 通用数据脱敏组件第二轮迭代  
技术基线：JDK8 + Spring Boot 2.x + Log4j2 2.x  
交付形态：`safe-output-spring-boot-starter` Java Starter 包  

---

## 1. 文档目标

本 PRD 基于 Safe Output MVP 版本完成第一次编码、验证和试点后的反馈整理，作为第二轮需求开发、概要设计、详细设计、Issue 拆分和 AI Coding 的输入文档。

第二轮不改变组件的核心定位：

> Safe Output 是面向传统 Java 服务的低侵入式输出侧敏感数据脱敏组件，优先覆盖接口 Response、Log4j2 日志打印和宿主系统主动调用三个场景。

第二轮重点目标是：

1. 提升内置脱敏策略对真实业务数据的适配性。
2. 修复配置和注解层面对自定义脱敏类型的扩展限制。
3. 增强日志脱敏的字段上下文识别和规则发现能力。
4. 新增主动脱敏服务，允许宿主系统在 DAO、缓存、业务代码中复用统一脱敏能力。
5. 将统计报告从运行指标升级为治理辅助能力。
6. 增强 Demo 的第二轮可验证性，并为第三轮竞赛展示看板预留需求。

---

## 2. 第二轮需求总览

| 编号 | 需求名称 | 优先级 | 类型 | 第二轮是否实施 |
|---|---|---:|---|---|
| R2-01 | 姓名脱敏策略增强 | P0 | 策略优化 | 是 |
| R2-02 | 身份证脱敏识别策略优化 | P0 | 策略优化 | 是 |
| R2-03 | 自定义脱敏类型配置放行 | P0 | 架构修正 | 是 |
| R2-04 | 脱敏类型标签 String 化改造 | P0 | 架构修正 | 是 |
| R2-05 | 日志脱敏支持配置字段 key-value 规则匹配 | P0 | 日志增强 | 是 |
| R2-06 | 主动脱敏服务能力 | P0 | 新增能力 | 是 |
| R2-07 | 统计分析与治理建议增强 | P0 | 报告增强 | 是 |
| R2-08 | Demo 可验证性增强 | P1 | Demo 验证 | 是 |
| R3-01 | Demo 竞赛展示看板 | P1 | 第三轮需求 | 第三轮实施 |

---

## 3. R2-01 姓名脱敏策略增强

### 3.1 背景

MVP 版本中的 `CHINESE_NAME` 策略主要面向 2～4 位中文姓名。试点后发现，真实业务数据中存在少数民族姓名、英文名、中英混合姓名、带分隔符姓名等情况。继续限制传统中文姓名长度，会导致适配性不足。

### 3.2 需求目标

将内置姓名脱敏策略从传统 2～4 位中文姓名处理，调整为更通用的姓名脱敏策略。

命中姓名规则后，不再强依赖姓名长度和中文字符类型，默认采用：

> 首尾保留，中间脱敏。

### 3.3 处理规则

1. 空值返回原值。
2. 空字符串返回原值。
3. 长度为 1 的姓名脱敏为 `maskChar`。
4. 长度为 2 的姓名保留首字符，其余字符脱敏。
5. 长度大于等于 3 的姓名保留首尾字符，中间字符全部脱敏。
6. 支持中文姓名、少数民族姓名、英文名、中英混合姓名和常见分隔符场景。
7. 该调整只影响姓名策略的脱敏算法，不放宽姓名字段的自动识别范围。

### 3.4 示例

| 原文 | 脱敏结果 |
|---|---|
| 张三 | 张* |
| 王小明 | 王*明 |
| 欧阳娜娜 | 欧**娜 |
| 迪丽热巴 | 迪**巴 |
| 买买提江 | 买***江 |
| Alice | A***e |
| Michael Zhang | M***********g |
| 张 Michael | 张********l |

### 3.5 边界

1. `name` 等模糊字段仍不默认进入强脱敏规则。
2. 商品名、角色名、机构名、菜单名、产品名等业务名称不能因为包含 `name` 而被误脱敏。
3. 姓名字段是否脱敏，仍由注解、明确 key、明确 path 或默认高置信字段决定。
4. `product.name`、`role.name` 等字段建议通过 ignore 或不纳入默认规则避免误伤。

### 3.6 验收标准

1. `张三` 脱敏为 `张*`。
2. `王小明` 脱敏为 `王*明`。
3. 长姓名、英文名、中英混合姓名可稳定脱敏。
4. `name` 字段默认不因字段名本身被强制脱敏。
5. 配置 `realName` 或 `customerName` 后，可按姓名策略脱敏。

---

## 4. R2-02 身份证脱敏识别策略优化

### 4.1 背景

MVP 版本已支持大陆身份证长度、生日、校验位等校验逻辑。试点后进一步确认：

1. Response 场景主要依赖字段、路径、注解和配置主动识别。
2. Log 场景虽然存在无上下文正则兜底，但允许一定程度误脱敏。
3. 组件定位是脱敏组件，不是身份证真伪认证系统。

因此第二轮不应将身份证识别设计为重型合法性认证能力，而应采用性能优先的折中策略。

### 4.2 需求目标

按上下文对身份证脱敏识别进行分层处理：

1. 明确上下文场景：命中即脱敏，性能优先。
2. 日志无上下文兜底场景：轻量校验后脱敏。
3. 行政区划校验：不作为默认强制能力，仅作为可选增强预留。

### 4.3 明确上下文场景

包括：

1. Response 字段命中 `ID_CARD`。
2. 注解指定 `ID_CARD`。
3. 配置 path 指定 `ID_CARD`。
4. 日志中明确 key-value 命中身份证字段。
5. 主动脱敏服务中用户指定 type 为 `ID_CARD`。

处理规则：

1. 字段或上下文已明确为身份证时，直接执行身份证脱敏。
2. 不强制执行完整身份证合法性校验。
3. 只做必要的空值、长度、字符串安全处理。
4. 即使值格式不完全合法，也优先脱敏，避免明文输出。

### 4.4 日志无上下文兜底场景

对于日志中孤立出现的疑似身份证字符串，采用轻量校验后脱敏。

默认校验建议包括：

1. 格式校验：17 位数字 + 1 位数字或 `X/x`。
2. 出生日期合法性校验。
3. 年份范围合理性校验。
4. 校验位校验可配置启用。

行政区划校验不默认启用。

### 4.5 行政区划校验策略

行政区划校验作为后续增强项或可配置项预留。

建议配置：

```yaml
safe-output:
  id-card:
    checksum-check-enabled: true
    region-check-enabled: false
```

默认策略：

1. `checksum-check-enabled: true`。
2. `region-check-enabled: false`。
3. 不引入外部行政区划库强依赖。
4. 不维护实时权威行政区划码表。

### 4.6 验收标准

1. Response 中 `idCard` 字段命中后可直接脱敏。
2. Response 中格式不完全合法但字段明确为身份证时，仍执行脱敏。
3. 日志中 `idCard=350102199001011234` 可直接脱敏。
4. 日志中孤立出现的疑似身份证号，经轻量校验后可脱敏。
5. 日志中明显不符合身份证日期格式的 18 位字符串不脱敏。
6. 行政区划校验不作为默认必选项。
7. 身份证识别过程不引入外部行政区划库强依赖。

---

## 5. R2-03 自定义脱敏类型配置放行

### 5.1 背景

MVP 版本中，如果 `rule.type` 绑定为 `MaskType` 枚举，当配置中出现内置枚举之外的类型时，会在应用启动阶段发生配置绑定错误，导致宿主系统无法启动。

这会限制业务方扩展自定义 `MaskStrategy` 的能力。

示例：

```yaml
safe-output:
  rules:
    - name: mobileM-rule
      keys:
        - mobileM
      type: mobileM
      enabled: true
```

如果 `type` 只能绑定 `MaskType`，则 `mobileM` 无法通过配置绑定。

### 5.2 需求目标

配置模型中的 `rule.type` 不再强绑定 `MaskType` 枚举，统一调整为 `String`。

内置 `MaskType` 仅作为系统内置类型常量和文档说明，不作为用户配置的唯一合法范围。

### 5.3 处理规则

1. `rule.type` 使用 `String` 承载。
2. 配置中出现内置类型之外的自定义 type 时，应用允许启动。
3. `MaskStrategyRegistry` 使用 String type 作为注册和查找 key。
4. 策略 type 注册与查找默认做 `trim` 和大小写归一化。
5. 未知 type 不阻断应用启动。
6. 运行期规则命中但未找到对应策略时，默认跳过该字段脱敏，并输出 warn 日志。
7. 未知 type 应纳入统计，便于发现配置错误。

### 5.4 未知 type 默认策略

当前实现策略：

```text
warn + DEFAULT fallback
```

命中未知 type 时不抛出业务异常，记录 warning 和 unknown type 聚合统计，并使用 `DEFAULT` 策略兜底脱敏。该实现优先避免未知 type 导致敏感值原样输出，同时通过报告暴露配置拼写错误或策略未注册问题。

历史讨论中预留过可配置策略：

```yaml
safe-output:
  strategy:
    unknown-type-policy: DEFAULT
```

该配置项当前尚未暴露为运行时开关。可选值仍可作为后续治理增强方向：

| 策略 | 含义 |
|---|---|
| SKIP | 跳过并告警 |
| DEFAULT | 当前实现，使用默认策略兜底并记录 unknown type |
| FAIL | 启动或运行时报错，适合强治理项目 |

### 5.5 验收标准

1. 配置 `type: mobileM` 时，应用可以正常启动。
2. 未注册 `mobileM` 策略时，命中该规则不抛出业务异常，字段使用 `DEFAULT` 兜底脱敏，并输出 warn 和 unknown type 统计。
3. 注册自定义 `mobileM` 策略 Bean 后，配置 `type: mobileM` 可以正常执行自定义脱敏。
4. 内置 `MOBILE`、`ID_CARD`、`EMAIL` 等类型仍保持兼容。
5. type 大小写差异不影响策略查找。
6. 自定义策略不会破坏内置策略。

---

## 6. R2-04 脱敏类型标签 String 化改造

### 6.1 背景

第 3 点解决的是配置 `rule.type` 的扩展性问题。但如果注解、策略接口、上下文对象、统计报告仍强绑定 `MaskType`，自定义类型仍无法贯穿全链路。

因此第二轮需要将对外脱敏类型标签统一调整为 String。

### 6.2 需求目标

采用双层设计：

> 对外扩展层统一使用 String；内置标准类型继续保留 `MaskType` 或 `MaskTypes` 常量作为规范来源。

### 6.3 需要调整为 String 的位置

1. 配置规则 `rule.type`。
2. `@Desensitize.type`。
3. `MaskStrategy.type()` 返回值。
4. `MaskStrategyRegistry` 注册和查找 key。
5. `MaskContext` 中的脱敏类型字段。
6. `MaskResult` 中的脱敏类型字段。
7. 统计报告中的类型标签。
8. unknown type 统计中的类型标签。

### 6.4 保留 MaskType / MaskTypes 的位置

`MaskType` 不再作为强约束类型，但可继续作为：

1. 内置类型清单。
2. 默认规则库引用来源。
3. 文档说明来源。
4. Demo 示例推荐值。
5. 内置策略注册时的常量来源。
6. 单元测试中的标准类型。

建议新增常量类：

```java
public final class MaskTypes {
    public static final String MOBILE = "MOBILE";
    public static final String ID_CARD = "ID_CARD";
    public static final String BANK_CARD = "BANK_CARD";
    public static final String EMAIL = "EMAIL";
    public static final String CHINESE_NAME = "CHINESE_NAME";
    public static final String ADDRESS = "ADDRESS";
    public static final String PASSWORD = "PASSWORD";
    public static final String DEFAULT = "DEFAULT";

    private MaskTypes() {}
}
```

推荐注解使用：

```java
@Desensitize(type = MaskTypes.MOBILE)
private String mobile;
```

自定义类型支持：

```java
@Desensitize(type = "mobileM")
private String mobileM;
```

### 6.5 类型安全补偿机制

由于 String 会降低编译期类型安全，需要通过以下方式补偿：

1. 提供内置 `MaskTypes` 常量类，减少裸字符串。
2. 启动期扫描规则并输出 unknown type 告警。
3. 运行期 unknown type 命中时 warn + DEFAULT fallback，并记录 unknown type 统计。
4. 统计报告记录 `unknownTypeCount` 和 unknown type 列表。
5. 文档中推荐业务优先使用 `MaskTypes` 常量。

### 6.6 验收标准

1. `@Desensitize(type = "mobileM")` 可以编译通过。
2. 配置 `type: mobileM` 可以绑定成功。
3. 自定义 `MaskStrategy.type() = "mobileM"` 注册后可以被配置和注解命中。
4. 内置 `MOBILE`、`ID_CARD`、`EMAIL` 等类型继续可用。
5. 统计报告可以按自定义 type 统计命中次数。
6. 未注册 type 不导致应用启动失败。
7. 未注册 type 命中时输出 warn，并纳入 unknown type 统计。

---

## 7. R2-05 日志脱敏支持配置字段 key-value 规则匹配

### 7.1 背景

MVP 版本日志脱敏已支持轻量 JSON-like key-value 识别、value 内轻量正则扫描、整条 message 正则兜底。

试点后发现，姓名、地址、自定义敏感字段等缺少稳定全局格式，仅靠手机号、邮箱、身份证等通用正则无法覆盖。业务方已经在 `rules.keys` 中配置字段语义，Log 场景应复用这些配置，提高日志脱敏覆盖率。

### 7.2 需求目标

日志脱敏模块新增配置字段 key-value 识别能力。

组件启动时读取 `enabled=true` 的 `rules.keys`，根据 key 与 type 建立日志字段匹配规则。当日志 message 中出现 key-value 轻量结构时，识别目标 value，并调用对应 `MaskStrategy` 执行脱敏。

### 7.3 支持格式

第二轮支持以下轻量结构：

```text
key=value
key: value
key = value
key : value
"key":"value"
"key": "value"
'key':'value'
key="value"
key='value'
```

示例：

```text
chineseName=张三
chineseName: 张三
"chineseName":"张三"
customerName='王小明'
mobileM=13812345678
```

### 7.4 处理规则

1. 日志 key-value 匹配复用 `rules.keys`。
2. 命中 key 后，根据 rule.type 查找对应 `MaskStrategy`。
3. type 支持内置类型和自定义 String 类型。
4. 未找到策略时，warn + DEFAULT fallback，不影响日志输出。
5. `rules.paths` 暂不作为日志文本匹配依据。
6. 如需匹配 `user.name` 这类文本 key，可显式加入 `rules.keys`。
7. 字段级 `ignore.keys` 对日志 key-value 匹配生效。
8. 接口级 ignore 不影响日志脱敏。

### 7.5 性能要求

日志脱敏是高频路径，必须以性能优先。

要求：

1. 不允许每条日志动态拼接并编译正则。
2. 启动或配置初始化时构建 key -> type 映射和 Pattern 缓存。
3. 支持最大日志长度限制。
4. 支持单个 value 最大扫描长度限制。
5. 支持参与日志匹配的 rule key 数量限制。
6. 超限时安全跳过或降级处理。
7. 日志脱敏异常不得影响日志输出。

建议配置：

```yaml
safe-output:
  log:
    key-value-rule-enabled: true
    max-message-length: 4096
    max-value-length: 256
    max-rule-key-count: 100
```

### 7.6 不做范围

第二轮不支持：

1. 完整 JSON Parser。
2. 嵌套 JSON path 解析。
3. 数组对象深度解析。
4. 跨行 value 解析。
5. 自然语言识别。
6. 日志模板参数反向解析。
7. 任意用户自定义复杂正则表达式。

### 7.7 验收标准

1. 配置 `keys: [chineseName] type: CHINESE_NAME` 后，日志 `chineseName: 张三` 输出为 `chineseName: 张*`。
2. 日志 `"chineseName":"王小明"` 可按姓名策略脱敏。
3. 配置自定义 `type: mobileM` 且注册对应策略后，日志 `mobileM=13812345678` 可按自定义策略脱敏。
4. 未注册自定义策略时，不影响日志输出，并记录 warn。
5. `ignore.keys` 命中的日志字段不脱敏。
6. 超过最大长度限制的日志处理不影响应用正常输出。
7. 不引入 fastjson、Jackson 等 JSON Parser 作为日志脱敏强依赖。

---

## 8. R2-06 主动脱敏服务能力

### 8.1 背景

MVP 版本主要通过 ResponseBodyAdvice 和 Log4j2 PatternConverter 在输出侧自动完成脱敏。

试点后发现，宿主系统在 DAO 层查询结果处理、写入缓存前处理、业务代码局部展示、消息发送前处理等场景中，也可能需要主动执行脱敏。

如果业务方另行实现脱敏逻辑，会导致：

1. 同一字段在 response、log、cache 中脱敏规则不一致。
2. 业务侧重复造轮子。
3. 自定义策略无法复用。
4. 后续规则变更时多处同步困难。

### 8.2 需求目标

组件新增主动脱敏服务接口，允许宿主系统在业务代码中主动调用统一脱敏能力。

主动服务底层必须复用组件已有：

1. `MaskStrategyRegistry`。
2. `MaskStrategy`。
3. 配置规则。
4. 注解规则。
5. 对象递归脱敏能力。
6. 日志强扫描能力。
7. 脱敏上下文。
8. 统计能力。

### 8.3 主动调用模式

第二轮主动脱敏服务支持三种模式：

1. 指定类型脱敏。
2. 对象规则脱敏，类似 Response。
3. 强扫描脱敏，类似 Log。

不再单独提供 `maskField(fieldName, value)` 作为主要模式。

---

### 8.4 模式一：指定类型脱敏

适用场景：宿主系统明确知道当前值是什么敏感类型。

示例：

```java
String maskedMobile = maskService.mask("13812345678", "MOBILE");
String maskedName = maskService.mask("张三", "CHINESE_NAME");
String maskedCustom = maskService.mask("13812345678", "mobileM");
```

处理规则：

1. 输入 `value + type`。
2. 根据 type 查找 `MaskStrategy`。
3. 执行对应策略。
4. 不做字段规则匹配。
5. 不做正则扫描。
6. 支持自定义 type。

特点：

1. 性能最高。
2. 误判最低。
3. 适合 DAO、缓存、局部变量、业务代码精确处理。

---

### 8.5 模式二：对象规则脱敏，类似 Response

适用场景：宿主系统直接传入 DTO、Map、List、缓存对象等结构化对象。

示例：

```java
UserDTO masked = maskService.maskObject(user);
Map<String, Object> maskedMap = maskService.maskObject(map);
List<UserDTO> maskedList = maskService.maskObject(userList);
```

处理规则：

1. 递归遍历 Bean、Map、Collection、Array。
2. 复用 Response 对象递归脱敏能力。
3. 按字段名、路径、注解和配置规则自动判断 type。
4. 默认不对所有字符串 value 做全局正则扫描。
5. 处理结果计入 `MANUAL` 场景统计。

边界：

1. 默认不扫描 remark、content 等普通字符串中的手机号或邮箱。
2. 默认行为应与 Response 自动脱敏结果保持一致。
3. 超过最大深度、最大集合数量时安全停止。

---

### 8.6 模式三：强扫描脱敏，类似 Log

适用场景：传入文本、备注、拼接字符串、非结构化内容，业务方希望尽量脱敏。

示例：

```java
String masked = maskService.maskStrong("客户张三，手机号13812345678，邮箱 a@b.com");
UserDTO masked = maskService.maskObjectStrong(user);
```

处理规则：

1. 对文本或对象中的字符串执行类似 Log 的处理。
2. 支持 key-value 轻量识别。
3. 支持 value 内轻量扫描。
4. 支持通用正则兜底。
5. 必须由业务方显式调用。
6. 允许一定误脱敏。

默认扫描类型：

1. `MOBILE`。
2. `EMAIL`。
3. `ID_CARD`。

默认不扫描：

1. `BANK_CARD`。
2. `CHINESE_NAME`。
3. `ADDRESS`。

除非用户显式配置开启。

### 8.7 推荐 API

```java
public interface SafeOutputMaskService {

    /**
     * 模式一：指定类型脱敏
     */
    String mask(String value, String type);

    String mask(String value, String type, MaskContext context);

    /**
     * 模式二：对象规则脱敏，类似 response
     */
    <T> T maskObject(T object);

    <T> T maskObject(T object, MaskContext context);

    /**
     * 模式三：强扫描脱敏，类似 log
     */
    String maskStrong(String text);

    String maskStrong(String text, MaskOptions options);

    <T> T maskObjectStrong(T object);

    <T> T maskObjectStrong(T object, MaskOptions options);
}
```

### 8.8 统计要求

1. 主动调用统一计入 `MANUAL` 场景基础统计。
2. 指定类型脱敏记录 type 维度统计。
3. 对象规则脱敏记录对象脱敏字段数。
4. 强扫描脱敏记录正则兜底、key-value 命中等统计。
5. 主动调用不默认计入 Response 接口风险统计。

### 8.9 不做范围

第二轮不做：

1. 数据库查询自动拦截。
2. MyBatis 插件。
3. 缓存框架自动拦截。
4. MQ 消息自动拦截。
5. 文件导出自动拦截。
6. 纯 static 工具类作为核心实现。

可提供工具类 Facade，但核心逻辑必须通过 Spring Bean 和统一服务实现。

### 8.10 验收标准

1. 宿主系统可注入 `SafeOutputMaskService`。
2. 调用 `mask("13812345678", "MOBILE")` 返回手机号脱敏结果。
3. 调用 `mask("13812345678", "mobileM")` 可命中自定义策略。
4. 调用 `maskObject(userDTO)` 时，可按 Response 规则自动脱敏。
5. 调用 `maskObject(map)` 时，可根据 key 自动选择策略。
6. 嵌套对象和集合中的字段可按规则自动脱敏。
7. `maskObject` 默认不做全量 value 正则扫描。
8. 调用 `maskStrong(text)` 时，可脱敏文本中的手机号、邮箱、身份证。
9. 主动调用产生的脱敏次数进入 `MANUAL` 场景统计。
10. 主动调用异常不影响宿主业务主流程。

---

## 9. R2-07 统计分析与治理建议增强

### 9.1 背景

MVP 版本统计报告主要记录运行指标，如脱敏次数、类型分布、耗时、失败次数、正则兜底命中次数和接口风险等级。

试点后发现，仅有运行统计对竞赛展示和老系统治理价值不足。第二轮需要将统计报告升级为：

> 基于运行摘要的脱敏治理辅助能力。

R2-07 包含两条线：

1. Response 风险画像分析。
2. Log 异步规则发现与配置建议。

共同边界：

1. 不保存敏感原文。
2. 不保存完整 response。
3. 不保存完整日志。
4. 不保存脱敏后完整值。
5. 不保存单次请求敏感字段明细。
6. Agent 只处理统计摘要。
7. Agent 不参与在线链路。
8. Agent 不自动修改运行配置。
9. 所有建议均需接入方人工确认。

---

## 9.2 能力一：Response 风险画像分析

### 9.2.1 定位

Response 侧不负责发现漏配置，而是负责：

1. 识别哪些接口敏感风险更高。
2. 解释为什么高风险。
3. 给出治理建议。
4. 展示脱敏性能是否合理。
5. 为竞赛展示提供接口风险画像。

Response 场景默认不做全量 value 正则扫描。

### 9.2.2 需求目标

基于 Response 脱敏摘要生成：

1. `riskScore`：风险分。
2. `riskLevel`：风险等级。
3. `riskReasons`：风险判断原因。
4. `governanceAdvice`：治理建议。
5. `topRiskApis`：高风险接口列表。
6. `responseRiskSummary`：总体风险摘要。
7. `performanceProfile`：接口脱敏性能画像。
8. `performanceWarnings`：性能警告。

### 9.2.3 Response 是否记录脱敏耗时

必须记录。

目的：

1. 判断组件在生产环境中的性能合理性。
2. 识别慢脱敏接口。
3. 识别大对象、批量返回、深层嵌套导致的性能问题。
4. 为接入方提供性能信心。

### 9.2.4 在线采集机制

每次 Response 脱敏后生成轻量事件：

```java
ResponseMaskEvent
```

事件只用于更新内存聚合，不落盘保存逐条明细。

建议字段：

1. `apiKey`。
2. HTTP method。
3. URI pattern。
4. Controller class。
5. Controller method。
6. scene = RESPONSE。
7. 脱敏耗时。
8. 脱敏字段数量。
9. 脱敏类型分布。
10. 规则命中次数。
11. 是否接口级 ignore。
12. ignore reason。
13. 是否成功。
14. errorType 可选。
15. timestamp bucket。

禁止包含：

1. 原始 response。
2. 脱敏后 response。
3. 敏感原文。
4. 单次请求敏感字段明细。

### 9.2.5 内存聚合机制

按接口维度聚合，聚合 key 建议为：

```text
HTTP_METHOD + URI_PATTERN + ControllerClass + ControllerMethod
```

不得直接使用原始 URI，以避免 `/user/1`、`/user/2` 被统计成多个接口。

聚合对象建议为：

```java
ResponseApiMetrics
```

维护字段：

1. `hitCount`。
2. `typeCounts`。
3. `totalMaskedFieldCount`。
4. `maxMaskedFieldsPerResponse`。
5. `avgMaskedFieldsPerResponse`。
6. `totalCostMillis`。
7. `avgCostMillis`。
8. `maxCostMillis`。
9. `slowCount`。
10. `ignoredCount`。
11. `failureCount`。
12. `lastSeenTime`。
13. `topHitKeys` 可选，仅记录 key/type/count 聚合，不记录值。

### 9.2.6 风险画像生成机制

在线链路只更新聚合指标，不生成复杂分析结论。

风险画像由定时报告任务异步生成：

```text
ResponseBodyAdvice
  -> 脱敏并计时
  -> 生成 ResponseMaskEvent
  -> 更新内存 ResponseApiMetrics
  -> 定时异步任务读取聚合快照
  -> RuleBasedResponseRiskAnalyzer 计算风险画像
  -> 生成 responseRiskSummary / topRiskApis
  -> 写入本地 JSON 报告
```

### 9.2.7 风险原因与治理建议

MVP 第二轮使用规则型分析器生成。

建议接口：

```java
public interface ResponseRiskAnalyzer {
    ResponseRiskAnalysis analyze(ResponseRiskContext context);
}
```

默认实现：

```text
RuleBasedResponseRiskAnalyzer
```

风险规则示例：

| 触发情况 | 风险原因 |
|---|---|
| 命中 ID_CARD / BANK_CARD | 接口返回身份或金融类高敏感数据 |
| 命中 PASSWORD / TOKEN / SECRET | 接口返回认证凭据类字段，需重点排查 |
| 单次脱敏字段数 >= 5 | 疑似批量敏感数据返回接口 |
| 高频访问 + 命中敏感字段 | 高频敏感数据接口，暴露面较大 |
| 接口级 ignore + 高敏感类型 | 接口已豁免脱敏但存在明文展示风险 |
| 脱敏耗时超过阈值 | 该接口脱敏耗时较高，建议关注返回对象大小和嵌套深度 |

### 9.2.8 性能画像

性能风险与敏感风险分开展示。

`riskScore` 主要代表敏感数据风险。  
`performanceWarnings` 用于说明脱敏性能风险，不直接混入敏感风险评分。

性能指标包括：

1. `avgCostMillis`。
2. `maxCostMillis`。
3. `slowCount`。
4. `slow-threshold-ms`。
5. cost bucket 可选。
6. `performanceLevel`。
7. `performanceWarnings`。

建议配置：

```yaml
safe-output:
  response:
    risk-analysis:
      enabled: true
      slow-threshold-ms: 20
      top-risk-api-limit: 20
```

### 9.2.9 Agent 分析摘要预留

后续版本可扩展：

```text
AgentResponseRiskAnalyzer
```

边界：

1. Agent 只接收 ResponseApiMetrics 统计摘要。
2. Agent 不接收敏感原文。
3. Agent 不接收完整 response。
4. Agent 不参与在线接口返回链路。
5. Agent 分析发生在异步报告生成阶段。
6. Agent 结论只作为治理建议。
7. Agent 不自动修改接口行为。

可提供简单 skill：

```text
response-risk-analysis-skill
```

Skill 输入为接口统计摘要，输出风险摘要、风险原因和治理建议。

### 9.2.10 Response 报告示例

```json
{
  "responseRiskSummary": {
    "totalApis": 32,
    "sensitiveApis": 18,
    "highRiskApis": 5,
    "ignoredRiskApis": 2,
    "topRiskTypes": ["ID_CARD", "MOBILE", "EMAIL"]
  },
  "topRiskApis": [
    {
      "api": "GET /api/customer/detail",
      "controller": "CustomerController",
      "method": "detail",
      "riskLevel": "HIGH",
      "riskScore": 82,
      "hitCount": 1200,
      "typeCounts": {
        "ID_CARD": 800,
        "MOBILE": 1200
      },
      "maxMaskedFieldsPerResponse": 8,
      "avgMaskedFieldsPerResponse": 3.2,
      "performanceProfile": {
        "avgCostMillis": 2.6,
        "maxCostMillis": 18,
        "slowCount": 3,
        "performanceLevel": "NORMAL"
      },
      "ignored": false,
      "riskReasons": [
        "命中 ID_CARD，接口返回身份类高敏感数据",
        "访问频次较高，敏感数据暴露面较大"
      ],
      "governanceAdvice": [
        "建议确认接口权限控制",
        "建议纳入敏感接口治理清单"
      ]
    }
  ]
}
```

### 9.2.11 验收标准

1. Response 脱敏后统计次数增加。
2. Response 脱敏耗时被记录。
3. 接口维度可聚合平均耗时、最大耗时、慢脱敏次数。
4. 定时报告可生成 responseRiskSummary。
5. 定时报告可生成 topRiskApis。
6. 高风险接口可展示 riskReasons。
7. 高风险接口可展示 governanceAdvice。
8. ignore 接口仍进入风险画像，并标记 ignored 和 ignoreReason。
9. 性能警告与敏感风险评分分开展示。
10. 报告不包含敏感原文、完整 response 或单次字段明细。

---

## 9.3 能力二：Log 异步规则发现与配置建议

### 9.3.1 定位

Log 侧负责：

1. 发现老系统真实字段命名。
2. 识别哪些 key 可能还没有进入 `rules.keys`。
3. 生成补充规则建议。
4. 输出 application.yml 配置片段。
5. 帮助 Response、Log、主动对象脱敏共同提升覆盖率。

Log 分析不是为了替代兜底脱敏，而是为了把兜底脱敏过程中发现的线索沉淀为配置建议。

### 9.3.2 核心价值

1. 从全局正则扫描逐步升级到 key-value 精准命中，提升性能。
2. 从格式识别升级到语义识别，提升准确性。
3. 反向发现老系统真实字段命名。
4. 降低兜底误伤和漏判。
5. 生成全局规则建议，补齐后可同时作用于 Response、Log、Manual Object。

### 9.3.3 数据来源

数据来源以 Log 场景为主，采集点位于 Log4j2 日志脱敏链路。

Response 场景不作为第二轮漏脱敏建议的数据来源。

### 9.3.4 在线与异步分层

采用：

```text
在线轻量采集 + 闲时异步生成建议
```

在线阶段只做：

1. 记录 regex fallback 命中的 type。
2. 提取 fallback 附近的 nearbyKey。
3. 判断 nearbyKey 是否已配置。
4. 累计 hitCount。
5. 记录 firstSeenTime / lastSeenTime。
6. 可选记录 loggerName。

在线阶段不做：

1. 不生成完整报告。
2. 不做复杂排序。
3. 不生成 YAML 片段。
4. 不保存完整日志。
5. 不保存敏感原文。
6. 不做重型上下文分析。

异步阶段由定时任务执行：

1. 读取内存聚合数据。
2. 合并 hitCount。
3. 判断是否超过 min-hit-count。
4. 计算 confidence。
5. 过滤已配置 key。
6. 排序 topN。
7. 生成建议列表。
8. 生成 application.yml 配置片段。
9. 写入本地 JSON 报告文件。

### 9.3.5 nearbyKey 提取规则

当正则兜底命中敏感值后，向前取有限长度上下文，尝试匹配最近 key。

示例：

```text
phoneNo=13812345678
certNum: 350102199001011234
mailAddr=test@example.com
```

提取：

```text
phoneNo -> MOBILE
certNum -> ID_CARD
mailAddr -> EMAIL
```

建议正则方向：

```text
([a-zA-Z0-9_.-]{2,50})\s*[:=]\s*$
```

仅保存 key、type、count、evidence，不保存 value。

### 9.3.6 建议生成规则

当以下条件成立时生成规则建议：

1. regex fallback 命中明确类型。
2. 命中附近存在 nearbyKey。
3. nearbyKey 尚未出现在 `rules.keys` 中。
4. 同一 nearbyKey 多次稳定命中同一敏感类型。
5. 达到最小命中次数阈值。

第二轮优先支持：

1. `MOBILE`。
2. `EMAIL`。
3. `ID_CARD`。

不默认生成自动配置建议：

1. `BANK_CARD`。
2. `CHINESE_NAME`。
3. `ADDRESS`。

上述类型可作为低置信待确认项或后续增强。

### 9.3.7 置信度规则

| 置信度 | 条件 |
|---|---|
| HIGH | 正则兜底命中明确类型，存在 nearbyKey，同一 key 多次命中同一 type |
| MEDIUM | 正则兜底命中明确类型，key 命中次数较少或稳定性一般 |
| LOW | 只有疑似敏感内容，没有明确 key，或只出现一次 |

配置片段默认只生成 HIGH / MEDIUM 建议，不生成 LOW 建议。

### 9.3.8 配置建议作用范围

Log 分析输出的是全局 `rules.keys` 建议。

接入方采纳后，可同时作用于：

1. Response 自动脱敏。
2. Log key-value 精准脱敏。
3. 主动对象规则脱敏。

建议报告中应标记：

```json
"effectScopes": ["RESPONSE", "LOG", "MANUAL_OBJECT"]
```

### 9.3.9 自动生效边界

第二轮只生成建议，不自动修改配置，不自动生效。

原因：

1. 可能存在误判。
2. 配置一旦加入 rules.keys，会影响 Response 和主动对象脱敏。
3. 需要接入方结合业务语义人工确认。

建议报告标记：

```json
"autoApply": false
```

### 9.3.10 配置示例

```yaml
safe-output:
  log:
    analysis:
      enabled: true
      sample-rate: 0.1
      max-suggestions: 200
      min-hit-count-for-suggestion: 3
      generate-config-snippet: true
      export-interval-seconds: 300
```

### 9.3.11 报告示例

```json
{
  "logRuleSuggestions": [
    {
      "key": "phoneNo",
      "suggestedType": "MOBILE",
      "hitCount": 128,
      "confidence": "HIGH",
      "evidence": "regex_fallback_near_key",
      "effectScopes": ["RESPONSE", "LOG", "MANUAL_OBJECT"],
      "autoApply": false
    },
    {
      "key": "certNum",
      "suggestedType": "ID_CARD",
      "hitCount": 36,
      "confidence": "HIGH",
      "evidence": "regex_fallback_near_key",
      "effectScopes": ["RESPONSE", "LOG", "MANUAL_OBJECT"],
      "autoApply": false
    }
  ],
  "configSnippet": "safe-output:\n  rules:\n    - name: suggested-phoneNo-mobile\n      keys:\n        - phoneNo\n      type: MOBILE\n      enabled: true\n"
}
```

### 9.3.12 Agent 写配置预留

后续版本可扩展：

```text
AgentLogConfigSuggestionAnalyzer
```

边界：

1. Agent 只基于摘要和建议生成配置片段。
2. Agent 不读取完整日志。
3. Agent 不读取敏感原文。
4. Agent 不自动写入应用配置。
5. 配置片段需要接入方人工确认后使用。

可提供简单 skill：

```text
log-rule-config-suggestion-skill
```

Skill 输入为当前规则摘要和建议列表，输出 YAML 配置片段。

### 9.3.13 验收标准

1. Log 正则兜底命中手机号后，可提取 nearbyKey。
2. nearbyKey 未配置时，可生成规则建议。
3. 同一 key 多次命中同一 type 后，confidence 提升。
4. 已配置 key 不重复生成建议。
5. 可生成 application.yml 配置片段。
6. 建议标记 effectScopes。
7. 建议默认不自动生效。
8. 报告不包含完整日志和敏感原文。
9. 异步分析异常不影响日志输出。
10. 配置建议可用于补齐 Response、Log、Manual Object 规则覆盖。

---

## 10. R2-08 Demo 可验证性增强

### 10.1 背景

MVP Demo 已支持固定接口的 Response 脱敏、Log 脱敏、ignore 和统计报告展示。

第二轮新增多个核心能力后，Demo 需要作为需求验证工具，确保新增能力可调用、可验证、可复现、可截图。

### 10.2 需求目标

第二轮 Demo 不重点追求前端美观，优先提供接口或轻量页面，覆盖第二轮新增能力验证。

关键词：

```text
可调用
可验证
可复现
可截图
```

### 10.3 需要覆盖的验证点

1. 指定 type 主动脱敏验证。
2. 对象规则主动脱敏验证。
3. 强扫描主动脱敏验证。
4. 幂等性验证。
5. Response 风险画像报告查看。
6. Response 性能画像查看。
7. Log 规则发现建议查看。
8. application.yml 配置片段生成查看。
9. MANUAL 场景统计验证。
10. 统计报告不保存敏感原文验证。

### 10.4 主动脱敏验证接口

#### 10.4.1 指定类型脱敏

```http
POST /demo/mask/by-type
```

请求：

```json
{
  "value": "13812345678",
  "type": "MOBILE"
}
```

响应：

```json
{
  "original": "13812345678",
  "masked": "138****5678",
  "secondMasked": "138****5678",
  "idempotent": true,
  "type": "MOBILE"
}
```

#### 10.4.2 对象规则脱敏

```http
POST /demo/mask/object
```

请求：

```json
{
  "realName": "张三",
  "mobile": "13812345678",
  "name": "商品A"
}
```

响应：

```json
{
  "masked": {
    "realName": "张*",
    "mobile": "138****5678",
    "name": "商品A"
  },
  "secondMasked": {
    "realName": "张*",
    "mobile": "138****5678",
    "name": "商品A"
  },
  "idempotent": true
}
```

#### 10.4.3 强扫描脱敏

```http
POST /demo/mask/strong
```

请求：

```json
{
  "text": "客户手机号 13812345678，邮箱 zhangsan@example.com"
}
```

响应：

```json
{
  "masked": "客户手机号 138****5678，邮箱 zha****@example.com",
  "secondMasked": "客户手机号 138****5678，邮箱 zha****@example.com",
  "idempotent": true
}
```

### 10.5 幂等性要求

定义：

> 同一脱敏策略对已经脱敏过的值再次处理时，不应继续破坏可读性，不应把结果越脱越短、越脱越乱。

内置核心策略应尽量幂等：

1. `MOBILE`。
2. `ID_CARD`。
3. `EMAIL`。
4. `BANK_CARD`。
5. `CHINESE_NAME`。
6. `PASSWORD`。
7. `DEFAULT`。

自定义策略不强制幂等，但应在文档中给出建议。

### 10.6 报告查看接口

#### 10.6.1 Response 风险画像

```http
GET /demo/report/response-risk
```

返回：

1. responseRiskSummary。
2. topRiskApis。
3. riskReasons。
4. governanceAdvice。
5. performanceProfile。
6. ignoredRiskApis。

#### 10.6.2 Log 规则建议

```http
GET /demo/report/log-suggestions
```

返回：

1. logRuleSuggestions。
2. suggestedType。
3. hitCount。
4. confidence。
5. evidence。
6. effectScopes。
7. configSnippet。

### 10.7 Demo 数据准备

建议准备：

1. `CustomerDTO`：手机号、身份证、邮箱、姓名。
2. `OrderDTO`：订单号、收货人、收货地址。
3. `ProductDTO`：`productName`，验证 `name` 不误脱敏。
4. `IgnoreDemoDTO`：验证字段级 ignore。
5. `LogSuggestionDemo`：打印 `phoneNo`、`certNum`、`mailAddr` 等未配置 key。
6. 自定义 `mobileM` 策略示例。

### 10.8 验收标准

1. 可通过接口验证指定 type 脱敏。
2. 可通过接口验证对象规则脱敏。
3. 可通过接口验证强扫描脱敏。
4. 可展示第一次脱敏和第二次脱敏结果。
5. 可展示 `idempotent=true/false`。
6. 可查看 Response 风险画像报告。
7. 可查看 Response 性能画像报告。
8. 可查看 Log 规则建议报告。
9. 可查看配置片段生成结果。
10. 主动调用进入 `MANUAL` 场景统计。
11. Demo 不将敏感原文写入统计报告或本地报告文件。

---

## 11. R3-01 Demo 竞赛展示看板

### 11.1 定位

第三轮需求，不纳入第二轮必须交付范围。

第三轮 Demo 的目标是：

> 作为竞赛展示产品，强调前端设计、统计图表、风险画像、规则建议、配置片段与 AI Skill 预留展示。

### 11.2 页面规划

建议建设轻量前端看板，包含 5 个页面。

#### 页面一：首页总览 Dashboard

展示：

1. 已脱敏总次数。
2. Response 脱敏次数。
3. Log 脱敏次数。
4. Manual 脱敏次数。
5. 高风险接口数。
6. 已发现配置建议数。
7. 平均脱敏耗时。
8. 最近报告生成时间。

图表：

1. 脱敏类型分布饼图。
2. Response / Log / Manual 场景趋势图。
3. 高风险接口 Top 5。
4. 性能耗时分布柱状图。

#### 页面二：Response 风险画像页

展示：

1. 接口风险排行榜。
2. riskScore。
3. riskLevel。
4. riskReasons。
5. governanceAdvice。
6. 命中类型分布。
7. 平均耗时 / 最大耗时 / slowCount。
8. ignored 接口标记。

图表：

1. 接口风险 Top N。
2. 风险等级分布。
3. 敏感类型堆叠柱状图。
4. 接口脱敏耗时排名。

#### 页面三：Log 规则发现页

展示：

1. 未配置 key 建议列表。
2. suggestedType。
3. hitCount。
4. confidence。
5. evidence。
6. effectScopes。
7. 配置片段预览。

操作：

1. 一键复制 YAML 配置片段。
2. 查看采纳后影响范围。
3. 标记已采纳 / 忽略，前端状态即可。

#### 页面四：主动脱敏实验室

提供三个 Tab：

1. 指定 type 脱敏。
2. 对象规则脱敏。
3. 强扫描脱敏。

展示：

1. 输入。
2. 第一次脱敏结果。
3. 第二次脱敏结果。
4. 幂等性判断。
5. 命中的策略 type。
6. 耗时。

#### 页面五：接入与配置说明页

展示：

1. Maven 依赖。
2. application.yml 示例。
3. log4j2.xml 示例。
4. 自定义 MaskStrategy 示例。
5. `@Desensitize` 示例。
6. ignore 配置示例。
7. Agent Skill 预留说明。

### 11.3 第三轮图表重点

1. 脱敏总次数。
2. Response / Log / Manual 场景分布。
3. 脱敏类型分布图。
4. 高风险接口排行榜。
5. 接口风险原因 riskReasons。
6. 治理建议 governanceAdvice。
7. Response 脱敏性能画像。
8. 慢脱敏接口排行。
9. Log 规则补充建议。
10. suggestedType / hitCount / confidence / evidence。
11. 配置片段 YAML 预览与复制。
12. 主动脱敏三种模式。
13. 第一次脱敏 / 第二次脱敏 / 幂等性判断。
14. Maven 依赖、application.yml、log4j2.xml、自定义策略、ignore 示例。
15. Agent Skill 预留展示区。

### 11.4 第三轮边界

1. 第三轮可引入前端页面和图表组件。
2. 不要求构建完整治理平台。
3. 不要求接入真实数据库。
4. 不自动修改宿主系统配置。
5. Agent 输出只作为展示和建议，不参与在线脱敏链路。

---

## 12. 配置项补充建议

第二轮建议在原配置基础上补充以下配置项。

### 12.1 策略扩展配置

```yaml
safe-output:
  strategy:
    unknown-type-policy: DEFAULT
```

说明：该配置项是后续扩展建议，当前代码尚未暴露运行时开关；当前固定行为为 `warn + DEFAULT fallback`。

### 12.2 身份证配置

```yaml
safe-output:
  id-card:
    checksum-check-enabled: true
    region-check-enabled: false
```

### 12.3 日志 key-value 匹配配置

```yaml
safe-output:
  log:
    key-value-rule-enabled: true
    max-message-length: 4096
    max-value-length: 256
    max-rule-key-count: 100
```

### 12.4 日志规则发现配置

```yaml
safe-output:
  log:
    analysis:
      enabled: true
      sample-rate: 0.1
      max-suggestions: 200
      min-hit-count-for-suggestion: 3
      generate-config-snippet: true
      export-interval-seconds: 300
```

### 12.5 Response 风险画像配置

```yaml
safe-output:
  response:
    risk-analysis:
      enabled: true
      slow-threshold-ms: 20
      top-risk-api-limit: 20
```

### 12.6 主动脱敏配置

```yaml
safe-output:
  manual:
    enabled: true
    strong-scan:
      enabled: true
      default-types:
        - MOBILE
        - EMAIL
        - ID_CARD
      max-depth: 5
      max-collection-size: 200
      max-string-length: 4096
```

---

## 13. 报告安全边界

第二轮所有统计分析报告必须遵守以下边界。

禁止保存：

1. 脱敏前原始值。
2. 脱敏后完整值。
3. 完整 response。
4. 完整日志内容。
5. 单次请求的敏感字段明细。
6. 脱敏前后完整对比。
7. 可反推出敏感原文的大段上下文。

允许保存：

1. type 统计。
2. key 统计。
3. path 聚合统计，可选。
4. 接口维度聚合指标。
5. 耗时指标。
6. 风险等级。
7. 风险原因。
8. 治理建议。
9. unknown type 名称。
10. nearbyKey。
11. suggestedType。
12. hitCount。
13. confidence。
14. evidence。
15. 配置片段。

---

## 14. 第二轮成功标准

第二轮完成后，组件应满足：

1. 姓名策略适配少数民族姓名、英文名、中英混合姓名。
2. 身份证策略按上下文采用性能优先的折中识别方式。
3. 配置中的自定义 type 不再导致启动失败。
4. 注解、策略注册、统计报告可贯穿自定义 String type。
5. 日志脱敏可复用 `rules.keys` 做 key-value 精准匹配。
6. 主动脱敏服务可用于业务代码主动调用。
7. 主动脱敏服务支持指定 type、对象规则、强扫描三种模式。
8. Response 统计可生成风险画像、风险原因、治理建议和性能画像。
9. Log 分析可异步生成规则补充建议和 YAML 配置片段。
10. Demo 可验证第二轮新增能力。
11. 所有报告不保存敏感原文、完整 response 或完整日志。
12. 第二轮能力可为第三轮竞赛展示看板提供数据基础。

---

## 15. 第二轮不做范围

第二轮不做：

1. 完整治理平台。
2. 数据库落库统计。
3. Redis / 分布式统计。
4. Prometheus / Actuator 指标上报。
5. 配置中心热更新。
6. 自动修改宿主系统配置。
7. Agent 在线参与脱敏链路。
8. Agent 自动写入配置。
9. MyBatis 自动脱敏插件。
10. 缓存框架自动拦截。
11. MQ 消息自动拦截。
12. 文件导出自动脱敏。
13. Logback 实现。
14. 完整前端竞赛看板。
15. 复杂 JSON Parser 日志解析。
16. 自然语言敏感信息识别。
17. Response 全量 value 正则扫描。
18. 每次脱敏明细落库。

---

## 16. 后续衔接建议

第二轮完成后，建议进入以下工作：

1. 基于本 PRD 更新概要设计说明书。
2. 基于 R2 需求拆分 Issue。
3. 更新 Cursor / Codex AI Coding 规则。
4. 针对 R2-07 编写两个简单 Skill：
   - `response-risk-analysis-skill`
   - `log-rule-config-suggestion-skill`
5. 完成第二轮 Demo 验证接口。
6. 进入第三轮 Demo 竞赛展示看板设计。

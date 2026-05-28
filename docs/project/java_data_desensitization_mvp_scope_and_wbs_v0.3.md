# 通用 Java 数据脱敏组件 MVP 需求范围说明书与 WBS 工作分解清单 v0.4

## 1. 文档说明

### 1.1 文档目的

本文档用于明确“通用 Java 数据脱敏组件”MVP 版本的需求范围、功能边界、验收标准和工作分解清单。

本文档有两个核心用途：

1. 作为 MVP 版本的需求范围说明书，明确第一版具体做什么、不做什么、如何验收。
2. 作为后续分阶段实施的任务分解依据，将项目拆解为边界清晰、可验收的工作包。

本文档聚焦 MVP 需求范围、阶段目标、里程碑、交付物和验收口径。WBS 仅用于定义可执行工作包边界，不展开过程沟通、任务提示词或工具规则细节。

### 1.2 版本说明

本文档版本为 v0.4。

相较 v0.3，v0.4 重点调整以下内容：

1. 删减 WBS 中的过程性沟通内容，不再在每个工作包中展开工具规则生成说明。
2. 将 WBS 从“工具执行规划”收敛为“需求范围和可交付工作包”。
3. 新增阶段性目标和里程碑，明确 MVP 从基础骨架到演示验收的推进节奏。
4. 新增整体需求澄清计划，按推荐结论一次性确认关键需求边界。

v0.4 继续继承 v0.3 已明确的以下边界：

1. 日志脱敏 MVP 优先支持 Log4j2 2.x，不再以 Logback 作为第一优先级。
2. 日志模块设计为可扩展日志适配层，后续预留兼容 Logback 的扩展点。
3. 统计报告不再只做内存聚合，需要增加定时写入本地 JSON 文件机制，避免应用重启后统计完全丢失。
4. 明确定时文件写入只输出聚合指标快照，不保存敏感原文、不保存完整 response、不保存完整日志。
5. 明确统计文件写入采用定时任务方式，不在请求链路同步刷盘。

v0.4 继续继承 v0.2 已明确的以下边界：

1. 模糊字段处理策略，例如 `name`、`id`、`code`、`no` 不进入默认强脱敏规则。
2. 字段级 ignore、接口级 ignore、包级 ignore 的脱敏豁免机制。
3. 规则优先级：接口级 ignore > 字段级 ignore > 注解规则 > path rule > key rule > default rule > regex fallback。
4. 日志场景不引入 fastjson，采用轻量 JSON-like key-value 识别 + 正则兜底。
5. 全局正则兜底默认支持手机号、邮箱、严格大陆身份证。
6. 银行卡不做无上下文全局正则兜底。
7. Spring Boot 2.x 自动装配使用 `spring.factories`，不得只适配 Spring Boot 3.x 自动装配机制。
8. 接口风险统计 MVP 只做 response 场景。
9. 统计报告不保存敏感原文、不保存完整 response、不保存完整日志。
10. 接口级 ignore 的接口不执行 response 脱敏，但仍进入接口风险统计。

### 1.3 项目定位

项目定位为：

> 面向传统 Java 服务的低侵入式输出侧敏感数据脱敏组件。

MVP 版本重点强调：

> 基于 JDK8 + Spring Boot 2.x，支持传统老项目通过 starter 和配置文件实现即插即用的数据脱敏能力，优先覆盖接口 response 和 Log4j2 2.x 日志打印两个核心输出场景，并在日志适配层预留后续兼容 Logback 的扩展空间。

### 1.4 MVP 核心竞争力

MVP 版本的核心竞争力不是单纯“可以脱敏”，而是：

1. 老项目即插即用。
2. 配置优先，注解可选。
3. 不要求大规模修改业务代码。
4. 同时覆盖接口显示和日志打印两个高风险场景。
5. 支持脱敏豁免机制，避免粗暴全局脱敏影响业务正确性。
6. 支持接口风险统计，便于识别敏感数据暴露接口。
7. 有基础统计报告，可支撑竞赛汇报和试点验证。
8. 架构边界清晰，便于后续分模块实现和持续迭代。

---

## 2. MVP 总体目标

### 2.1 业务目标

MVP 版本需要满足以下业务目标：

1. 支持试点 Spring Boot 老项目快速接入。
2. 降低接口 response 和日志打印中的敏感数据泄露风险。
3. 减少业务代码中手工脱敏改造工作量。
4. 统一常见敏感数据的脱敏规则。
5. 支持必要的脱敏豁免，避免影响必须展示敏感明文的业务接口。
6. 支持对触发脱敏的接口进行统计，为接口安全风险分级提供基础数据。
7. 为竞赛汇报提供可演示、可说明、可量化的成果。
8. 为后续版本扩展权限动态脱敏、MyBatis 脱敏、配置中心热更新、治理平台等能力预留架构空间。

### 2.2 技术目标

MVP 版本需要满足以下技术目标：

1. 兼容 JDK8。
2. 兼容 Spring Boot 2.x。
3. 提供 Spring Boot Starter 自动装配。
4. Spring Boot 2.x 自动装配使用 `META-INF/spring.factories`。
5. 不使用 Spring Boot 3.x 专属自动装配机制作为唯一入口。
6. 不强制覆盖宿主系统 Spring Boot 版本。
7. 支持 ResponseBodyAdvice 方式进行接口 response 脱敏。
8. MVP 优先支持 Log4j2 2.x 日志输出脱敏。
9. 日志脱敏模块需要预留后续兼容 Logback 的适配扩展点。
10. 支持 YAML 配置化规则。
11. 支持注解精确脱敏。
12. 支持字段级和接口级脱敏豁免。
13. 支持常见敏感数据类型的内置脱敏策略。
14. 支持对象、Map、Collection、Array 等常见返回结构。
15. 支持基础统计指标采集和报告输出。
16. 支持 response 场景接口风险统计。

### 2.3 即插即用目标

MVP 版本必须支持老项目即插即用，具体要求如下：

1. 试点项目引入 starter 依赖后，核心 Bean 自动注册。
2. 不强制 Controller 增加额外注解。
3. 不强制 DTO/VO 增加字段注解。
4. 可通过 application.yml 配置字段名规则完成脱敏。
5. 提供默认字段规则库，降低初始配置成本。
6. Response 脱敏默认可通过配置开关开启。
7. 日志脱敏通过 Log4j2 2.x 配置接入，不要求修改所有日志打印代码。
8. Logback 不进入 MVP 第一优先级，但接口和核心日志脱敏能力需保持日志框架适配层边界。
9. 脱敏异常不应中断主业务流程。
10. 统计异常不应影响脱敏和业务主流程。
11. 对不支持处理的对象自动跳过。
12. 支持一键关闭整体脱敏能力，便于灰度和回退。
13. 支持接口级 ignore，避免影响必须返回敏感明文的业务接口。

---

## 3. MVP 用户与使用场景

### 3.1 目标用户

MVP 面向以下用户：

1. 传统 Java 服务端项目开发人员。
2. Spring Boot 项目维护人员。
3. 系统安全治理人员。
4. 架构组或公共组件维护人员。
5. 竞赛评审或试点项目验收人员。

### 3.2 典型使用场景一：老项目快速接入

老项目维护人员希望在尽量不修改业务代码的情况下，对接口 response 中的手机号、身份证、银行卡等字段进行脱敏。

期望接入方式：

1. 引入 starter。
2. 增加少量 YAML 配置。
3. 启动应用。
4. 原接口返回自动脱敏。

### 3.3 典型使用场景二：接口显示脱敏

接口返回对象中包含敏感字段：

```json
{
  "name": "张三",
  "mobile": "13812345678",
  "idCard": "350102199001011234",
  "email": "zhangsan@example.com"
}
```

开启组件后返回：

```json
{
  "name": "张*",
  "mobile": "138****5678",
  "idCard": "350102********1234",
  "email": "zha****@example.com"
}
```

### 3.4 典型使用场景三：日志打印脱敏

业务代码中已有日志：

```java
log.info("用户手机号：{}，身份证：{}", mobile, idCard);
log.info("请求参数：{}", requestJson);
```

通过 Log4j2 2.x 脱敏配置后，日志输出为：

```text
用户手机号：138****5678，身份证：350102********1234
请求参数：{"mobile":"138****5678","idCard":"350102********1234"}
```

### 3.5 典型使用场景四：注解精确脱敏

对于新开发的 DTO 或重点对象，开发人员可以使用注解明确脱敏类型：

```java
public class UserDTO {

    @Desensitize(type = DesensitizeType.MOBILE)
    private String mobile;

    @Desensitize(type = DesensitizeType.ID_CARD)
    private String idCard;
}
```

### 3.6 典型使用场景五：字段级豁免

某些字段名虽然包含 `name`，但并不是自然人姓名，例如商品名、角色名、机构名，不应被脱敏。

可以配置字段级 ignore：

```yaml
safe-output:
  ignore:
    keys:
      - productName
      - roleName
      - orgName
    paths:
      - product.name
      - role.name
```

### 3.7 典型使用场景六：接口级豁免

部分接口的业务目的就是展示敏感信息，例如查询完整手机号、实名信息确认、内部客服查询等。

可以配置接口级 ignore：

```yaml
safe-output:
  ignore:
    apis:
      - method: GET
        path: /api/user/mobile/query
        scenes: [response]
        reason: 查询手机号接口，业务要求返回明文
```

命中接口级 ignore 后，该接口 response 不执行脱敏，但仍进入接口风险统计。

### 3.8 典型使用场景七：竞赛演示

竞赛演示中可以展示：

1. 未接入组件前，接口和日志中存在明文敏感数据。
2. 引入 starter 和配置后，接口 response 自动脱敏。
3. 增加 Log4j2 2.x 配置后，日志输出自动脱敏。
4. 展示注解与配置双模式。
5. 展示字段级 ignore 解决误脱敏问题。
6. 展示接口级 ignore 解决业务明文展示问题。
7. 展示接口风险统计和脱敏统计报告。
8. 展示后续版本规划。

---

## 4. MVP 功能范围

### 4.1 功能范围总览

| 编号 | 功能模块 | 功能名称 | 优先级 | 是否进入 MVP |
|---|---|---|---|---|
| M01 | 核心能力 | 脱敏类型与策略模型 | P0 | 是 |
| M02 | 核心能力 | 内置常见脱敏策略 | P0 | 是 |
| M03 | 核心能力 | 策略注册与查找 | P0 | 是 |
| M04 | 配置能力 | YAML 配置绑定 | P0 | 是 |
| M05 | 配置能力 | 默认规则库 | P0 | 是 |
| M06 | 配置能力 | ignore 配置 | P0 | 是 |
| M07 | 规则能力 | 字段名 key 匹配 | P0 | 是 |
| M08 | 规则能力 | 字段路径 path 匹配 | P0 | 是 |
| M09 | 规则能力 | 模糊字段处理策略 | P0 | 是 |
| M10 | 规则能力 | 注解识别 | P0 | 是 |
| M11 | 豁免能力 | 字段级 ignore | P0 | 是 |
| M12 | 豁免能力 | 接口级 ignore | P0 | 是 |
| M13 | Response 场景 | ResponseBodyAdvice 拦截 | P0 | 是 |
| M14 | Response 场景 | Bean 脱敏 | P0 | 是 |
| M15 | Response 场景 | Map 脱敏 | P0 | 是 |
| M16 | Response 场景 | Collection/Array 脱敏 | P0 | 是 |
| M17 | Log 场景 | Log4j2 PatternConverter | P0 | 是 |
| M18 | Log 场景 | JSON-like key-value 日志脱敏 | P0 | 是 |
| M19 | Log 场景 | 正则兜底日志脱敏 | P1 | 是 |
| M20 | 正则能力 | 严格大陆身份证校验 | P0 | 是 |
| M21 | 统计报告 | 内存聚合指标采集 | P1 | 是 |
| M22 | 统计报告 | 基础报告输出 | P1 | 是 |
| M23 | 统计报告 | Response 接口风险统计 | P1 | 是 |
| M24 | Demo | 示例项目 | P0 | 是 |
| M25 | 测试 | 单元测试和集成测试 | P0 | 是 |
| M26 | 文档 | 接入说明和演示说明 | P1 | 是 |

---

## 5. 详细功能需求

## 5.1 核心脱敏模型

### 5.1.1 功能说明

提供脱敏组件的基础领域模型，包括脱敏类型、脱敏场景、脱敏上下文、脱敏结果和脱敏策略接口。

### 5.1.2 需求内容

需要定义以下核心概念：

1. `MaskType`：脱敏类型。
2. `MaskScene`：脱敏场景，例如 response、log。
3. `MaskContext`：脱敏上下文。
4. `MaskResult`：脱敏结果。
5. `MaskStrategy`：脱敏策略接口。
6. `MaskStrategyRegistry`：策略注册与查找器。

### 5.1.3 验收标准

1. 支持通过 `MaskType` 查找对应策略。
2. 支持调用统一策略接口完成脱敏。
3. 支持空值、空字符串、短字符串安全处理。
4. 支持未知类型安全跳过或返回原值。
5. 单元测试覆盖核心策略调用链。

---

## 5.2 内置脱敏策略

### 5.2.1 功能说明

MVP 内置常见敏感数据脱敏策略，满足试点项目和竞赛演示需要。

### 5.2.2 支持类型

| 类型 | 示例原文 | 示例脱敏结果 | 优先级 |
|---|---|---|---|
| MOBILE | 13812345678 | 138****5678 | P0 |
| ID_CARD | 350102199001011234 | 350102********1234 | P0 |
| BANK_CARD | 6222021234567890123 | 622202*********0123 | P0 |
| EMAIL | zhangsan@example.com | zha****@example.com | P1 |
| CHINESE_NAME | 张三 / 王小明 | 张* / 王** | P1 |
| ADDRESS | 福建省福州市鼓楼区xxx | 福建省福州市**** | P1 |
| PASSWORD | abc123456 | ******** | P1 |
| DEFAULT | 任意字符串 | 按默认规则处理 | P1 |

### 5.2.3 验收标准

1. 每种内置策略均有单元测试。
2. 对空值和异常格式不抛出业务异常。
3. 脱敏结果符合默认规则预期。
4. 支持后续新增策略而不修改主流程。

---

## 5.3 Spring Boot 自动装配

### 5.3.1 功能说明

提供 Spring Boot Starter，使业务系统引入依赖后自动注册核心脱敏能力。

### 5.3.2 需求内容

1. 提供 `safe-output-spring-boot-starter` 模块。
2. 支持 Spring Boot 2.x 自动装配机制。
3. 使用 `META-INF/spring.factories` 注册自动装配类。
4. 不得仅使用 Spring Boot 3.x 的 `AutoConfiguration.imports` 作为唯一自动装配入口。
5. 自动注册配置属性类。
6. 自动注册脱敏服务。
7. 自动注册 response 脱敏 Advice。
8. 自动注册规则匹配器。
9. 自动注册默认策略。
10. 支持用户自定义策略 Bean 注入。
11. 支持 `safe-output.enabled=false` 关闭能力。
12. 不强制覆盖宿主系统 Spring Boot 版本。

### 5.3.3 依赖约束

1. 必须兼容 JDK8。
2. 必须兼容 Spring Boot 2.x。
3. 不使用 Spring Boot 3.x 专属 API。
4. 不使用 Java 9+ API。
5. starter 不应强制引入与宿主系统冲突的重依赖。

### 5.3.4 验收标准

1. Demo 项目引入 starter 后可以自动装配。
2. 不需要手动声明核心 Bean。
3. 设置 `safe-output.enabled=false` 后 response 脱敏不生效。
4. 启动过程无额外强制依赖异常。
5. 兼容 JDK8 和 Spring Boot 2.x。
6. 自动装配文件使用 `spring.factories` 生效。

---

## 5.4 配置文件规则

### 5.4.1 功能说明

通过 application.yml 配置敏感字段规则，支持老项目低侵入接入。

### 5.4.2 配置示例

```yaml
safe-output:
  enabled: true
  mask-char: "*"
  max-depth: 8
  max-collection-size: 1000
  scenes:
    response:
      enabled: true
    log:
      enabled: true
  rules:
    - name: mobile
      keys: [mobile, phone, tel, telephone, userMobile]
      type: MOBILE
    - name: idCard
      keys: [idCard, certNo, identityNo, certificateNo]
      type: ID_CARD
    - name: bankCard
      keys: [bankCard, cardNo, bankNo]
      type: BANK_CARD
    - name: email
      keys: [email, mail]
      type: EMAIL
```

### 5.4.3 需求内容

1. 支持全局开关。
2. 支持 response 场景开关。
3. 支持 log 场景开关。
4. 支持 maskChar 配置。
5. 支持 maxDepth 配置。
6. 支持 maxCollectionSize 配置。
7. 支持 rules 配置。
8. rules 支持 name、keys、paths、type、enabled。
9. 支持默认规则库。
10. 用户配置规则可以覆盖或补充默认规则。
11. 支持 ignore 配置。
12. 支持 report 配置。

### 5.4.4 验收标准

1. application.yml 配置可以正确绑定。
2. 配置字段名命中后能正确脱敏。
3. 未配置字段但命中默认规则时能正确脱敏。
4. 关闭某条规则后不再生效。
5. 关闭某个场景后该场景不再脱敏。
6. ignore 配置命中后优先豁免。

---

## 5.5 模糊字段处理策略

### 5.5.1 功能说明

全局规则匹配中存在语义模糊字段，例如 `name`、`id`、`code`、`no` 等。这些字段可能是敏感信息，也可能是业务字段、商品字段、角色字段、机构字段或流水号字段。

MVP 需要明确模糊字段不进行激进默认脱敏，避免老项目即插即用后产生大量误脱敏。

### 5.5.2 典型模糊字段

以下字段默认不进入强脱敏规则：

1. `name`
2. `id`
3. `code`
4. `no`
5. `number`
6. `title`
7. `account`
8. `userName`

其中 `userName` 可能是登录名，不一定是自然人姓名，不应默认按中文姓名脱敏。

### 5.5.3 默认强规则字段

MVP 默认强规则优先覆盖语义明确字段，例如：

1. `mobile`
2. `phone`
3. `telephone`
4. `idCard`
5. `certNo`
6. `identityNo`
7. `bankCard`
8. `cardNo`
9. `email`
10. `password`
11. `secret`
12. `token`

### 5.5.4 模糊字段处理方式

对于模糊字段，支持以下方式精确启用脱敏：

1. 通过注解指定。
2. 通过配置 keys 指定更明确字段名，例如 `realName`、`customerName`、`receiverName`。
3. 通过配置 paths 指定字段路径，例如 `user.name`、`customer.name`。
4. 通过 ignore 显式排除误脱敏字段。

### 5.5.5 验收标准

1. 默认规则中不因字段名为 `name` 就直接脱敏。
2. 配置 `realName` 后可以按姓名脱敏。
3. 配置 `user.name` path 后可以按姓名脱敏。
4. 配置 `product.name` ignore 后不会脱敏。
5. 单元测试覆盖模糊字段和明确字段差异。

---

## 5.6 脱敏豁免机制

### 5.6.1 功能说明

MVP 需要支持脱敏豁免机制，用于解决以下问题：

1. 局部字段误脱敏。
2. 某些接口业务上必须返回敏感明文。
3. 某些包或类型不应进入递归扫描。
4. 即插即用后的业务兼容性控制。

### 5.6.2 ignore 配置示例

```yaml
safe-output:
  ignore:
    keys:
      - productName
      - roleName
      - orderNo

    paths:
      - product.name
      - role.name
      - order.serialNo

    packages:
      - com.xxx.product

    apis:
      - method: GET
        path: /api/user/mobile/query
        scenes: [response]
        reason: 查询手机号接口，业务要求返回明文

      - method: POST
        path: /api/customer/real-info/detail
        scenes: [response]
        reason: 内部实名信息查询接口
```

### 5.6.3 字段级 ignore

字段级 ignore 用于解决局部字段误脱敏。

MVP 支持：

1. `ignore.keys`
2. `ignore.paths`

P1 可支持：

1. `ignore.packages`
2. `ignore.classes`

### 5.6.4 接口级 ignore

接口级 ignore 用于解决整个接口不需要 response 脱敏的场景。

典型场景包括：

1. 查询完整手机号。
2. 查询实名信息。
3. 内部客服查看联系方式。
4. 内部风控核验明文信息。

MVP 要求：

1. 支持 `method + path` 配置。
2. 支持 Ant 风格 path pattern，例如 `/api/user/*/mobile`、`/api/customer/**/raw`。
3. 支持 scenes 配置。
4. 接口级 ignore 默认只作用于 response 场景。
5. response ignore 不自动放开 log 脱敏。
6. 命中接口级 ignore 后，本次 response 不执行脱敏。
7. 命中接口级 ignore 的接口仍进入接口风险统计。
8. 报告中应标记 `ignored=true` 和 `ignoreReason`。

### 5.6.5 注解豁免

MVP 可选支持接口级注解豁免。

示例：

```java
@IgnoreDesensitize(scene = MaskScene.RESPONSE, reason = "业务要求展示完整手机号")
@GetMapping("/api/user/mobile/query")
public Result<String> queryMobile() {
    return Result.success("13812345678");
}
```

字段级注解豁免示例：

```java
@IgnoreDesensitize
private String productName;
```

注解豁免可作为 P1，但配置型接口 ignore 为 P0。

### 5.6.6 规则优先级

MVP 规则优先级明确为：

```text
接口级 ignore
>
字段级 ignore
>
注解规则
>
path rule
>
key rule
>
default rule
>
regex fallback
```

### 5.6.7 验收标准

1. 命中 `ignore.keys` 的字段不脱敏。
2. 命中 `ignore.paths` 的字段不脱敏。
3. 命中 `ignore.apis` 的接口 response 不脱敏。
4. 命中接口级 ignore 后，日志脱敏不受影响。
5. 命中接口级 ignore 的接口仍进入统计报告。
6. 报告中可以看到 ignored 接口的 hitCount、reason 和 riskLevel。

---

## 5.7 注解脱敏

### 5.7.1 功能说明

支持通过注解进行字段级精确脱敏，适用于新代码或重点对象。

### 5.7.2 注解示例

```java
public class UserDTO {

    @Desensitize(type = DesensitizeType.MOBILE)
    private String mobile;

    @Desensitize(type = DesensitizeType.ID_CARD)
    private String idCard;
}
```

### 5.7.3 需求内容

1. 提供 `@Desensitize` 注解。
2. 注解支持指定 type。
3. 注解支持指定 scene，MVP 可选。
4. 注解优先级高于配置字段名匹配。
5. 支持字段注解缓存。

### 5.7.4 验收标准

1. 字段添加注解后，即使字段名不在配置规则中，也能按注解类型脱敏。
2. 注解和配置同时存在时，优先使用注解。
3. 无注解字段可继续通过配置规则匹配。
4. 单元测试覆盖注解识别逻辑。

---

## 5.8 Response 接口返回脱敏

### 5.8.1 功能说明

通过 Spring MVC `ResponseBodyAdvice` 在接口响应写出前进行统一脱敏。

### 5.8.2 支持对象范围

MVP 需要支持：

1. Java Bean。
2. Map。
3. Collection。
4. List。
5. Array。
6. 嵌套对象。
7. 统一响应结构。
8. ResponseEntity body。

MVP 可选支持：

1. JSON String 尝试轻量处理。
2. Page-like 分页对象。

### 5.8.3 跳过对象范围

以下类型默认跳过：

1. 文件流。
2. 图片。
3. 二进制响应。
4. Spring 内部对象。
5. Servlet 原生对象。
6. 基础类型和日期类型。
7. 配置中指定忽略的包或类型。

### 5.8.4 需求内容

1. 自动注册 ResponseBodyAdvice。
2. 判断全局开关。
3. 判断 response 场景是否启用。
4. 在 response 脱敏前判断接口级 ignore。
5. 命中接口级 ignore 时跳过 response 脱敏。
6. 对返回对象执行递归脱敏。
7. 支持最大递归深度。
8. 支持集合最大处理数量。
9. 支持循环引用保护。
10. 支持异常 fail-open。
11. 支持统计 response 场景脱敏次数和耗时。
12. 支持 response 场景接口风险统计。

### 5.8.5 验收标准

1. Controller 返回 Java Bean 时敏感字段自动脱敏。
2. Controller 返回 Map 时敏感 key 自动脱敏。
3. Controller 返回 List 时内部元素自动脱敏。
4. 嵌套对象中的敏感字段自动脱敏。
5. 超过最大深度时安全停止。
6. 出现脱敏异常时不影响接口正常返回。
7. 关闭 response 场景后不再脱敏。
8. 命中接口级 ignore 后 response 不脱敏。
9. 命中接口级 ignore 后接口仍进入风险统计。

---

## 5.9 Log4j2 日志脱敏

### 5.9.1 功能说明

MVP 优先支持 Log4j2 2.x 日志脱敏，通过 Log4j2 扩展点在日志输出前对敏感数据进行脱敏。

日志脱敏模块需要保持适配层边界，避免核心脱敏能力与某一个日志框架强耦合。后续版本可在同一套 core 策略与 log masker 基础上增加 Logback 适配。

### 5.9.2 接入示例

Log4j2 2.x 可通过自定义 PatternConverter 接入。示例：

```xml
<Configuration status="WARN" packages="com.xxx.safeoutput.log4j2">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%t] %logger{36} - %desensitizeMsg%n"/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="info">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

说明：

1. `%desensitizeMsg` 表示使用组件提供的脱敏消息转换器输出日志内容。
2. MVP 默认采用 PatternConverter 方案，便于老项目通过日志配置改造接入。
3. 如试点项目已有复杂 Appender 或 RewriteAppender 体系，可在后续设计中扩展 RewritePolicy 适配，但不作为 MVP 必选实现。

### 5.9.3 日志识别策略

MVP 日志场景不引入 fastjson、Jackson 或其他 JSON Parser 作为强依赖。

日志脱敏采用：

1. 轻量 JSON-like key-value 识别。
2. value 内轻量正则扫描。
3. 整条 message 正则兜底。

### 5.9.4 需求内容

1. 提供 Log4j2 PatternConverter。
2. 支持对最终日志 message 进行脱敏。
3. 支持 JSON-like key-value 结构识别。
4. 支持 value 中包含“文字 + 敏感信息”的轻量正则扫描。
5. 支持手机号正则兜底。
6. 支持邮箱正则兜底。
7. 支持严格大陆身份证正则兜底。
8. 银行卡不做无上下文全局正则兜底。
9. 银行卡仅在 key 命中或上下文增强时脱敏。
10. 支持 log 场景开关。
11. 支持复用 core 策略。
12. 支持统计 log 场景脱敏次数和耗时。
13. 日志适配层不得反向污染 core 模块，Logback 兼容作为后续扩展。

### 5.9.5 性能控制

日志正则处理需要受配置控制：

```yaml
safe-output:
  log:
    framework: LOG4J2
    max-message-length: 5000
    max-value-length: 300
    regex-fallback:
      enabled: true
      types: [MOBILE, EMAIL, ID_CARD]
```

要求：

1. 超过 maxMessageLength 的日志可跳过正则兜底。
2. 超过 maxValueLength 的 value 可跳过 value 内扫描。
3. 正则 Pattern 应缓存，不允许每条日志重复编译。
4. 日志脱敏异常不得影响日志输出。
5. Log4j2 插件初始化失败时，不得影响 response 脱敏能力。

### 5.9.6 验收标准

1. 日志中 `"mobile":"13812345678"` 可脱敏。
2. 日志中直接出现 `13812345678` 可按手机号规则脱敏。
3. 日志中合法大陆身份证可通过严格校验后脱敏。
4. 普通 18 位流水号如果不符合身份证校验规则，不应被当作身份证脱敏。
5. 日志中邮箱可脱敏。
6. 日志中无上下文银行卡号默认不做全局兜底脱敏。
7. 关闭 log 场景后不再脱敏。
8. 未配置 Log4j2 PatternConverter 时，不影响应用启动和 response 脱敏。
9. 日志脱敏实现不得依赖 Logback API。

---

## 5.10 正则兜底边界

### 5.10.1 功能说明

正则兜底用于日志场景补充脱敏，不作为 response 场景主要识别方式。

### 5.10.2 默认支持类型

MVP 全局正则兜底默认支持：

1. 手机号。
2. 邮箱。
3. 严格大陆身份证。

### 5.10.3 严格大陆身份证校验要求

大陆身份证正则兜底必须满足：

1. 格式校验：18 位，前 17 位数字，最后一位数字或 X/x。
2. 出生日期校验：第 7-14 位必须是合法日期。
3. 日期范围校验：年份在合理范围内，例如 1900 至当前年份。
4. 校验位校验：按大陆身份证校验码算法验证最后一位。
5. 可选行政区划基础校验。

只有通过严格校验的字符串才可认定为高置信大陆身份证。

### 5.10.4 银行卡正则边界

银行卡不做无上下文全局正则兜底。

银行卡仅在以下场景脱敏：

1. key 命中 `bankCard`、`cardNo`、`bankNo` 等配置规则。
2. 日志中存在“银行卡”“卡号”“bankCard”“cardNo”等上下文。
3. 用户显式配置开启银行卡正则兜底。

### 5.10.5 Response 场景边界

Response 场景默认不对所有字符串 value 做全局正则扫描，避免误伤订单号、流水号、业务编号。

Response 场景优先使用：

1. 接口级 ignore。
2. 字段级 ignore。
3. 注解规则。
4. path rule。
5. key rule。
6. default rule。

### 5.10.6 验收标准

1. 手机号可被正则兜底识别。
2. 邮箱可被正则兜底识别。
3. 合法大陆身份证可被严格识别。
4. 非法身份证格式不脱敏。
5. 不合法校验位的 18 位数字不脱敏。
6. 无上下文银行卡号默认不脱敏。
7. Response 字符串字段不会因全局正则被大量误脱敏。

---

## 5.11 自定义脱敏策略

### 5.11.1 功能说明

允许业务系统通过实现策略接口扩展自定义脱敏逻辑。

### 5.11.2 示例

```java
@Component
public class CustomerNoMaskStrategy implements MaskStrategy {

    @Override
    public MaskType supportType() {
        return MaskType.CUSTOMER_NO;
    }

    @Override
    public String mask(String rawValue, MaskContext context) {
        // 自定义脱敏逻辑
        return rawValue;
    }
}
```

### 5.11.3 需求内容

1. 提供稳定的 `MaskStrategy` 接口。
2. 自动识别 Spring 容器中的自定义策略。
3. 自定义策略可覆盖或补充内置策略。
4. 策略注册冲突时有明确处理方式。

### 5.11.4 验收标准

1. Demo 中可增加一个自定义策略并成功生效。
2. 自定义策略不会破坏内置策略。
3. 策略注册失败时有清晰日志。

---

## 5.12 统计报告与存储边界

### 5.12.1 功能说明

MVP 支持基础统计指标，服务于试点验证和竞赛汇报。

统计报告必须明确存储边界，避免组件在高频使用后引入性能问题和新的数据治理风险。

相较只存内存的方案，v0.3 明确增加定时写入本地文件机制。统计数据运行时仍以内存聚合为主，但需要按固定周期将聚合快照导出到本地 JSON 文件，避免应用重启后统计结果完全丢失。

### 5.12.2 统计数据分类

MVP 只做运行指标类统计，不做明细审计类统计。

运行指标类包括：

1. 脱敏总次数。
2. response 脱敏次数。
3. log 脱敏次数。
4. 按脱敏类型统计次数。
5. 按接口统计脱敏次数。
6. 脱敏失败次数。
7. 平均耗时。
8. 最大耗时。
9. 正则兜底命中次数。
10. 接口风险等级。
11. 最近一次文件快照写入时间。
12. 文件快照写入失败次数。

MVP 不做明细审计类数据存储，例如：

1. 不保存脱敏前原始值。
2. 不保存脱敏后完整值。
3. 不保存完整 response。
4. 不保存完整日志内容。
5. 不保存单次请求的敏感字段明细。
6. 不保存脱敏前后完整对比。

### 5.12.3 存储方式

MVP 采用：

```text
内存聚合统计
+
定时写入本地 JSON 文件快照
+
可选日志输出
+
不落明细数据
```

要求：

1. 请求链路只更新内存聚合指标。
2. 文件写入由定时任务完成，不在业务请求线程同步刷盘。
3. 文件内容为聚合指标快照，不包含敏感原文、完整 response 或完整日志。
4. 文件格式优先采用 JSON，便于竞赛演示、人工查看和后续工具分析。
5. 支持配置文件输出目录、文件名前缀、写入间隔、单文件大小或保留数量。
6. 文件写入失败只记录组件内部日志和失败计数，不影响业务主流程。

MVP 不支持：

1. 数据库落库。
2. Redis 存储。
3. 远程接口上报。
4. Prometheus 指标。
5. 治理平台实时上报。

这些能力作为后续版本规划。

### 5.12.4 配置示例

```yaml
safe-output:
  report:
    enabled: true
    storage: MEMORY_AND_FILE
    include-api-metrics: true
    include-field-path: true
    include-raw-value: false
    export:
      enabled: true
      mode: FILE
      interval-seconds: 300
      file:
        directory: ./logs/safe-output
        filename-prefix: safe-output-report
        file-extension: json
        max-history: 20
      log-enabled: false
    api-metrics:
      enabled: true
      max-size: 1000
    performance:
      enabled: true
```

### 5.12.5 性能与容量控制

要求：

1. 统计采用内存聚合，不在请求链路同步写数据库、远程服务或本地文件。
2. 定时文件写入应使用独立调度任务，写入过程异常不得影响脱敏主流程。
3. 接口统计 key 优先使用 Spring MVC mapping pattern，例如 `/api/user/{id}`。
4. 避免直接使用动态 URI 导致统计 key 爆炸。
5. 支持 `apiMetrics.maxSize` 限制接口统计容量。
6. 超过 maxSize 后归入 `__overflow__`。
7. 高频计数建议使用 `LongAdder` 或类似低竞争计数方式。
8. 统计报告只记录聚合信息，不记录敏感原文。
9. 文件快照建议采用覆盖最新文件或按时间滚动文件，避免无限增长。
10. 文件导出任务需要支持开关，便于压测或生产灰度时关闭。

### 5.12.6 验收标准

1. response 脱敏后统计次数增加。
2. log 脱敏后统计次数增加。
3. 按类型统计结果正确。
4. 接口维度统计结果正确。
5. 可以输出基础报告。
6. 报告不包含敏感原文。
7. 报告不包含完整 response 或完整日志。
8. 统计模块异常不影响主流程。
9. 超过接口统计容量后可归入 overflow。
10. 开启文件导出后，可以按配置间隔生成 JSON 报告文件。
11. 应用重启后，历史报告文件仍可用于查看最近一次统计快照。
12. 文件写入失败不会影响接口返回、日志输出和脱敏主流程。

---

## 5.13 Response 接口风险统计

### 5.13.1 功能说明

MVP 只在 response 场景中进行接口风险统计，不做 log 与接口的关联统计。

### 5.13.2 统计维度

Response 接口风险统计包括：

1. HTTP method。
2. URI 或 mapping pattern。
3. Controller class。
4. Controller method。
5. 脱敏字段数量。
6. 脱敏类型分布。
7. 规则命中次数。
8. 本次脱敏耗时。
9. 总命中次数。
10. 风险等级。
11. 是否接口级 ignore。
12. ignore reason。

### 5.13.3 风险等级建议

MVP 支持简单规则：

| 触发情况 | 风险等级 |
|---|---|
| 命中 PASSWORD / SECRET / TOKEN | CRITICAL |
| 命中 ID_CARD / BANK_CARD | HIGH |
| 单接口单次脱敏字段数 >= 5 | HIGH |
| 命中 MOBILE / EMAIL / CHINESE_NAME | MEDIUM |
| 少量普通敏感字段 | LOW |
| 接口级 ignore 且 reason 表示业务明文展示 | HIGH 或 IGNORED_HIGH |

### 5.13.4 示例报告

```json
{
  "api": "GET /api/user/{id}",
  "controller": "UserController",
  "method": "detail",
  "hitCount": 120,
  "totalMaskCount": 380,
  "typeCounts": {
    "MOBILE": 120,
    "ID_CARD": 120,
    "EMAIL": 140
  },
  "riskLevel": "HIGH",
  "ignored": false,
  "avgCostMs": 1.8,
  "maxCostMs": 7.2
}
```

接口级 ignore 示例：

```json
{
  "api": "GET /api/user/mobile/query",
  "controller": "UserController",
  "method": "queryMobile",
  "hitCount": 32,
  "ignored": true,
  "ignoreReason": "查询手机号接口，业务要求返回明文",
  "riskLevel": "HIGH"
}
```

### 5.13.5 MVP 不做范围

MVP 不做：

1. Log 场景接口归因。
2. MDC 绑定 request path。
3. 异步线程日志接口归因。
4. traceId 维度聚合。
5. 分布式接口风险统计。

### 5.13.6 验收标准

1. Response 脱敏接口可进入接口维度统计。
2. 能统计接口脱敏类型分布。
3. 能生成简单风险等级。
4. 命中接口级 ignore 的接口也进入风险统计。
5. 接口统计报告不包含敏感原文。

---

## 5.14 Demo 示例项目

### 5.14.1 功能说明

提供一个可运行的 Spring Boot Demo 项目，用于验证和演示 MVP 能力。

### 5.14.2 Demo 内容

Demo 应包含：

1. starter 引入示例。
2. application.yml 配置示例。
3. log4j2.xml 配置示例。
4. UserDTO 示例。
5. OrderDTO 嵌套对象示例。
6. Map 返回示例。
7. List 返回示例。
8. 注解脱敏示例。
9. 字段级 ignore 示例。
10. 接口级 ignore 示例。
11. 日志打印脱敏示例。
12. 严格大陆身份证日志兜底示例。
13. 接口风险统计报告示例。
14. 基础统计报告示例。

### 5.14.3 验收标准

1. Demo 可本地启动。
2. 可访问接口看到 response 脱敏效果。
3. 可查看控制台日志脱敏效果。
4. 可查看字段级 ignore 效果。
5. 可查看接口级 ignore 效果。
6. 可查看 response 接口风险统计。
7. 可查看基础统计报告。
8. Demo 说明文档清晰。

---

## 6. 非功能需求

### 6.1 兼容性要求

1. 必须兼容 JDK8。
2. 必须兼容 Spring Boot 2.x。
3. Spring Boot 2.x 自动装配必须支持 `spring.factories`。
4. 不使用 Java 9+ API。
5. 不强制依赖 Spring Security。
6. 不强制依赖 MyBatis。
7. 不强制依赖配置中心。
8. MVP 优先支持 Log4j2 2.x。
9. 后续预留兼容 Logback 的日志适配扩展点。
10. 日志处理不强制引入 fastjson 或其他 JSON Parser。

### 6.2 性能要求

1. 普通 response 对象脱敏应控制在可接受耗时内。
2. 支持 Class 元信息缓存。
3. 支持字段规则缓存。
4. 支持正则 Pattern 缓存。
5. 支持最大递归深度限制。
6. 支持最大集合处理数量限制。
7. 支持日志最大 message 长度限制。
8. 支持日志 value 最大扫描长度限制。
9. 不对明显不可处理对象做深度扫描。
10. 统计采用内存聚合，避免同步落库影响性能。
11. 统计报告文件由定时任务异步写入，避免请求链路同步刷盘。

### 6.3 稳定性要求

1. 脱敏失败不影响业务接口返回。
2. 日志脱敏失败不影响日志输出。
3. 统计失败不影响脱敏主流程。
4. 配置错误应有提示，但不导致应用不可启动，除非是关键配置严重错误。
5. 对循环引用对象有保护。
6. 对超大集合有保护。
7. 对高频接口统计有容量保护。

### 6.4 安全要求

1. 统计报告不得保存脱敏前原始值。
2. 统计报告不得保存完整 response。
3. 统计报告不得保存完整日志。
4. 接口级 ignore 需要记录 reason，便于后续治理。
5. 正则兜底不应对银行卡做无上下文全局脱敏。
6. 严格大陆身份证必须通过合法性校验后才脱敏。

### 6.5 可扩展性要求

1. 脱敏策略可扩展。
2. 规则匹配方式可扩展。
3. 输出场景可扩展。
4. ignore 类型可扩展。
5. 报告输出方式可扩展。
6. 未来可扩展权限动态脱敏。
7. 未来可扩展 MyBatis、Logback、FastJson 等能力。
8. 未来可扩展 Prometheus、Actuator、治理平台等能力。

### 6.6 可维护性要求

1. 模块边界清晰。
2. 命名清晰一致。
3. 核心能力有单元测试。
4. Demo 能覆盖主要场景。
5. 文档说明清楚。
6. 适合分模块实现、测试和维护。

---

## 7. MVP 边界说明

### 7.1 MVP 明确不支持

MVP 第一版不支持以下内容：

1. 基于用户权限的动态脱敏。
2. 根据角色显示明文。
3. MyBatis 查询结果脱敏。
4. 数据库存储加密。
5. 配置中心热更新。
6. 分布式规则管理。
7. 分布式统计平台。
8. Logback 日志脱敏。
9. FastJson 深度集成。
10. 业务文件导出内容脱敏。
11. 消息队列脱敏。
12. System.out 全局拦截。
13. 第三方 SDK 内部日志完全覆盖。
14. 全链路 DLP 平台能力。
15. Log 场景接口归因统计。
16. 每次脱敏明细落库。
17. 保存敏感原文。

### 7.2 即插即用边界

MVP 支持老项目即插即用，但该能力有明确边界。

MVP 可覆盖：

1. 标准 Spring MVC Controller response。
2. Spring Boot 默认 Jackson JSON 返回。
3. Java Bean、Map、List、Array 等常见对象结构。
4. 配置字段名命中的敏感字段。
5. 配置字段路径命中的敏感字段。
6. 配置接口级 ignore 命中的 response 豁免。
7. Log4j2 pattern 中接入脱敏 PatternConverter 的日志输出。
8. 日志中常见 key-value 和正则可识别的高置信敏感数据。

MVP 不承诺覆盖：

1. 未经过 Spring MVC response 链路的输出。
2. 未接入 Log4j2 PatternConverter 的日志格式。
3. 二进制、文件流、图片流。
4. 非标准日志框架。
5. 第三方组件内部特殊输出。
6. 极复杂、无规律的字符串内容。
7. 没有上下文的银行卡号全局识别。

---

## 8. MVP 验收标准

### 8.1 接入验收

1. Demo 项目引入 starter 后可正常启动。
2. 不修改 Controller 代码即可实现 response 脱敏。
3. 不修改 DTO 代码，仅通过 YAML 配置即可实现字段脱敏。
4. 添加注解后可实现更精确脱敏。
5. 配置 `safe-output.enabled=false` 后组件能力关闭。
6. Spring Boot 2.x 自动装配通过 `spring.factories` 生效。

### 8.2 Response 脱敏验收

1. Java Bean 字段脱敏成功。
2. Map key 脱敏成功。
3. List 元素脱敏成功。
4. 嵌套对象脱敏成功。
5. 注解规则优先于配置规则。
6. 未命中规则的字段保持原样。
7. 模糊字段 `name` 默认不强制脱敏。
8. 字段级 ignore 生效。
9. 接口级 ignore 生效。
10. 异常对象不影响接口返回。

### 8.3 Log 脱敏验收

1. Log4j2 PatternConverter 配置后日志脱敏生效。
2. JSON-like 日志字段脱敏成功。
3. value 中包含“文字 + 手机号”时可以脱敏。
4. 直接出现的手机号脱敏成功。
5. 直接出现的邮箱脱敏成功。
6. 合法大陆身份证通过严格校验后脱敏成功。
7. 不合法身份证或普通 18 位流水号不误脱敏。
8. 无上下文银行卡号默认不全局脱敏。
9. 关闭 log 场景后日志不再脱敏。

### 8.4 策略验收

1. 手机号脱敏结果符合预期。
2. 身份证脱敏结果符合预期。
3. 银行卡脱敏结果符合预期。
4. 邮箱脱敏结果符合预期。
5. 姓名脱敏结果符合预期。
6. 密码字段全隐藏。
7. 自定义策略可以被注册和调用。

### 8.5 统计报告验收

1. 能统计脱敏总次数。
2. 能区分 response 和 log 场景。
3. 能按脱敏类型统计。
4. 能统计失败次数。
5. 能统计基础耗时。
6. 能统计 response 接口维度。
7. 能生成接口风险等级。
8. 能记录接口级 ignore 的接口。
9. 报告不包含敏感原文。
10. 报告不包含完整 response 或完整日志。
11. 能输出可读报告。

### 8.6 兼容性验收

1. JDK8 编译通过。
2. Spring Boot 2.x Demo 启动成功。
3. 不依赖 Java 9+ API。
4. 不强制依赖 Spring Security。
5. 不强制依赖 MyBatis。
6. 不强制依赖 fastjson。

---

## 9. 推荐项目结构

MVP 推荐项目结构如下：

```text
safe-output
├── pom.xml
├── docs
│   ├── 01-feasibility-analysis.md
│   ├── 02-mvp-scope.md
│   ├── 03-architecture-design.md
│   └── 04-ai-coding-guide.md
├── safe-output-core
│   └── 核心脱敏模型与策略
├── safe-output-spring-boot-starter
│   └── 自动装配、配置、response 脱敏
├── safe-output-log4j2
│   └── Log4j2 日志脱敏
├── safe-output-report
│   └── 统计报告
└── safe-output-demo
    └── 演示项目
```

---

## 10. WBS 工作分解原则

### 10.1 分解目标

本 WBS 不是传统项目管理中的泛化任务清单，而是面向 MVP 交付的工程化工作分解。

每个工作包应满足：

1. 能被独立理解和验收。
2. 有清晰职责边界。
3. 有明确输入和输出。
4. 有明确不做范围。
5. 能进一步拆成具体实现任务。
6. 能明确依赖关系和阶段归属。
7. 能配套单元测试或集成测试。

### 10.2 分解原则

工作包分解遵循以下原则：

1. 按模块边界分解，而不是按人员分解。
2. 按可交付工程任务分解，而不是按抽象职能分解。
3. 每个工作包职责单一。
4. 每个工作包尽量对应一个可独立验收的工程产物。
5. 工作包之间依赖关系清晰。
6. 先核心模型，再自动装配，再场景能力，再报告和 Demo。
7. 每个工作包都应包含测试要求。
8. MVP 不做的能力不进入工作包实现范围。

---

## 11. WBS 工作分解清单

### 11.1 WBS 总览

| WBS 编号 | 工作包名称 | 对应模块 | 优先级 | 主要目标 |
|---|---|---|---|---|
| WBS-00 | 项目骨架与工程约束 | Root | P0 | 建立 Maven 多模块和 JDK8 约束 |
| WBS-01 | 核心领域模型 | core | P0 | 定义 MaskType、MaskScene、上下文、结果对象 |
| WBS-02 | 内置脱敏策略 | core | P0 | 实现手机号、身份证、银行卡等策略 |
| WBS-03 | 策略注册与扩展机制 | core/starter | P0 | 支持内置和自定义策略注册 |
| WBS-04 | 配置属性模型 | starter | P0 | 实现 YAML 配置绑定、ignore、report 配置 |
| WBS-05 | 规则匹配能力 | starter | P0 | 支持字段名、路径、注解、默认规则、ignore 优先级 |
| WBS-06 | 对象递归脱敏引擎 | starter | P0 | 支持 Bean、Map、Collection、Array |
| WBS-07 | Response 脱敏接入 | starter | P0 | 实现 ResponseBodyAdvice、接口级 ignore、接口统计 |
| WBS-08 | Log4j2 日志脱敏 | log4j2 | P0 | 实现 Log4j2 PatternConverter、JSON-like、正则边界 |
| WBS-09 | 统计指标与报告 | report | P1 | 实现内存聚合、定时文件快照、接口风险统计、报告输出 |
| WBS-10 | Demo 示例项目 | demo | P0 | 演示即插即用、ignore、统计报告 |
| WBS-11 | 测试体系 | all | P0 | 单元测试、集成测试、Demo 验证 |
| WBS-12 | 接入文档与演示材料 | docs | P1 | 编写接入说明和竞赛演示说明 |

---

## 12. WBS 工作包说明

## WBS-00 项目骨架与工程约束

### 工作包目标

建立 Maven 多模块工程骨架，明确 JDK8、Spring Boot 2.x、依赖边界和基础编码约束。

### 职责范围

1. 创建父工程 `safe-output`。
2. 创建 Maven 子模块。
3. 配置 JDK8 编译参数。
4. 配置基础依赖版本。
5. 配置单元测试依赖。
6. 建立基础包名规范。
7. 建立 docs 目录。
8. 配置 Spring Boot 2.x 自动装配所需 `spring.factories`。

### 输出物

1. 根 `pom.xml`。
2. 各子模块 `pom.xml`。
3. 基础目录结构。
4. 初始 README。
5. `META-INF/spring.factories` 示例。

### 不做范围

1. 不实现具体脱敏逻辑。
2. 不实现 response 拦截。
3. 不实现日志脱敏。

---

## WBS-01 核心领域模型

### 工作包目标

定义脱敏组件的核心领域模型，为所有场景提供统一抽象。

### 职责范围

1. 定义 `MaskType`。
2. 定义 `MaskScene`。
3. 定义 `MaskContext`。
4. 定义 `MaskResult`。
5. 定义 `MaskStrategy` 接口。
6. 定义基础异常或错误码，MVP 可保持轻量。

### 输出物

1. 核心模型类。
2. 核心接口。
3. 基础单元测试。

### 不做范围

1. 不实现 Spring Boot 自动装配。
2. 不读取 YAML 配置。
3. 不处理对象递归。
4. 不处理日志。

---

## WBS-02 内置脱敏策略

### 工作包目标

实现 MVP 内置敏感数据脱敏策略。

### 职责范围

1. 手机号脱敏策略。
2. 身份证脱敏策略。
3. 银行卡脱敏策略。
4. 邮箱脱敏策略。
5. 中文姓名脱敏策略。
6. 地址脱敏策略。
7. 密码全隐藏策略。
8. 默认脱敏策略。
9. 严格大陆身份证校验工具。
10. 空值、短值、非法格式保护。

### 输出物

1. 各策略实现类。
2. 身份证校验工具类。
3. 策略单元测试。
4. 策略测试用例数据。

### 不做范围

1. 不做字段识别。
2. 不做规则匹配。
3. 不做日志正则识别。
4. 不做权限判断。

---

## WBS-03 策略注册与扩展机制

### 工作包目标

实现策略注册、查找和扩展机制，支持内置策略和用户自定义策略。

### 职责范围

1. 实现 `MaskStrategyRegistry`。
2. 注册内置策略。
3. 支持通过 Spring Bean 加载自定义策略。
4. 处理策略冲突。
5. 提供按 `MaskType` 查找策略能力。
6. 提供默认 fallback 策略。

### 输出物

1. 策略注册器。
2. Spring 集成注册逻辑。
3. 自定义策略示例。
4. 单元测试。

### 不做范围

1. 不做权限动态策略。
2. 不做远程规则加载。
3. 不做配置中心热更新。

---

## WBS-04 配置属性模型

### 工作包目标

实现 `application.yml` 配置绑定，为即插即用、规则控制、ignore、统计报告提供配置基础。

### 职责范围

1. 定义 `SafeOutputProperties`。
2. 定义全局开关。
3. 定义场景开关。
4. 定义规则配置。
5. 定义 ignore.keys。
6. 定义 ignore.paths。
7. 定义 ignore.packages，P1。
8. 定义 ignore.apis。
9. 定义 report 配置。
10. 定义默认 maskChar。
11. 定义 maxDepth。
12. 定义 maxCollectionSize。
13. 定义 log maxMessageLength。
14. 定义 log maxValueLength。
15. 支持默认值。

### 输出物

1. 配置属性类。
2. 配置规则类。
3. ignore 配置类。
4. report 配置类。
5. 配置绑定测试。
6. YAML 示例。

### 不做范围

1. 不做配置中心。
2. 不做运行时热更新。
3. 不做复杂规则表达式。
4. 不做远程配置拉取。

---

## WBS-05 规则匹配能力

### 工作包目标

实现敏感字段识别、规则匹配、模糊字段控制和 ignore 优先级能力。

### 职责范围

1. 字段名 key 匹配。
2. 字段路径 path 匹配。
3. 默认字段规则库。
4. 模糊字段默认不强脱敏。
5. 注解识别。
6. 注解优先级高于普通配置规则。
7. 字段级 ignore 匹配。
8. 接口级 ignore 匹配。
9. 支持规则启用/禁用。
10. 支持字段匹配缓存。
11. 输出匹配结果。

### 输出物

1. `RuleMatcher`。
2. `SensitiveFieldResolver`。
3. `IgnoreMatcher`。
4. `ApiIgnoreMatcher`。
5. 默认规则定义。
6. 模糊字段清单。
7. 注解解析逻辑。
8. 单元测试。

### 不做范围

1. 不做权限判断。
2. 不做机器学习识别。
3. 不做数据库字段扫描。
4. 不做复杂表达式规则。

---

## WBS-06 对象递归脱敏引擎

### 工作包目标

实现 response 场景下的对象递归脱敏能力。

### 职责范围

1. Bean 字段遍历。
2. Map key-value 处理。
3. Collection 遍历。
4. Array 遍历。
5. 嵌套对象处理。
6. 最大递归深度控制。
7. 最大集合数量控制。
8. 循环引用保护。
9. Class 元信息缓存。
10. 字段级 ignore 判断。
11. 不支持类型跳过。

### 输出物

1. `ObjectMasker`。
2. `BeanMasker`。
3. `MapMasker`。
4. `CollectionMasker`。
5. `ArrayMasker`。
6. 单元测试。

### 不做范围

1. 不处理日志字符串。
2. 不处理数据库结果集。
3. 不修改文件流或二进制对象。
4. 不做 response 场景全局正则扫描。

---

## WBS-07 Response 脱敏接入

### 工作包目标

通过 Spring MVC `ResponseBodyAdvice` 接入 response 脱敏能力，并实现接口级 ignore 和接口风险统计。

### 职责范围

1. 实现 `DesensitizeResponseAdvice`。
2. 判断全局开关。
3. 判断 response 场景开关。
4. 判断接口级 ignore。
5. 命中接口级 ignore 时跳过 response 脱敏。
6. 调用对象递归脱敏引擎。
7. 处理 `ResponseEntity`。
8. 跳过不支持的返回类型。
9. 收集 response 场景指标。
10. 收集接口风险统计。
11. 记录 ignored 接口统计。
12. 异常 fail-open。

### 输出物

1. ResponseBodyAdvice 实现。
2. 自动装配配置。
3. 接口级 ignore 集成测试。
4. 接口统计集成测试。
5. Demo Controller 示例。

### 不做范围

1. 不实现 WebFlux。
2. 不实现 RPC/Dubbo Filter。
3. 不实现 Servlet Filter 全局响应包装。
4. 不处理文件下载 response。
5. 不做 log 接口归因。

---

## WBS-08 Log4j2 日志脱敏

### 工作包目标

实现 Log4j2 2.x 日志脱敏能力，覆盖 MVP 的“脱敏打印”场景，并为后续 Logback 适配预留边界。

### 职责范围

1. 实现 Log4j2 PatternConverter。
2. 实现日志消息脱敏器。
3. 支持 JSON-like key-value 脱敏。
4. 支持 value 内轻量正则扫描。
5. 支持手机号正则兜底。
6. 支持邮箱正则兜底。
7. 支持严格大陆身份证正则兜底。
8. 不引入 fastjson。
9. 不做无上下文银行卡全局兜底。
10. 复用 core 策略。
11. 支持 log 场景开关。
12. 收集 log 场景指标。

### 输出物

1. `DesensitizeMessagePatternConverter`。
2. `LogMasker`。
3. `JsonLikeLogMasker`。
4. `RegexLogMasker`。
5. `MainlandIdCardDetector`。
6. log4j2.xml 示例。
7. 日志脱敏测试。

### 不做范围

1. 不实现 Logback。
2. 不拦截 System.out。
3. 不保证第三方 SDK 内部日志全部覆盖。
4. 不做复杂自然语言敏感信息识别。
5. 不做 log 与接口关联统计。
6. 不引入 JSON Parser 作为强依赖。

---

## WBS-09 统计指标与报告

### 工作包目标

实现基础脱敏统计、接口风险统计和报告输出能力。

### 职责范围

1. 统计脱敏总次数。
2. 统计 response 场景次数。
3. 统计 log 场景次数。
4. 按 MaskType 统计。
5. 统计失败次数。
6. 统计耗时。
7. 统计 response 接口维度。
8. 统计接口风险等级。
9. 统计 ignored 接口。
10. 内存聚合存储。
11. 接口统计容量限制。
12. 输出报告对象。
13. 支持定时写入本地 JSON 文件。
14. 支持可选日志输出。
15. 禁止保存敏感原文。

### 输出物

1. `MaskMetricsCollector`。
2. `MaskMetrics`。
3. `ApiMaskMetrics`。
4. `ApiRiskLevel`。
5. `MaskReport`。
6. `MaskReportExporter`。
7. `ScheduledFileReportExporter`。
8. 报告 JSON 文件示例。
8. 单元测试。

### 不做范围

1. 不做分布式聚合。
2. 不做数据库落库。
3. 不做 Redis 存储。
4. 不做 Prometheus。
5. 不做可视化后台。
6. 不强制接入 Actuator。
7. 不保存敏感原文。
8. 不保存完整 response 或日志。

---

## WBS-10 Demo 示例项目

### 工作包目标

提供可运行 Demo，展示 MVP 即插即用能力和核心场景。

### 职责范围

1. 创建 Spring Boot Demo。
2. 引入 starter。
3. 配置 application.yml。
4. 配置 log4j2.xml。
5. 提供 Bean 返回接口。
6. 提供 Map 返回接口。
7. 提供 List 返回接口。
8. 提供嵌套对象接口。
9. 提供注解脱敏示例。
10. 提供字段级 ignore 示例。
11. 提供接口级 ignore 示例。
12. 提供日志脱敏示例。
13. 提供严格大陆身份证日志兜底示例。
14. 提供统计报告示例。

### 输出物

1. Demo 项目代码。
2. Demo 配置文件。
3. Demo 接口说明。
4. Demo 演示步骤。

### 不做范围

1. 不接入真实数据库。
2. 不接入真实业务系统。
3. 不实现前端页面。
4. 不实现复杂登录权限。

---

## WBS-11 测试体系

### 工作包目标

建立 MVP 的基本测试体系，保证核心能力可验证。

### 职责范围

1. 策略单元测试。
2. 严格大陆身份证校验测试。
3. 规则匹配测试。
4. 模糊字段测试。
5. ignore 测试。
6. 配置绑定测试。
7. 对象递归脱敏测试。
8. Response 集成测试。
9. 接口级 ignore 集成测试。
10. 接口风险统计测试。
11. 日志脱敏测试。
12. 统计报告测试。
13. Demo 手工验证清单。

### 输出物

1. 单元测试代码。
2. 集成测试代码。
3. 测试数据。
4. 手工验收清单。

### 不做范围

1. 不做大规模性能压测平台。
2. 不做安全扫描平台。
3. 不做全链路自动化测试。

---

## WBS-12 接入文档与演示材料

### 工作包目标

编写 MVP 接入说明和竞赛演示说明。

### 职责范围

1. 编写 README。
2. 编写快速接入文档。
3. 编写配置说明。
4. 编写注解使用说明。
5. 编写 ignore 使用说明。
6. 编写 Log4j2 接入说明。
7. 编写统计报告说明。
8. 编写 Demo 演示步骤。
9. 编写 MVP 验收清单。
10. 编写竞赛演示脚本初稿。

### 输出物

1. README.md。
2. docs/quick-start.md。
3. docs/configuration.md。
4. docs/ignore-guide.md。
5. docs/log4j2-guide.md。
6. docs/report-guide.md。
7. docs/demo-guide.md。
8. docs/acceptance-checklist.md。

### 不做范围

1. 不写完整竞赛 PPT。
2. 不写详细设计文档。
3. 不写后续版本完整方案。

---

## 13. 阶段性目标与里程碑

### 13.1 阶段划分原则

MVP 按可运行、可接入、可演示、可验收的顺序推进。每个阶段必须形成可验证输出，避免只完成代码片段但无法证明整体能力。

### 13.2 阶段目标

| 阶段 | 覆盖 WBS | 阶段目标 | 里程碑产物 | 通过标准 |
|---|---|---|---|---|
| P0 基础骨架阶段 | WBS-00、WBS-01 | 建立工程结构和核心领域模型 | 多模块 Maven 工程、核心枚举、上下文、结果对象 | JDK8 编译通过，核心模型单测通过 |
| P1 核心策略阶段 | WBS-02、WBS-03 | 完成内置策略和策略注册扩展 | 内置策略、策略注册器、自定义策略接口 | 手机号、邮箱、身份证、银行卡等策略单测通过 |
| P2 规则配置阶段 | WBS-04、WBS-05 | 完成 YAML 配置、默认规则、ignore 和优先级 | 配置属性模型、规则匹配器、默认规则库 | key、path、注解、ignore 优先级测试通过 |
| P3 Response 脱敏阶段 | WBS-06、WBS-07 | 完成对象递归脱敏和接口 response 接入 | 对象脱敏引擎、ResponseBodyAdvice、接口级 ignore | Demo 接口无需改 Controller 即可脱敏，接口级 ignore 生效 |
| P4 Log4j2 日志阶段 | WBS-08 | 完成 Log4j2 2.x 日志输出脱敏 | PatternConverter、JSON-like 识别、正则兜底 | Demo 日志中手机号、邮箱、身份证可脱敏，不引入 fastjson |
| P5 统计报告阶段 | WBS-09 | 完成基础统计、接口风险统计和本地文件快照 | 内存聚合指标、接口风险统计、JSON 报告文件 | 定时文件写入可配置，不保存敏感原文，不影响主流程 |
| P6 演示验收阶段 | WBS-10、WBS-11、WBS-12 | 完成 Demo、测试体系和接入文档 | Demo 项目、测试报告、接入说明、验收清单 | 可完整演示即插即用、response、log、ignore、统计报告 |

### 13.3 推荐实施顺序

1. 先完成 P0 至 P2，保证核心脱敏模型、策略和规则优先级稳定。
2. P3 是 MVP 主链路，必须优先完成 response 脱敏、接口级 ignore 和 fail-open。
3. P4 可在 P3 基础稳定后并行推进，但不得影响核心模块边界。
4. P5 先实现轻量本地统计报告，不引入数据库、Redis、Prometheus 或可视化后台。
5. P6 以 Demo 和验收清单为准，不追加超出 MVP 的治理平台、动态权限、配置中心热更新能力。

---

## 14. MVP 交付物清单

### 14.1 代码交付物

1. `safe-output-core`
2. `safe-output-spring-boot-starter`
3. `safe-output-log4j2`
4. `safe-output-report`
5. `safe-output-demo`

### 14.2 文档交付物

1. 可行性分析报告。
2. MVP 需求范围说明书。
3. 概要设计说明书。
4. 快速接入文档。
5. 配置说明文档。
6. ignore 使用说明。
7. Log4j2 接入说明。
8. 统计报告说明。
9. Demo 演示说明。
10. 验收清单。

### 14.3 测试交付物

1. 策略单元测试。
2. 严格身份证校验测试。
3. 规则匹配测试。
4. ignore 测试。
5. 配置绑定测试。
6. Response 集成测试。
7. Log4j2 日志脱敏测试。
8. 统计报告测试。
9. Demo 验证清单。

### 14.4 竞赛展示交付物

1. 未脱敏与已脱敏对比截图。
2. 接口 response 脱敏演示。
3. 日志脱敏演示。
4. 配置化规则演示。
5. 注解脱敏演示。
6. 字段级 ignore 演示。
7. 接口级 ignore 演示。
8. 接口风险统计演示。
9. 统计报告演示。
10. 后续版本规划说明。

---

## 15. MVP 成功标准

MVP 可以视为成功，需要满足以下条件：

1. 能在 JDK8 + Spring Boot 2.x Demo 中运行。
2. 能通过 starter 完成自动装配。
3. 能通过 YAML 配置完成老项目字段脱敏。
4. 能在不修改 Controller 的情况下实现 response 脱敏。
5. 能通过 Log4j2 2.x 配置实现日志脱敏。
6. 能支持常见敏感数据类型。
7. 能处理模糊字段误脱敏风险。
8. 能支持字段级 ignore。
9. 能支持接口级 response ignore。
10. 能区分 response 和 log 两个场景。
11. 能输出基础统计报告。
12. 能按配置定时写入本地 JSON 统计报告文件。
13. 能输出 response 接口风险统计。
14. 统计报告不保存敏感原文。
15. 能演示即插即用价值。
16. 代码结构适合后续分模块继续迭代。

---

## 16. 需求澄清计划

本节按照 grill-with-docs 的要求对需求树进行一次性澄清，推荐答案作为默认确认结论。若无异议，后续设计和实现均按“推荐结论”执行。

| 编号 | 澄清主题 | 需要确认的问题 | 推荐结论 | 对范围的影响 |
|---|---|---|---|---|
| C01 | MVP 主目标 | MVP 优先证明什么能力？ | 优先证明传统 Spring Boot 2.x 老项目低侵入接入，并覆盖 response 与 Log4j2 日志两个输出场景。 | 保持 response 和 Log4j2 为 MVP 核心，不把 MyBatis、权限动态脱敏、治理后台纳入 MVP。 |
| C02 | 默认启用策略 | 引入 starter 后是否默认立即脱敏？ | 推荐默认总开关开启，但 response 和 log 场景开关可独立关闭；Demo 展示默认开启。 | 便于体现即插即用，同时保留灰度回退能力。 |
| C03 | Response 处理边界 | ResponseBodyAdvice 是否处理所有返回值？ | 只处理普通 JSON 响应中的 Bean、Map、Collection、Array；跳过 String、byte[]、文件下载、流式响应和不可安全遍历对象。 | 降低误处理风险，MVP 不做全局响应包装。 |
| C04 | 日志处理边界 | 日志脱敏是否追求完整 JSON 解析？ | 不做完整 JSON 解析；Log4j2 使用 PatternConverter，采用 JSON-like key-value 识别和正则兜底。 | 避免引入 fastjson 等重依赖，优先满足老项目接入。 |
| C05 | 模糊字段 | `name`、`id`、`code`、`no` 是否默认脱敏？ | 不进入默认强规则，只能通过 path rule、key rule 或注解显式配置。 | 降低误脱敏，避免影响业务显示。 |
| C06 | 规则优先级 | 多种规则同时命中时如何裁决？ | 固定为接口级 ignore > 字段级 ignore > 注解规则 > path rule > key rule > default rule > regex fallback。 | 所有规则匹配、测试和文档按此优先级验收。 |
| C07 | 接口级 ignore 统计 | 接口级 ignore 后是否还进入统计？ | 进入接口风险统计，但不执行 response 脱敏；报告中标记 ignored。 | 既保护业务豁免，又保留风险可见性。 |
| C08 | 统计报告数据 | 统计报告能否保存样本原文？ | 禁止保存敏感原文、完整 response、完整日志；只保存聚合指标、类型、计数、接口标识和风险等级。 | 降低组件自身的数据安全风险。 |
| C09 | 统计存储 | MVP 是否需要持久化数据库？ | 不需要；采用内存聚合 + 定时写入本地 JSON 快照。 | 不引入数据库、Redis、消息队列或治理后台。 |
| C10 | 身份证识别 | 身份证正则是否只看 18 位格式？ | 必须做严格大陆身份证校验，包括地区码、出生日期、校验位和合理年份。 | 提升准确性，避免日志中普通 18 位编号误脱敏。 |
| C11 | 银行卡兜底 | 银行卡是否进入无上下文全局正则？ | 不进入；仅在字段名、路径、注解或明确类型命中时脱敏银行卡。 | 避免对订单号、流水号、长编号造成误脱敏。 |
| C12 | 异常策略 | 脱敏失败时是否阻断业务？ | fail-open，记录失败指标，返回原业务结果或原日志内容。 | 保证组件不会扩大业务可用性风险。 |
| C13 | 兼容性基线 | MVP 的 Java 和 Spring Boot 基线是什么？ | JDK8 + Spring Boot 2.x，自动装配使用 `META-INF/spring.factories`。 | 不以 Spring Boot 3.x 新机制作为唯一入口。 |
| C14 | 后续扩展 | 哪些能力只预留扩展点？ | Logback、WebFlux、RPC/Dubbo、MyBatis、配置中心热更新、动态权限、治理平台只预留边界，不进入 MVP。 | 防止 MVP 范围膨胀。 |
| C15 | 验收方式 | MVP 最终按什么验收？ | 以 Demo 可运行、测试通过、接入文档完整、response/log/ignore/统计报告可演示为验收主线。 | 验收聚焦可运行证据，而不是过程文档数量。 |

### 16.1 待一次性确认的默认决策

如无异议，以下决策直接作为后续概要设计和实现输入：

1. MVP 名称沿用“通用 Java 数据脱敏组件”，工程模块使用 `safe-output-*`。
2. Response 脱敏为 MVP 主链路，Log4j2 日志脱敏为第二主链路。
3. 所有场景必须支持全局开关和场景开关。
4. 统计报告仅保存聚合指标，不保存敏感原文。
5. Demo 必须覆盖即插即用、response 脱敏、日志脱敏、字段级 ignore、接口级 ignore、统计报告。
6. 本文档不再展开过程沟通和工具提示词，后续实施文档只保留与交付、验收、架构边界直接相关的信息。

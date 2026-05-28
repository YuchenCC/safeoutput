# Safe Output 完整需求说明书

版本：整合版 / 覆盖 MVP 至 R3  
适用范围：Safe Output Java 8 / Spring Boot 2.x 输出侧脱敏 starter、可选 Dashboard、Demo 样板系统  
来源：`safe-output-mvp-prd.md`、`safe-output-r2-prd.md`、`safe-output-r25-prd.md`、`safe-output-r25-supplemental-prd.md`、`safe-output-r26-dashboard-prd.md`、`safe-output-r3-prd.md`、需求变更记录、需求澄清说明  

## 1. 项目定位

Safe Output 是面向 Java 8 / Spring Boot 2.x 存量系统的低侵入输出侧敏感数据脱敏组件。它以 `safe-output-spring-boot-starter` 为对外推荐接入入口，优先覆盖接口 Response、Log4j2 日志和业务主动调用三个场景，并通过聚合统计、风险画像、报告、规则建议和可选 Dashboard 支撑治理复盘。

项目目标不是替代完整 DLP 平台，而是在少改业务代码的前提下，为老系统提供可接入、可配置、可扩展、可观测且不保存敏感原文的输出侧治理能力。

## 2. 目标用户与使用场景

目标用户包括：

- Java 8 / Spring Boot 2.x 老项目维护人员。
- 需要统一治理接口返回和日志输出的业务研发团队。
- 需要沉淀敏感字段规则、接口风险和日志规则建议的安全治理人员。
- 架构组或公共组件维护者。
- 需要可运行 Demo 和接入样板的试点或评审人员。

核心场景包括：

- 业务系统引入 starter 后，在不改 Controller 主逻辑的情况下自动脱敏 JSON response。
- 业务日志通过 Log4j2 `%safeOutputMsg` 输出脱敏后的 message。
- 业务代码通过 `SafeOutputMaskService` 主动执行指定 type、对象规则或强文本扫描脱敏。
- 报告和 Dashboard 展示脱敏总量、场景分布、类型分布、接口风险、ignore 风险、性能画像和日志规则建议。
- 接入方根据候选 YAML 规则人工复核并补齐配置。

## 3. 版本需求总览

| 版本 | 目标 | 主要需求 |
|---|---|---|
| MVP | 建立低侵入 starter 基线 | Response、Log4j2、配置规则、注解、ignore、基础统计、Demo |
| R2 | 增强扩展性和治理能力 | String type、自定义策略、主动脱敏、风险画像、Log 规则建议 |
| R2.5 | 强化 Demo 接入样板 | 业务工作台、五类业务域、实验室、历史报告、只读日志场景 |
| R2.5 补充 | 校准最终 Demo 口径 | 白底后台风格、固定两轮实验室、日志真实聚合、报告 Tab 收敛 |
| R2.6 | 抽离 Dashboard 附加包 | 默认关闭、Spring MVC、POST API、实时/历史/上传/实验室 |
| R3 | 增强日志长度策略 | 整条跳过兼容模式、前缀扫描窗口模式、输出不截断 |

## 4. 功能需求

### 4.1 Core 脱敏模型

Safe Output 必须提供统一脱敏领域模型，支撑 Response、Log、Manual 和 Report 多场景复用。

需求包括：

- 提供脱敏场景、上下文、结果、策略接口和策略注册表。
- 内置常见脱敏策略：`MOBILE`、`ID_CARD`、`BANK_CARD`、`EMAIL`、`CHINESE_NAME`、`ADDRESS`、`PASSWORD`、`DEFAULT`。
- 对外脱敏类型标签使用 String，内置类型可通过常量类作为推荐值。
- 支持业务自定义 `MaskStrategy`，并允许配置和注解引用自定义 type。
- 未知 type 不阻断应用启动；当前运行期命中后按 `warn + DEFAULT fallback` 处理，并进入未知类型统计。
- 策略对空值、空字符串、异常格式保持安全处理，不向业务链路抛出不可控异常。

### 4.2 规则匹配与 ignore

规则体系必须统一处理注解、配置规则、默认规则、字段 ignore、接口 ignore 和日志 fallback。

需求包括：

- 支持 `safe-output.rules[]` 配置规则，字段包含 `name`、`keys`、`paths`、`type`、`enabled`。
- 支持默认规则库，覆盖语义明确字段名；`name`、`id`、`code`、`no` 等歧义字段不进入默认强规则。
- 支持通过 `safe-output.rules.default-enabled=false` 关闭默认规则库。
- 支持字段级 `ignore.keys` 和 `ignore.paths`。
- 支持接口级 `ignore.apis`，按 method、path pattern 和 scene 进行豁免。
- 接口级 ignore 命中后 response 可返回明文，但必须进入风险统计。
- 规则优先级固定为：API ignore / 字段 ignore > 注解 > 配置或默认规则 > regex fallback。
- `rules[].paths` 和 `ignore.paths` 使用组件递归路径语义，不承诺完整 JSONPath。

### 4.3 Response 自动脱敏

Response 场景通过 Spring MVC `ResponseBodyAdvice` 在 JSON 序列化前处理返回对象。

需求包括：

- 支持 Bean、Map、Collection、Array、嵌套对象和统一响应结构中的 data path。
- 支持最大递归深度、集合处理上限和循环引用保护。
- 支持字段注解 `@Desensitize(type = "...")`，注解优先于配置和默认规则。
- 支持接口级 ignore，并记录 ignored 风险事件。
- 支持 fail-open：脱敏异常不得影响业务接口返回。
- 默认不对所有字符串 value 做全局 regex 扫描，避免误伤订单号、流水号、商品名等业务值。
- 跳过文件、图片、二进制、流式响应、Servlet 原生对象和不可安全遍历对象。

### 4.4 Log4j2 日志脱敏

日志场景通过 Log4j2 `%safeOutputMsg` PatternConverter 处理最终 message。

需求包括：

- 支持 JSON-like/key-value 轻量识别，例如 `key=value`、`key: value`、`"key":"value"` 等形式。
- 日志 key-value 匹配复用 `rules.keys`，并支持自定义 String type。
- 字段级 `ignore.keys` 对日志 key-value 匹配生效；接口级 ignore 不影响日志脱敏。
- 支持有限 regex fallback，默认覆盖手机号、邮箱和按 R2 分层策略处理的身份证。
- 银行卡号默认不做无上下文全局 regex fallback。
- 不引入 fastjson、Jackson 或其他 JSON Parser 作为日志脱敏强依赖。
- 日志脱敏异常或 converter 初始化异常不得影响日志输出。
- starter 通过 runtime bridge 将 Spring 配置规则、自定义策略、日志选项、统计 collector 和建议 collector 提供给真实 Log4j2 converter。

### 4.5 R3 日志长度策略

日志长度策略需要同时满足兼容性、性能和安全收益。

需求包括：

- 保留 `maxMessageLength` 整条限制模式：超过阈值时整条日志不处理，保持 R2 兼容。
- 新增前缀扫描窗口模式：通过 `max-scan-length` 限制扫描前 N 个字符，尾部原样拼回。
- 前缀扫描模式不得截断最终日志输出。
- 扫描窗口外不做 key-value 解析、不做 regex fallback、不做脱敏。
- key-value 片段或 regex 候选值跨越窗口边界时保守处理，不做破坏性替换。
- `maxValueLength` 继续限制单个 value 或候选值处理成本，不改变为输出截断语义。
- 该策略只影响 Log4j2 日志脱敏，不改变 Response 和主动脱敏行为。

### 4.6 主动脱敏服务

主动脱敏服务用于业务代码显式复用统一策略。

需求包括：

- 提供可注入的 `SafeOutputMaskService`。
- 支持指定 type 脱敏：输入 value 和 type，直接调用对应策略。
- 支持对象规则脱敏：复用 Response 对象递归、注解、配置、默认规则和 ignore。
- 支持强扫描脱敏：对文本或对象字符串执行类似 Log 的 key-value 和有限 regex fallback。
- 主动调用统一计入 `MANUAL` 场景统计，但不默认计入 Response 接口风险统计。
- Demo 和 Dashboard 实验室固定展示两轮脱敏结果、每轮耗时和二次脱敏稳定性。
- 强扫描必须由业务方显式调用，允许一定误脱敏；Response 默认不采用强扫描语义。

### 4.7 统计报告与风险画像

统计报告必须基于聚合摘要，不保存敏感原文。

需求包括：

- 采集脱敏总次数、场景分布、类型分布、失败次数、平均耗时、最大耗时和 unknown type 统计。
- Response 维度聚合接口、Controller、方法、命中类型、字段数量、耗时、ignored 状态和 ignore reason。
- 生成 Response 风险画像、风险等级、风险原因、治理建议和性能画像。
- Log 分析基于 fallback 附近 key 聚合生成规则建议，包含 key、suggestedType、hitCount、confidence、evidence 和候选 YAML。
- 候选 YAML 默认 `enabled:false`，不自动写配置、不自动生效。
- 报告以本地 JSON 快照为权威产物；报告导出异常不得影响业务链路。
- 报告禁止保存脱敏前原始值、脱敏后完整值、完整 response、完整日志 message、单次字段明细或可反推敏感值的大段上下文。

### 4.8 Spring Boot starter

对外接入入口为 `safe-output-spring-boot-starter`。

需求包括：

- 兼容 Java 8 和 Spring Boot 2.x。
- 自动装配使用 `META-INF/spring.factories`。
- starter 聚合 core、log4j2、report 等内部模块能力，业务系统不需要手动引用内部模块。
- 支持 `safe-output.enabled=false` 和 response/log/report 等场景开关。
- 支持配置绑定、自定义策略 Bean 注册、默认规则开关、ignore、report 和 log 选项。
- 不使用 Boot 3 专属 API 作为唯一自动装配入口。

### 4.9 Dashboard starter

Dashboard 是可选本地治理附加包，不属于业务 starter 的默认暴露面。

需求包括：

- 新增 `safe-output-dashboard-spring-boot-starter`，默认关闭。
- 仅在 Spring MVC Servlet Web 应用且 `safe-output.dashboard.enabled=true` 时装配。
- 默认路径前缀为 `/safe-output/dashboard`，入口为 `/safe-output/dashboard/index.html`。
- 静态页面和资源使用 GET；治理 API 全部使用 POST。
- 页面包含实时概览、接口风险、日志建议、历史报告、上传报告临时查看和脱敏实验室。
- 历史报告读取限制在 `safe-output.report.directory` 内，只允许合法报告文件。
- 上传报告只在请求内解析，不写入报告目录，不进入历史报告列表。
- Dashboard 不包含 Demo 业务域、小眼睛 raw 明文查看、权限系统、审计、数据库、多租户或公网防护。
- 接入方必须通过内网、网关、Spring Security 或运维策略保护 Dashboard 入口。

### 4.10 Demo 样板系统

Demo 用于展示真实业务系统接入 Safe Output 后的行为。

需求包括：

- Demo 入口为 `http://localhost:8080/index.html`。
- 业务工作台覆盖客户档案、订单履约、支付核验、工单处理、账户安全等业务域。
- 列表和详情接口走真实 Response 脱敏链路。
- `/{id}/raw` 接口走 API ignore，用于明文查看演示，并进入风险统计。
- 工作台总览展示默认规则、YAML 配置、注解、字段 ignore 和 API ignore 的接入说明。
- 脱敏实验室调用真实 `SafeOutputMaskService`，固定连续执行两轮。
- 日志场景页是只读聚合视图，不提供专用触发按钮，不读取原始日志文件。
- Demo 可以引入可选 Dashboard starter；旧 `/demo/report/**` 和 `/demo/mask/**` 兼容接口可保留。
- 前端采用白底业务后台风格，避免深色驾驶舱、粒子背景、玻璃拟态和营销页式 hero。

### 4.11 竞赛 PPT 模板规范

竞赛 PPT 页面生成必须以 `ai-contest-deliverables/assets/` 下已抽离的模板资产为视觉基准，避免把标题左侧标识误写为普通占位 logo，后续生成流程不得依赖回读 `pptmob.pptx`。

需求包括：

- 画布使用 16:9 宽屏，尺寸为 `13.333 x 7.5 in`，背景保持白色或接近白色。
- 内容页采用左上标题 + 中部主体内容结构，标题文本位置参考 `x=0.84in, y=0.59in`，左对齐。
- 标题左侧默认展示模板小蓝条，资产路径为 `ai-contest-deliverables/assets/pptmob-title-marker.svg`。
- 小蓝条填充色为 `#4372C4`，无描边，不加阴影、渐变、图标或文字。
- 小蓝条坐标为 `x=0.70in, y=0.75in`，尺寸为 `w=0.10in, h=0.40in`。
- 后续通过该规范生成的 PPT 内容页标题左侧必须保留该小蓝条，并与模板标题位置保持一致；除封面、结束页或特殊大标题页另有明确设计外，不得用其他占位 logo、装饰线、图标或品牌标识替代。
- 右下角页脚 logo 仍使用 `ai-contest-deliverables/assets/pptmob-default-logo.png` 或同目录 SVG，坐标和尺寸以 `ai-contest-deliverables/assets/pptmob-ppt-spec-prompt.md` 为准。

## 5. 非功能需求

### 5.1 兼容性

- 必须兼容 Java 8。
- 必须兼容 Spring Boot 2.x。
- 不强制宿主系统升级 Spring Boot。
- 不引入 Boot 3 API 作为唯一入口。
- 日志第一阶段支持 Log4j2 2.x，Logback 只预留扩展边界。
- Dashboard 第一阶段只支持 Spring MVC Servlet Web，不支持 WebFlux。

### 5.2 稳定性

- Response 脱敏异常 fail-open，返回原业务结果并记录失败或风险指标。
- 日志脱敏异常 fail-open，输出原 message。
- 报告导出或 Dashboard 聚合异常不得影响业务链路。
- 对循环引用、超大集合、超长日志和高频接口统计必须有上限保护。

### 5.3 性能

- 对象递归支持最大深度和集合数量限制。
- 日志支持整条长度限制、前缀扫描窗口、单值长度限制和规则 key 数量限制。
- 统计采用内存聚合，本地 JSON 报告异步或按需导出。
- 不在请求链路保存明细或同步写入大量数据。

### 5.4 安全

- 不保存敏感原文。
- 不保存完整 response。
- 不保存完整日志 message。
- 不保存单次请求敏感字段明细。
- 不引入 fastjson。
- 不做粗暴全局正则乱扫。
- 银行卡默认不做无上下文全局 fallback。
- API ignore 可以返回明文，但必须进入风险统计。
- Dashboard 默认关闭，且不内置公网访问保护。

## 6. 非目标

Safe Output 当前不做：

- 完整 DLP 平台。
- 数据库、Redis 或分布式统计平台。
- 配置中心热更新。
- 基于用户权限或角色的动态脱敏。
- MyBatis 自动脱敏插件。
- MQ、文件导出、缓存框架自动拦截。
- Logback 实现。
- WebFlux 支持。
- 完整 JSON Parser 日志解析。
- 自然语言敏感信息识别。
- 规则建议自动采纳或自动修改 YAML。
- Dashboard 登录、权限、审计、多租户、数据库或公网防护。
- 保存原始 response、完整日志或敏感样本。

## 7. 验收标准

整体成功标准包括：

- 能在 Java 8 / Spring Boot 2.x Demo 中运行。
- 能生成并安装 `safe-output-spring-boot-starter` Jar。
- 业务系统只引用 starter 即可完成自动装配。
- Response 自动脱敏覆盖 Bean、Map、Collection、Array 和嵌套对象。
- Log4j2 `%safeOutputMsg` 能处理 key-value、JSON-like 和有限 fallback。
- 自定义 String type 能贯穿配置、注解、策略、日志和统计报告。
- 主动脱敏服务支持指定 type、对象规则和强扫描三种模式。
- 报告能展示场景分布、类型分布、Response 风险画像、性能画像和 Log 规则建议。
- Dashboard 默认关闭，启用后可通过本地页面查看实时概览、历史报告、日志建议和实验室。
- Demo 能展示业务工作台、API ignore、脱敏实验室、日志只读聚合和报告路径。
- 所有报告、Dashboard、日志建议和文档不包含敏感原文或完整日志样本。

## 8. 测试要求

- core：内置策略、String type、自定义策略、规则匹配、ignore、对象递归、循环引用和边界输入。
- starter：配置绑定、自动装配、ResponseBodyAdvice、API ignore、默认规则开关和自定义策略 Bean。
- log4j2：key-value 匹配、ignore.keys、regex fallback、超长日志整条跳过、前缀扫描窗口、无 Spring runtime bridge options。
- report：聚合指标、风险画像、Log 规则建议、JSON 导出、安全字段边界。
- dashboard starter：默认不启用、显式启用、POST API、静态资源 GET、报告安全读取、上传不落盘、实验室不返回原始输入。
- demo：业务工作台、raw ignore、实验室两轮结果、日志只读聚合、旧兼容接口和报告文件安全读取。

推荐验证命令以项目实际说明为准：

```bash
cd safe-output && mvn test
cd safe-output && mvn -pl safe-output-demo -am test
cd safe-output && mvn -pl safe-output-spring-boot-starter -am test
cd safe-output && mvn -pl safe-output-dashboard-spring-boot-starter -am test
cd safe-output && mvn verify
```

## 9. 版本差异与当前口径

- MVP 文档中的“严格大陆身份证校验”统一口径已被 R2 分层策略覆盖。
- R2 中部分配置项属于建议或后续预留；当前实现口径以 `.codex-memory` 和实际代码为准。
- R2.5 原始设想中的独立接入说明页、日志触发按钮、独立报告中心和批量性能实验，已被 R2.5 补充 PRD 收敛。
- R2.6 Dashboard 是通用附加包，不承载 Demo 业务工作台或小眼睛明文查看。
- R3 当前聚焦日志长度策略；Demo 竞赛展示看板是后续展示增强方向，不应视为已全部落地的核心能力。

## 10. 来源索引

- `docs/project/prd/safe-output-mvp-prd.md`
- `docs/project/prd/safe-output-r2-prd.md`
- `docs/project/prd/safe-output-r25-prd.md`
- `docs/project/prd/safe-output-r25-supplemental-prd.md`
- `docs/project/prd/safe-output-r26-dashboard-prd.md`
- `docs/project/prd/safe-output-r3-prd.md`
- `docs/project/prd/safe-output-requirements-change-log.md`
- `docs/project/prd/safe-output-requirements-clarifications.md`
- `ai-contest-deliverables/assets/pptmob-ppt-spec-prompt.md`
- `.codex-memory/00-project-current-state.md`
- `.codex-memory/01-module-map.md`
- `.codex-memory/02-core-flow-map.md`
- `.codex-memory/03-decision-and-boundary.md`
- `.codex-memory/04-next-round-handoff.md`

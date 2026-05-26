# 当前项目状态

## 项目目标

Safe Output 是面向 Spring Boot 2.x / Java 8 老项目的通用数据脱敏 starter，目标是在尽量少改业务代码的前提下，对 Response、Log4j2 日志和主动调用场景做敏感信息脱敏，并输出不含敏感原文的聚合统计报告。参赛材料语境中的 `JUP` 指内部使用 Java 8 的统一平台，属于 Safe Output 面向的存量 Java 8 系统背景；正式文档首次出现时应解释为“内部 JUP 统一 Java 8 平台”。

## 第一、二轮完成范围

- 第一轮：Maven 多模块骨架、core 策略与规则、Spring Boot starter 自动装配、Response 脱敏、Log4j2 接入、Demo 和基础测试。
- 第二轮：String 类型标签、自定义策略、主动脱敏、Response 风险画像、性能画像、Log 规则建议、报告扩展、可选 Dashboard starter 和 Demo 前端控制台。

## 已实现能力

- Spring Boot 2.x 自动装配：`safe-output-spring-boot-starter/src/main/resources/META-INF/spring.factories` 注册 `SafeOutputAutoConfiguration`、`SafeOutputMvcAutoConfiguration`。
- 注解模式脱敏：`@Desensitize(type=...)`，由 `SensitiveFieldResolver` 解析字段注解。
- 配置模式脱敏：`safe-output.rules[]` 绑定到 `SafeOutputProperties`，转换为 `MaskRule` 后进入 `MaskRuleMatcher`；`safe-output.rules.default-enabled=false` 可在自动装配时关闭默认规则库。
- 内置默认字段规则库集中在 `DefaultMaskRules`，供 `MaskRuleMatcher` 使用；文档默认规则表应以该类为准。
- 内置策略：`MOBILE`、`ID_CARD`、`BANK_CARD`、`EMAIL`、`CHINESE_NAME`、`ADDRESS`、`PASSWORD`、`DEFAULT`。
- 对象递归脱敏：`ObjectMasker` 支持 Bean、Map、Collection、数组，带最大深度、集合上限、循环引用保护。
- Response 返回值脱敏：`SafeOutputResponseBodyAdvice` 在 JSON 序列化前处理，可配置 `response.body-data-path`。
- ignore：字段级 `ignore.keys` / `ignore.paths`；接口级 `ignore.apis` 命中后明文返回但记录风险事件。
- 日志接入：Log4j2 `%safeOutputMsg`，实现类 `SafeOutputMessagePatternConverter`。
- JSON-like 日志轻量识别：`SafeOutputLogMessageMasker` 用 key-value 正则处理 `"key":"value"`、`key=value` 等片段。
- Spring Boot starter 会把 `safe-output.rules[].keys`、`safe-output.ignore.keys`、自定义 `MaskStrategy`、默认规则开关、`safe-output.log.*` 选项和 report collector 桥接给真实 `%safeOutputMsg`。
- 统计采集、风险分析、报告输出：`MaskMetricsCollector`、`ResponseRiskAnalyzer`、`MaskReportExporter`；开启 report 后真实 Log4j2 脱敏会记录 `LOG` 计数和 fallback 规则线索。
- 报告导出、Dashboard 和 Demo Log 规则建议接口会复用 `safe-output.rules[].keys` 过滤已配置 key，避免重复治理建议；过滤只影响报告建议，不影响 Log4j2 在线脱敏和聚合计数；未过滤的低置信度建议也会生成 `enabled:false` YAML 候选片段供人工复核。
- 可选治理 Dashboard：`safe-output-dashboard-spring-boot-starter` 已抽离为独立附加包，默认关闭；仅 Spring MVC Servlet Web 应用在 `safe-output.dashboard.enabled=true` 时装配，默认入口 `/safe-output/dashboard/index.html`，API 全部使用 POST，覆盖实时概览、接口风险、Log 规则建议、历史报告、临时报告上传和通用脱敏实验室。
- Dashboard 安全边界：不包含 Demo 业务工作台、小眼睛明文查看、权限系统、审计、数据库、多租户或公网防护；接入方需要自行保护入口；历史报告读取限制在 `safe-output.report.directory` 且只允许 `file-prefix-*.json`，上传报告只在请求内解析，不写入报告目录。
- Demo：R2.5/R3 主入口为“业务系统敏感数据治理工作台”，覆盖业务工作台、脱敏实验室和日志场景，并通过导航跳转到独立治理 Dashboard；日志场景页为只读聚合视图，不再提供日志模块专用触发入口。
- R2.5 Demo 业务域：客户、订单、支付、工单、账户 mock 数据源与业务服务，业务接口覆盖 Bean、Map、Collection、嵌套对象，并覆盖 `MOBILE`、`ID_CARD`、`BANK_CARD`、`EMAIL`、`CHINESE_NAME`、`ADDRESS`、`PASSWORD`、`DEFAULT`。
- R3 Demo 业务工作台：客户档案、订单履约、支付核验、工单处理、账户安全已扩展为独立业务页面模型；每个域都有动态 mock 列表、详情和 `/{id}/raw` 明文查看接口，列表/详情走 Response 脱敏，raw 接口走 API ignore 并进入风险统计。
- R3 Demo 后端结构：`DemoApplication` 保留在 `com.safeoutput.demo` 根包作为扫描入口；后端按场景拆为 `business`、`response`、`lab`、`logs`、`report`、`guide` 子包。业务工作台日志模板集中在 `business/DemoBusinessLog`；报告文件安全读取集中在 `report/DemoReportFileStore`，实时/历史 dashboard 组装集中在 `report/DemoReportDashboardAssembler`。
- R2.5/R3 Demo 前端：`safe-output-demo/src/main/resources/static/index.html` 只保留壳层，页面拆到 `static/css/app.css`、`static/js/api.js`、`static/js/views/*` 和 `static/js/components/*`；默认路由为 `#workbench`，侧边栏业务分组为“工作台”，包含总览、客户档案、订单履约、支付核验、工单处理、账户安全，对应 `#workbench`、`#workbench/{customers|orders|payments|tickets|accounts}`；顶部 Dashboard 导航跳转到独立 `/safe-output/dashboard/index.html`，工作台总览直接展示接入说明内容，旧 `#guide` 和 `#workbench/integration` 兼容跳转到 `#workbench`，继续使用本地 `vendor/` 资源。
- R3 Demo 视觉：前端已从深色驾驶舱改为白底业务后台风格，采用浅色侧边栏、白色面板、细边框、蓝/青/绿语义色和代码片段高亮；接入说明卡片不再展示跳转入口。
- R3 Dashboard 抽离：通用治理 Dashboard 已从 demo 模块抽到 `safe-output-dashboard-spring-boot-starter`，复用聚合统计、报告文件安全读取、Log 规则建议和主动脱敏服务；demo 模块保留业务工作台、mock 业务域、raw 明文演示和旧 `/demo/report/**`、`/demo/mask/**` 兼容接口。
- R2.5 报告中心：Demo 可导出、列出、读取配置报告目录内的 JSON 报告，并基于 JSON 聚合字段派生单报告 dashboard；读取限制在 `safe-output.report.directory` 且只允许 `file-prefix-*.json`，不读取任意文件。
- Demo 脱敏实验室：`/demo/mask/by-type`、`/demo/mask/object`、`/demo/mask/strong` 固定连续执行两轮，不再接收前端 iterations 输入；接口返回两条 `{round,result,elapsedNanos,sameAsPrevious}` 记录，前端将纳秒耗时统一转为 ms 展示，用于直观看首次脱敏、二次脱敏稳定性和单轮耗时；业务对象面板支持编辑 `realName`、`mobile`、`name`，用于验证命中脱敏和商品名不误脱敏。
- Demo 日志场景：`/demo/logs/scenarios` 返回 JSON-like、key=value、regex fallback 三类只读场景、当前 `LOG` 脱敏计数、日志规则建议和 YAML 片段；真实日志采集来源于业务工作台接口和脱敏实验室接口，已移除 `/demo/logs` 与 `/demo/logs/scenarios/{id}/trigger`。
- 测试：core、starter、dashboard starter、log4j2、report、demo 均有单元或集成测试。

## 未实现或未确认能力

- 未确认支持 Spring Boot 3.x：当前以 Boot 2.7.18、`spring.factories`、`javax.servlet` 测试为主。
- 未实现强 JSON Parser 日志解析：设计上只做轻量 JSON-like 识别。
- 未实现报告持久化数据库或集中式可视化后端，只输出本地 JSON 快照、Demo 兼容接口和单应用进程内 Dashboard。
- 未实现自动采纳配置建议，`LogRuleSuggestionAnalyzer` 生成的候选规则默认 `enabled: false`。
- 未确认支持跨应用上下文并发隔离的 Log4j2 runtime bridge；当前按单应用 Spring Boot 进程使用。

## 已知风险

- `ObjectMasker` 对 Bean 是原地修改字段；如果调用方复用响应对象实例，需要注意副作用。
- `MaskRuleMatcher.decide` 当前确认优先级是 API ignore / 字段 ignore > 注解 > 配置/默认规则 > regex fallback；后续改动需同步测试该优先级边界。
- 日志和强扫描允许对 `MOBILE` / `ID_CARD` / `EMAIL` 做无上下文 regex fallback；除手机、身份证、邮箱外，不做无上下文全局兜底。
- 日志 `%safeOutputMsg` 在 starter 场景会消费 Spring `safe-output.rules[]`、默认规则开关、自定义策略和 report collector；无 Spring runtime bridge 时回退到内置默认规则。
- 报告 JSON 使用手写序列化，字段较稳定但不是通用 JSON 序列化框架。

## 成熟度判断

当前代码达到可演示、可集成验证的 MVP/R2 成熟度：核心链路清晰、fail-open 边界明确、测试覆盖主要场景。仍不宜视为生产级全场景脱敏网关，第三轮应优先补齐配置一致性、报告/画像可解释性和 Demo 展示质量。

## 第三轮适合扩展方向

- 强化 Demo 竞赛展示看板和交互式验证；R2.5/R2.6 已完成主路径重构和 Dashboard 抽离，后续可继续做浏览器人工视觉验收和截图级 polish。
- 增强 Response 风险画像、接口治理建议和性能分析。
- 增强 Log 规则建议的配置生成、人工确认流和采纳状态。
- 预留 Agent 摘要接口，但继续保持报告不保存敏感原文。

## 竞赛 PPT 转化辅助工程

- 仓库根目录新增 `ppt-template-lab/`，是独立 Node/TypeScript 工程，不属于 Maven reactor，也不改变 Safe Output Java starter 模块职责。
- 当前能力：解析 `ai-contest-deliverables/assets/pptmob.pptx` 的 PPTX XML，生成 `reports/pptmob-design-audit.md` 和 `data/template-profile.json`；并用 `samples/example.md` 生成可编辑 PPTX 样例 `outputs/example-from-md.pptx`。
- 构件策略：截图只作为风格参考，正式组件以结构化 slots、布局和样式定义保存；默认用 `pptxgenjs` 输出 PowerPoint 原生文本和形状，保留后续接入 Presentations 插件的 Renderer 接口空间。
- PPT 标题默认标识已抽离为 `ai-contest-deliverables/assets/pptmob-title-marker.svg`：标题左侧竖向圆角小蓝条，`x=0.70in, y=0.75in, w=0.10in, h=0.40in, fill=#4372C4`；后续内容页生成需直接使用该资产，不能依赖回读 `pptmob.pptx`，也不能用占位 logo 或其他装饰替代。

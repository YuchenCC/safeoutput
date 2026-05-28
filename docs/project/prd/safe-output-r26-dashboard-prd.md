# Safe Output R2.6 可选治理 Dashboard 附加包需求说明书 PRD

版本：v0.1 / R2.6
基准版本：R2.5 Demo PRD 与补充 PRD
适用范围：Demo 与 Dashboard 模块边界收敛、可选 Dashboard 附加包
技术基线：JDK8 + Spring Boot 2.x + Spring MVC
交付形态：`safe-output-dashboard-spring-boot-starter` 可选附加包

---

## Problem Statement

R2.5 已经把 `safe-output-demo` 从功能验证控制台升级为真实接入样板系统。当前 Demo 可以展示业务工作台、Response 脱敏、Log4j2 日志脱敏、主动脱敏实验室、实时治理 Dashboard、历史报告和日志规则建议。

但随着 Dashboard 能力增强，`safe-output-demo` 同时承担了三类职责：

1. 作为模拟业务系统，提供客户、订单、支付、工单、账户等业务接口和 mock 数据。
2. 作为接入样板，展示 YAML、注解、默认规则、ignore、Log4j2 和主动脱敏的接入方式。
3. 作为治理 Dashboard，读取聚合指标、报告目录、历史报告和日志规则建议，并提供前端页面。

这种形态适合竞赛展示，但不利于接入方理解哪些能力属于 Demo，哪些能力可以在自己的应用中直接复用。接入方如果想在生产或测试应用中查看 Safe Output 聚合指标，当前只能参考 Demo 代码自行复制 Dashboard 后端和前端，容易引入定制化代码、路径不一致和安全边界偏差。

R2.6 的问题是：需要把通用治理 Dashboard 从 Demo 中抽离为可选附加包，让 Demo 回归“模拟接入方”，同时让真实接入应用可以通过显式配置启用本地 Dashboard。

## Solution

R2.6 新增 `safe-output-dashboard-spring-boot-starter` 模块，作为 Safe Output 的可选运行时治理 Dashboard 附加包。

Dashboard 附加包面向 Spring MVC Web 应用，默认关闭。接入方显式开启后，应用会暴露一个本地治理页面，用于查看当前进程聚合指标、接口风险、日志规则建议、历史报告、上传报告和通用脱敏实验室。

核心方案如下：

1. 新增 Dashboard starter，不依赖 `safe-output-demo`，只依赖 Safe Output 通用能力。
2. Dashboard 默认关闭，通过 `safe-output.dashboard.enabled=true` 启用。
3. Dashboard 默认路径前缀为 `/safe-output/dashboard`，支持通过 `safe-output.dashboard.path-prefix` 修改。
4. Dashboard 只支持 Spring MVC Web 应用，不支持 WebFlux，也不支持非 Web 应用启动页面。
5. Dashboard API 全部使用 POST；静态页面和静态资源仍按浏览器机制使用 GET。
6. Dashboard 复用当前应用真实的 `MaskMetricsCollector`、`MaskReportExporter`、`SafeOutputMaskService`、规则配置、策略注册表、报告目录和日志规则建议聚合。
7. Dashboard 包含实时概览、接口风险、日志规则建议、历史报告、报告上传查看和脱敏实验室。
8. Dashboard 不包含任何 Demo 业务域，不包含客户、订单、支付、工单、账户工作台，也不包含“小眼睛查看明文”。
9. Dashboard 不内置登录、角色、权限、多租户或审计系统；文档要求接入方通过内网、网关、Spring Security 或运维策略限制访问。
10. Dashboard 不保存、不读取、不展示敏感原文、完整 response 或完整日志 message。
11. `safe-output-demo` 后续可以像普通业务系统一样引入 Dashboard starter 并开启页面；旧 `/demo/report/**` 和 `/demo/mask/**` 第一阶段保留兼容。

## Dashboard Frontend Design

R2.6 Dashboard starter 不只是提供后端 POST API，还必须提供可直接访问的本地治理前端页面。页面入口为 `{path-prefix}/index.html`，默认是 `/safe-output/dashboard/index.html`；CSS、JavaScript 和静态资源按浏览器机制使用 GET 加载，所有治理数据、报告查看、报告上传和实验室请求都通过 `{path-prefix}/api/...` 的 POST API 获取。

Dashboard starter 前端应复用 R2.5/R3 Demo 存量 Dashboard 的成熟布局和视觉经验，尤其是 `safe-output-demo/src/main/resources/static/js/views/reports.js` 中已经验证过的治理信息结构：Hero 摘要、工具栏、实时/历史数据切换、指标卡、图表区、接口风险表、日志建议表、报告文件选择、单报告详情和打印入口。复用的是布局结构、信息层级、组件语义和安全展示边界，不复用 Demo 专属接口路径、业务 mock 数据、小眼睛 raw 明文面板或客户/订单/支付/工单/账户工作台。

Dashboard starter 前端信息架构采用左侧固定导航：

1. 实时概览：当前进程内存聚合快照。
2. 接口风险：Response 风险画像、ignore 接口和慢接口。
3. 日志建议：日志 fallback 聚合线索、置信度和 YAML 候选片段。
4. 历史报告：报告目录、单报告可视化和临时上传报告查看。
5. 脱敏实验室：按类型标签、对象脱敏和强文本扫描三类主动验证。

### Frontend Layout Requirements

实时概览页面必须在首屏展示当前页面标题、刷新动作和至少四个关键指标。指标卡应包含总脱敏次数、Response/Log/Manual 场景计数、失败次数、平均耗时和最大耗时；主体区域展示场景分布、类型 Top、高风险接口摘要、日志建议摘要和性能/异常拆解。图表可以使用轻量 Canvas 或表格替代，但必须保留与 Demo Dashboard 一致的可扫描信息层级。

接口风险页面应以聚合表格为主，展示接口、调用次数、脱敏字段数、类型标签、风险标签和 ignore 原因。高风险接口、ignore 接口和慢接口需要分区展示或通过清晰的 badge 区分。页面不得展示原始 response 或可反推出敏感值的上下文。

日志建议页面应展示建议 key、建议类型、命中次数、置信度、脱敏 evidence 摘要和默认 `enabled:false` 的 YAML 候选片段。低置信度建议可以展示，但文案必须保持“人工复核后启用”的语义，不提供自动采纳按钮。

历史报告页面应包含报告文件列表和单报告详情区域。报告列表展示文件名、大小、修改时间和可查看状态；报告查看通过 POST body 传递文件名。单报告详情复用 Demo Dashboard 的报告视图结构：总览指标、Response/Log/Manual 场景分布、类型 Top、高风险接口、ignore 风险接口、日志规则建议和性能指标。页面不得展示 JSON 报告原文。

报告上传查看应与历史单报告详情复用同一套可视化模型。上传后的报告只在当前请求内解析和展示，不写入报告目录，不进入报告列表，不产生上传历史。上传页不展示原始 JSON 内容，只展示聚合后的 dashboard 模型或可诊断错误。

脱敏实验室页面采用三块并列或响应式堆叠的实验面板：按类型标签、对象脱敏、强文本扫描。每个面板展示输入控件、执行按钮、Round 1 / Round 2 脱敏结果、`elapsedNanos` 转换后的耗时和 `sameAsPrevious` 幂等判断。响应区域不得回显原始输入，只展示脱敏结果和必要规则摘要。

### Frontend Visual Requirements

Dashboard starter 前端沿用 R2.5/R3 Demo 的“白底业务后台 + 治理实证卡片”风格：浅色侧边栏、白色面板、细边框、蓝/青/绿/琥珀/红语义色、可扫描表格和浅底代码块。避免深色指挥舱、大面积紫蓝渐变、粒子背景、玻璃拟态和营销页式 hero。

桌面端优先保证 1920x1080 投屏可读，首屏必须能看到页面标题、主操作、至少四个关键指标和当前场景状态。卡片圆角控制在 6-8px；按钮文案短且面向动作；风险等级和置信度 badge 同时使用颜色和文字表达。打印历史报告时应隐藏侧边导航、刷新按钮、上传控件、Tab 控件和其他交互控件，保留报告标题、文件名、关键指标、图表旁的表格化数据和风险/建议摘要。

### Frontend Safety Requirements

Dashboard starter 前端可以展示聚合指标、脱敏 evidence、脱敏后的实验结果、接口路径、类型标签、置信度和候选 YAML；不得展示原始 response、完整日志 message、原始业务日志文件、敏感命中值、上传报告 JSON 原文或 Demo raw 明文字段。小眼睛明文查看只能留在 Demo 业务工作台，不进入 Dashboard starter 前端。

## User Stories

1. As a 接入方开发者, I want 通过一个可选附加包启用 Safe Output Dashboard, so that 我不需要复制 Demo 中的 Dashboard 代码。
2. As a 接入方开发者, I want Dashboard 默认关闭, so that 引入依赖不会自动暴露管理页面。
3. As a 接入方开发者, I want 通过配置显式开启 Dashboard, so that 我可以在测试、预发或受控生产环境按需使用。
4. As a 接入方开发者, I want 配置 Dashboard 的访问前缀, so that 它可以适配公司已有网关和路由规范。
5. As a 接入方开发者, I want Dashboard 使用当前应用真实脱敏配置, so that 页面展示结果和业务运行行为一致。
6. As a 接入方开发者, I want Dashboard 不依赖 Demo 模块, so that 我的业务应用不需要引入 Demo mock 数据或业务页面。
7. As a 接入方开发者, I want Dashboard 只支持 Spring MVC, so that 第一阶段实现能匹配当前技术栈并保持边界清晰。
8. As a 安全治理人员, I want 查看当前进程实时脱敏统计, so that 我可以判断组件是否正在生效。
9. As a 安全治理人员, I want 查看 Response、Log 和 Manual 场景分布, so that 我可以理解敏感输出主要来自哪些场景。
10. As a 安全治理人员, I want 查看类型标签 Top 排名, so that 我可以识别高频敏感类型。
11. As a 安全治理人员, I want 查看 fail-open 失败次数, so that 我可以发现脱敏异常或报告导出异常。
12. As a 安全治理人员, I want 查看平均耗时和最大耗时, so that 我可以判断脱敏成本是否异常。
13. As a 安全治理人员, I want 查看接口风险统计, so that 我可以识别高风险响应接口。
14. As a 安全治理人员, I want 查看 ignore 接口列表, so that 明文豁免行为可以被人工复核。
15. As a 安全治理人员, I want 查看慢接口提示, so that 我可以评估脱敏处理对接口的性能影响。
16. As a 安全治理人员, I want 查看日志规则建议, so that 我可以发现日志中出现但尚未配置的敏感 key。
17. As a 安全治理人员, I want 看到日志建议的置信度和建议类型, so that 我可以判断是否需要人工采纳。
18. As a 安全治理人员, I want 获取默认关闭的 YAML 候选片段, so that 我可以人工复核后再纳入配置。
19. As a 安全治理人员, I want 查看当前报告目录下的历史报告, so that 我可以分析已导出的聚合快照。
20. As a 安全治理人员, I want 选择单个历史报告渲染 Dashboard, so that 我不用直接阅读 JSON 原文。
21. As a 安全治理人员, I want 上传单个 JSON 报告临时查看, so that 生产环境不开 Dashboard 时也能把报告带到测试环境分析。
22. As a 安全治理人员, I want 上传报告不落盘, so that 临时分析不会引入额外敏感资料管理负担。
23. As a 安全治理人员, I want 上传报告校验扩展名、大小和结构, so that 非报告文件不会被误解析。
24. As a 安全治理人员, I want 上传报告页面不展示 JSON 原文, so that 报告查看仍以聚合可视化为主。
25. As a 接入方开发者, I want 在 Dashboard 中使用按类型标签脱敏实验室, so that 我可以验证内置和自定义类型标签的效果。
26. As a 接入方开发者, I want 在 Dashboard 中使用对象脱敏实验室, so that 我可以验证当前配置规则、注解和 ignore 对结构化对象的影响。
27. As a 接入方开发者, I want 在 Dashboard 中使用强文本扫描实验室, so that 我可以理解主动 strong scan 和 Response 自动脱敏的边界。
28. As a 接入方开发者, I want 实验室固定展示两轮脱敏结果, so that 我可以验证主动脱敏幂等性。
29. As a 接入方开发者, I want 实验室展示每轮耗时, so that 我可以观察单次调用成本。
30. As a 接入方开发者, I want 实验室调用计入 MANUAL 场景, so that 统计结果和真实主动脱敏行为一致。
31. As a 接入方开发者, I want 实验室响应不返回原始输入, so that 用户输入的敏感样例不会进入后端响应或报告。
32. As a Demo 使用者, I want Demo 继续保留业务工作台, so that 我仍能通过客户、订单、支付、工单、账户场景理解接入效果。
33. As a Demo 使用者, I want Demo 继续保留“小眼睛查看明文”, so that API ignore 的演示场景不进入通用 Dashboard。
34. As a Demo 使用者, I want Demo 可以接入新的 Dashboard 附加包, so that Demo 展示路径更接近真实接入方。
35. As a 组件维护者, I want Dashboard API 不使用 GET 查询参数, so that 类型标签、报告名和输入内容不会出现在 URL、浏览器历史或代理日志中。
36. As a 组件维护者, I want Dashboard API 全部使用 POST, so that 查询和实验请求都能通过 body 传参。
37. As a 组件维护者, I want 报告文件名通过请求体传递, so that 文件名不进入 URL path。
38. As a 组件维护者, I want Dashboard 静态资源仍使用 GET, so that 浏览器可以正常加载页面和资产。
39. As a 组件维护者, I want 旧 `/demo/report/**` 和 `/demo/mask/**` 暂时保留, so that R2.5 测试和演示路径不会一次性失效。
40. As a 组件维护者, I want Dashboard 不做规则自动采纳, so that 治理建议仍保持人工确认边界。
41. As a 组件维护者, I want Dashboard 不保存上传报告, so that 第一阶段不引入报告仓库或清理策略。
42. As a 组件维护者, I want Dashboard 不引入数据库, so that 它保持轻量附加包定位。
43. As a 组件维护者, I want Dashboard 不内置权限系统, so that 它不膨胀为治理平台。
44. As a 测试维护者, I want Dashboard 后端接口有集成测试, so that 可选启用、POST API 和安全边界可以回归验证。
45. As a 测试维护者, I want Dashboard 不启用时不会注册 API, so that 引入依赖不会改变应用暴露面。
46. As a 测试维护者, I want Demo 迁移测试保留, so that Demo 业务能力不因 Dashboard 抽离而回退。

## Implementation Decisions

- 新增 `safe-output-dashboard-spring-boot-starter`，作为可选 Dashboard 附加包。
- Dashboard starter 不依赖 `safe-output-demo`，也不包含 Demo 业务域、mock 数据或小眼睛明文查看能力。
- Dashboard starter 依赖 Safe Output 现有 starter、report 能力和 Spring MVC Web 能力；第一阶段不支持 WebFlux。
- Dashboard 通过自动装配启用，条件包括 Web 应用环境和 `safe-output.dashboard.enabled=true`。
- Dashboard 默认路径前缀为 `/safe-output/dashboard`，允许通过 `safe-output.dashboard.path-prefix` 调整。
- Dashboard API 路径统一挂在 `path-prefix + /api/...` 下。
- Dashboard 后端 API 全部使用 POST，包括实时概览、报告列表、报告查看、报告上传和实验室调用。
- Dashboard 静态页面与静态资源继续使用浏览器 GET 加载，不纳入“API 全 POST”约束。
- Dashboard 前端页面随 dashboard starter 一起交付，默认入口为 `path-prefix + /index.html`，不依赖 Demo 静态资源或 Demo controller。
- Dashboard 前端布局复用 R2.5/R3 Demo 存量 Dashboard 的信息架构和视觉规范，包括指标卡、图表区、风险表、日志建议表、报告列表、单报告详情和打印样式。
- Dashboard 前端必须替换 Demo 内置 Dashboard 的接口层，统一调用 `path-prefix + /api/...` POST API，不调用 `/demo/report/**`、`/demo/mask/**` 或 Demo 业务接口作为通用能力。
- 建议 Dashboard API 命名为概览、报告列表、报告查看、报告上传、按类型脱敏、对象脱敏、强文本扫描七类能力。
- 报告查看的文件名通过请求体传递，不放入 URL path 或 query string。
- 报告上传默认支持，不提供单独的 `upload.enabled` 开关；第一阶段上传大小使用代码默认值，后续如有需要再开放配置。
- 报告上传使用临时解析，不默认写入报告目录或其他持久化位置。
- 上传报告只接受合法 JSON 报告快照，必须校验扩展名、大小、结构和关键字段。
- 报告目录读取继续限制在 `safe-output.report.directory` 内，只允许读取合法报告文件，防止路径穿越和任意文件读取。
- Dashboard 实时概览复用当前进程内 `MaskMetricsCollector` 快照。
- Dashboard 接口风险复用 `ResponseRiskAnalysis` 和已有接口风险聚合模型。
- Dashboard 日志规则建议复用 `LogRuleSuggestionAnalyzer`，候选配置默认 `enabled:false`，由接入方人工复核。
- Dashboard 历史报告复用 `MaskReportExporter` 产出的 JSON 报告结构，JSON 报告仍是权威产物。
- Dashboard 脱敏实验室调用当前应用真实 `SafeOutputMaskService`、规则匹配、策略注册和强文本扫描能力。
- Dashboard 脱敏实验室保留三类标准能力：按类型标签脱敏、对象脱敏、强文本扫描。
- Dashboard 脱敏实验室固定执行两轮，用于展示首次结果、二次脱敏稳定性和每轮耗时。
- Dashboard 脱敏实验室调用允许计入 `MANUAL` 场景；第一阶段不扩展核心统计模型区分 `LAB` 来源。
- Dashboard 接口响应不返回用户输入原文；实验室只返回脱敏结果、耗时、稳定性和必要的规则摘要。
- Dashboard 页面、报告查看和上传报告展示不得显示完整原始 response、完整日志 message 或敏感命中值。
- Dashboard 不内置登录、权限、角色、多租户、审计日志或访问控制策略；接入方必须自行保护访问入口。
- `safe-output-demo` 第一阶段保留 `/demo/report/**` 和 `/demo/mask/**` 兼容接口。
- `safe-output-demo` 后续可引入 Dashboard starter 并将前端默认治理入口迁移到通用 Dashboard，但不立刻删除旧接口。
- `.codex-memory` 后续需要在实现模块拆分、运行命令、测试命令或 Demo 能力变化时同步更新。

## Testing Decisions

- 测试以外部可观察行为为主，不断言 Dashboard 内部 DTO 组装细节或前端 DOM 结构。
- Dashboard 自动装配测试需要覆盖默认不启用和显式启用两种情况。
- Dashboard 自动装配测试需要验证非 Web 或非 Spring MVC 场景不会暴露 Dashboard API。
- Dashboard API 测试需要验证通用 API 全部使用 POST；GET 调用业务 API 应被拒绝或不可用。
- Dashboard 静态资源测试需要验证页面和静态资源仍可通过浏览器 GET 正常加载。
- Dashboard 前端验收需要人工或浏览器检查默认入口、左侧导航、实时概览、接口风险、日志建议、历史报告、上传报告和脱敏实验室页面均可访问。
- Dashboard 前端验收需要确认页面不展示 JSON 报告原文、原始 response、完整日志 message、敏感命中值或 Demo raw 明文字段。
- Dashboard 前端验收需要确认 1920x1080 桌面视口下首屏可见页面标题、主操作、至少四个关键指标和当前场景状态。
- Dashboard 概览测试需要覆盖总脱敏次数、场景分布、类型计数、失败次数和耗时指标。
- Dashboard 接口风险测试需要覆盖高风险接口、ignore 接口和慢接口聚合展示。
- Dashboard 日志规则建议测试需要覆盖建议列表、置信度、候选类型和默认关闭的 YAML 片段。
- Dashboard 报告目录测试需要覆盖报告列表、报告查看、非法文件名、路径穿越和非报告文件拒绝。
- Dashboard 上传报告测试需要覆盖合法报告上传、非 JSON 文件拒绝、超大文件拒绝、结构缺失拒绝和不落盘。
- Dashboard 脱敏实验室测试需要覆盖按类型标签脱敏、对象脱敏和强文本扫描。
- Dashboard 脱敏实验室测试需要覆盖两轮结果、`elapsedNanos`、`sameAsPrevious` 和不返回原始输入。
- Dashboard 脱敏实验室测试需要验证调用会进入 `MANUAL` 场景统计。
- Demo 回归测试需要覆盖业务工作台、小眼睛明文查看、旧 `/demo/report/**` 和旧 `/demo/mask/**` 兼容接口。
- 回归至少运行 Dashboard 模块测试和 Demo 模块测试；如果改动 starter、report 或 core，需要同步运行受影响模块测试。

## Out of Scope

- 不把 Safe Output 改造成完整安全治理平台。
- 不新增数据库、配置中心、登录鉴权、角色权限、多租户或审计系统。
- 不内置公网访问保护；接入方必须自行通过网关、内网或 Spring Security 等方式保护入口。
- 不支持 WebFlux。
- 不支持非 Web 应用启动 Dashboard。
- 不把 Demo 业务工作台迁入 Dashboard。
- 不把客户、订单、支付、工单、账户 mock 数据迁入 Dashboard。
- 不把“小眼睛查看明文”迁入 Dashboard。
- 不删除第一阶段旧 `/demo/report/**` 和 `/demo/mask/**` 兼容接口。
- 不保存上传报告，不做上传报告历史列表。
- 不做多报告趋势分析、报告合并、报告分享链接或远程拉取生产报告。
- 不展示 JSON 报告原文。
- 不读取、展示或导出原始业务日志文件内容。
- 不保存原始 response、完整日志 message 或敏感命中值。
- 不实现规则建议自动采纳或自动修改 YAML。
- 不改变 `MaskRuleMatcher` 规则优先级。
- 不改变 Response 脱敏 fail-open 边界。
- 不改变日志 regex fallback 的安全边界。

## Further Notes

- R2.6 的核心不是新增脱敏算法，而是收敛 Demo 与 Dashboard 的模块边界。
- Dashboard 是本地运行时治理视图，不是集中式治理平台。
- Dashboard 允许接入生产应用，但必须默认关闭，并由接入方负责访问控制。
- 报告 JSON 继续作为 canonical report artifact；Dashboard 只是聚合展示层。
- API 全 POST 是 Dashboard 后端 API 约束，不适用于静态页面和静态资源。
- 上传报告的主要使用场景是：生产环境只启用 report，不启用 Dashboard；运维或开发把 JSON 报告带到测试环境的 Dashboard 临时查看。

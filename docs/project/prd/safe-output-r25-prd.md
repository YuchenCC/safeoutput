# Safe Output 数据脱敏组件 R2.5 Demo 接入样板系统需求说明书 PRD

版本：v0.46 / R2.5
基准版本：R2 PRD v0.4
适用范围：Java 通用数据脱敏组件 R2 与 R3 之间的 Demo 增强迭代
技术基线：JDK8 + Spring Boot 2.x + Log4j2 2.x
交付形态：`safe-output-demo` 接入样板系统
实现校准补充：`docs/project/prd/safe-output-r25-supplemental-prd.md`

---

## Problem Statement

当前 Safe Output 已经完成 Response 脱敏、Log4j2 日志脱敏、主动脱敏、统计采集、风险画像、规则建议和本地 JSON 报告快照能力。现有 Demo 能验证这些能力，但整体形态仍偏向功能 API 和控制台集合，不像一个真实业务系统接入组件后的样板。

对接入方、评审者和后续开发者来说，现有 Demo 存在几个问题：

1. 业务语境较弱，接口以 `bean`、`map`、`list`、`nested` 等技术形态为主，不能充分说明组件在真实客户、订单、工单、支付等业务场景中的价值。
2. 接入方式展示不完整，YAML、注解、默认规则、ignore、Log4j2 PatternConverter 和主动脱敏服务虽然已经存在，但缺少一个能把“业务场景”和“接入选择”对应起来的工作台总览。
3. 脱敏实验能力较分散，当前能验证主动脱敏和幂等，但还不能系统展示不同类型、强文本扫描、对象脱敏和单轮耗时。
4. 报告能力已有 JSON 快照，但 Demo 还不能在治理 Dashboard 中区分当前进程实时聚合与历史报告快照，并可视化展示风险、Top 排名、遗漏建议和性能指标。
5. 如果直接把 JSON 报告交给用户，机器可读性强但展示效果弱；如果第一版引入后端 PDF 生成，又会增加中文字体、分页、图表渲染和额外依赖复杂度。

R2.5 的目标是在不提前侵入 R3 核心能力的前提下，把 Demo 重写为一个真实接入系统样板，让它既能作为竞赛展示，也能作为接入方理解 Safe Output 的可运行参考。

---

## Solution

R2.5 将 `safe-output-demo` 从“功能验证控制台”升级为“真实接入样板系统”。

Demo 应模拟一个已经接入 Safe Output 的业务系统。该系统拥有自己的 mock 数据源、业务接口、日志输出、主动脱敏工具、治理 Dashboard 和前端控制台。Safe Output 作为这个业务系统的输出侧脱敏能力存在，而不是页面上的孤立工具按钮。

核心方案如下：

1. 建立模拟业务域，优先覆盖客户、订单、工单、支付或账户等常见高敏场景。
2. 用业务字段覆盖内置所有类型标签，包括 `MOBILE`、`ID_CARD`、`BANK_CARD`、`EMAIL`、`CHINESE_NAME`、`ADDRESS`、`PASSWORD`、`DEFAULT`。
3. 通过业务接口展示 Response 自动脱敏，覆盖 Bean、Map、Collection、嵌套对象和接口 ignore。
4. 通过工作台总览展示 YAML、注解、默认规则、字段级 ignore 和 API ignore 的适用场景；Log4j2 PatternConverter 和 `SafeOutputMaskService` 分别在日志场景和脱敏实验室中体现。
5. 通过脱敏实验室展示按类型脱敏、对象主动脱敏、强文本扫描、固定两轮幂等验证和单轮耗时。
6. 通过日志场景页面只读展示 JSON-like、key=value、regex fallback 三类真实 Log4j2 聚合结果、统计和规则建议；日志来源于业务工作台接口和脱敏实验室接口，不再提供日志页专用触发按钮。
7. 通过治理 Dashboard 导出脱敏统计 JSON 报告、列出当前报告目录中的 JSON 文件、读取单个报告并渲染实时/历史两类可视化报表。
8. JSON 报告继续作为权威数据源；前端可视化和浏览器打印 PDF 作为派生展示能力，不在 R2.5 引入后端 PDF 生成依赖。

---

## User Stories

1. As a 业务系统开发者, I want 看到一个真实业务系统如何接入 Safe Output, so that 我可以把 Demo 当作接入参考。
2. As a 业务系统开发者, I want Demo 有自己的 mock 数据源, so that 我能区分业务数据生产和脱敏组件处理边界。
3. As a 业务系统开发者, I want 查看客户列表和客户详情接口, so that 我能看到姓名、手机号、邮箱、身份证等字段的 Response 脱敏效果。
4. As a 业务系统开发者, I want 查看订单详情接口, so that 我能看到收货地址、银行卡、客户信息等嵌套对象脱敏效果。
5. As a 业务系统开发者, I want 查看工单或客服备注接口, so that 我能看到非结构化文本和业务备注如何被安全处理。
6. As a 业务系统开发者, I want 查看支付或账户场景, so that 我能看到银行卡、密码、默认兜底类型等高敏字段的展示方式。
7. As a 接入方, I want Demo 覆盖所有内置类型标签, so that 我能一次性确认组件预设策略的输出效果。
8. As a 接入方, I want 每个字段能说明规则来源, so that 我知道该字段由 YAML、注解、默认规则还是 ignore 影响。
9. As a 接入方, I want 看到 YAML 配置规则适合哪些字段, so that 老系统可以通过少量配置接入。
10. As a 接入方, I want 看到注解规则适合哪些字段, so that 新 DTO 或歧义字段可以精确声明类型标签。
11. As a 接入方, I want 看到默认规则库命中的字段, so that 我理解 starter 的低侵入默认能力。
12. As a 接入方, I want 看到字段级 ignore 的场景, so that 商品名、备注等不应脱敏字段可以被豁免。
13. As a 接入方, I want 看到接口级 ignore 的场景, so that 必须返回明文的接口可以被明确豁免并进入风险统计。
14. As a 接入方, I want 看到 Log4j2 `%safeOutputMsg` 的真实运行聚合结果, so that 我知道日志脱敏不是 controller 手写替换。
15. As a 接入方, I want 看到主动脱敏服务的调用入口, so that 我可以在业务代码、导出前处理或临时工具中复用统一脱敏能力。
16. As a 安全治理人员, I want 查看业务接口的风险画像, so that 我能识别哪些接口脱敏量高或存在 ignore 风险。
17. As a 安全治理人员, I want 查看日志规则建议, so that 我能发现日志中出现但尚未配置的敏感 key。
18. As a 安全治理人员, I want 查看报告中的 Top 排名, so that 我能快速识别主要敏感类型和高频场景。
19. As a 安全治理人员, I want 查看遗漏清单或建议列表, so that 我能安排人工确认和后续治理。
20. As a Demo 使用者, I want 手动输入值和类型标签执行脱敏, so that 我能快速理解不同策略的格式。
21. As a Demo 使用者, I want 对同一个值连续脱敏两次, so that 我能验证主动脱敏幂等性。
22. As a Demo 使用者, I want 对对象执行主动脱敏, so that 我能验证对象递归能力不仅存在于 Response 场景。
23. As a Demo 使用者, I want 对非结构化文本执行强扫描, so that 我能理解主动 strong scan 和普通 Response 脱敏的边界。
24. As a Demo 使用者, I want 查看每轮主动脱敏耗时, so that 我能观察不同输入在单次调用下的成本。
25. As a Demo 使用者, I want 查看不同日志识别样例的聚合说明, so that 我能看到 JSON-like、key=value 和 fallback 的差异。
26. As a Demo 使用者, I want 查看当前报告目录下有多少 JSON 文件, so that 我能确认报告导出是否成功。
27. As a Demo 使用者, I want 点击某个 JSON 报告查看可视化图表, so that 我不用直接阅读原始 JSON。
28. As a Demo 使用者, I want 从历史报告页面打印报告, so that 我可以把报告用于演示和评审材料。
29. As a 组件维护者, I want JSON 报告继续作为权威数据源, so that 后续 Agent 摘要、趋势分析和 CI 比较能复用同一产物。
30. As a 组件维护者, I want R2.5 不引入后端 PDF 依赖, so that Demo 重写不会扩大核心组件维护成本。
31. As a 组件维护者, I want Demo 不读取或展示原始业务日志文件, so that 日志展示不会突破不保存敏感原文的安全边界。
32. As a 测试维护者, I want 主要 Demo 接口有集成测试, so that 后续页面重构不会破坏可验证行为。
33. As a 评审者, I want 首屏看到的是一个接入系统而不是工具集合, so that 我能快速理解 Safe Output 的业务价值。
34. As a 评审者, I want 业务工作台、脱敏实验室和报告导出都能从页面直接触发, so that 展示过程不依赖命令行或人工解释源码。
35. As a 后续 R3 实现者, I want R2.5 和 R3 日志长度策略边界清晰, so that Demo 重写不会提前混入 R3 核心需求。

---

## Implementation Decisions

- R2.5 主要修改 Demo 模块，目标是重写 Demo 业务形态和前端控制台；核心脱敏模块、Log4j2 模块、report 模块和 starter 模块只在 Demo 需要只读查询或数据结构复用时做最小改动。
- Demo 应拆出稳定的模拟业务层，包含 mock 数据源、业务服务、Web 接口和前端展示 DTO，避免继续把所有样例对象散落在 controller 中。
- 模拟业务域至少覆盖客户、订单、工单和支付或账户场景；字段设计必须覆盖所有内置类型标签，并包含歧义字段、ignore 字段和嵌套对象。
- 工作台总览的接入方式说明应以“业务场景 -> 接入方式 -> 示例接口 -> 字段 -> 规则来源 -> 输出效果”为核心结构，不做长篇静态说明。
- Response 演示必须保留 fail-open、安全统计和 ignore 边界，不通过 Demo 手写脱敏结果替代真实 `ResponseBodyAdvice` 行为。
- 日志演示必须通过业务工作台接口和脱敏实验室接口中的真实 Log4j2 logger 触发 `%safeOutputMsg`，日志场景页只读展示聚合结果，不在 Demo controller 中手工 seed 日志建议。
- 主动脱敏实验室继续使用 `SafeOutputMaskService`，覆盖按类型、对象、强文本和固定两轮幂等场景，并展示每轮耗时。
- 治理 Dashboard 新增 Demo 侧报告文件浏览能力，读取 `safe-output.report.directory` 下的 JSON 报告文件，展示文件名、大小、修改时间和报告数量。
- 单报告可视化以 JSON 报告内容为输入，展示总量、场景分布、类型 Top、高风险接口、ignore 风险接口、日志规则建议、性能指标和遗漏或治理建议。
- JSON 报告是权威产物；页面可视化和浏览器打印 PDF 是派生产物。R2.5 不引入 OpenPDF、iText、Flying Saucer 或类似后端 PDF 生成依赖。
- 前端报告页面应提供打印入口，并提供适合浏览器打印的样式；打印产物不应包含原始敏感值。
- 报告文件读取接口必须限制在配置的报告目录内，只允许读取 JSON 报告快照，避免路径穿越和任意文件读取。
- Demo 页面应优先成为一个业务系统控制台，默认首屏展示治理 Dashboard；工具型能力放入脱敏实验室，报告文件能力收敛到 Dashboard 的历史报告 Tab。
- Demo 前端可以继续使用本地静态资源和 Chart.js，不引入复杂前端构建链路。
- 文档、页面说明、报告展示和测试数据不得包含真实敏感原文；mock 数据允许使用虚构样例，但报告中只能保存聚合信息或脱敏 evidence。
- R2.5 不提前实现 R3 日志长度策略、Agent 摘要、规则自动采纳流、配置中心、数据库持久化或权限系统。

---

## Testing Decisions

- 测试以外部可观察行为为主，避免断言 Demo 内部 mock 数据组织方式或前端 DOM 细节。
- Demo 集成测试需要覆盖主要业务接口，验证 Response 自动脱敏对 Bean、集合、嵌套对象和 Map 结构生效。
- Demo 集成测试需要覆盖内置类型标签的代表字段，至少确保手机号、身份证、银行卡、邮箱、姓名、地址、密码和默认类型有可验证样例。
- 接入方式测试需要覆盖 YAML 规则、注解规则、默认规则、字段级 ignore、接口级 ignore、Log4j2 PatternConverter 和主动脱敏服务。
- 日志演示测试需要验证业务接口或实验室接口能产生 LOG 场景统计和日志规则建议，不要求读取原始日志文件，也不要求日志页提供触发入口。
- 实验室测试需要覆盖按类型脱敏、对象主动脱敏、强文本扫描和连续两轮脱敏结果结构。
- Dashboard 历史报告测试需要覆盖导出 JSON、列出报告文件、读取单个报告、单报告 dashboard 数据，以及非法文件名或路径穿越输入被拒绝。
- 前端验收以人工运行 Demo 为主，确认业务工作台、工作台总览接入说明、日志只读聚合、脱敏实验室和治理 Dashboard 可以完整演示。
- 回归测试至少运行 Demo 模块测试；如果改动 starter、report 或 log4j2 模块，需要同步运行受影响模块测试。

---

## Out of Scope

- 不改 Safe Output 的核心定位：仍是输出侧脱敏组件，不变成完整安全治理平台。
- 不新增数据库、配置中心、登录鉴权、多租户或权限系统。
- 不新增后端 PDF 生成依赖，不保证服务端直接产出 PDF 文件。
- 不读取、展示或导出原始业务日志文件内容。
- 不把报告改成保存原始 response、完整日志 message 或敏感命中值。
- 不新增完整 JSON Parser 日志解析能力。
- 不改变 `MaskRuleMatcher` 规则优先级。
- 不改变 Response 脱敏 fail-open 边界。
- 不改变日志无上下文 regex fallback 的安全边界。
- 不提前实现 R3 `max-scan-length` 日志长度策略。
- 不实现规则建议自动采纳或自动修改 YAML。
- 不把 Demo mock 数据抽象为可复用生产数据源。

---

## Further Notes

- R2.5 的成功标准不是新增更多脱敏算法，而是让现有能力通过一个真实接入系统被完整解释和验证。
- Demo 的页面语言应围绕业务动作、接入方式和治理结果展开，避免只展示底层 API 名称。
- 报告 JSON 的机器可读性是后续能力的基础，应保持为 canonical report artifact。
- PDF 展示需求优先通过浏览器打印历史报告满足，待 HTML 报告结构稳定后，再考虑是否在后续版本引入服务端 PDF 渲染。
- 如果 R2.5 实现过程中发现必须修改模块职责、核心调用链、设计边界、运行命令、测试命令或交接信息，需要同步修订 `.codex-memory/` 对应文档。

# Safe Output 参赛项目总述

## 1. 项目定位

Safe Output 是一个由 AI 完整参与研发的 Java 8 / Spring Boot 2.x 输出侧脱敏 starter，同样适配内部 JUP 统一 Java 8 平台，面向存量业务系统中 response、日志和主动脱敏场景的敏感信息治理。

本项目的核心目标是制作一个脱敏依赖组件，在尽量少改业务代码的前提下，把常见敏感输出治理能力封装成可接入、可验证、可观测的组件能力，因其适配接入成本低，特别适合老系统。业务系统引入 starter 后，可以通过自动装配、字段注解、配置规则和 Log4j2 pattern converter，在 response 返回、日志输出和业务主动调用时复用统一脱敏规则与策略。

本项目使用“纯 AI 研发”的工程化过程：AI 参与了 PRD 拆解、issue 化、Maven 多模块实现、测试补齐、文档同步、Demo 构建和项目记忆维护。Safe Output 因此既是一个数据脱敏组件，也是一份 AI 原生研发方式在 Java 基础组件研发上的实践样本。

## 2. 真实问题背景与痛点调研

在大量 Java 8 / Spring Boot 2.x 老系统中，敏感信息并不只存在于数据库里，也会通过接口响应、业务日志、排查日志、导出报告和跨系统调用过程离开应用。考虑到业务系统，尤其是老系统，在设计文档缺失、代码可维护性不高背景下，若需要补齐输出侧治理能力，在不重写接口层是最优解。

实际治理中常见痛点包括：

- 接口、字段和日志清单依赖人工梳理，容易遗漏新增接口和新字段。
- 各业务系统脱敏写法不统一，常见做法是在 Controller、DTO 转换或日志打印前临时插入处理逻辑，后续维护成本高。
- 日志链路常被忽略，上下游排查日志、JSON-like 日志和 key-value 日志中可能继续出现敏感输出。
- 团队交接后，很难判断哪些字段已经治理、哪些接口因为业务原因允许明文返回、哪些日志 key 仍需要补规则。
- 安全组件如果保存原始 response、完整日志或敏感样本，本身会变成新的泄露源。
- 脱敏不能影响业务可用性，异常时需要 fail-open，性能边界也必须可控。

Safe Output 针对这些痛点，把“输出侧脱敏”收敛为组件能力：默认规则覆盖常见字段，配置规则覆盖业务差异，ignore 记录风险但不阻断业务，报告和 Dashboard 只基于聚合信息做治理复盘。

## 3. 目标用户

Safe Output 面向需要治理敏感输出的 Java 存量系统研发团队，尤其适合以下场景：

- 系统仍运行在 Java 8 / Spring Boot 2.x（JUP），短期内无法升级到新框架。
- 希望以 starter 方式接入脱敏能力，不想在大量 Controller 或日志打印点手工改造。
- 需要同时覆盖 response、Log4j2 日志和业务主动脱敏调用。
- 需要通过报告、风险画像和 Dashboard 向团队说明治理现状。
- 需要一套可运行 Demo 和接入文档，帮助业务系统快速验证效果。

## 4. 实现难点和核心治理闭环

Safe Output 的实现难点不在于单个手机号或邮箱掩码函数，而在于把脱敏能力做成可复用、可配置、可观测且不保存敏感原文的组件服务。

技术实现上的主要难点包括：

- Response 对象递归脱敏要处理 Bean、Map、集合、数组、嵌套对象、最大深度、集合上限和循环引用，且异常时不能影响业务接口返回。
- 规则命中要统一注解、配置规则、默认字段规则、字段 ignore、接口 ignore 和 regex fallback，并保持明确优先级。
- Log4j2 日志治理只能做轻量 JSON-like / key-value 识别和有限 fallback，不能粗暴全局正则扫描，也不引入重 JSON Parser。
- 主动脱敏需要复用同一套类型标签、规则、策略和统计模型，避免 response、日志、手动调用三套逻辑分叉。
- 报告只能保存聚合指标、类型、计数、接口标识、风险画像和脱敏 evidence，不能保存原始 response、完整日志或敏感值。
- Dashboard 要体现治理闭环，但必须保持可选附加包定位
- 组件需要兼容 Java 8 / Spring Boot 2.x，继续使用 `spring.factories` 自动装配，避免引入 Boot 3 API 和不必要依赖。

如果完全纯手工实现，这类组件不是一个简单工具类的工作量，而是需要同时完成多模块工程、Spring Boot starter 自动装配、Log4j2 插件、对象递归规则引擎、报告模型、Demo 前后端、测试用例、接入文档和边界文档。Safe Output 的参赛价值之一，是展示 AI 如何在这些工程环节中持续产出并保持约束一致。

组件治理覆盖的基础场景包括：

- Response 自动脱敏：通过 Spring MVC `ResponseBodyAdvice` 在 JSON 序列化前处理返回值。
- Log4j2 日志同步治理：通过 `%safeOutputMsg` 处理日志 message 中的 JSON-like、key-value 和有限 fallback 内容。
- 主动脱敏：通过 `SafeOutputMaskService` 支持业务显式处理字符串、对象和强扫描文本。
- 聚合报告：记录脱敏次数、类型分布、接口风险、ignore、失败和性能画像。
- Dashboard 展示：可选启用治理视图，查看实时概览、历史报告、风险画像和规则建议。

核心治理闭环为：

```text
业务接口产生敏感输出
  -> Response 自动脱敏
  -> Log4j2 日志同步治理
  -> 主动脱敏复用统一策略
  -> 聚合统计形成风险画像
  -> 报告/Dashboard 输出治理建议
  -> 修订、补充敏感配置，健壮脱敏能力
```

## 5. 已完成交付能力

当前 Safe Output 已完成从核心规则到 Demo 展示的端到端交付：

- `safe-output-core`：提供类型标签、默认规则、脱敏策略、规则匹配、字段注解、对象递归和主动脱敏基础能力。
- `safe-output-spring-boot-starter`：提供 Spring Boot 2.x 自动装配、配置绑定、ResponseBodyAdvice、ignore 和报告 Bean 创建。
- `safe-output-log4j2`：提供 Log4j2 `%safeOutputMsg` 日志脱敏，支持 JSON-like、key-value 和有限 regex fallback。
- `safe-output-report`：提供聚合指标、Response 风险画像、性能画像、本地 JSON 报告和 Log 规则建议。
- `safe-output-dashboard-spring-boot-starter`：提供可选治理 Dashboard，默认关闭，启用后展示实时概览、接口风险、日志规则建议、历史报告和脱敏实验室。
- `safe-output-demo`：提供可运行的业务工作台、脱敏实验室、日志场景、报告中心和接入说明。

Demo 可验证入口：

- 业务工作台：`http://localhost:8080/index.html`
- 可选治理 Dashboard：`http://localhost:8080/safe-output/dashboard/index.html`

## 6. 项目亮点

**覆盖场景多。** Safe Output 同时覆盖 Response 脱敏、Log4j2 日志脱敏、主动脱敏、聚合报告、风险画像、规则建议和 Demo 验证，形成输出侧治理闭环。

**接入文档齐全，并提供接入 Demo。** 项目包含 README、组件接入手册、项目代码概览、上下文文档和 Demo 控制台，评审和业务研发都可以从文档和可运行示例理解接入方式。

**可视化 Dashboard 支持报告分析。** 可选 Dashboard 能读取当前进程聚合指标和本地 JSON 报告快照，展示风险画像、类型分布、日志建议和脱敏实验室结果，帮助研发团队从“是否脱敏”进一步走到“哪里风险高、哪里需要补规则”。

**敏感风险建议和配置生成建议。** 组件基于日志 fallback 线索和聚合统计生成规则建议，并输出候选配置片段。候选规则默认关闭，需要人工复核后采纳，避免组件自动修改线上治理策略。

**AI 原生研发过程可追踪。** 项目通过 PRD、issue、memory、上下文文档、测试和交接说明约束 AI 多轮产出，避免安全类项目中常见的边界遗忘、能力夸大和文档漂移。

## 7. 能力边界

Safe Output 的能力边界分为默认支持、配置后支持和治理建议边界。

默认支持的能力包括：

- 常见字段名的 Response 脱敏，例如手机号、身份证号、银行卡号、邮箱、密码等明确字段。
- 字段注解脱敏，通过 `@Desensitize(type = "...")` 指定类型标签。
- 内置脱敏策略，包括 `MOBILE`、`ID_CARD`、`BANK_CARD`、`EMAIL`、`CHINESE_NAME`、`ADDRESS`、`PASSWORD`、`DEFAULT`。
- Log4j2 JSON-like / key-value 日志脱敏。
- 手机号、身份证号、邮箱等有限 regex fallback。
- 聚合统计采集，包括脱敏次数、类型分布、场景分布、失败和耗时信息。

配置后支持的能力包括：

- 自定义字段规则，通过 `safe-output.rules[]` 配置字段名、路径和类型标签。
- 路径规则和字段 ignore，通过 `rules[].paths`、`ignore.keys`、`ignore.paths` 精细控制字段处理。
- 接口 ignore，通过 `ignore.apis` 允许特定接口明文返回，同时进入风险统计。
- 自定义 `MaskStrategy`，业务可扩展自己的类型标签和脱敏算法。
- 关闭默认规则库，通过 `safe-output.rules.default-enabled=false` 只保留显式配置和注解规则。
- 报告导出，通过 `safe-output.report.enabled=true` 输出本地 JSON 聚合快照。
- 可选 Dashboard，通过额外引入 dashboard starter 并配置 `safe-output.dashboard.enabled=true` 启用。
- 主动脱敏服务，业务代码可显式调用统一脱敏能力处理字符串、对象或非结构化文本。

治理建议类能力的边界包括：

- 报告、Dashboard 和规则建议只基于聚合指标、类型分布、接口标识、脱敏 evidence 和日志 key 线索。
- 组件不保存原始 response、完整日志或敏感值。
- Log 规则建议只输出候选配置，默认 `enabled:false`，不自动采纳、不自动改写 YAML。
- Dashboard 是可选附加包，不提供登录、权限、审计、数据库、多租户或公网防护，需要接入方自行保护入口。
- 当前主要验证 Java 8 / Spring Boot 2.x，Spring Boot 3.x 兼容性未确认。

## 8. 下一步

后续增强会继续围绕“基于聚合信息治理敏感输出”，不突破不保存敏感原文的安全边界。

- AI 风险摘要：基于报告快照、接口风险画像、类型分布、ignore 和失败统计生成面向研发负责人的摘要，帮助快速判断治理重点。
- AI 配置建议：基于日志规则线索、接口风险和类型分布生成更完整的候选配置建议，仍保持人工复核后采纳。
- 建议确认流：围绕规则建议增加确认、忽略、已处理等状态表达，让治理建议从报告信息进一步变成可跟踪事项。
- 老系统接入样例：补充更多 Java 8 / Spring Boot 2.x 存量服务接入模板，降低业务系统试用成本。
- Demo 和评审材料增强：继续完善演示脚本、截图材料和浏览器验证路径，让评委和接入方更快看到端到端效果。

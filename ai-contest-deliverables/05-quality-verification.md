# Safe Output 质量验证材料

## 1. 文档定位

本文用于说明 Safe Output 当前质量验证闭环。重点不是逐个罗列测试方法，而是说明项目已经覆盖哪些模块级、集成级和端到端验证场景，以及仍有哪些需要在正式演示前人工确认的边界。

结论先行：当前仓库不是只收集了单元测试。`safe-output-core`、`safe-output-log4j2`、`safe-output-report` 中有偏模块契约的单元测试；`safe-output-spring-boot-starter`、`safe-output-dashboard-spring-boot-starter`、`safe-output-demo` 中有 Spring Boot 自动装配、`MockMvc` 和 `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` 集成测试，已经覆盖服务启动后的 HTTP 联调路径。

同时，本文不把自动化集成测试夸大成完整人工黑盒验收。正式答辩或交付前，仍建议补一轮真实启动 demo 后的浏览器黑盒走查。

## 2. 推荐验证命令

从 `safe-output/` 目录执行：

```powershell
mvn test
mvn -pl safe-output-demo -am test
mvn -pl safe-output-spring-boot-starter -am test
mvn -pl safe-output-dashboard-spring-boot-starter -am test
mvn verify
```

命令口径：

- `mvn test`：快速全量测试，覆盖所有 reactor 模块的单元测试和集成测试。
- `mvn -pl safe-output-demo -am test`：验证 demo 端到端场景，包括业务工作台、Response、Log、Manual、Report 和 Dashboard 入口。
- `mvn -pl safe-output-spring-boot-starter -am test`：验证业务 starter 自动装配、配置绑定、ResponseBodyAdvice、Log4j2 bridge 和 report bean。
- `mvn -pl safe-output-dashboard-spring-boot-starter -am test`：验证可选 Dashboard starter 的自动装配、POST API、静态资源、报告安全读取和脱敏实验室。
- `mvn verify`：发布前完整验证，额外包含 Java 8 API 检查、Checkstyle、打包等 verify 阶段检查。

## 3. 测试覆盖结构

| 模块 | 覆盖重点 | 质量边界 |
|---|---|---|
| `safe-output-core` | 内置策略、规则匹配、注解解析、策略注册、对象递归、主动脱敏服务 | 保持 Java 8 兼容；字段 ignore、路径匹配、未知 type fallback 和循环引用边界可验证 |
| `safe-output-log4j2` | `%safeOutputMsg` converter、JSON-like/key-value 日志脱敏、regex fallback、开关和长度限制 | 日志只做轻量识别；超长 message fail-open；fallback 只覆盖当前支持的低误伤类型 |
| `safe-output-report` | 聚合计数、场景分布、Response 风险画像、报告导出、保留数量、导出失败记录 | 报告只保存聚合信息或脱敏 evidence，不保存原始 response、完整日志或敏感值 |
| `safe-output-spring-boot-starter` | 自动装配、属性绑定、规则构造、ResponseBodyAdvice、API ignore、Log4j2 runtime bridge、report 装配 | Response 脱敏异常 fail-open；API ignore 明文返回但进入风险统计；starter 仍基于 Spring Boot 2.x `spring.factories` |
| `safe-output-dashboard-spring-boot-starter` | Dashboard 默认关闭、启用后静态入口、自定义 path prefix、POST-only API、历史报告读取、上传临时查看、实验室两轮脱敏 | 报告读取限制在配置目录；上传不落盘；Dashboard 不自带鉴权、审计、数据库、多租户或公网防护 |
| `safe-output-demo` | 随机端口 Spring Boot 集成测试，覆盖工作台、业务域详情、raw 明文接口、日志聚合、报告导出、Dashboard 入口和脱敏实验室 | Demo 证明端到端链路可运行；raw/API ignore 仅用于演示兼容明文接口，并必须进入风险统计 |

关键自动化联调证据包括：

- `DemoResponseIntegrationTest` 使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 和 `TestRestTemplate` 调用真实 HTTP endpoint，覆盖 `/demo/business/**`、`/demo/mask/**`、`/demo/report/**`、`/safe-output/dashboard/**` 等路径。
- `DashboardWebIntegrationTest` 和 `DashboardCustomPrefixIntegrationTest` 启动随机端口 Web 应用，验证 Dashboard POST API、静态资源、报告读取、上传、实验室和自定义路径前缀。
- `SafeOutputResponseBodyAdviceIntegrationTest` 用 `MockMvc` 验证 Bean、Map、List、`ResponseEntity`、`body-data-path`、API ignore 和 fail-open。
- `SafeOutputLog4j2StarterIntegrationTest` 通过真实 Log4j2 `PatternLayout` 验证 starter 配置如何驱动 `%safeOutputMsg`。

## 4. 关键验收场景

| 场景 | 已有验证方式 | 验收点 |
|---|---|---|
| Response 脱敏 | starter `MockMvc` + demo 随机端口 HTTP 测试 | Bean、Map、List、嵌套对象、注解、配置规则、默认规则和 `body-data-path` 生效 |
| fail-open | core、starter、report 测试 | Response advice 或报告导出异常不阻断业务主链路；异常指标可记录 |
| ignore 风险统计 | starter 和 demo 集成测试 | 字段 ignore 跳过字段脱敏；API ignore 返回明文但记录 `ignored=true` 风险事件 |
| Log4j2 脱敏 | log4j2 单元测试 + starter Log4j2 集成测试 + demo 日志场景 | JSON-like/key-value 脱敏、有限 regex fallback、超长日志 fail-open、规则建议 evidence 不含原文 |
| 报告与 Dashboard | report 测试 + dashboard 随机端口 HTTP 测试 + demo 报告接口 | 聚合统计、Response 风险画像、Log 规则建议、报告文件安全读取、上传临时查看均不保存敏感原文 |
| 主动脱敏 | core 服务测试 + demo/dashboard 实验室 HTTP 测试 | by-type、object、strong scan 固定两轮返回，验证幂等性和 `MANUAL` 场景统计 |

## 5. 黑盒联调判断

当前自动化测试已经不是纯单元测试，主要后端联调路径有随机端口 HTTP 测试覆盖。因此，本材料不要求为了证明“有联调”而新增一批重复的黑盒自动化测试。

最终演示前的人工黑盒记录已补齐，材料包入口为 [06-black-box-test-cases.md](06-black-box-test-cases.md)。该入口统一关联用例清单、详细报告、原始接口结果、复核后判定结果、截图证据和 Demo 启动日志。黑盒实际结论为：P0、P1 全部通过；P2 中 10 项通过、1 项失败、5 项需专项环境验证；核心黑盒验收结论为通过。

建议复核路径：

1. 启动 demo：`mvn -pl safe-output-demo -am spring-boot:run`。
2. 打开 `http://localhost:8080/index.html`，检查工作台总览、客户档案、订单履约、支付核验、工单处理、账户安全页面可访问。
3. 进入详情页，确认列表和详情字段已脱敏；点击 raw 明文演示接口后，确认风险统计可在报告或 Dashboard 中看到 ignored 记录。
4. 打开脱敏实验室，分别验证 by-type、object、strong scan 两轮结果和耗时展示。
5. 打开 `http://localhost:8080/safe-output/dashboard/index.html`，确认实时概览、接口风险、日志规则建议、历史报告、上传查看可访问。
6. 导出报告并检查 JSON 中不包含原始手机号、身份证号、银行卡号、邮箱、密码或完整日志。

## 6. 本轮实际运行结果

运行日期：2026-05-24，时区：Asia/Shanghai。以下只记录本轮实际执行过的命令。

| 命令 | 结果 | 记录 |
|---|---|---|
| `mvn test` | 通过 | 2026-05-24 11:47:59 完成，7 个 reactor 模块 `BUILD SUCCESS` |
| `mvn -pl safe-output-spring-boot-starter -am test` | 通过 | 2026-05-24 11:48:17 完成，5 个 reactor 模块 `BUILD SUCCESS` |
| `mvn -pl safe-output-demo -am test` | 通过 | 2026-05-24 11:48:31 完成，7 个 reactor 模块 `BUILD SUCCESS` |
| `mvn -pl safe-output-dashboard-spring-boot-starter -am test` | 通过 | 首次与其他命令并行运行时，依赖模块 `safe-output-report` 在 JUnit 临时目录清理阶段出现一次 Windows `DirectoryNotEmptyException`；随后单独重跑，2026-05-24 11:48:55 完成，6 个 reactor 模块 `BUILD SUCCESS` |
| `mvn verify` | 未通过 | 2026-05-24 11:49:12 在 `safe-output-report` 的 `MaskReportExporterTest.scheduledExporterWritesSnapshotsAndRetainsNewestFiles` 后置临时目录清理阶段失败，错误为 Windows 临时目录 `DirectoryNotEmptyException`；测试断言无 failure，但 verify 阶段最终 `BUILD FAILURE` |

对 `mvn verify` 的判断：失败点出现在 JUnit `@TempDir` 清理阶段，不是脱敏逻辑断言失败；但因为命令实际返回失败，不能在材料中写成已通过。正式发布前应修复或规避该 Windows 临时目录清理问题，并重新运行 `mvn verify`。

## 7. 剩余风险

- Spring Boot 3.x 未确认：当前测试基线是 Java 8、Spring Boot 2.7.18、`spring.factories` 和 `javax.servlet`。
- 报告没有数据库持久化：当前是内存聚合和本地 JSON 快照，不是集中式治理平台。
- Dashboard 无内置鉴权：默认关闭，启用后需由接入方通过内网、网关、Spring Security 或运维平台保护入口。
- Log4j2 runtime bridge 当前适合单应用 Spring Boot 进程，未验证多应用上下文隔离。
- `ObjectMasker` 对 Bean 原地修改字段，调用方复用响应对象实例时需要关注副作用。
- `mvn verify` 本轮在 Windows 环境出现临时目录清理失败，发布前需要重新验证完整 verify 阶段。

## 8. 自检结论

| 验收项 | 结论 |
|---|---|
| 列出推荐验证命令 | 已列出 `mvn test`、`mvn verify`、demo/starter/dashboard 指定模块测试 |
| 按模块说明测试关注点 | 已按 6 个模块归纳覆盖重点和质量边界 |
| 覆盖关键质量边界 | 已覆盖 fail-open、ignore 风险统计、日志 fallback、报告不含敏感原文 |
| 明确剩余风险 | 已说明 Boot 3 未确认、报告无数据库、Dashboard 无内置鉴权等风险 |
| 不声称未运行测试通过 | 已如实记录实际运行结果，并明确 `mvn verify` 本轮未通过 |

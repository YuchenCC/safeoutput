# 第三轮交接

## 当前最稳定的基础能力

- core 规则匹配、内置策略、自定义策略注册、对象递归脱敏。
- 内置默认字段规则已从 `MaskRuleMatcher` 提取到 `DefaultMaskRules`，后续修改默认 key 时同步更新该类、`MaskRuleMatcherTest.defaultRuleLibraryIsTheSingleSourceForBuiltInFieldRules` 和文档默认规则表。
- starter 自动装配和 ResponseBodyAdvice 接入。
- starter 到 Log4j2 `%safeOutputMsg` 的 runtime bridge，可复用 Spring 配置规则、自定义策略、日志选项、日志脱敏计数和日志规则建议采集。
- 默认规则库总开关：`safe-output.rules.default-enabled=false` 可关闭内置默认字段规则，配置规则和注解仍生效。
- report 聚合指标、Response 风险画像、真实 Log4j2 `LOG` 计数和 fallback 规则线索、本地 JSON 快照。
- report exporter 与 Demo 规则建议入口已复用 `safe-output.rules[].keys` 过滤已配置日志 key；若后续扩展建议采纳流，应继续使用同一 configured-key 提取逻辑。
- Log 规则建议的 YAML 片段会覆盖所有未配置候选 key，包括 `LOW` 置信度建议；候选规则默认 `enabled:false`，由人工复核后再采纳。
- 可选 Dashboard starter 已实现：`safe-output-dashboard-spring-boot-starter` 默认关闭，`safe-output.dashboard.enabled=true` 时在 Spring MVC Servlet Web 应用中提供 `/safe-output/dashboard/index.html` 和 POST API，覆盖实时概览、接口风险、Log 规则建议、历史报告、临时报告上传和通用脱敏实验室。
- demo 端到端测试覆盖 Response、Log、Manual、Report，以及 R2.5 业务工作台、接入说明、日志场景、报告文件中心和安全读取边界。
- 父工程已在 pluginManagement 管理 `spring-boot-maven-plugin`，非 demo 模块默认 skip，`safe-output-demo` 显式启用；可直接从父工程运行 `mvn -pl safe-output-demo -am spring-boot:run` 启动 demo。
- Demo 部署文档已新增：`docs/project/deployment-login-record.md` 记录服务器 SSH 登录与免密 key 验证；`docs/project/demo-deployment-runbook.md` 是后续自动化部署的流程依据，当前约定本地 Maven 打包 `safe-output-demo`，上传可执行 jar 到 `122.51.95.83`，由远程 `systemd` 运行并通过 `http://122.51.95.83:8080/index.html` 展示。2026-05-28 已完成首次远程部署，服务端本机 `127.0.0.1:8080` 返回 200；云厂商控制台放行 `TCP 8080` 后，公网 `http://122.51.95.83:8080/index.html` 和 Dashboard 入口均返回 200。

## 当前最适合扩展的模块

- `safe-output-report`：风险画像、统计图表数据、Agent 摘要和配置建议都应从聚合模型扩展。
- `safe-output-demo`：适合增强展示看板、交互式验证和接入指南。
- `safe-output-spring-boot-starter`：适合补齐配置一致性和暴露更多可插拔 Bean。
- `ppt-template-lab`：适合继续扩展竞赛 PPT 转化链路，包括更细的页面分类、更多组件、Markdown 大纲转 PPT、截图 QA 和 Presentations renderer 适配；该工程独立于 Java Maven reactor。

## 当前不建议大改的模块

- `safe-output-core/ObjectMasker`：递归、fail-open、统计和规则耦合较集中，除非先补测试。
- `safe-output-log4j2/SafeOutputLogMessageMasker`：正则边界容易引入误伤，改动前先明确 fallback 策略。
- 父 POM Java 8 / Boot 2.x 基线：第三轮若无明确需求，不建议升级。

## 第三轮切入建议

- R2.6 Dashboard 模块边界收敛已完成：`safe-output-dashboard-spring-boot-starter` 是可选附加包。Dashboard 默认关闭，通过 `safe-output.dashboard.enabled=true` 启用，默认路径前缀 `/safe-output/dashboard`，仅支持 Spring MVC Servlet Web 应用；后端 API 全部使用 POST，静态页面/资源仍使用 GET。第一期包含实时概览、接口风险、日志规则建议、历史报告、报告上传临时查看、通用脱敏实验室；不包含 Demo 业务工作台、小眼睛明文查看、权限系统、数据库、多报告趋势或规则自动采纳。Demo 仍保留 `/demo/report/**` 和 `/demo/mask/**` 兼容接口。
- 日志长度策略增强：新增 R3 PRD `docs/project/prd/safe-output-r3-prd.md`，要求支持 `maxMessageLength` 整条超限跳过模式与 `max-scan-length` 前缀扫描窗口模式切换；默认应兼容 R2，不截断最终日志输出，不保存原始日志。
- 增强 Demo：R2.5/R3 后从 `safe-output-demo/src/main/resources/static/js/views/*`、`static/css/app.css`、`business/DemoBusinessController`、`guide/DemoIntegrationGuideController`、`report/DemoReportController`、`lab/DemoManualMaskController` 切入；`index.html` 只是静态壳层，`DemoApplication` 只作为根包扫描入口。
- 增强风险画像：从 `ResponseRiskAnalyzer`、`ApiMaskMetrics`、`ResponseRiskApiProfile`、`MaskReportExporter.toJson` 切入。
- 增强统计图表：优先扩展 `report/DemoReportDashboardAssembler` 和 `DemoReportController.dashboard` 返回结构，再更新 `static/index.html`。
- 增强 Agent 分析摘要：预留 `MaskReport` / `ResponseRiskAnalysis` 到摘要 DTO 的纯函数接口；输入只用聚合指标，不传原始 response/log。
- 增强配置建议生成：从 `LogRuleSuggestionAnalyzer` 扩展，保留 `enabled:false` 和人工确认；可增加建议来源、影响范围、置信度原因。
- Log4j2 report bridge 已补齐：业务工作台接口和脱敏实验室接口会产生真实 `LOG` 计数和 `certNum` / `mailAddr` fallback 规则线索，后续不要再用 Demo controller 手动 seed 日志建议。
- R3 日志场景保留 `/demo/logs/scenarios` 作为只读聚合接口，返回 JSON-like、key=value、regex fallback 三类模板摘要、聚合计数、建议和 YAML 片段；已移除 `/demo/logs` 与 `/demo/logs/scenarios/{id}/trigger`，日志场景页不再提供触发日志功能。
- R2.5/R3 报告中心包含 `/demo/report/files`、`/demo/report/files/{name}`、`/demo/report/files/{name}/dashboard`；安全读取只接受配置前缀 JSON 文件，逻辑集中在 `report/DemoReportFileStore`，继续禁止报告和页面展示敏感原文。
- R3 导航与 Dashboard：demo 默认入口已改为 `#workbench`，顶部 Dashboard 导航跳转到独立 `/safe-output/dashboard/index.html`；通用治理 Dashboard 已从 demo 抽到 dashboard starter。demo 原业务工作台、mock 域、raw 明文演示和旧 `/demo/report/**`、`/demo/mask/**` 兼容接口仍保留。
- R3 工作台侧边栏“工作台”分组包含总览、客户档案、订单履约、支付核验、工单处理、账户安全：`#workbench`、`#workbench/customers`、`#workbench/orders`、`#workbench/payments`、`#workbench/tickets`、`#workbench/accounts`。工作台总览直接展示 `/demo/integration-guide` 的接入说明内容，不再保留接入说明子菜单；旧 `#guide` 和 `#workbench/integration` 兼容跳转到 `#workbench`。后端新增对应 `/demo/business/{domain}`、`/demo/business/{domain}/{id}`、`/demo/business/{domain}/{id}/raw`；raw 接口通过 `safe-output.ignore.apis` 的 Ant pattern 配置为 API ignore，用于“小眼睛查看明文”演示，并保留风险统计。
- Demo 脱敏实验室当前约定：前端不再暴露 `iterations`，三类主动脱敏接口固定执行两轮；响应是数组，第一条为首次脱敏结果，第二条为对首次结果再次脱敏后的结果，每条包含 `round`、`result`、`elapsedNanos`、`sameAsPrevious`；前端通过 `static/js/components/formatters.js` 将 `elapsedNanos` / `*ElapsedNanos` 转为 `ms` 展示；业务对象面板提交 `realName`、`mobile`、`name` 表单字段，空值回退默认样例。
- R3 前端整体风格已切换为白底业务后台：浅色侧边栏、白色面板、细边框、蓝/青/绿语义图表色和浅底代码块。后续 UI polish 应基于该白底风格，不再恢复深色驾驶舱。
- 本轮浏览器插件的执行工具未暴露，已完成本地 HTTP 与静态资源加载验证；下一轮若要做 UI polish，应人工打开 `http://localhost:8080/index.html` 验证五个主页面和打印样式。

## 编码前必须阅读

- `.codex-memory/00-project-current-state.md`
- `.codex-memory/01-module-map.md`
- `.codex-memory/02-core-flow-map.md`
- `.codex-memory/03-decision-and-boundary.md`
- `.codex-memory/04-next-round-handoff.md`
- `safe-output/README.md`
- `safe-output/safe-output-core/src/main/java/com/safeoutput/core/MaskRuleMatcher.java`
- `safe-output/safe-output-core/src/main/java/com/safeoutput/core/DefaultMaskRules.java`
- `safe-output/safe-output-core/src/main/java/com/safeoutput/core/ObjectMasker.java`
- `safe-output/safe-output-spring-boot-starter/src/main/java/com/safeoutput/spring/boot/autoconfigure/SafeOutputResponseBodyAdvice.java`
- `safe-output/safe-output-report/src/main/java/com/safeoutput/report/MaskMetricsCollector.java`

## 测试前必须运行

- 快速验证：`cd safe-output && mvn test`
- 指定 demo：`cd safe-output && mvn -pl safe-output-demo -am test`
- 指定 starter：`cd safe-output && mvn -pl safe-output-spring-boot-starter -am test`
- 指定 dashboard starter：`cd safe-output && mvn -pl safe-output-dashboard-spring-boot-starter -am test`
- 发布前完整验证：`cd safe-output && mvn verify`

## PPT 转化辅助工程命令

- 安装依赖：`cd ppt-template-lab && npm install`
- 生成设计审计和模板 JSON：`cd ppt-template-lab && npm run audit`
- 从 Markdown 示例生成 PPTX：`cd ppt-template-lab && npm run generate:example`
- 验证工程：`cd ppt-template-lab && npm run build && npm test && npm audit --audit-level=moderate`
- PPT 内容页标题左侧默认标识已抽离为 `ai-contest-deliverables/assets/pptmob-title-marker.svg`：`x=0.70in, y=0.75in, w=0.10in, h=0.40in, fill=#4372C4`；后续按模板生成页面时必须直接使用该资产并保留该小蓝条，不能依赖回读 `pptmob.pptx`。

## 文档路径交接

- 项目级文档入口已从旧根目录 `doc/` 迁移到 `docs/project/`；后续新增 PRD、设计说明和模块深挖文档应放入 `docs/project/`。
- Demo 部署相关文档位于 `docs/project/deployment-login-record.md` 和 `docs/project/demo-deployment-runbook.md`；后续编写自动化部署脚本时必须以 runbook 中变量、发布目录、systemd 服务和健康检查步骤为准。
- Agent 协作说明继续放在 `docs/agents/`；组件接入方文档继续放在 `safe-output/doc/`。
- 本地产物目录 `tmp/`、`outputs/` 和 `.vscode/` 已在根 `.gitignore` 中忽略；不要把临时截图或本地 IDE 配置提交进版本库。

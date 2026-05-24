# Safe Output 黑盒测试材料包入口

## 1. 文档定位

本文基于 `ai-contest-deliverables/05-quality-verification.md` 和 `doc/prd/safe-output-full-requirements-spec.md`，整理 Safe Output 的人工黑盒测试用例。用例面向评审、验收和演示前联调，重点验证外部可观察行为，不依赖内部类或单元测试断言。

本文是黑盒测试材料包的唯一入口，同时承载测试用例、实际执行结果索引和过程证明索引。详细报告、原始接口结果、修订判定结果、截图和 Demo 启动日志均通过本文关联引用，便于评审从一个入口追溯完整证据链。

## 2. 实际执行结果与证据包

本轮黑盒验收已于 2026-05-24 执行，执行入口为 Demo 工作台 `http://localhost:8080/index.html` 和治理 Dashboard `http://localhost:8080/safe-output/dashboard/index.html`。核心结论：P0、P1 全部通过；P2 中 10 项通过、1 项失败、5 项需专项环境验证；核心黑盒验收结论为通过。

| 材料 | 路径 | 用途 |
|---|---|---|
| 详细黑盒测试报告 | [07-black-box-test-report.md](07-black-box-test-report.md) | 汇总执行环境、自动化基线、P0/P1/P2 结果、风险和安全检查结论 |
| 原始接口执行结果 | [black-box-results.json](assets/black-box-test/black-box-results.json) | 保存黑盒脚本首次采集的接口响应和状态码，作为原始证据 |
| 修订后判定结果 | [black-box-results-corrected.json](assets/black-box-test/black-box-results-corrected.json) | 保存复核后的 PASS/FAIL/NOT_RUN 判定；其中 `BB-P2-011` 已修正 `MOBILEM` 误判 |
| 过程启动日志 | [safe-output-demo-run.out.log](assets/black-box-test/safe-output-demo-run.out.log) | 证明 Demo 按 localhost 方式启动并服务黑盒请求 |
| 启动错误日志 | [safe-output-demo-run.err.log](assets/black-box-test/safe-output-demo-run.err.log) | 本轮为空文件，用于证明启动过程未输出 stderr |
| Dashboard 上传大文件样例 | [big-report.json](assets/black-box-test/big-report.json) | `BB-P2-003` 上传大小限制失败项的输入样例 |
| Dashboard 非法报告样例 | [bad-report.json](assets/black-box-test/bad-report.json) | `BB-P2-004` 上传结构校验的输入样例 |
| 截图证据目录 | [assets/black-box-test/](assets/black-box-test/) | 保存 Demo、Dashboard、风险画像、日志建议、报告中心和移动视口截图 |

执行结果摘要：

| 优先级 | 通过 | 失败 | 未执行 / 待专项验证 | 结论 |
|---|---:|---:|---:|---|
| P0 | 22/22 | 0 | 0 | 核心黑盒验收通过 |
| P1 | 20/20 | 0 | 0 | 治理链路通过 |
| P2 | 10/16 | 1 | 5 | 记录边界风险 |

自动化基线均已通过：

| 命令 | 结果 |
|---|---|
| `mvn test` | 通过，7 个 reactor 模块 `BUILD SUCCESS` |
| `mvn -pl safe-output-demo -am test` | 通过，7 个 reactor 模块 `BUILD SUCCESS` |
| `mvn -pl safe-output-spring-boot-starter -am test` | 通过，5 个 reactor 模块 `BUILD SUCCESS` |
| `mvn -pl safe-output-dashboard-spring-boot-starter -am test` | 通过，6 个 reactor 模块 `BUILD SUCCESS` |

关键复核说明：

- `BB-P2-003` 仍记录为失败：Dashboard 上传超过 1MB JSON 时返回 HTTP 500，预期为 413 或等价可诊断错误。
- `BB-P2-011` 已由失败修正为通过：`MOBILEM` 会按 type 归一化命中 Demo 已注册的自定义 `mobileM` 策略，不属于未知 type；真正未知 type 应使用 `MOBILE_UNKNOWN` 等未注册标签验证 DEFAULT fallback。
- 原始执行结果文件保留首次采集事实；修订判定结果文件保留复核后的最终判定，二者共同构成可追溯过程证明。

用例结果关联：

| 用例范围 | 实际结果入口 | 过程证据 |
|---|---|---|
| `BB-P0-001` 到 `BB-P0-022` | [详细报告第 6 节](07-black-box-test-report.md#6-p0-验证结果)，[修订判定结果](assets/black-box-test/black-box-results-corrected.json) | Demo/Dashboard 截图见 [assets/black-box-test/](assets/black-box-test/)；接口原始响应见 [black-box-results.json](assets/black-box-test/black-box-results.json) |
| `BB-P1-001` 到 `BB-P1-020` | [详细报告第 7 节](07-black-box-test-report.md#7-p1-验证结果)，[修订判定结果](assets/black-box-test/black-box-results-corrected.json) | 实验室、风险画像、日志建议、报告中心截图见 [assets/black-box-test/](assets/black-box-test/) |
| `BB-P2-001` 到 `BB-P2-016` | [详细报告第 8 节](07-black-box-test-report.md#8-p2-验证结果与风险)，[修订判定结果](assets/black-box-test/black-box-results-corrected.json) | 大文件和非法报告输入样例、移动视口截图见 [assets/black-box-test/](assets/black-box-test/) |

关键截图索引：

| 用例 | 截图 |
|---|---|
| `BB-P0-001` Demo 首页 | [BB-P0-001-demo-home.png](assets/black-box-test/BB-P0-001-demo-home.png) |
| `BB-P0-007` 客户详情脱敏 | [BB-P0-007-business-customer-masked.png](assets/black-box-test/BB-P0-007-business-customer-masked.png) |
| `BB-P0-013` raw 明文演示 | [BB-P0-013-raw-plaintext.png](assets/black-box-test/BB-P0-013-raw-plaintext.png) |
| `BB-P0-014` ignored API 风险统计 | [BB-P0-014-risk-ignored-api.png](assets/black-box-test/BB-P0-014-risk-ignored-api.png) |
| `BB-P0-017` Dashboard 首页 | [BB-P0-017-dashboard-home.png](assets/black-box-test/BB-P0-017-dashboard-home.png) |
| `BB-P1-002` by-type 实验室 | [BB-P1-002-lab-by-type.png](assets/black-box-test/BB-P1-002-lab-by-type.png) |
| `BB-P1-009` Dashboard 风险画像 | [BB-P1-009-dashboard-risk.png](assets/black-box-test/BB-P1-009-dashboard-risk.png) |
| `BB-P1-010` Log 规则建议 | [BB-P1-010-log-suggestions.png](assets/black-box-test/BB-P1-010-log-suggestions.png) |
| `BB-P1-012` 报告中心 | [BB-P1-012-reports.png](assets/black-box-test/BB-P1-012-reports.png) |
| `BB-P2-006` 移动视口 | [BB-P2-006-mobile-demo.png](assets/black-box-test/BB-P2-006-mobile-demo.png), [BB-P2-006-mobile-dashboard.png](assets/black-box-test/BB-P2-006-mobile-dashboard.png) |

## 3. 优先级定义

| 优先级 | 含义 | 失败影响 |
|---|---|---|
| P0 | 核心主链路、安全边界和参赛演示必测项 | 失败会影响组件可用性、安全可信度或答辩主路径 |
| P1 | 重要治理能力、端到端展示能力和高频集成路径 | 失败会影响治理闭环或 Demo 说服力，但不一定阻断核心脱敏 |
| P2 | 增强能力、边界条件、非目标确认和兼容性说明 | 失败通常作为风险记录或后续优化项 |

## 4. 测试环境准备

建议先执行自动化基线，再执行人工黑盒：

```powershell
cd safe-output
mvn test
mvn -pl safe-output-demo -am test
mvn -pl safe-output-spring-boot-starter -am test
mvn -pl safe-output-dashboard-spring-boot-starter -am test
mvn -pl safe-output-demo -am spring-boot:run
```

浏览器入口：

- Demo 工作台：`http://localhost:8080/index.html`
- 可选治理 Dashboard：`http://localhost:8080/safe-output/dashboard/index.html`

测试数据约束：

- 只使用 Demo 内置 mock 数据和文档中的假样例。
- 不输入真实手机号、身份证号、银行卡号、邮箱、密码或客户数据。
- 报告、截图和缺陷记录不得保存真实敏感原文。

## 5. P0 黑盒用例

| ID | 场景 | 前置条件 | 操作步骤 | 预期结果 |
|---|---|---|---|---|
| BB-P0-001 | Demo 首页可访问 | Demo 已启动 | 打开 `/index.html` | 页面加载成功，默认进入业务工作台，不出现服务端错误 |
| BB-P0-002 | 工作台导航主路径 | Demo 首页已打开 | 依次进入总览、客户档案、订单履约、支付核验、工单处理、账户安全 | 页面均可访问，布局可读，接口请求无 5xx |
| BB-P0-003 | 基础 Bean Response 脱敏 | Demo 已启动 | GET `/demo/bean` | `mobile` 脱敏，`name` 脱敏，`plainNote` 因字段 ignore 保持样例文本 |
| BB-P0-004 | Map Response 脱敏 | Demo 已启动 | GET `/demo/map` | `email` 和 `mobile` 脱敏，不返回原始邮箱或手机号字段值 |
| BB-P0-005 | List Response 脱敏 | Demo 已启动 | GET `/demo/list` | 列表内每个对象的姓名和手机号均脱敏 |
| BB-P0-006 | 嵌套对象 Response 脱敏 | Demo 已启动 | GET `/demo/nested` | 嵌套客户信息和银行卡号脱敏，订单号不被误脱敏 |
| BB-P0-007 | 客户域列表与详情脱敏 | Demo 已启动 | GET `/demo/business/customers`，再 GET `/demo/business/customers/C-1001` | 姓名、手机号、身份证、邮箱、地址脱敏；客户编号保留业务可读 |
| BB-P0-008 | 订单域列表与详情脱敏 | Demo 已启动 | GET `/demo/business/orders`，再 GET `/demo/business/orders/ORD-20260518-001` | 手机号、银行卡、地址脱敏；订单号保留业务可读 |
| BB-P0-009 | 支付域列表与详情脱敏 | Demo 已启动 | GET `/demo/business/payments`，再 GET `/demo/business/payments/PAY-8840` | 手机号、银行卡、邮箱、安全答案脱敏 |
| BB-P0-010 | 工单域列表与详情脱敏 | Demo 已启动 | GET `/demo/business/tickets`，再 GET `/demo/business/tickets/TK-20260518-01` | 请求人姓名、手机号、邮箱等敏感字段脱敏 |
| BB-P0-011 | 账户域列表与详情脱敏 | Demo 已启动 | GET `/demo/business/accounts`，再 GET `/demo/business/accounts/AC-7780` | 手机号、邮箱、密码、地址脱敏 |
| BB-P0-012 | API ignore 明文兼容 | Demo 已启动 | GET `/demo/ignored` 和 `/demo/business/legacy-plaintext` | 接口按配置返回明文样例，用于兼容演示；请求成功无 5xx |
| BB-P0-013 | raw 明文查看接口 | Demo 已启动 | 分别访问五类业务域的 `/{id}/raw` 接口 | raw 接口返回对应业务明文样例，只限配置过的 ignore API |
| BB-P0-014 | API ignore 进入风险统计 | 已访问 ignore/raw 接口 | GET `/demo/report/response-risk` 或打开 Dashboard 接口风险页 | 可看到 ignored 风险 API 或 ignored 统计，不把明文豁免静默吞掉 |
| BB-P0-015 | 报告快照不含敏感原文 | 已调用若干业务接口 | GET `/demo/report/snapshot` | 返回聚合统计字段，不包含原始手机号、身份证、银行卡、邮箱、密码 |
| BB-P0-016 | 报告导出不含敏感原文 | 已调用若干业务接口和日志场景 | GET `/demo/report/export`，再读取最新报告 | 报告 JSON 只包含聚合信息、风险画像和脱敏 evidence，不包含完整 response 或完整日志 |
| BB-P0-017 | Dashboard 首页可访问 | Demo 已启动且 dashboard enabled | 打开 `/safe-output/dashboard/index.html` | 页面加载成功，展示治理 Dashboard，不进入 Demo 业务工作台 |
| BB-P0-018 | Dashboard API 只允许 POST | Dashboard 可访问 | GET `/safe-output/dashboard/api/overview`，再 POST 同一路径 | GET 返回 4xx；POST 返回聚合概览 |
| BB-P0-019 | Dashboard 报告读取防穿越 | Dashboard 可访问 | POST `/safe-output/dashboard/api/reports/view`，body 使用 `../pom.xml` | 返回 4xx 或 not found，不读取配置目录外文件 |
| BB-P0-020 | Dashboard 上传不落盘 | Dashboard 可访问 | 上传合法报告 JSON 到 `/safe-output/dashboard/api/reports/upload`，再查 `/reports/list` | 上传可临时查看，但历史列表不新增上传文件 |
| BB-P0-021 | Log 场景不展示原始日志 | 已触发业务接口和实验室接口 | GET `/demo/logs/scenarios` | 返回场景摘要、聚合计数、规则建议和 YAML 片段，不返回完整日志 message 或原始敏感值 |
| BB-P0-022 | Dashboard 安全边界说明 | Dashboard 首页可访问 | 检查页面和接口行为 | Dashboard 不出现登录、权限、审计、数据库、多租户等已实现承诺 |

## 6. P1 黑盒用例

| ID | 场景 | 前置条件 | 操作步骤 | 预期结果 |
|---|---|---|---|---|
| BB-P1-001 | 接入说明内容 | Demo 已启动 | GET `/demo/integration-guide` 或工作台总览查看接入说明 | 展示默认规则、YAML 配置、注解、字段 ignore、API ignore 等说明 |
| BB-P1-002 | by-type 主动脱敏 | Demo 已启动 | POST `/demo/mask/by-type`，body `{"value":"13800138000","type":"MOBILE"}` | 返回两轮结果，手机号脱敏，第二轮 `sameAsPrevious=true` |
| BB-P1-003 | object 主动脱敏 | Demo 已启动 | POST `/demo/mask/object`，body 包含 `realName`、`mobile`、`name` | 姓名、手机号脱敏；商品名不误脱敏；返回两轮耗时 |
| BB-P1-004 | strong scan 主动脱敏 | Demo 已启动 | POST `/demo/mask/strong`，body `{"text":"手机号13800138000邮箱foo@example.com"}` | 手机号和邮箱脱敏；返回两轮结果 |
| BB-P1-005 | Dashboard by-type 实验室 | Dashboard 可访问 | POST `/safe-output/dashboard/api/lab/by-type` | 返回两轮结果，不返回原始敏感输入 |
| BB-P1-006 | Dashboard object 实验室 | Dashboard 可访问 | POST `/safe-output/dashboard/api/lab/object` | 姓名和手机号脱敏，商品名保留 |
| BB-P1-007 | Dashboard strong 实验室 | Dashboard 可访问 | POST `/safe-output/dashboard/api/lab/strong` | 文本中的手机号、邮箱等支持类型脱敏 |
| BB-P1-008 | 实时概览聚合 | 已调用 Response、Log、Manual 场景 | POST `/safe-output/dashboard/api/overview` | 返回总量、场景分布、类型分布、风险摘要、日志建议等聚合数据 |
| BB-P1-009 | Response 风险画像 | 已调用普通接口和 raw 接口 | POST `/safe-output/dashboard/api/response-risk` | 返回风险等级、topRiskApis、ignoredRiskApis、耗时和字段统计 |
| BB-P1-010 | Log 规则建议 | 已触发业务日志和强扫描日志 | GET `/demo/report/log-suggestions` 或 POST `/safe-output/dashboard/api/log-suggestions` | 返回候选 key、suggestedType、confidence、evidence 和 `enabled:false` YAML |
| BB-P1-011 | 已配置日志 key 过滤 | 已触发包含已配置 key 的日志 | 查看日志规则建议 | 已配置 key 不重复生成治理建议；未配置 fallback key 可出现候选 |
| BB-P1-012 | 历史报告列表 | 已至少导出一次报告 | GET `/demo/report/files` 或 POST Dashboard `/reports/list` | 返回报告数量和可查看文件，文件名限定为报告前缀 JSON |
| BB-P1-013 | 单报告查看 | 已有历史报告 | 打开 demo 单报告接口或 POST Dashboard `/reports/view` | 返回单报告聚合视图，不包含敏感原文 |
| BB-P1-014 | 非报告文件拒绝 | 已有 report 目录 | 请求读取 `.txt` 或非 JSON 文件 | 返回 4xx 或 not found |
| BB-P1-015 | Dashboard 导出报告 | Dashboard 可访问 | POST `/safe-output/dashboard/api/reports/export` | 返回导出路径；导出失败不得影响业务接口 |
| BB-P1-016 | 日志场景只读 | Demo 已启动 | 尝试访问旧触发路径 `/demo/logs/scenarios/configured-vs-missing/trigger` | 返回 4xx；日志场景页不提供专用触发按钮 |
| BB-P1-017 | 字段 ignore 可观察 | Demo 已启动 | GET `/demo/bean` 或客户接口 | 配置为 ignore 的 `plainNote` 保持原样，其他敏感字段仍脱敏 |
| BB-P1-018 | 业务编号不误脱敏 | Demo 已启动 | 查看订单号、客户号、支付号、工单号、账户号 | 业务编号保持可读，不被默认规则误伤 |
| BB-P1-019 | 密码类字段脱敏 | Demo 已启动 | GET `/demo/business/accounts/AC-7780` | 密码字段输出为脱敏值，不返回原始密码 |
| BB-P1-020 | 统计场景分布 | 已分别调用 Response、Log、Manual | GET `/demo/report/dashboard` | responseCount、logCount、manualCount 等字段存在且能反映调用增长 |

## 7. P2 黑盒用例

| ID | 场景 | 前置条件 | 操作步骤 | 预期结果 |
|---|---|---|---|---|
| BB-P2-001 | Dashboard 自定义 path-prefix | 使用测试应用或临时配置 `safe-output.dashboard.path-prefix=/ops/safe-dashboard` | 启动后访问 `/ops/safe-dashboard/index.html` 和 POST `/ops/safe-dashboard/api/health` | 自定义路径生效，默认路径不作为唯一入口 |
| BB-P2-002 | Dashboard 默认关闭 | 在不启用 `safe-output.dashboard.enabled=true` 的宿主应用中接入 dashboard starter | 访问 `/safe-output/dashboard/index.html` | 默认不暴露 Dashboard |
| BB-P2-003 | Dashboard 上传文件大小限制 | Dashboard 可访问 | 上传超过 1MB 的 JSON 文件 | 返回 413 或等价错误，不写入报告目录 |
| BB-P2-004 | Dashboard 上传结构校验 | Dashboard 可访问 | 上传 `{}` 或缺少报告关键字段的 JSON | 返回 4xx，不生成临时报告视图 |
| BB-P2-005 | Dashboard 静态资源加载 | Dashboard 首页打开 | 检查 CSS、JS、Chart 资源请求 | 静态资源 GET 成功，页面主要区域无空白 |
| BB-P2-006 | 移动端基础可读性 | Demo 和 Dashboard 可访问 | 使用浏览器移动视口查看首页、工作台、Dashboard | 主要文字不重叠，导航和表格可基本操作 |
| BB-P2-007 | R3 整条超长日志跳过策略 | 配置较小 `safe-output.log.max-message-length` | 写入超过阈值日志 | 日志原文输出不被截断，整条跳过脱敏；该风险需通过配置说明识别 |
| BB-P2-008 | R3 前缀扫描窗口策略 | 若当前实现支持 `max-scan-length` | 写入前缀内和窗口外均含敏感值的长日志 | 只扫描窗口内，窗口外原样拼回；若实现尚未落地，记录为待确认 |
| BB-P2-009 | 银行卡无上下文 fallback 边界 | 日志 fallback 开启 | 写入无 key 的银行卡样例文本 | 默认不做无上下文银行卡全局脱敏，避免误伤长数字 |
| BB-P2-010 | 强扫描显式入口边界 | Demo 已启动 | 普通 Response 中放置业务文本，另用 strong scan 输入同类文本 | Response 不做粗暴全局扫描；strong scan 显式入口可脱敏支持类型 |
| BB-P2-011 | 未知 type 运行期行为 | 构造或使用测试宿主配置未注册 type，例如 `MOBILE_UNKNOWN`；不要使用已注册自定义 type 的大小写变体 | 调用命中未知 type 的主动脱敏或配置规则 | 不阻断应用启动；命中后按 DEFAULT fallback 并进入未知类型统计 |
| BB-P2-012 | Spring Boot 3.x 非承诺确认 | 阅读交付物和启动 Boot 3 宿主前置说明 | 检查文档和页面表述 | 不把 Boot 3.x 写成已验证支持 |
| BB-P2-013 | WebFlux 非目标确认 | 阅读交付物和 Dashboard 行为 | 检查文档和页面表述 | 不把 WebFlux 支持写成已实现 |
| BB-P2-014 | Logback 非目标确认 | 阅读交付物和日志配置 | 检查文档和页面表述 | 不把 Logback 实现写成已实现 |
| BB-P2-015 | 数据库持久化非目标确认 | 查看报告和 Dashboard | 检查是否要求数据库连接或展示数据库能力 | 当前只使用内存聚合和本地 JSON，不承诺数据库持久化 |
| BB-P2-016 | 规则建议不自动采纳 | 已生成日志规则建议 | 检查配置文件和运行行为 | 候选 YAML 默认 `enabled:false`，不会自动写 YAML 或自动启用规则 |

## 8. 需求覆盖矩阵

| 需求域 | 覆盖用例 |
|---|---|
| Core 脱敏模型 | BB-P0-003 到 BB-P0-011，BB-P1-002 到 BB-P1-004，BB-P2-011 |
| 规则匹配与 ignore | BB-P0-012 到 BB-P0-014，BB-P1-017，BB-P1-018 |
| Response 自动脱敏 | BB-P0-003 到 BB-P0-011，BB-P1-019 |
| Log4j2 日志脱敏 | BB-P0-021，BB-P1-010，BB-P1-011，BB-P2-007 到 BB-P2-010 |
| 主动脱敏服务 | BB-P1-002 到 BB-P1-007，BB-P2-010 |
| 统计报告与风险画像 | BB-P0-014 到 BB-P0-016，BB-P1-008 到 BB-P1-015，BB-P1-020 |
| Spring Boot starter | BB-P0-003 到 BB-P0-016，BB-P2-012 |
| Dashboard starter | BB-P0-017 到 BB-P0-020，BB-P1-005 到 BB-P1-009，BB-P2-001 到 BB-P2-006 |
| Demo 样板系统 | BB-P0-001 到 BB-P0-013，BB-P1-001，BB-P1-016 |
| 非功能与非目标 | BB-P0-022，BB-P2-012 到 BB-P2-016 |

## 9. 执行记录模板

| 字段 | 内容 |
|---|---|
| 执行日期 |  |
| 执行人 |  |
| 代码版本 / commit |  |
| Java / Maven / OS |  |
| Demo 地址 |  |
| Dashboard 地址 |  |
| P0 结果 | 通过： / 失败： / 阻塞： |
| P1 结果 | 通过： / 失败： / 阻塞： |
| P2 结果 | 通过： / 失败： / 阻塞： |
| 失败证据 | 缺陷编号、日志片段、截图路径或报告文件名 |
| 剩余风险 |  |

## 10. 判定规则

- P0 全部通过，且报告/Dashboard/日志建议不包含敏感原文，才可判定为“核心黑盒验收通过”。
- P1 允许存在不阻断主链路的展示或治理细节问题，但必须记录缺陷和影响范围。
- P2 中属于非目标或未确认能力的项目，应记录为“符合边界”或“待确认”，不能写成已实现通过。
- 任何发现敏感原文被报告、Dashboard、日志建议或测试材料保存的情况，均按 P0 安全问题处理。

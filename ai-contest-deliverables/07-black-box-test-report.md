# Safe Output 黑盒测试报告

归档说明：黑盒测试材料包的唯一入口是 [06-black-box-test-cases.md](06-black-box-test-cases.md)。本文作为详细执行报告，由入口文档关联引用。

## 1. 执行摘要

本次按 `ai-contest-deliverables/06-black-box-test-cases.md` 执行黑盒验收。结论：

- P0 核心链路：通过，22 个用例均有结论；其中导航和 raw 接口按子路径拆成 31 个子检查，全部通过。
- P1 治理链路：通过，20 个用例全部通过。
- P2 边界项：10 个通过，1 个失败，5 个需专项环境验证。
- 核心黑盒验收结论：通过。唯一失败项为 P2 边界/增强项，不阻断 Response 脱敏、Log 统计、报告导出和 Dashboard 主链路。

本报告中的明文截图均来自 Demo/mock 测试样例，非真实用户数据，用于证明 raw/ignore 兼容链路和风险统计可观察。

## 2. 执行环境

| 字段 | 内容 |
|---|---|
| 执行日期 | 2026-05-24 |
| 执行人 | Codex |
| 代码版本 / commit | `8eae13f` |
| Java | OpenJDK `1.8.0_482` Temurin |
| Maven | Apache Maven `3.9.15` |
| OS | Microsoft Windows 11 专业版 `10.0.26200`，64 位 |
| Demo 地址 | `http://localhost:8080/index.html` |
| Dashboard 地址 | `http://localhost:8080/safe-output/dashboard/index.html` |

## 3. 自动化基线

| 命令 | 结果 |
|---|---|
| `mvn test` | 通过，7 个 reactor 模块 `BUILD SUCCESS` |
| `mvn -pl safe-output-demo -am test` | 通过，7 个 reactor 模块 `BUILD SUCCESS` |
| `mvn -pl safe-output-spring-boot-starter -am test` | 通过，5 个 reactor 模块 `BUILD SUCCESS` |
| `mvn -pl safe-output-dashboard-spring-boot-starter -am test` | 通过，6 个 reactor 模块 `BUILD SUCCESS` |

## 4. 结果汇总

| 优先级 | 通过 | 失败 | 未执行 / 待专项验证 | 结论 |
|---|---:|---:|---:|---|
| P0 | 22/22 | 0 | 0 | 核心黑盒验收通过 |
| P1 | 20/20 | 0 | 0 | 治理链路通过 |
| P2 | 10/16 | 1 | 5 | 记录边界风险 |

P0 的 `BB-P0-002` 和 `BB-P0-013` 分别按多个页面/接口子路径执行，因此原始结果文件中 P0 子检查数为 31。

## 5. 关键截图证据

| 用例 | 截图 | 验证点 |
|---|---|---|
| `BB-P0-001` | [BB-P0-001-demo-home.png](assets/black-box-test/BB-P0-001-demo-home.png) | Demo 首页可访问，默认进入业务工作台 |
| `BB-P0-007` | [BB-P0-007-business-customer-masked.png](assets/black-box-test/BB-P0-007-business-customer-masked.png) | 客户详情接口姓名、手机号、身份证、邮箱、地址脱敏 |
| `BB-P0-013` | [BB-P0-013-raw-plaintext.png](assets/black-box-test/BB-P0-013-raw-plaintext.png) | raw 接口返回 Demo 测试明文，作为 ignore 演示 |
| `BB-P0-014` | [BB-P0-014-risk-ignored-api.png](assets/black-box-test/BB-P0-014-risk-ignored-api.png) | raw/ignore 接口进入风险统计 |
| `BB-P0-017` | [BB-P0-017-dashboard-home.png](assets/black-box-test/BB-P0-017-dashboard-home.png) | Dashboard 首页可访问 |
| `BB-P1-009` | [BB-P1-009-dashboard-risk.png](assets/black-box-test/BB-P1-009-dashboard-risk.png) | Response 风险画像和 ignoredRiskApis 可观察 |
| `BB-P1-010` | [BB-P1-010-log-suggestions.png](assets/black-box-test/BB-P1-010-log-suggestions.png) | Log 规则建议展示 `enabled: false` 人工复核候选 |
| `BB-P1-012` | [BB-P1-012-reports.png](assets/black-box-test/BB-P1-012-reports.png) | 历史报告列表和单报告视图可访问 |
| `BB-P1-002` | [BB-P1-002-lab-by-type.png](assets/black-box-test/BB-P1-002-lab-by-type.png) | 主动脱敏实验室可访问 |
| `BB-P2-006` | [BB-P2-006-mobile-demo.png](assets/black-box-test/BB-P2-006-mobile-demo.png), [BB-P2-006-mobile-dashboard.png](assets/black-box-test/BB-P2-006-mobile-dashboard.png) | 移动视口抽样可读 |

Chrome 插件已成功连接并打开 localhost 页面；插件截图通道在 `Page.captureScreenshot` 上超时，因此截图文件使用本机 Chrome headless 通过 Playwright 采集，验证对象仍为同一 Demo/Dashboard 地址。

## 6. P0 验证结果

| 用例范围 | 结果 | 说明 |
|---|---|---|
| `BB-P0-001` 到 `BB-P0-002` | 通过 | Demo 首页和工作台总览、客户、订单、支付、工单、账户页面均可访问，无 5xx |
| `BB-P0-003` 到 `BB-P0-006` | 通过 | Bean、Map、List、Nested Response 均按预期脱敏；`plainNote` 作为字段 ignore 保持测试样例 |
| `BB-P0-007` 到 `BB-P0-011` | 通过 | 五类业务域列表和详情均脱敏，业务编号保持可读 |
| `BB-P0-012` 到 `BB-P0-014` | 通过 | ignore/raw 接口返回 Demo 明文样例，同时进入 Response 风险统计 |
| `BB-P0-015` 到 `BB-P0-016` | 通过 | 报告快照和导出 JSON 未发现测试原文手机号、身份证、银行卡、邮箱等 raw needles |
| `BB-P0-017` 到 `BB-P0-020` | 通过 | Dashboard 可访问；API 只允许 POST；路径穿越被拒绝；合法报告上传仅临时查看，不写入历史目录 |
| `BB-P0-021` 到 `BB-P0-022` | 通过 | 日志场景只返回摘要、计数、建议和 YAML 片段；Dashboard 未承诺登录、权限、审计、多租户或数据库能力 |

## 7. P1 验证结果

| 用例范围 | 结果 | 说明 |
|---|---|---|
| `BB-P1-001` | 通过 | 接入说明包含默认规则、YAML、注解、ignore 和 API ignore 内容 |
| `BB-P1-002` 到 `BB-P1-007` | 通过 | Demo 与 Dashboard 的 by-type、object、strong 主动脱敏均返回两轮结果，二次脱敏稳定 |
| `BB-P1-008` 到 `BB-P1-011` | 通过 | 实时概览、Response 风险画像、Log 规则建议和已配置 key 过滤均可观察 |
| `BB-P1-012` 到 `BB-P1-015` | 通过 | 历史报告列表、单报告查看、非报告文件拒绝和 Dashboard 导出报告均符合预期 |
| `BB-P1-016` 到 `BB-P1-020` | 通过 | 旧日志触发路径返回 4xx；字段 ignore、业务编号保留、密码脱敏和统计场景分布均符合预期 |

## 8. P2 验证结果与风险

| 用例 | 结果 | 说明 |
|---|---|---|
| `BB-P2-003` Dashboard 上传文件大小限制 | 失败 | 上传超过 1MB 的 JSON 文件时返回 HTTP 500，预期为 413 或等价可诊断错误；建议后续补充 multipart size exception 处理 |
| `BB-P2-011` 未知 type 运行期行为 | 通过 | 复核后确认 `MOBILEM` 会按 type 归一化命中 Demo 已注册的自定义 `mobileM` 策略，不属于未知 type；真正未注册的 type 应使用 `MOBILE_UNKNOWN` 等标签验证 DEFAULT fallback |
| `BB-P2-001` Dashboard 自定义 path-prefix | 未执行 | 当前 live Demo 使用默认 path-prefix；自动化基线已覆盖 dashboard starter 自定义前缀 |
| `BB-P2-002` Dashboard 默认关闭 | 未执行 | 当前 live Demo 显式启用 Dashboard；需另起未启用宿主专项验证 |
| `BB-P2-007` 超长日志跳过策略 | 未执行 | 需较小 `safe-output.log.max-message-length` 专项配置 |
| `BB-P2-008` 前缀扫描窗口策略 | 未执行 | 当前 live Demo 未专项启用 `max-scan-length` |
| `BB-P2-009` 银行卡无上下文 fallback 边界 | 未执行 | 需写入无 key 日志样例专项验证 |
| 其他 P2 项 | 通过 | 上传结构校验、静态资源、移动视口、强扫描显式边界、非目标声明和规则建议不自动采纳均通过 |

## 9. 安全检查结论

- Response 主链路脱敏通过；报告快照和导出报告未保存测试敏感原文。
- raw/ignore 明文只出现在预期的 Demo 明文接口和截图证据中，且风险画像中可见 ignored API。
- Log 场景接口不返回完整日志 message 或原始敏感值，只返回场景摘要、聚合计数、规则建议和 `enabled: false` YAML 片段。
- Dashboard 上传报告合法文件只做临时解析，不进入历史报告列表。

## 10. 证据文件

- 接口执行原始结果：`ai-contest-deliverables/assets/black-box-test/black-box-results.json`
- 修订后的判定结果：`ai-contest-deliverables/assets/black-box-test/black-box-results-corrected.json`
- 截图目录：`ai-contest-deliverables/assets/black-box-test/`
- Demo 启动日志：`ai-contest-deliverables/assets/black-box-test/safe-output-demo-run.out.log`

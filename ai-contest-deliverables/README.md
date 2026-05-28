# 赛道1 脱敏通用工具 文档材料导航

本目录收集 脱敏通用工具 参赛提交、研发过程、需求演进、架构边界、质量验证、黑盒测试和 PPT 工作稿等材料。当前 `00` 至 `08` 编号材料共 9 份文档，按“项目总述 -> 过程说明 -> 需求与架构 -> 证据验证 -> 演示呈现”的顺序组织，便于评审、复盘和后续制作参赛物料时快速定位事实来源。

## 文档目录

| 序号 | 文档 | 简要介绍 |
|---|---|---|
| 00 | [00-submission-overview.md](./00-submission-overview.md) | 参赛项目总述，说明 Safe Output 的项目定位、真实问题背景、目标用户、核心治理闭环、已完成能力、项目亮点、能力边界和下一步方向。 |
| 01 | [01-ai-development-process.md](./01-ai-development-process.md) | AI 研发过程说明，记录 AI 在需求澄清、PRD、Issue 拆解、Triage、实现、TDD、诊断、架构复盘和文档交接中的参与方式与证据链。 |
| 02 | [02-requirement-evolution.md](./02-requirement-evolution.md) | 需求演进说明，梳理从 MVP 到 R2、R2.5、R2.6、R3 的需求变化，解释 Demo、Dashboard、日志策略和展示口径如何逐步收敛。 |
| 03 | [03-architecture-decisions.md](./03-architecture-decisions.md) | 架构决策与安全边界说明，覆盖模块边界、完整调用链、starter 低侵入接入、String 类型标签、规则优先级、日志轻量识别、报告聚合和 Dashboard 抽离等关键决策。 |
| 04 | [04-efficiency-evidence.md](./04-efficiency-evidence.md) | 研发提效证据，定义统计口径，映射 AI 辅助研发带来的提效证据，并区分定量事实、定性判断和安全类项目中的约束。 |
| 05 | [05-quality-verification.md](./05-quality-verification.md) | 质量验证材料，汇总推荐验证命令、测试覆盖结构、关键验收场景、黑盒联调判断、实际运行结果、剩余风险和自检结论。 |
| 06 | [06-black-box-test-cases.md](./06-black-box-test-cases.md) | 黑盒测试材料包入口，提供测试环境准备、P0/P1/P2 黑盒用例、需求覆盖矩阵、执行记录模板和判定规则。 |
| 07 | [07-black-box-test-report.md](./07-black-box-test-report.md) | 黑盒测试报告，记录执行摘要、执行环境、自动化基线、结果汇总、关键截图证据、P0/P1/P2 验证结果、安全检查结论和证据文件。 |
| 08 | [08-presentation-working-notes.md](./08-presentation-working-notes.md) | PPT 工作稿，沉淀整体视觉风格、模板图片资产要求、当前大纲、20 页逐页终稿说明和最终制作自检清单。 |

## 使用建议

- 快速了解项目：先读 `00-submission-overview.md`，再读 `03-architecture-decisions.md`。
- 复盘 AI 研发过程：按 `01-ai-development-process.md`、`02-requirement-evolution.md`、`04-efficiency-evidence.md` 的顺序阅读。
- 验证交付质量：从 `05-quality-verification.md` 进入，再查看 `06-black-box-test-cases.md` 和 `07-black-box-test-report.md`。
- 准备演示或答辩：优先查看 `08-presentation-working-notes.md`，并回到前述材料核对能力边界与事实来源。

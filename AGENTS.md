# Agent Instructions

- 后续所有输出以中文优先；仅在代码、命令、文件名、API 名称或用户明确要求时使用英文。

## 项目简介

Safe Output 是 Java 8 / Spring Boot 2.x 通用数据脱敏 starter，用于在少改业务代码的前提下处理 Response、Log4j2 日志、主动脱敏和聚合统计报告。

## 技术栈

- Java 8
- Maven multi-module
- Spring Boot 2.7.18
- Spring MVC `ResponseBodyAdvice`
- Log4j2 `PatternConverter`
- JUnit 5 / Spring Boot Test

## 目录结构

- `safe-output/`: Maven 父工程。
- `safe-output/safe-output-core/`: 规则、策略、注解、对象递归、主动脱敏。
- `safe-output/safe-output-log4j2/`: `%safeOutputMsg` 日志脱敏。
- `safe-output/safe-output-report/`: 指标、风险画像、报告导出、规则建议。
- `safe-output/safe-output-spring-boot-starter/`: 自动装配和对外 starter。
- `safe-output/safe-output-demo/`: Spring Boot demo 和前端控制台。
- `.codex-memory/`: 后续编码前必须读取的项目记忆。

## 运行命令

- Demo：`cd safe-output && mvn -pl safe-output-demo -am spring-boot:run`
- 浏览器入口：`http://localhost:8080/index.html`
- 本地安装：`cd safe-output && mvn install`

## 测试命令

- 快速全量测试：`cd safe-output && mvn test`
- 完整验证：`cd safe-output && mvn verify`
- Demo 集成测试：`cd safe-output && mvn -pl safe-output-demo -am test`
- Starter 测试：`cd safe-output && mvn -pl safe-output-spring-boot-starter -am test`

## 编码约束

- 修改前先按实际文件名依次读取以下项目记忆文件，避免只按编号猜测文件名：
  - `.codex-memory/00-project-current-state.md`
  - `.codex-memory/01-module-map.md`
  - `.codex-memory/02-core-flow-map.md`
  - `.codex-memory/03-decision-and-boundary.md`
  - `.codex-memory/04-next-round-handoff.md`
- 后续任何编码操作如果改变模块职责、核心调用链、设计边界、运行命令、测试命令、Demo 能力或第三轮交接信息，必须同步修订 `.codex-memory/` 中对应文档。
- 保持 Java 8 和 Spring Boot 2.x 兼容，不随意引入 Boot 3 API。
- 优先沿用现有 String 类型标签、`MaskStrategy`、`MaskRuleMatcher`、`MaskMetricsCollector` 模型。
- 新增脱敏能力必须补单元或集成测试。
- 文档和报告不得包含敏感原文。

## 安全边界

- 不引入 fastjson。
- 不做粗暴全局正则乱扫。
- Response 脱敏异常必须 fail-open，不能影响业务接口。
- 统计、报告、规则建议只保存聚合信息或脱敏 evidence，不保存原始 response、完整日志或敏感值。
- API ignore 可以返回明文，但必须进入风险统计。
- 日志只做轻量 JSON-like/key-value 识别，不强制依赖 JSON Parser。

## 禁止行为

- 不要未经要求重构模块边界。
- 不要删除已有文件。
- 不要引入新依赖解决小问题。
- 不要把日志/报告改成保存敏感样本。
- 不要把未知 type 自动回退成 `DEFAULT`，除非需求明确并同步测试。
- 不要跳过 fail-open 边界。

## Issue tracker

Issues are tracked as local Markdown files under `.scratch/safe-output-mvp/`. See `docs/agents/issue-tracker.md`.

## Triage labels

Triage uses the default canonical role names for category and state labels. See `docs/agents/triage-labels.md`.

## Domain docs

This repo uses a single-context domain layout with root `CONTEXT.md`. See `docs/agents/domain.md`.

## 演讲 PPT 工作流

- 当前阶段需要制作竞赛演讲 PPT；每次修改 PPT、讲稿、截图说明、页面文案或其他参赛材料前，必须先通读 `ai-contest-deliverables/` 下相关 Markdown 文档，并以这些材料作为内容事实来源。
- 每次确认 PPT 页面内容后，必须同步生成对应的 PPT 生图提示词；提示词应匹配已确认页面主题、画面主体、风格、比例和需要避开的敏感原文。
- PPT 与生图提示词不得编造未在项目代码、`.codex-memory/` 或 `ai-contest-deliverables/` 中确认的能力、指标或结论。
- 当用户要求生成 PPT 页面图片时，必须使用 image 生图能力，不得只输出文字提示词或用占位图替代。
- 生图前必须先确认用户需要生成的 PPT 页码；若用户未明确页码，先询问页码，不得直接生成。
- 页码确认后，先读取并整合 `ai-contest-deliverables/08-presentation-working-notes.md` 中的整体风格基调、模板图片资产要求和对应页码的逐页说明，汇总成当前页专用生图提示词。
- 执行生图前，必须先把汇总后的生图提示词发给用户二次确认；用户确认后才能调用 image 生图能力。
- 生图完成后，必须将图片资产落在 `ai-contest-deliverables/ppt/` 目录下，并按页码正确重命名；文件名使用两位页码前缀，例如 `01-cover.png`、`08-output-side-integration.png`，避免覆盖无关页面资产。
- 生图资产不得包含真实手机号、身份证、银行卡、邮箱、密码、完整日志、完整 response 或业务敏感原文；如页面涉及 raw 明文，只能表达 Demo/mock 风险统计概念，不生成具体明文字段值。

## Definition of Done

- 代码路径符合 `.codex-memory` 中的设计边界，或明确记录实现偏差。
- 相关模块测试通过，至少运行受影响模块的 `mvn -pl ... -am test`。
- 新能力有 Demo 或测试可验证。
- 不保存敏感原文。
- 最终说明变更文件、测试命令和任何剩余风险。

## 工作原则

- 提交代码时，必须按语义拆分为细颗粒度 commit，并遵守commit规范
- 对现有功能实现有疑问时，优先通过排查代码进行排查，确认无误或修订完成后应修订文档

# safeoutput-java

Safe Output 是面向 Spring Boot 2.x / Java 8 的输出侧脱敏组件。主工程位于 `safe-output/`，提供 response 脱敏、Log4j2 日志脱敏、主动脱敏、聚合报告和 R2 Demo 验证接口。

## 目录地图

| 路径 | 定位 |
|---|---|
| `safe-output/` | Maven 多模块产品工程，包含 core、log4j2、report、starter、dashboard starter 和 demo。 |
| `docs/project/` | 维护者文档、PRD、设计说明和模块深挖资料。 |
| `docs/agents/` | Agent 协作约定、issue tracker 和领域文档说明。 |
| `.codex-memory/` | 编码前必须读取的项目记忆和交接信息。 |
| `.scratch/safe-output-mvp/` | 本地 Markdown issue tracker。 |
| `ai-contest-deliverables/` | 竞赛交付材料、演讲稿、PPT 工作说明和图片资产。 |
| `.agents/` | 项目内 Agent 技能定义。 |

## 快速验证

```sh
cd safe-output
mvn test
```

完整使用说明见 `safe-output/README.md`，项目级文档入口见 `docs/README.md`。

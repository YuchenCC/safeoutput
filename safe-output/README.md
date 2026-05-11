# Safe Output 源码入口

本目录是 Safe Output 的 Maven 源码入口。仓库级规划、领域文档和本地 issue 继续保留在 `doc/`、`docs/` 和 `.scratch/` 下，避免和源码工程混放。

## 模块

- `safe-output-core`: 内部模块，承载核心模型、策略和规则基础。
- `safe-output-log4j2`: 内部模块，承载 Log4j2 输出侧脱敏适配。
- `safe-output-report`: 内部模块，承载指标统计和报告快照能力。
- `safe-output-spring-boot-starter`: 对外发布入口，供 Spring Boot 2.x 应用直接引用。
- `safe-output-demo`: Demo 空骨架，预留给后续验收场景。

业务应用应引用 `com.safeoutput:safe-output-spring-boot-starter:0.1.0-SNAPSHOT`。

## 构建

```sh
mvn verify
```

构建会校验 Java 8 编译约束、Java 8 API 使用边界、Checkstyle 规则和 starter 打包契约。

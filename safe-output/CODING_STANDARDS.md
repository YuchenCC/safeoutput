# Safe Output 编码规范

Safe Output 面向运行在 JDK8 上的遗留 Spring Boot 2.x 服务。以下规则用于保证 starter 易于被老项目接入，并减少依赖冲突和运行时意外。

## Java 基线

- 使用 Java 8 source/target 兼容性编译。
- 不使用 Java 9+ API、语言特性、模块描述符或 Spring Boot 3.x 专属 API。
- 优先设计明确且小的公共接口。除扩展点或 Spring 集成入口外，实现类应尽量保持包内可见。
- 脱敏代码应把 null、空值和格式异常值视为普通输入。公共 API 不应因常见脏数据抛出业务异常。

## Maven 与依赖

- `safe-output-spring-boot-starter` 是业务应用唯一推荐的外部依赖入口。
- `safe-output-core`、`safe-output-log4j2` 和 `safe-output-report` 保持为 starter 聚合的内部模块。
- 子模块不显式声明 Spring Boot 依赖版本，版本由父工程 dependency management 统一管理。
- 除非具体 feature issue 明确要求，否则避免引入服务端、Web 容器、数据库和日志实现依赖。
- 当集成依赖不是消费方应用编译期必需依赖时，应标记为 optional。

## 包与模块边界

- 使用包名前缀 `com.safeoutput`。
- 核心领域抽象保持与 Spring 无关。
- Spring Boot 自动装配代码放在 `com.safeoutput.spring.boot.autoconfigure`。
- Log4j2 专属代码不得进入 core。
- 报告持久化和调度逻辑不得进入 core 脱敏策略代码。

## 测试

- 测试应验证公共行为和模块契约，不绑定私有 helper。
- 每个测试优先覆盖一个行为，测试名描述可观察结果。
- 验证 Maven 打包、starter 资源或自动装配连线时，优先使用集成风格测试。
- 修改行为前先补一个失败测试。

## 风格

- 代码默认使用 ASCII；领域示例或文档需要中文时可以使用非 ASCII。
- 使用 `CONTEXT.md` 中定义的清晰术语：`MaskType`、`MaskScene`、`Rule`、`Ignore`、`Regex fallback` 和 `报告快照`。
- 只有在解释非显而易见的约束或兼容性原因时才添加注释。
- 生成物、构建输出和本地报告不得提交到源码控制。

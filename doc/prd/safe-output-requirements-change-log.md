# Safe Output 需求变更记录

本文档用于管理 Safe Output MVP 的需求迭代、范围调整和后续工作下发。主需求以 [Safe Output MVP 主需求文档](./safe-output-mvp-prd.md) 为准；关键需求决策以 [需求澄清说明](./safe-output-requirements-clarifications.md) 为准。

## 1. 使用规则

1. 所有需求变更先记录在本文档，再决定是否同步修改主 PRD。
2. 已确认并影响交付范围的变更，必须补充影响分析和后续工作项。
3. 只影响解释口径、不改变范围的内容，优先记录到澄清说明文档。
4. 已进入实现的变更应关联 issue、WBS 或里程碑。
5. 变更记录不删除历史结论；废弃项通过状态标记。

## 2. 状态定义

| 状态 | 含义 |
|---|---|
| Proposed | 已提出，尚未确认 |
| Accepted | 已确认，需要进入主 PRD 或后续 issue |
| Rejected | 已拒绝，不进入范围 |
| Superseded | 已被后续变更替代 |
| Implemented | 已完成实现或文档同步 |

## 3. 变更记录模板

```md
### CR-YYYYMMDD-NN 变更标题

- 状态：
- 来源：
- 关联文档：
- 关联 WBS：
- 关联 issue：
- 变更摘要：
- 变更原因：
- 范围影响：
- 验收影响：
- 后续工作：
```

## 4. 当前基线

### CR-20260511-01 建立 v0.4 MVP 需求基线

- 状态：Accepted
- 来源：`doc/java_data_desensitization_mvp_scope_and_wbs_v0.3.md`
- 关联文档：[Safe Output MVP 主需求文档](./safe-output-mvp-prd.md)
- 关联 WBS：WBS-00 至 WBS-12
- 变更摘要：将原始综合 PRD 拆分为主 PRD、需求变更记录、需求澄清说明三份文档。
- 变更原因：原始文档同时承载需求范围、版本沿革、WBS、澄清计划和后续决策，不利于后续 issue 拆分、迭代管理和关键变更纪要维护。
- 范围影响：不改变 MVP 实现范围，只调整需求管理结构。
- 验收影响：后续 issue 拆分以主 PRD 的 WBS 和验收标准为准。
- 后续工作：后续新增或调整需求时，先在本文档记录，再同步主 PRD 或澄清说明。

### CR-20260511-02 明确最终交付物为 Spring Boot 2.x Starter Jar

- 状态：Implemented
- 来源：用户要求“最终的交付物应该是个 java 包，可以供 springboot2.x 系统直接引用接入，同时更新 WBS”
- 关联文档：[Safe Output MVP 主需求文档](./safe-output-mvp-prd.md)
- 关联 WBS：WBS-00、WBS-03、WBS-04、WBS-07、WBS-08、WBS-10、WBS-12、WBS-13
- 变更摘要：将 MVP 对外交付物明确为 `safe-output-spring-boot-starter` Maven Jar，业务系统通过 Spring Boot 2.x starter 直接引用接入；新增 `WBS-13 Starter 打包与发布验证`。
- 变更原因：原 PRD 虽然包含 starter 模块，但交付物章节仍以多模块源码为主，容易让后续 issue 偏向源码工程实现，而不是可被业务系统直接接入的 Java 包。
- 范围影响：不新增脱敏能力范围，但新增 starter 打包、安装、依赖树、自动装配和 Demo 外部引用验证要求。
- 验收影响：MVP 成功标准新增“能生成 `safe-output-spring-boot-starter` Jar”和“Spring Boot 2.x 业务系统只引用 starter 即可完成自动装配”。
- 后续工作：从主 PRD 拆 issue 时，需要为 WBS-13 单独创建打包与引用验证 issue；Demo issue 必须禁止直接依赖内部模块绕过 starter。

## 5. 已纳入基线的版本沿革

### 5.1 v0.4 调整

1. 删减 WBS 中的过程性沟通内容，不再在每个工作包中展开工具规则生成说明。
2. 将 WBS 从“工具执行规划”收敛为“需求范围和可交付工作包”。
3. 新增阶段性目标和里程碑，明确 MVP 从基础骨架到演示验收的推进节奏。
4. 新增整体需求澄清计划，按推荐结论一次性确认关键需求边界。

### 5.2 v0.3 边界

1. 日志脱敏 MVP 优先支持 Log4j2 2.x，不再以 Logback 作为第一优先级。
2. 日志模块设计为可扩展日志适配层，后续预留兼容 Logback 的扩展点。
3. 统计报告不再只做内存聚合，需要增加定时写入本地 JSON 文件机制，避免应用重启后统计完全丢失。
4. 定时文件写入只输出聚合指标快照，不保存敏感原文、不保存完整 response、不保存完整日志。
5. 统计文件写入采用定时任务方式，不在请求链路同步刷盘。

### 5.3 v0.2 边界

1. 模糊字段处理策略：`name`、`id`、`code`、`no` 不进入默认强脱敏规则。
2. 支持字段级 ignore、接口级 ignore、包级 ignore 的脱敏豁免机制。
3. 规则优先级：接口级 ignore > 字段级 ignore > 注解规则 > path rule > key rule > default rule > regex fallback。
4. 日志场景不引入 fastjson，采用轻量 JSON-like key-value 识别 + 正则兜底。
5. 全局正则兜底默认支持手机号、邮箱、严格大陆身份证。
6. 银行卡不做无上下文全局正则兜底。
7. Spring Boot 2.x 自动装配使用 `spring.factories`，不得只适配 Spring Boot 3.x 自动装配机制。
8. 接口风险统计 MVP 只做 response 场景。
9. 统计报告不保存敏感原文、不保存完整 response、不保存完整日志。
10. 接口级 ignore 的接口不执行 response 脱敏，但仍进入接口风险统计。

## 6. 后续变更待办池

以下能力当前不进入 MVP，如后续要推进，应先新增变更记录，再拆 issue：

1. Logback 日志脱敏。
2. WebFlux 支持。
3. RPC/Dubbo Filter 支持。
4. MyBatis 查询结果脱敏。
5. 配置中心热更新。
6. 基于用户权限或角色的动态脱敏。
7. 治理平台与可视化后台。
8. Prometheus、Actuator 指标集成。
9. 分布式统计聚合。
10. Log 场景接口归因。
11. 业务文件导出内容脱敏。
12. 消息队列脱敏。
13. FastJson 深度集成。

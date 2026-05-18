# 设计决策与边界

- 不引入 fastjson：当前 core/report/log/starter 未引入 fastjson，报告 JSON 为手写输出，starter 测试只用 Jackson 作为测试转换器。
- 不做粗暴全局正则乱扫：Response 默认不全局扫字符串；日志仅先处理 key-value/JSON-like 片段，再做有限 fallback。
- 除手机、身份证、邮箱外，不做无上下文全局兜底：`SafeOutputLogMessageMasker` 和 `StrongTextMasker` 默认允许对 `MOBILE` / `ID_CARD` / `EMAIL` 做无上下文 fallback；`StrongTextMasker` 还可通过配置启用 `BANK_CARD` fallback，需注意该配置可能超出默认边界。
- 日志只做轻量 JSON-like 识别，不强制依赖 JSON Parser：`SafeOutputLogMessageMasker.KEY_VALUE` 正则处理 `"key":"value"`、`key=value`，无 JSON Parser 依赖。
- Response ignore 后仍应进入风险统计：`SafeOutputResponseBodyAdvice` 命中 API ignore 后返回原 body，但调用 `recordRisk(ignored=true)`。
- 统计不保存敏感原文：`MaskMetricsCollector`、`ResponseRiskEvent`、报告输出只保留计数、类型、接口、耗时、脱敏字段数量；日志建议 evidence 为 `key=<type>` 形态。
- 脱敏异常不能影响主业务：Response、Object、Log、Manual、Report 均采用 fail-open 或记录失败指标。
- 老项目即插即用优先：Java 8、Spring Boot 2.7.18、`spring.factories`、starter 聚合内部模块，支持 `single-jar` profile。
- 脱敏规则优先级：`MaskRuleMatcher.decide` 当前确认顺序为 API ignore / 字段 ignore > 注解 > 配置/默认规则 > regex fallback；后续改动需同步测试该优先级边界。
- 内置默认字段规则由 `DefaultMaskRules.all()` 统一维护；文档默认规则表应以该类为准，避免在 `MaskRuleMatcher` 或文档中重复维护分叉清单。
- 默认规则库可通过 starter 配置 `safe-output.rules.default-enabled=false` 关闭；关闭后只移除内置默认规则，不影响注解、用户配置 rules、ignore 或 regex fallback。
- Spring Boot 2.x 兼容优先：使用 `spring.factories` 和 `spring-boot-autoconfigure`，未迁移 Boot 3 `AutoConfiguration.imports`。

## 额外边界

- 未知脱敏类型默认 `warn + DEFAULT fallback`，并进入未知类型聚合统计；`DEFAULT` 对非空字符串统一输出 `****`。
- 默认字段规则只覆盖语义明确字段名，`name/id/code/no` 这类歧义字段需配置或注解；老系统担心默认 key 误伤时应关闭默认规则库并显式配置。
- `rules[].paths` 和 `ignore.paths` 使用 Safe Output 递归路径，不是完整 JSONPath；`$` 是被脱敏对象根节点，`.` 是字段或 Map key 层级，`[*]` 仅匹配集合/数组数字下标段，不支持 `**`、条件表达式、字段通配或模糊匹配。
- Log4j2 converter 由日志系统创建，starter 通过 `SafeOutputLog4j2Runtime` 做进程级桥接，把 Spring 规则、策略、log 选项、未知类型 recorder、日志脱敏计数 recorder 和日志规则建议 collector 提供给真实 `%safeOutputMsg`；无 Spring 注册时仍使用 `%safeOutputMsg{...}` options 和默认规则。
- `logCount` 统计单位是成功脱敏的日志值次数，不是日志行数；日志脱敏禁用、超长 message fail-open 或策略异常 fail-open 不记录成功计数。
- 日志长文本只在 `maxMessageLength` 限制内完整处理多个 key-value 和 fallback；超过限制整条 fail-open，避免不可控扫描成本。
- 报告只做聚合快照和建议生成，不做自动治理决策。

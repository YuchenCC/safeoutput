# safe-output-core

`safe-output-core` 是 Safe Output 的核心引擎模块，不依赖 Spring。它负责定义脱敏领域模型、规则匹配、内置脱敏策略、注解解析和响应对象递归脱敏。

## 职责

- 定义 `MaskTypes`、`MaskType`、`MaskScene`、`MaskRule`、`RuleMatch` 等核心模型。
- 通过 `MaskRuleMatcher` 执行固定优先级的规则决策。
- 通过 `ObjectMasker` 遍历 Bean、Map、Collection 和数组，并在命中规则时改写字符串值。
- 通过 `SafeOutputMaskService.mask(value, type)` 提供指定类型的主动脱敏入口。
- 通过 `MaskStrategyRegistry` 注册内置策略和调用方自定义策略。
- 通过 `SensitiveFieldResolver` 缓存字段元数据，并解析 `@Desensitize` 注解。

## 规则优先级

`MaskRuleMatcher.decide()` 的优先级从高到低为：

1. API Ignore
2. 字段 Ignore
3. `@Desensitize` 注解
4. 配置 Rule
5. 默认 Rule
6. Regex fallback

其中 `name`、`id`、`code`、`no` 这类歧义字段不会被默认规则命中，需要显式 Rule 或注解覆盖。

`CHINESE_NAME` 是兼容旧命名的通用姓名类型。命中该类型后会按首尾保留、中间脱敏处理中文姓名、英文姓名、中英混合姓名和带空格等分隔符的姓名；`name` 这类歧义字段仍不会因为字段名本身被默认规则强制脱敏。

`ID_CARD` 在响应、注解、配置 path/key 或日志 key-value 等明确上下文命中时直接脱敏；无字段上下文的日志 fallback 仍只在通过轻量格式、日期、年份和可选校验位检查后脱敏。

核心扩展契约使用 String 类型标签贯穿策略、规则、上下文、结果和统计链路。`MaskTypes` 提供内置标准类型常量，`MaskType` 保留为内置清单和兼容入口。

规则命中但未找到对应策略时默认跳过当前字段，不回退到 `DEFAULT`；核心会记录 warning，并通过 `UnknownTypeRecorder` 暴露聚合统计钩子。成功脱敏会通过 `MaskEventRecorder` 记录场景、String 类型标签和耗时，便于内置类型和自定义类型共用同一统计链路。

## 关键入口

- `ObjectMasker.mask(Object)`: 响应对象脱敏入口。
- `SafeOutputMaskService.mask(String, String)`: 主动按类型标签脱敏单个字符串。
- `SafeOutputMaskService.maskObject(Object)`: 主动按响应对象规则递归脱敏 Bean、Map、Collection 和数组。
- `MaskRuleMatcher.decide(MaskRuleRequest)`: 统一规则决策入口。
- `MaskStrategyRegistry.withBuiltIns(...)`: 内置策略和自定义策略注册入口。
- `BuiltInMaskStrategies.strategies()`: 内置策略集合。

## 本模块验证

在 `safe-output/` 根目录执行：

```sh
mvn -pl safe-output-core test
```

核心测试覆盖默认规则、注解解析、对象递归脱敏、循环引用保护、集合限制、策略注册和内置策略边界。


## 工程解析：Response 拦截与脱敏

整个方案分两层：**Spring MVC 拦截层**（`safe-output-spring-boot-starter`）负责"在哪里拦截"，**Core 引擎**（`safe-output-core`）负责"如何脱敏"。

R2 起，脱敏类型使用 String 类型标签贯穿规则、策略、上下文和统计链路。`MaskType` 仍保留为内置清单和兼容入口；业务自定义类型通过 `MaskStrategy.type()` 与配置中的 `rules[].type` 对齐。未知 type 的默认处理是 `warn + skip`，不会回退到 `DEFAULT`。

---

### 一、Response 拦截方式：`ResponseBodyAdvice`

Spring MVC 提供了 `ResponseBodyAdvice` 接口，可在消息体序列化（JSON 输出）**之前**修改返回值。这是最干净的拦截点——不需要 Filter/AOP，也不会影响返回值类型。

```20:66:safe-output/safe-output-spring-boot-starter/src/main/java/com/safeoutput/spring/boot/autoconfigure/SafeOutputResponseBodyAdvice.java
@ControllerAdvice
public class SafeOutputResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return properties != null && properties.isEnabled() && properties.getResponse().isEnabled();
        // 全局开关：两个配置项都开启才激活
    }

    @Override
    public Object beforeBodyWrite(Object body, ...) {
        try {
            Optional<ApiIgnoreMatch> apiIgnore = matchApiIgnore(request);
            if (apiIgnore.isPresent()) {
                recordRisk(... ignored=true ...);
                return body;   // 命中 API Ignore → 原样放行，但保留风险画像基础数据
            }
            String bodyDataPath = properties.getResponse().getBodyDataPath();
            if (bodyDataPath != null && !bodyDataPath.isEmpty()) {
                Object data = extractData(body, bodyDataPath);   // 按路径提取业务数据
                if (data != null) {
                    MaskingResult result = objectMasker.maskWithResult(data, MaskScene.RESPONSE);
                    setData(body, bodyDataPath, result.getValue());   // 脱敏后回写，包装层不变
                    recordRisk(... result.getMaskTypeCounts() ...);
                }
                return body;   // 只脱敏 data，包装层原样保留
            }
            MaskingResult result = objectMasker.maskWithResult(body, MaskScene.RESPONSE);
            recordRisk(... result.getMaskTypeCounts() ...);
            return result.getValue();   // 否则 → 进脱敏引擎并记录轻量聚合摘要
        } catch (RuntimeException ex) {
            return body;   // fail-open：异常时原样输出
        }
    }
}
```

**自动装配**通过 `SafeOutputMvcAutoConfiguration` 注册为 Spring Bean，条件是 classpath 上有 `ResponseBodyAdvice`（即有 spring-webmvc）：

```19:31:safe-output/safe-output-spring-boot-starter/src/main/java/com/safeoutput/spring/boot/autoconfigure/SafeOutputMvcAutoConfiguration.java
@Configuration
@ConditionalOnClass(ResponseBodyAdvice.class)
public class SafeOutputMvcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SafeOutputResponseBodyAdvice safeOutputResponseBodyAdvice(...) {
        return new SafeOutputResponseBodyAdvice(objectMasker, properties,
                new ApiIgnoreMatcher(properties.getIgnore()), recorders);
    }
}
```

**包装层跳过**（`bodyDataPath`）：许多业务系统在 Controller 返回值外层套了 `Result<T>` 等统一包装，其中 `code`/`message` 等字段无需脱敏。配置 `safe-output.response.body-data-path` 后，Advice 会先按路径提取业务数据，只对 data 部分调用脱敏引擎，然后将脱敏后的 data 回写到 body 原位。包装层不参与脱敏，不消耗 depth，也不触发无关字段的规则匹配。

```yaml
safe-output:
  response:
    body-data-path: data          # 取 body.data 脱敏
    # body-data-path: result.data # 多层：取 body.result.data 脱敏
```

路径解析规则：点分路径逐级导航，Bean 通过反射取字段（含父类），Map 按 key 取值。路径不存在时 fail-open，原样返回 body。

---

### 二、脱敏引擎：三层流水线

#### 第一层：对象图遍历 `ObjectMasker`

`ObjectMasker.mask(body)` 从根节点出发，递归遍历整个对象图。Response 链路使用 `maskWithResult(...)`，额外返回本次调用的脱敏字段数量和类型分布摘要；摘要不包含字段路径、敏感原文或脱敏后的完整 response。

```41:72:safe-output/safe-output-core/src/main/java/com/safeoutput/core/ObjectMasker.java
private Object maskValue(Object value, String path, String key, int depth, Set<Object> visiting) {
    if (value == null || isUnsupported(value) || isSimpleValue(value)) {
        return value;  // Number/Boolean/Date/Enum 直接跳过
    }
    if (visiting.contains(value)) {
        return value;  // 循环引用保护（IdentityHashMap）
    }
    if (depth > options.getMaxDepth()) {
        return value;  // 深度限制
    }
    if (value instanceof String)    → maskString(...)
    if (value instanceof Map)       → maskMap(...)      // 按 key 拼 JSONPath
    if (value instanceof Collection)→ maskCollection(...)
    if (value.getClass().isArray()) → maskArray(...)
    else                            → maskBean(...)     // 反射遍历字段
}
```

**Bean 字段处理**（反射 + 原地改写）：

```116:140:safe-output/safe-output-core/src/main/java/com/safeoutput/core/ObjectMasker.java
private Object maskBean(Object bean, String path, int depth, Set<Object> visiting) {
    for (Field field : fields(bean.getClass())) {
        field.setAccessible(true);
        Object current = field.get(bean);
        Optional<RuleMatch> match = fieldResolver.resolve(field, childPath(path, field.getName()));
        if (current instanceof String && match.isPresent() && match.get().getAction() == RuleAction.MASK) {
            field.set(bean, applyStrategy(...));   // 字符串字段 → 直接改写
        } else {
            field.set(bean, maskValue(current, ...));  // 其他类型 → 递归
        }
    }
    return bean;
}
```

路径格式是 JSONPath 风格（如 `$.user.mobile`），支持 path 精准匹配。

---

#### 第二层：规则匹配 `MaskRuleMatcher`

匹配优先级（硬编码，测试用例固定）：

| 优先级 | 来源 | 动作 |
|--------|------|------|
| 1 | `apiIgnored=true`（API 白名单） | IGNORE |
| 2 | `ignoreKeys` / `ignorePaths`（字段豁免） | IGNORE |
| 3 | 字段上 `@Desensitize` 注解 | MASK（注解指定类型） |
| 4 | 配置规则 path → 配置规则 key | MASK |
| 5 | 默认规则 path → 默认规则 key | MASK |
| 6 | `regexFallbackType`（正则回退） | MASK |

```47:61:safe-output/safe-output-core/src/main/java/com/safeoutput/core/MaskRuleMatcher.java
public Optional<RuleMatch> match(String key, String path) {
    // 配置规则 path → 配置规则 key → 默认规则 path → 默认规则 key
    Optional<RuleMatch> configuredPath = matchPath(configuredRules, path);
    ...
    return matchKey(defaultRules, key);
}
```

**内置默认 key 列表**（key 匹配大小写不敏感）：

```java
mobile / phone / telephone / tel / userMobile  → MOBILE
idCard / certNo / identityNo / certificateNo   → ID_CARD
bankCard / cardNo / bankNo                     → BANK_CARD
email / mail                                   → EMAIL
password / secret / token                      → PASSWORD
```

---

#### 第三层：脱敏策略 `BuiltInMaskStrategies`

每种 `MaskType` 对应一个纯函数策略，注册在 `EnumMap` 中：

```49:108:safe-output/safe-output-core/src/main/java/com/safeoutput/core/BuiltInMaskStrategies.java
// 手机号：138****8888（正则校验 1[3-9]\d{9}）
return rawValue.substring(0, 3) + "****" + rawValue.substring(7);

// 身份证：320123********1234（MainlandIdCards.isValid 校验 18 位）
return rawValue.substring(0, 6) + "********" + rawValue.substring(14);

// 银行卡：622526****1234（12-19 位纯数字）
return rawValue.substring(0, 6) + repeat('*', len - 10) + rawValue.substring(len - 4);

// 邮箱：abc****@qq.com
return rawValue.substring(0, 3) + "****" + rawValue.substring(atIndex);

// 密码：固定 ********
return "********";

// DEFAULT：保留首 2 + 尾 2
return rawValue.substring(0, 2) + "****" + rawValue.substring(len - 2);
```

策略遵循 **fail-open**：格式校验不通过（如不是合法手机号）时返回原值而非报错。自定义策略可通过 `MaskStrategyRegistry.withBuiltIns(customList)` 注入。

---

### 三、完整调用链路

```
HTTP 请求
  └→ Spring MVC DispatcherServlet
       └→ Controller 方法执行，返回 body 对象
            └→ SafeOutputResponseBodyAdvice.beforeBodyWrite()
                 ├─ [命中 API Ignore] → 原样返回，并记录 ignored 风险事件
                 ├─ [配置 bodyDataPath] → extractData(body, path) 提取业务数据
                 │    └→ ObjectMasker.maskWithResult(data) 只脱敏 data
                 │    └→ setData(body, path, maskedData) 回写，包装层不变
                 │    └→ 返回原始 body（包装层 + 脱敏后 data）
                 └─ [正常] → ObjectMasker.maskWithResult(body)
                              └→ maskValue() 递归遍历对象图
                                   ├─ Bean 字段 → SensitiveFieldResolver
                                   │              └→ @Desensitize 注解 or MaskRuleMatcher.decide()
                                   ├─ Map key   → MaskRuleMatcher.decide(key, path)
                                   └─ 命中规则  → MaskStrategyRegistry.find(type)
                                                  └→ BuiltInMaskStrategy.mask(rawValue)
                                                       └→ 脱敏后的字符串（原地替换）
                              └→ 脱敏后的 body 对象
  └→ HttpMessageConverter 序列化 → JSON 输出给客户端
```

---

### 四、R2 统计和报告边界

- `MANUAL` 场景来自 `SafeOutputMaskService` 的主动脱敏调用，用于统计显式调用量和类型分布；它不默认进入 Response 接口风险统计。
- Response 风险统计只记录稳定接口标识、耗时、失败状态、ignore 状态、脱敏字段数量和类型分布，不记录原始 response、脱敏后 response、敏感原文或单次字段明细。
- Log regex fallback 可采集 nearbyKey 线索；线索只包含 key、type、hitCount、时间和脱敏后的 evidence，不保存完整日志或命中值。
- 报告中的风险原因、性能告警和规则建议均为治理辅助信息，不自动修改运行配置。

### 五、设计要点总结

| 特性 | 实现 |
|------|------|
| **拦截点** | `ResponseBodyAdvice.beforeBodyWrite`，序列化前，无侵入 |
| **拦截顺序** | `@Order(Ordered.HIGHEST_PRECEDENCE)`，确保在其他 Advice 之前拿到原始 body |
| **包装层跳过** | `bodyDataPath` 配置点分路径（如 `data`、`result.data`），只提取并脱敏业务数据，包装层原样保留 |
| **字段识别** | 字段名 key 匹配（大小写不敏感）+ JSONPath 精准 path 匹配 |
| **注解支持** | `@Desensitize(type = MaskType.XXX)` 标注在字段上，优先级高于规则 |
| **循环引用** | `IdentityHashMap` 访问集防止死循环 |
| **安全降级** | 全链路 fail-open，任何异常均返回原值，不影响业务 |
| **可扩展性** | `MaskStrategy` SPI，注入自定义 String 类型标签策略即可覆盖或新增脱敏类型 |
| **API Ignore** | 配置路径/方法，整个接口不脱敏，并通过 `ResponseRiskRecorder` 记录 ignored 风险事件 |
| **报告安全边界** | 只保存聚合指标和治理建议，不保存敏感原文、完整 response 或完整日志 |

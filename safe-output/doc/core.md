
## 工程解析：Response 拦截与脱敏

整个方案分两层：**Spring MVC 拦截层**（`safe-output-spring-boot-starter`）负责"在哪里拦截"，**Core 引擎**（`safe-output-core`）负责"如何脱敏"。

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
                recordRisk(request, true, apiIgnore.get().getReason());
                return body;   // 命中 API 白名单 → 原样放行
            }
            recordRisk(request, false, null);
            return objectMasker.mask(body);   // 否则 → 进脱敏引擎
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

---

### 二、脱敏引擎：三层流水线

#### 第一层：对象图遍历 `ObjectMasker`

`ObjectMasker.mask(body)` 从根节点出发，递归遍历整个对象图：

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
                 ├─ [命中 API 白名单] → 原样返回
                 └─ [正常] → ObjectMasker.mask(body)
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

### 四、设计要点总结

| 特性 | 实现 |
|------|------|
| **拦截点** | `ResponseBodyAdvice.beforeBodyWrite`，序列化前，无侵入 |
| **字段识别** | 字段名 key 匹配（大小写不敏感）+ JSONPath 精准 path 匹配 |
| **注解支持** | `@Desensitize(type = MaskType.XXX)` 标注在字段上，优先级高于规则 |
| **循环引用** | `IdentityHashMap` 访问集防止死循环 |
| **安全降级** | 全链路 fail-open，任何异常均返回原值，不影响业务 |
| **可扩展性** | `MaskStrategy` SPI，注入自定义策略即可覆盖或新增脱敏类型 |
| **API 豁免** | 配置白名单路径/方法，整个接口不脱敏，并通过 `ResponseRiskRecorder` 记录风险事件 |
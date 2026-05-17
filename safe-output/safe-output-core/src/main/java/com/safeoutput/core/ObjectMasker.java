package com.safeoutput.core;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public final class ObjectMasker {

    private static final Logger LOGGER = Logger.getLogger(ObjectMasker.class.getName());

    private final MaskStrategyRegistry strategyRegistry;

    private final MaskRuleMatcher ruleMatcher;

    private final SensitiveFieldResolver fieldResolver;

    private final ObjectMaskerOptions options;

    private final UnknownTypeRecorder unknownTypeRecorder;

    private final MaskEventRecorder maskEventRecorder;

    public ObjectMasker(MaskStrategyRegistry strategyRegistry, MaskRuleMatcher ruleMatcher,
            ObjectMaskerOptions options) {
        this(strategyRegistry, ruleMatcher, null, options);
    }

    public ObjectMasker(MaskStrategyRegistry strategyRegistry, MaskRuleMatcher ruleMatcher,
            SensitiveFieldResolver fieldResolver, ObjectMaskerOptions options) {
        this(strategyRegistry, ruleMatcher, fieldResolver, options, null);
    }

    public ObjectMasker(MaskStrategyRegistry strategyRegistry, MaskRuleMatcher ruleMatcher,
            SensitiveFieldResolver fieldResolver, ObjectMaskerOptions options,
            UnknownTypeRecorder unknownTypeRecorder) {
        this(strategyRegistry, ruleMatcher, fieldResolver, options, unknownTypeRecorder, null);
    }

    public ObjectMasker(MaskStrategyRegistry strategyRegistry, MaskRuleMatcher ruleMatcher,
            SensitiveFieldResolver fieldResolver, ObjectMaskerOptions options, UnknownTypeRecorder unknownTypeRecorder,
            MaskEventRecorder maskEventRecorder) {
        this.strategyRegistry = strategyRegistry == null ? MaskStrategyRegistry.withBuiltIns() : strategyRegistry;
        this.ruleMatcher = ruleMatcher == null ? MaskRuleMatcher.withDefaultRules() : ruleMatcher;
        this.fieldResolver = fieldResolver == null ? new SensitiveFieldResolver(this.ruleMatcher) : fieldResolver;
        this.options = options == null ? ObjectMaskerOptions.defaults() : options;
        this.unknownTypeRecorder = unknownTypeRecorder;
        this.maskEventRecorder = maskEventRecorder;
    }

    public Object mask(Object value) {
        return mask(value, MaskScene.RESPONSE);
    }

    public Object mask(Object value, MaskScene scene) {
        return maskWithResult(value, scene).getValue();
    }

    public MaskingResult maskWithResult(Object value, MaskScene scene) {
        MaskingSummary summary = new MaskingSummary();
        Object masked = maskValue(value, "$", null, 0, identitySet(), scene == null ? MaskScene.RESPONSE : scene,
                summary);
        return new MaskingResult(masked, summary.maskTypeCounts, summary.maskedFieldCount);
    }

    private Object maskValue(Object value, String path, String key, int depth, Set<Object> visiting, MaskScene scene,
            MaskingSummary summary) {
        // 递归入口统一处理跳过、深度、循环引用和类型分发，避免各容器分支重复这些保护逻辑。
        if (shouldSkipValue(value, depth)) {
            return value;
        }
        if (visiting.contains(value)) {
            return value;
        }
        if (value instanceof String) {
            return maskString((String) value, path, key, scene, summary);
        }

        visiting.add(value);
        try {
            if (value instanceof Map<?, ?>) {
                return maskMap((Map<?, ?>) value, path, depth, visiting, scene, summary);
            }
            if (value instanceof Collection<?>) {
                return maskCollection((Collection<?>) value, path, depth, visiting, scene, summary);
            }
            if (value.getClass().isArray()) {
                return maskArray(value, path, depth, visiting, scene, summary);
            }
            return maskBean(value, path, depth, visiting, scene, summary);
        } catch (RuntimeException ex) {
            return value;
        } finally {
            visiting.remove(value);
        }
    }

    private Map<Object, Object> maskMap(Map<?, ?> source, String path, int depth, Set<Object> visiting,
            MaskScene scene, MaskingSummary summary) {
        Map<Object, Object> masked = new LinkedHashMap<Object, Object>();
        int index = 0;
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (shouldMaskCollectionElement(index)) {
                value = maskValue(value, childPath(path, key), key, depth + 1, visiting, scene, summary);
            }
            masked.put(entry.getKey(), value);
            index++;
        }
        return masked;
    }

    private List<Object> maskCollection(Collection<?> source, String path, int depth, Set<Object> visiting,
            MaskScene scene, MaskingSummary summary) {
        List<Object> masked = new ArrayList<Object>(source.size());
        int index = 0;
        for (Object value : source) {
            if (shouldMaskCollectionElement(index)) {
                masked.add(maskValue(value, path + "[" + index + "]", null, depth + 1, visiting, scene, summary));
            } else {
                masked.add(value);
            }
            index++;
        }
        return masked;
    }

    private Object maskArray(Object source, String path, int depth, Set<Object> visiting, MaskScene scene,
            MaskingSummary summary) {
        int length = Array.getLength(source);
        Object masked = Array.newInstance(source.getClass().getComponentType(), length);
        for (int i = 0; i < length; i++) {
            Object value = Array.get(source, i);
            if (shouldMaskCollectionElement(i)) {
                value = maskValue(value, path + "[" + i + "]", null, depth + 1, visiting, scene, summary);
            }
            Array.set(masked, i, value);
        }
        return masked;
    }

    private Object maskBean(Object bean, String path, int depth, Set<Object> visiting, MaskScene scene,
            MaskingSummary summary) {
        for (Field field : fields(bean.getClass())) {
            try {
                field.setAccessible(true);
                Object current = field.get(bean);
                int childDepth = depth + 1;
                if (childDepth > options.getMaxDepth()) {
                    continue;
                }
                String childPath = childPath(path, field.getName());
                Optional<RuleMatch> match = fieldResolver.resolve(field, childPath);
                // 字符串字段可以直接按当前字段规则脱敏；复杂对象继续递归，让内部字段自行决策。
                if (current instanceof String && match.isPresent() && match.get().getAction() == RuleAction.MASK) {
                    field.set(bean, applyStrategy((String) current, match.get(), childPath, field.getName(), scene,
                            summary));
                } else {
                    field.set(bean, maskValue(current, childPath, field.getName(), childDepth, visiting, scene,
                            summary));
                }
            } catch (RuntimeException ex) {
                // 输出侧脱敏必须 fail-open：反射或策略异常不能影响业务响应。
                return bean;
            } catch (IllegalAccessException ex) {
                // 输出侧脱敏必须 fail-open：反射或策略异常不能影响业务响应。
                return bean;
            }
        }
        return bean;
    }

    private boolean shouldSkipValue(Object value, int depth) {
        return value == null || isUnsupported(value) || isSimpleValue(value) || depth > options.getMaxDepth();
    }

    private boolean shouldMaskCollectionElement(int index) {
        return index < options.getMaxCollectionSize();
    }

    private String maskString(String value, String path, String key, MaskScene scene, MaskingSummary summary) {
        Optional<RuleMatch> match = ruleMatcher.decide(MaskRuleRequest.builder()
                .key(key)
                .path(path)
                .build());
        if (match.isPresent() && match.get().getAction() == RuleAction.MASK) {
            return applyStrategy(value, match.get(), path, key, scene, summary);
        }
        return value;
    }

    private String applyStrategy(String value, RuleMatch match, String path, String key, MaskScene scene,
            MaskingSummary summary) {
        try {
            Optional<MaskStrategy> strategy = strategyRegistry.find(match.getMaskType());
            String effectiveType = match.getMaskType();
            if (!strategy.isPresent()) {
                recordUnknownType(match.getMaskType());
                strategy = defaultStrategy();
                effectiveType = MaskTypes.DEFAULT;
            }
            long startedAt = System.nanoTime();
            MaskResult result = strategy.get().apply(MaskContext.builder()
                    .maskType(effectiveType)
                    .scene(scene)
                    .path(path)
                    .fieldName(key)
                    .rawValue(value)
                    .build());
            if (result.isMasked()) {
                recordMask(scene, result.getContext().getMaskType(), System.nanoTime() - startedAt);
                summary.record(result.getContext().getMaskType());
            }
            return result.getValue();
        } catch (RuntimeException ex) {
            return value;
        }
    }

    private void recordMask(MaskScene scene, String type, long elapsedNanos) {
        if (maskEventRecorder != null) {
            maskEventRecorder.recordMask(scene, type, elapsedNanos);
        }
    }

    private void recordUnknownType(String type) {
        // 未知类型回退 DEFAULT，但保留告警和聚合统计，帮助定位配置里拼错或未注册的策略。
        LOGGER.warning("Fallback masking to default because no strategy registered for type "
                + MaskTypes.normalize(type));
        if (unknownTypeRecorder != null) {
            unknownTypeRecorder.recordUnknownType(type, MaskScene.RESPONSE);
        }
    }

    private Optional<MaskStrategy> defaultStrategy() {
        Optional<MaskStrategy> strategy = strategyRegistry.find(MaskTypes.DEFAULT);
        if (strategy.isPresent()) {
            return strategy;
        }
        return Optional.of(BuiltInMaskStrategies.get(MaskTypes.DEFAULT));
    }

    private static List<Field> fields(Class<?> type) {
        List<Field> fields = new ArrayList<Field>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static boolean isUnsupported(Object value) {
        return value instanceof InputStream
                || value instanceof byte[]
                || value.getClass().getName().startsWith("javax.servlet.")
                || value.getClass().getName().startsWith("jakarta.servlet.");
    }

    private static boolean isSimpleValue(Object value) {
        return value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Date
                || value instanceof Enum<?>
                || value instanceof BigDecimal
                || value instanceof BigInteger;
    }

    private static String childPath(String parent, String key) {
        if (parent == null || parent.isEmpty() || "$".equals(parent)) {
            return "$." + key;
        }
        return parent + "." + key;
    }

    private static Set<Object> identitySet() {
        return java.util.Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
    }

    private static final class MaskingSummary {

        private final Map<String, Integer> maskTypeCounts = new LinkedHashMap<String, Integer>();

        private int maskedFieldCount;

        private void record(String type) {
            // 只聚合本次脱敏调用的类型和数量，不保留字段路径或敏感原文。
            String normalizedType = MaskTypes.normalize(type);
            Integer previous = maskTypeCounts.get(normalizedType);
            maskTypeCounts.put(normalizedType, previous == null ? 1 : previous + 1);
            maskedFieldCount++;
        }
    }
}

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

public final class ObjectMasker {

    private final MaskStrategyRegistry strategyRegistry;

    private final MaskRuleMatcher ruleMatcher;

    private final SensitiveFieldResolver fieldResolver;

    private final ObjectMaskerOptions options;

    public ObjectMasker(MaskStrategyRegistry strategyRegistry, MaskRuleMatcher ruleMatcher,
            ObjectMaskerOptions options) {
        this(strategyRegistry, ruleMatcher, null, options);
    }

    public ObjectMasker(MaskStrategyRegistry strategyRegistry, MaskRuleMatcher ruleMatcher,
            SensitiveFieldResolver fieldResolver, ObjectMaskerOptions options) {
        this.strategyRegistry = strategyRegistry == null ? MaskStrategyRegistry.withBuiltIns() : strategyRegistry;
        this.ruleMatcher = ruleMatcher == null ? MaskRuleMatcher.withDefaultRules() : ruleMatcher;
        this.fieldResolver = fieldResolver == null ? new SensitiveFieldResolver(this.ruleMatcher) : fieldResolver;
        this.options = options == null ? ObjectMaskerOptions.defaults() : options;
    }

    public Object mask(Object value) {
        return maskValue(value, "$", null, 0, identitySet());
    }

    private Object maskValue(Object value, String path, String key, int depth, Set<Object> visiting) {
        if (value == null || isUnsupported(value) || isSimpleValue(value)) {
            return value;
        }
        if (visiting.contains(value)) {
            return value;
        }
        if (depth > options.getMaxDepth()) {
            return value;
        }
        if (value instanceof String) {
            return maskString((String) value, path, key);
        }

        visiting.add(value);
        try {
            if (value instanceof Map<?, ?>) {
                return maskMap((Map<?, ?>) value, path, depth, visiting);
            }
            if (value instanceof Collection<?>) {
                return maskCollection((Collection<?>) value, path, depth, visiting);
            }
            if (value.getClass().isArray()) {
                return maskArray(value, path, depth, visiting);
            }
            return maskBean(value, path, depth, visiting);
        } catch (RuntimeException ex) {
            return value;
        } finally {
            visiting.remove(value);
        }
    }

    private Map<Object, Object> maskMap(Map<?, ?> source, String path, int depth, Set<Object> visiting) {
        Map<Object, Object> masked = new LinkedHashMap<Object, Object>();
        int index = 0;
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (index < options.getMaxCollectionSize()) {
                value = maskValue(value, childPath(path, key), key, depth + 1, visiting);
            }
            masked.put(entry.getKey(), value);
            index++;
        }
        return masked;
    }

    private List<Object> maskCollection(Collection<?> source, String path, int depth, Set<Object> visiting) {
        List<Object> masked = new ArrayList<Object>(source.size());
        int index = 0;
        for (Object value : source) {
            if (index < options.getMaxCollectionSize()) {
                masked.add(maskValue(value, path + "[" + index + "]", null, depth + 1, visiting));
            } else {
                masked.add(value);
            }
            index++;
        }
        return masked;
    }

    private Object maskArray(Object source, String path, int depth, Set<Object> visiting) {
        int length = Array.getLength(source);
        Object masked = Array.newInstance(source.getClass().getComponentType(), length);
        for (int i = 0; i < length; i++) {
            Object value = Array.get(source, i);
            if (i < options.getMaxCollectionSize()) {
                value = maskValue(value, path + "[" + i + "]", null, depth + 1, visiting);
            }
            Array.set(masked, i, value);
        }
        return masked;
    }

    private Object maskBean(Object bean, String path, int depth, Set<Object> visiting) {
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
                if (current instanceof String && match.isPresent() && match.get().getAction() == RuleAction.MASK) {
                    field.set(bean, applyStrategy((String) current, match.get(), childPath, field.getName()));
                } else {
                    field.set(bean, maskValue(current, childPath, field.getName(), childDepth, visiting));
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

    private String maskString(String value, String path, String key) {
        Optional<RuleMatch> match = ruleMatcher.decide(MaskRuleRequest.builder()
                .key(key)
                .path(path)
                .build());
        if (match.isPresent() && match.get().getAction() == RuleAction.MASK) {
            return applyStrategy(value, match.get(), path, key);
        }
        return value;
    }

    private String applyStrategy(String value, RuleMatch match, String path, String key) {
        try {
            Optional<MaskStrategy> strategy = strategyRegistry.find(match.getMaskType());
            if (!strategy.isPresent()) {
                return value;
            }
            MaskResult result = strategy.get().apply(MaskContext.builder()
                    .maskType(match.getMaskType())
                    .scene(MaskScene.RESPONSE)
                    .path(path)
                    .fieldName(key)
                    .rawValue(value)
                    .build());
            return result.getValue();
        } catch (RuntimeException ex) {
            return value;
        }
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
}

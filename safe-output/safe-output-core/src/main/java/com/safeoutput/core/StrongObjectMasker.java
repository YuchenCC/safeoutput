package com.safeoutput.core;

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
import java.util.Set;

final class StrongObjectMasker {

    private final StrongTextMasker textMasker;

    private final ObjectMaskerOptions options;

    StrongObjectMasker(StrongTextMasker textMasker, ObjectMaskerOptions options) {
        this.textMasker = textMasker;
        this.options = options == null ? ObjectMaskerOptions.defaults() : options;
    }

    Object mask(Object value) {
        return maskValue(value, 0, identitySet());
    }

    private Object maskValue(Object value, int depth, Set<Object> visiting) {
        // 强扫描是调用方显式进入的能力，但仍保留深度和循环保护，避免扫描任意大对象图。
        if (shouldSkipValue(value, depth)) {
            return value;
        }
        if (value instanceof String) {
            return textMasker.mask((String) value);
        }
        if (visiting.contains(value)) {
            return value;
        }
        visiting.add(value);
        try {
            if (value instanceof Map<?, ?>) {
                return maskMap((Map<?, ?>) value, depth, visiting);
            }
            if (value instanceof Collection<?>) {
                return maskCollection((Collection<?>) value, depth, visiting);
            }
            if (value.getClass().isArray()) {
                return maskArray(value, depth, visiting);
            }
            return maskBean(value, depth, visiting);
        } catch (RuntimeException ex) {
            return value;
        } finally {
            visiting.remove(value);
        }
    }

    private Map<Object, Object> maskMap(Map<?, ?> source, int depth, Set<Object> visiting) {
        Map<Object, Object> masked = new LinkedHashMap<Object, Object>();
        int index = 0;
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (shouldMaskCollectionElement(index)) {
                value = maskValue(value, depth + 1, visiting);
            }
            masked.put(entry.getKey(), value);
            index++;
        }
        return masked;
    }

    private List<Object> maskCollection(Collection<?> source, int depth, Set<Object> visiting) {
        List<Object> masked = new ArrayList<Object>(source.size());
        int index = 0;
        for (Object value : source) {
            masked.add(shouldMaskCollectionElement(index) ? maskValue(value, depth + 1, visiting) : value);
            index++;
        }
        return masked;
    }

    private Object maskArray(Object source, int depth, Set<Object> visiting) {
        int length = Array.getLength(source);
        Object masked = Array.newInstance(source.getClass().getComponentType(), length);
        for (int i = 0; i < length; i++) {
            Object value = Array.get(source, i);
            Object element = shouldMaskCollectionElement(i) ? maskValue(value, depth + 1, visiting) : value;
            Array.set(masked, i, element);
        }
        return masked;
    }

    private Object maskBean(Object bean, int depth, Set<Object> visiting) {
        for (Field field : fields(bean.getClass())) {
            try {
                field.setAccessible(true);
                field.set(bean, maskValue(field.get(bean), depth + 1, visiting));
            } catch (RuntimeException ex) {
                // 主动强扫描失败必须 fail-open，不能阻断业务侧手工调用。
                return bean;
            } catch (IllegalAccessException ex) {
                // 主动强扫描失败必须 fail-open，不能阻断业务侧手工调用。
                return bean;
            }
        }
        return bean;
    }

    private boolean shouldSkipValue(Object value, int depth) {
        return value == null || isSimpleValue(value) || depth > options.getMaxDepth();
    }

    private boolean shouldMaskCollectionElement(int index) {
        return index < options.getMaxCollectionSize();
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

    private static boolean isSimpleValue(Object value) {
        return value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Date
                || value instanceof Enum<?>
                || value instanceof BigDecimal
                || value instanceof BigInteger;
    }

    private static Set<Object> identitySet() {
        return java.util.Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
    }
}

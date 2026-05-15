package com.safeoutput.spring.boot.autoconfigure;

import java.lang.reflect.Field;
import java.util.Map;

final class ResponseBodyDataAccessor {

    @SuppressWarnings("unchecked")
    Object extract(Object target, String path) {
        String[] segments = path.split("\\.");
        Object current = target;
        for (String segment : segments) {
            if (current == null) {
                return null;
            }
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(segment);
            } else {
                current = getFieldValue(current, segment);
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    void set(Object target, String path, Object value) {
        String[] segments = path.split("\\.");
        Object parent = target;
        for (int i = 0; i < segments.length - 1; i++) {
            if (parent instanceof Map) {
                parent = ((Map<String, Object>) parent).get(segments[i]);
            } else {
                parent = getFieldValue(parent, segments[i]);
            }
        }
        String lastSegment = segments[segments.length - 1];
        if (parent instanceof Map) {
            ((Map<String, Object>) parent).put(lastSegment, value);
        } else {
            setFieldValue(parent, lastSegment, value);
        }
    }

    private static Object getFieldValue(Object target, String fieldName) {
        Class<?> clazz = target.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ex) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException ex) {
                return null;
            }
        }
        return null;
    }

    private static void setFieldValue(Object target, String fieldName, Object value) {
        Class<?> clazz = target.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException ex) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException ex) {
                return;
            }
        }
    }
}

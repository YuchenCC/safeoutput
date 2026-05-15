package com.safeoutput.spring.boot.autoconfigure;

import com.safeoutput.core.ObjectMasker;
import com.safeoutput.core.MaskScene;
import com.safeoutput.core.MaskingResult;
import com.safeoutput.core.ResponseRiskEvent;
import com.safeoutput.core.ResponseRiskRecorder;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SafeOutputResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final Pattern NUMERIC_SEGMENT = Pattern.compile("/\\d+(?=/|$)");

    private static final Pattern UUID_SEGMENT = Pattern.compile(
            "/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)");

    private final ObjectMasker objectMasker;

    private final SafeOutputProperties properties;

    private final ApiIgnoreMatcher apiIgnoreMatcher;

    private final List<ResponseRiskRecorder> riskRecorders;

    public SafeOutputResponseBodyAdvice(ObjectMasker objectMasker, SafeOutputProperties properties) {
        this(objectMasker, properties, null, Collections.<ResponseRiskRecorder>emptyList());
    }

    SafeOutputResponseBodyAdvice(ObjectMasker objectMasker, SafeOutputProperties properties,
            ApiIgnoreMatcher apiIgnoreMatcher, List<ResponseRiskRecorder> riskRecorders) {
        this.objectMasker = objectMasker;
        this.properties = properties;
        this.apiIgnoreMatcher = apiIgnoreMatcher;
        this.riskRecorders = riskRecorders == null ? Collections.<ResponseRiskRecorder>emptyList() : riskRecorders;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return properties != null && properties.isEnabled() && properties.getResponse().isEnabled();
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
            ServerHttpResponse response) {
        if (objectMasker == null || body == null) {
            return body;
        }
        try {
            Optional<ApiIgnoreMatch> apiIgnore = matchApiIgnore(request);
            if (apiIgnore.isPresent()) {
                // API Ignore 返回明文，但仍记录风险事件，便于报告里看见显式豁免接口。
                recordRisk(request, true, apiIgnore.get().getReason(), false, 0,
                        Collections.<String, Integer>emptyMap(), 0);
                return body;
            }
            String bodyDataPath = properties.getResponse().getBodyDataPath();
            if (bodyDataPath != null && !bodyDataPath.isEmpty()) {
                return maskDataPath(body, bodyDataPath, request);
            }
            long startedAt = System.nanoTime();
            MaskingResult result = objectMasker.maskWithResult(body, MaskScene.RESPONSE);
            recordRisk(request, false, null, false, result.getMaskedFieldCount(), result.getMaskTypeCounts(),
                    System.nanoTime() - startedAt);
            return result.getValue();
        } catch (RuntimeException ex) {
            // 响应脱敏必须 fail-open，避免安全组件异常放大成业务接口故障。
            recordRisk(request, false, null, true, 0, Collections.<String, Integer>emptyMap(), 0);
            return body;
        }
    }

    private Object maskDataPath(Object body, String bodyDataPath, ServerHttpRequest request) {
        Object data = extractData(body, bodyDataPath);
        if (data == null) {
            return body;
        }
        long startedAt = System.nanoTime();
        MaskingResult result = objectMasker.maskWithResult(data, MaskScene.RESPONSE);
        setData(body, bodyDataPath, result.getValue());
        recordRisk(request, false, null, false, result.getMaskedFieldCount(), result.getMaskTypeCounts(),
                System.nanoTime() - startedAt);
        return body;
    }

    @SuppressWarnings("unchecked")
    private Object extractData(Object target, String path) {
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
    private void setData(Object target, String path, Object value) {
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

    private Optional<ApiIgnoreMatch> matchApiIgnore(ServerHttpRequest request) {
        if (apiIgnoreMatcher == null || request == null) {
            return Optional.empty();
        }
        return apiIgnoreMatcher.match(request.getMethodValue(), request.getURI().getPath(), MaskScene.RESPONSE);
    }

    private void recordRisk(ServerHttpRequest request, boolean ignored, String reason, boolean failed,
            int maskedFieldCount, java.util.Map<String, Integer> maskTypeCounts, long elapsedNanos) {
        if (request == null) {
            return;
        }
        ResponseRiskEvent event = new ResponseRiskEvent(request.getMethodValue(), request.getURI().getPath(),
                stableApiKey(request), ignored, reason, failed, maskedFieldCount, maskTypeCounts, elapsedNanos);
        for (ResponseRiskRecorder recorder : riskRecorders) {
            recorder.record(event);
        }
    }

    private String stableApiKey(ServerHttpRequest request) {
        Object pattern = bestMatchingPattern();
        if (pattern instanceof String && !((String) pattern).trim().isEmpty()) {
            return (String) pattern;
        }
        String path = request.getURI().getPath();
        // 没有 MVC pattern 时做轻量归一化，避免把用户 ID 这类高基数原始 URI 作为聚合 key。
        return NUMERIC_SEGMENT.matcher(UUID_SEGMENT.matcher(path).replaceAll("/{id}")).replaceAll("/{id}");
    }

    private Object bestMatchingPattern() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes == null ? null
                : attributes.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                        RequestAttributes.SCOPE_REQUEST);
    }
}

package com.safeoutput.spring.boot.autoconfigure;

import com.safeoutput.core.ObjectMasker;
import com.safeoutput.core.MaskScene;
import com.safeoutput.core.ResponseRiskEvent;
import com.safeoutput.core.ResponseRiskRecorder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class SafeOutputResponseBodyAdvice implements ResponseBodyAdvice<Object> {

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
                recordRisk(request, true, apiIgnore.get().getReason());
                return body;
            }
            recordRisk(request, false, null);
            return objectMasker.mask(body);
        } catch (RuntimeException ex) {
            // 响应脱敏必须 fail-open，避免安全组件异常放大成业务接口故障。
            return body;
        }
    }

    private Optional<ApiIgnoreMatch> matchApiIgnore(ServerHttpRequest request) {
        if (apiIgnoreMatcher == null || request == null) {
            return Optional.empty();
        }
        return apiIgnoreMatcher.match(request.getMethodValue(), request.getURI().getPath(), MaskScene.RESPONSE);
    }

    private void recordRisk(ServerHttpRequest request, boolean ignored, String reason) {
        if (request == null) {
            return;
        }
        ResponseRiskEvent event = new ResponseRiskEvent(request.getMethodValue(), request.getURI().getPath(), ignored,
                reason);
        for (ResponseRiskRecorder recorder : riskRecorders) {
            recorder.record(event);
        }
    }
}

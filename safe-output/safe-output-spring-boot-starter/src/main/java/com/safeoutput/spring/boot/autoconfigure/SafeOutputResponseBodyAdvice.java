package com.safeoutput.spring.boot.autoconfigure;

import com.safeoutput.core.ObjectMasker;

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

    public SafeOutputResponseBodyAdvice(ObjectMasker objectMasker, SafeOutputProperties properties) {
        this.objectMasker = objectMasker;
        this.properties = properties;
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
            return objectMasker.mask(body);
        } catch (RuntimeException ex) {
            return body;
        }
    }
}

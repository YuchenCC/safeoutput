package com.safeoutput.demo;

import com.safeoutput.core.SafeOutputMaskService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoManualMaskController {

    private final SafeOutputMaskService maskService;

    public DemoManualMaskController(SafeOutputMaskService maskService) {
        this.maskService = maskService;
    }

    @PostMapping("/demo/mask/by-type")
    public Map<String, Object> byType(@RequestBody ByTypeRequest request) {
        // Demo 连续执行两次，用响应里的 idempotent 字段展示主动脱敏不会反复破坏格式。
        int iterations = iterations(request.getIterations());
        long startedAt = System.nanoTime();
        String first = null;
        String second = null;
        for (int i = 0; i < iterations; i++) {
            first = maskService.mask(request.getValue(), request.getType());
            second = maskService.mask(first, request.getType());
        }
        return result(first, second, iterations, System.nanoTime() - startedAt);
    }

    @PostMapping("/demo/mask/object")
    public Map<String, Object> object(@RequestBody(required = false) ObjectRequest request) {
        int iterations = request == null ? 1 : iterations(request.getIterations());
        long startedAt = System.nanoTime();
        ManualOrder first = null;
        ManualOrder second = null;
        for (int i = 0; i < iterations; i++) {
            first = (ManualOrder) maskService.maskObject(manualOrder());
            second = (ManualOrder) maskService.maskObject(first);
        }
        return result(first, second, iterations, System.nanoTime() - startedAt);
    }

    @PostMapping("/demo/mask/strong")
    public Map<String, Object> strong(@RequestBody StrongRequest request) {
        // 强扫描必须由调用方显式进入，普通对象主动脱敏不会默认全局 regex 扫描文本。
        int iterations = iterations(request.getIterations());
        long startedAt = System.nanoTime();
        String first = null;
        String second = null;
        for (int i = 0; i < iterations; i++) {
            first = maskService.maskStrong(request.getText());
            second = maskService.maskStrong(first);
        }
        return result(first, second, iterations, System.nanoTime() - startedAt);
    }

    private static Map<String, Object> result(Object first, Object second, int iterations, long totalElapsedNanos) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("first", first);
        response.put("second", second);
        response.put("idempotent", first == null ? second == null : first.equals(second));
        response.put("iterations", iterations);
        response.put("totalElapsedNanos", totalElapsedNanos);
        response.put("averageElapsedNanos", iterations == 0 ? 0 : totalElapsedNanos / iterations);
        return response;
    }

    private static int iterations(Integer iterations) {
        if (iterations == null) {
            return 1;
        }
        return Math.max(1, Math.min(iterations.intValue(), 1000));
    }

    private static ManualOrder manualOrder() {
        return new ManualOrder("张三", "13800138000", "演示商品");
    }

    public static final class ByTypeRequest {

        private String value;

        private String type;

        private Integer iterations;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getIterations() {
            return iterations;
        }

        public void setIterations(Integer iterations) {
            this.iterations = iterations;
        }
    }

    public static final class ObjectRequest {

        private Integer iterations;

        public Integer getIterations() {
            return iterations;
        }

        public void setIterations(Integer iterations) {
            this.iterations = iterations;
        }
    }

    public static final class StrongRequest {

        private String text;

        private Integer iterations;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Integer getIterations() {
            return iterations;
        }

        public void setIterations(Integer iterations) {
            this.iterations = iterations;
        }
    }

    public static final class ManualOrder {

        private String realName;

        private String mobile;

        private String name;

        ManualOrder(String realName, String mobile, String name) {
            this.realName = realName;
            this.mobile = mobile;
            this.name = name;
        }

        public String getRealName() {
            return realName;
        }

        public String getMobile() {
            return mobile;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ManualOrder)) {
                return false;
            }
            ManualOrder that = (ManualOrder) other;
            return Objects.equals(realName, that.realName)
                    && Objects.equals(mobile, that.mobile)
                    && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(realName, mobile, name);
        }
    }
}

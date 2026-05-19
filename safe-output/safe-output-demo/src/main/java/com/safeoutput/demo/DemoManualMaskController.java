package com.safeoutput.demo;

import com.safeoutput.core.SafeOutputMaskService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    public List<Map<String, Object>> byType(@RequestBody ByTypeRequest request) {
        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        String previous = null;
        String current = request.getValue();
        for (int round = 1; round <= 2; round++) {
            long startedAt = System.nanoTime();
            current = maskService.mask(current, request.getType());
            appendRound(response, round, current, System.nanoTime() - startedAt, previous);
            previous = (String) snapshotResult(current);
        }
        return response;
    }

    @PostMapping("/demo/mask/object")
    public List<Map<String, Object>> object(@RequestBody(required = false) ObjectRequest request) {
        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        Object previous = null;
        ManualOrder current = manualOrder(request);
        for (int round = 1; round <= 2; round++) {
            long startedAt = System.nanoTime();
            current = (ManualOrder) maskService.maskObject(current);
            appendRound(response, round, current, System.nanoTime() - startedAt, previous);
            previous = snapshotResult(current);
        }
        return response;
    }

    @PostMapping("/demo/mask/strong")
    public List<Map<String, Object>> strong(@RequestBody StrongRequest request) {
        // 强扫描必须由调用方显式进入，普通对象主动脱敏不会默认全局 regex 扫描文本。
        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        String previous = null;
        String current = request.getText();
        for (int round = 1; round <= 2; round++) {
            long startedAt = System.nanoTime();
            current = maskService.maskStrong(current);
            appendRound(response, round, current, System.nanoTime() - startedAt, previous);
            previous = (String) snapshotResult(current);
        }
        return response;
    }

    private static void appendRound(List<Map<String, Object>> response, int round, Object result, long elapsedNanos,
            Object previous) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("round", round);
        item.put("result", snapshotResult(result));
        item.put("elapsedNanos", elapsedNanos);
        item.put("sameAsPrevious", previous != null && previous.equals(result));
        response.add(item);
    }

    private static Object snapshotResult(Object result) {
        if (result instanceof ManualOrder) {
            ManualOrder order = (ManualOrder) result;
            return new ManualOrder(order.getRealName(), order.getMobile(), order.getName());
        }
        return result;
    }

    private static ManualOrder manualOrder() {
        return new ManualOrder("张三", "13800138000", "演示商品");
    }

    private static ManualOrder manualOrder(ObjectRequest request) {
        ManualOrder defaults = manualOrder();
        if (request == null) {
            return defaults;
        }
        return new ManualOrder(valueOrDefault(request.getRealName(), defaults.getRealName()),
                valueOrDefault(request.getMobile(), defaults.getMobile()),
                valueOrDefault(request.getName(), defaults.getName()));
    }

    private static String valueOrDefault(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    public static final class ByTypeRequest {

        private String value;

        private String type;

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
    }

    public static final class ObjectRequest {

        private String realName;

        private String mobile;

        private String name;

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static final class StrongRequest {

        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
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

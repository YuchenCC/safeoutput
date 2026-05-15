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
        String first = maskService.mask(request.getValue(), request.getType());
        String second = maskService.mask(first, request.getType());
        return result(first, second);
    }

    @PostMapping("/demo/mask/object")
    public Map<String, Object> object() {
        ManualOrder first = (ManualOrder) maskService.maskObject(manualOrder());
        ManualOrder second = (ManualOrder) maskService.maskObject(first);
        return result(first, second);
    }

    @PostMapping("/demo/mask/strong")
    public Map<String, Object> strong(@RequestBody StrongRequest request) {
        // 强扫描必须由调用方显式进入，普通对象主动脱敏不会默认全局 regex 扫描文本。
        String first = maskService.maskStrong(request.getText());
        String second = maskService.maskStrong(first);
        return result(first, second);
    }

    private static Map<String, Object> result(Object first, Object second) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("first", first);
        response.put("second", second);
        response.put("idempotent", first == null ? second == null : first.equals(second));
        return response;
    }

    private static ManualOrder manualOrder() {
        return new ManualOrder("张三", "13800138000", "演示商品");
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

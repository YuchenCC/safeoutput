package com.safeoutput.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoIntegrationGuideController {

    @GetMapping("/demo/integration-guide")
    public Map<String, Object> guide() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("items", items());
        return response;
    }

    private static List<Map<String, Object>> items() {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        items.add(item("yaml-rule", "YAML 配置规则", "工单 realName / 客户 shippingAddress / payment securityAnswer",
                "/demo/business/tickets", "demoRealName / demoAddress / demoDefault"));
        items.add(item("annotation", "字段注解", "客户 displayName 是歧义字段，使用 @Desensitize",
                "/demo/business/customer", "ANNOTATION"));
        items.add(item("default-rule", "默认规则库", "mobile / email / idCard / bankCard / password",
                "/demo/business/order", "DEFAULT_RULE"));
        items.add(item("field-ignore", "字段级 ignore", "plainNote 保留原文但其它字段继续脱敏",
                "/demo/business/customer", "FIELD_IGNORE"));
        items.add(item("api-ignore", "接口级 ignore", "legacy-plaintext 明文返回并进入风险统计",
                "/demo/business/legacy-plaintext", "API_IGNORE"));
        items.add(item("log4j2", "Log4j2 PatternConverter", "%safeOutputMsg 处理 JSON-like、key=value 和 fallback",
                "/demo/logs/scenarios", "LOG4J2"));
        items.add(item("manual", "SafeOutputMaskService", "实验室主动按 type、对象、强扫描和批量性能脱敏",
                "/demo/mask/by-type", "MANUAL"));
        return items;
    }

    private static Map<String, Object> item(String id, String title, String businessField, String endpoint,
            String ruleSource) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("id", id);
        item.put("title", title);
        item.put("businessField", businessField);
        item.put("endpoint", endpoint);
        item.put("ruleSource", ruleSource);
        item.put("actionHash", actionHash(id));
        return item;
    }

    private static String actionHash(String id) {
        if ("log4j2".equals(id)) {
            return "#logs";
        }
        if ("manual".equals(id)) {
            return "#lab";
        }
        return "#workbench";
    }
}

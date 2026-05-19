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
        items.add(item("default-rule", "默认字段规则", "mobile / idCard / bankCard / email / password",
                "/demo/business/orders", "DEFAULT_RULE", "DefaultMaskRules",
                "字段名命中内置规则库即可脱敏，业务代码不需要写注解或调用服务。",
                "public final class DefaultMaskRules {\n"
                        + "  rules.add(MaskRule.defaults(\"default.mobile\")\n"
                        + "      .keys(Arrays.asList(\"mobile\", \"phone\", \"telephone\", \"tel\", \"userMobile\"))\n"
                        + "      .type(MaskTypes.MOBILE)\n"
                        + "      .build());\n"
                        + "  rules.add(MaskRule.defaults(\"default.bank-card\")\n"
                        + "      .keys(Arrays.asList(\"bankCard\", \"cardNo\", \"bankNo\"))\n"
                        + "      .type(MaskTypes.BANK_CARD)\n"
                        + "      .build());\n"
                        + "}"));
        items.add(item("yaml-rule", "YAML 配置规则", "shippingAddress / securityAnswer",
                "/demo/business/payments", "CONFIGURED_RULE", "application.yml",
                "业务字段名明确但不在默认规则里时，通过 safe-output.rules 声明 key 与脱敏类型。",
                "safe-output:\n"
                        + "  rules:\n"
                        + "    - name: demoAddress\n"
                        + "      keys:\n"
                        + "        - shippingAddress\n"
                        + "      type: ADDRESS\n"
                        + "    - name: demoDefault\n"
                        + "      keys:\n"
                        + "        - securityAnswer\n"
                        + "      type: DEFAULT"));
        items.add(item("annotation", "字段注解", "displayName / customerName / payerName / requesterName / realName",
                "/demo/business/customers", "ANNOTATION", "DemoBusinessDataSource.java",
                "字段名有业务语义但不适合放入全局默认规则时，在响应 DTO 字段上显式声明类型。",
                "public static final class CustomerProfile {\n"
                        + "  @Desensitize(type = MaskTypes.CHINESE_NAME)\n"
                        + "  private String displayName;\n"
                        + "}\n\n"
                        + "public static final class PaymentVerification {\n"
                        + "  @Desensitize(type = MaskTypes.CHINESE_NAME)\n"
                        + "  private String payerName;\n"
                        + "}"));
        items.add(item("field-ignore", "字段级 Ignore", "plainNote",
                "/demo/business/tickets", "FIELD_IGNORE", "application.yml",
                "个别字段需要按业务约定保留原样时，用 ignore.keys 跳过字段脱敏；其它字段仍继续脱敏。",
                "safe-output:\n"
                        + "  ignore:\n"
                        + "    keys:\n"
                        + "      - plainNote"));
        items.add(item("api-ignore", "接口级 Ignore", "/demo/business/{domain}/{id}/raw",
                "/demo/business/customers/C-1001/raw", "API_IGNORE", "application.yml",
                "业务控制台的小眼睛查看明文走独立 raw 接口；接口返回明文，但会进入 Response 风险统计。",
                "safe-output:\n"
                        + "  ignore:\n"
                        + "    apis:\n"
                        + "      - method: GET\n"
                        + "        pattern: /demo/business/customers/*/raw\n"
                        + "        reason: business console reveal customer sensitive fields\n"
                        + "      - method: GET\n"
                        + "        pattern: /demo/business/payments/*/raw\n"
                        + "        reason: business console reveal payment sensitive fields"));
        return items;
    }

    private static Map<String, Object> item(String id, String title, String businessField, String endpoint,
            String ruleSource, String sourceFile, String description, String snippet) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("id", id);
        item.put("title", title);
        item.put("businessField", businessField);
        item.put("endpoint", endpoint);
        item.put("ruleSource", ruleSource);
        item.put("sourceFile", sourceFile);
        item.put("description", description);
        item.put("snippet", snippet);
        item.put("actionHash", actionHash(id));
        return item;
    }

    private static String actionHash(String id) {
        return "#workbench";
    }
}

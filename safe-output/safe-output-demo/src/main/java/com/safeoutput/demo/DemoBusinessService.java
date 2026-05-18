package com.safeoutput.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class DemoBusinessService {

    private final DemoBusinessDataSource dataSource;

    public DemoBusinessService(DemoBusinessDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Map<String, Object>> scenarioMetadata() {
        List<Map<String, Object>> scenarios = new ArrayList<Map<String, Object>>();
        scenarios.add(scenario("customer", "客户档案", "Bean", "/demo/business/customer",
                "默认规则、注解姓名、字段 ignore"));
        scenarios.add(scenario("order", "订单履约", "嵌套对象", "/demo/business/order",
                "嵌套客户、银行卡、地址"));
        scenarios.add(scenario("payment", "支付核验", "Map", "/demo/business/payment",
                "Map 字段、DEFAULT 配置规则"));
        scenarios.add(scenario("ticket", "工单处理", "Collection", "/demo/business/tickets",
                "集合对象、YAML 姓名规则"));
        scenarios.add(scenario("account", "账户安全", "Bean", "/demo/business/account",
                "密码、邮箱、地址"));
        scenarios.add(scenario("legacyPlaintext", "接口豁免", "Bean", "/demo/business/legacy-plaintext",
                "接口级 ignore 进入风险统计"));
        return scenarios;
    }

    public Map<String, Object> workbenchSummary() {
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("scenarioCount", scenarioMetadata().size());
        summary.put("businessDomains", "客户 / 订单 / 支付 / 工单 / 账户");
        summary.put("maskTypes", "MOBILE, ID_CARD, BANK_CARD, EMAIL, CHINESE_NAME, ADDRESS, PASSWORD, DEFAULT");
        summary.put("primaryRoute", "#workbench");
        return summary;
    }

    public DemoBusinessDataSource.DemoCustomer customer() {
        return dataSource.customer();
    }

    public DemoBusinessDataSource.DemoOrder order() {
        return dataSource.order();
    }

    public Map<String, Object> payment() {
        return dataSource.payment();
    }

    public List<DemoBusinessDataSource.DemoTicket> tickets() {
        return dataSource.tickets();
    }

    public DemoBusinessDataSource.DemoAccount account() {
        return dataSource.account();
    }

    public DemoBusinessDataSource.DemoCustomer legacyPlaintextCustomer() {
        return dataSource.legacyPlaintextCustomer();
    }

    private static Map<String, Object> scenario(String id, String name, String shape, String endpoint,
            String governance) {
        Map<String, Object> scenario = new LinkedHashMap<String, Object>();
        scenario.put("id", id);
        scenario.put("name", name);
        scenario.put("responseShape", shape);
        scenario.put("endpoint", endpoint);
        scenario.put("governance", governance);
        return scenario;
    }
}

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
        scenarios.add(scenario("customers", "客户档案", "Table + Detail", "/demo/business/customers",
                "客户列表、客户详情、敏感字段按 Response 脱敏"));
        scenarios.add(scenario("orders", "订单履约", "Table + Detail", "/demo/business/orders",
                "履约订单、收货地址、银行卡与联系人脱敏"));
        scenarios.add(scenario("payments", "支付核验", "Table + Detail", "/demo/business/payments",
                "支付流水、核验问题、卡号和邮箱脱敏"));
        scenarios.add(scenario("tickets", "工单处理", "Table + Detail", "/demo/business/tickets",
                "工单队列、提交人信息、字段 ignore 样例"));
        scenarios.add(scenario("accounts", "账户安全", "Table + Detail", "/demo/business/accounts",
                "账户状态、密码、邮箱、手机号和地址脱敏"));
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

    public List<DemoBusinessDataSource.CustomerProfile> customers() {
        return dataSource.customerProfiles();
    }

    public DemoBusinessDataSource.CustomerProfile customerDetail(String id) {
        return dataSource.customerProfile(id);
    }

    public List<DemoBusinessDataSource.OrderFulfillment> orders() {
        return dataSource.orderFulfillments();
    }

    public DemoBusinessDataSource.OrderFulfillment orderDetail(String id) {
        return dataSource.orderFulfillment(id);
    }

    public List<DemoBusinessDataSource.PaymentVerification> payments() {
        return dataSource.paymentVerifications();
    }

    public DemoBusinessDataSource.PaymentVerification paymentDetail(String id) {
        return dataSource.paymentVerification(id);
    }

    public List<DemoBusinessDataSource.SupportTicket> supportTickets() {
        return dataSource.supportTickets();
    }

    public DemoBusinessDataSource.SupportTicket supportTicketDetail(String id) {
        return dataSource.supportTicket(id);
    }

    public List<DemoBusinessDataSource.AccountSecurity> accounts() {
        return dataSource.accountSecurities();
    }

    public DemoBusinessDataSource.AccountSecurity accountDetail(String id) {
        return dataSource.accountSecurity(id);
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

    public List<DemoBusinessDataSource.SupportTicket> tickets() {
        return dataSource.supportTickets();
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

package com.safeoutput.demo.business;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoBusinessController {

    private final DemoBusinessService businessService;

    public DemoBusinessController(DemoBusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping("/demo/workbench")
    public Map<String, Object> workbench() {
        DemoBusinessLog.workbenchAccess();
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("summary", businessService.workbenchSummary());
        response.put("scenarios", businessService.scenarioMetadata());
        return response;
    }

    @GetMapping("/demo/business/customer")
    public DemoBusinessDataSource.DemoCustomer customer() {
        DemoBusinessLog.customerAccess("customer");
        return businessService.customer();
    }

    @GetMapping("/demo/business/customers")
    public List<DemoBusinessDataSource.CustomerProfile> customers() {
        DemoBusinessLog.customerAccess("customers");
        return businessService.customers();
    }

    @GetMapping("/demo/business/customers/{id}")
    public DemoBusinessDataSource.CustomerProfile customerDetail(@PathVariable("id") String id) {
        DemoBusinessLog.customerAccess("customers-detail");
        return businessService.customerDetail(id);
    }

    @GetMapping("/demo/business/customers/{id}/raw")
    public DemoBusinessDataSource.CustomerProfile customerRaw(@PathVariable("id") String id) {
        DemoBusinessLog.customerAccess("customers-raw");
        return businessService.customerDetail(id);
    }

    @GetMapping("/demo/business/order")
    public DemoBusinessDataSource.DemoOrder order() {
        DemoBusinessLog.orderAccess("order");
        return businessService.order();
    }

    @GetMapping("/demo/business/orders")
    public List<DemoBusinessDataSource.OrderFulfillment> orders() {
        DemoBusinessLog.orderAccess("orders");
        return businessService.orders();
    }

    @GetMapping("/demo/business/orders/{id}")
    public DemoBusinessDataSource.OrderFulfillment orderDetail(@PathVariable("id") String id) {
        DemoBusinessLog.orderDetailAccess("orders-detail");
        return businessService.orderDetail(id);
    }

    @GetMapping("/demo/business/orders/{id}/raw")
    public DemoBusinessDataSource.OrderFulfillment orderRaw(@PathVariable("id") String id) {
        DemoBusinessLog.orderAccess("orders-raw");
        return businessService.orderDetail(id);
    }

    @GetMapping("/demo/business/payment")
    public Map<String, Object> payment() {
        DemoBusinessLog.paymentAccess("payment");
        return businessService.payment();
    }

    @GetMapping("/demo/business/payments")
    public List<DemoBusinessDataSource.PaymentVerification> payments() {
        DemoBusinessLog.paymentAccess("payments");
        return businessService.payments();
    }

    @GetMapping("/demo/business/payments/{id}")
    public DemoBusinessDataSource.PaymentVerification paymentDetail(@PathVariable("id") String id) {
        DemoBusinessLog.paymentDetailAccess("payments-detail");
        return businessService.paymentDetail(id);
    }

    @GetMapping("/demo/business/payments/{id}/raw")
    public DemoBusinessDataSource.PaymentVerification paymentRaw(@PathVariable("id") String id) {
        DemoBusinessLog.paymentAccess("payments-raw");
        return businessService.paymentDetail(id);
    }

    @GetMapping("/demo/business/tickets")
    public Object tickets() {
        DemoBusinessLog.ticketAccess("tickets");
        return businessService.tickets();
    }

    @GetMapping("/demo/business/tickets/{id}")
    public DemoBusinessDataSource.SupportTicket ticketDetail(@PathVariable("id") String id) {
        DemoBusinessLog.ticketDetailAccess("tickets-detail");
        return businessService.supportTicketDetail(id);
    }

    @GetMapping("/demo/business/tickets/{id}/raw")
    public DemoBusinessDataSource.SupportTicket ticketRaw(@PathVariable("id") String id) {
        DemoBusinessLog.ticketAccess("tickets-raw");
        return businessService.supportTicketDetail(id);
    }

    @GetMapping("/demo/business/account")
    public DemoBusinessDataSource.DemoAccount account() {
        DemoBusinessLog.accountAccess("account");
        return businessService.account();
    }

    @GetMapping("/demo/business/accounts")
    public List<DemoBusinessDataSource.AccountSecurity> accounts() {
        DemoBusinessLog.accountAccess("accounts");
        return businessService.accounts();
    }

    @GetMapping("/demo/business/accounts/{id}")
    public DemoBusinessDataSource.AccountSecurity accountDetail(@PathVariable("id") String id) {
        DemoBusinessLog.accountDetailAccess("accounts-detail");
        return businessService.accountDetail(id);
    }

    @GetMapping("/demo/business/accounts/{id}/raw")
    public DemoBusinessDataSource.AccountSecurity accountRaw(@PathVariable("id") String id) {
        DemoBusinessLog.accountAccess("accounts-raw");
        return businessService.accountDetail(id);
    }

    @GetMapping("/demo/business/legacy-plaintext")
    public DemoBusinessDataSource.DemoCustomer legacyPlaintext() {
        DemoBusinessLog.customerAccess("legacy-plaintext");
        return businessService.legacyPlaintextCustomer();
    }
}

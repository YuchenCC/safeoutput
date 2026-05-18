package com.safeoutput.demo;

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
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("summary", businessService.workbenchSummary());
        response.put("scenarios", businessService.scenarioMetadata());
        return response;
    }

    @GetMapping("/demo/business/customer")
    public DemoBusinessDataSource.DemoCustomer customer() {
        return businessService.customer();
    }

    @GetMapping("/demo/business/customers")
    public List<DemoBusinessDataSource.CustomerProfile> customers() {
        return businessService.customers();
    }

    @GetMapping("/demo/business/customers/{id}")
    public DemoBusinessDataSource.CustomerProfile customerDetail(@PathVariable("id") String id) {
        return businessService.customerDetail(id);
    }

    @GetMapping("/demo/business/customers/{id}/raw")
    public DemoBusinessDataSource.CustomerProfile customerRaw(@PathVariable("id") String id) {
        return businessService.customerDetail(id);
    }

    @GetMapping("/demo/business/order")
    public DemoBusinessDataSource.DemoOrder order() {
        return businessService.order();
    }

    @GetMapping("/demo/business/orders")
    public List<DemoBusinessDataSource.OrderFulfillment> orders() {
        return businessService.orders();
    }

    @GetMapping("/demo/business/orders/{id}")
    public DemoBusinessDataSource.OrderFulfillment orderDetail(@PathVariable("id") String id) {
        return businessService.orderDetail(id);
    }

    @GetMapping("/demo/business/orders/{id}/raw")
    public DemoBusinessDataSource.OrderFulfillment orderRaw(@PathVariable("id") String id) {
        return businessService.orderDetail(id);
    }

    @GetMapping("/demo/business/payment")
    public Map<String, Object> payment() {
        return businessService.payment();
    }

    @GetMapping("/demo/business/payments")
    public List<DemoBusinessDataSource.PaymentVerification> payments() {
        return businessService.payments();
    }

    @GetMapping("/demo/business/payments/{id}")
    public DemoBusinessDataSource.PaymentVerification paymentDetail(@PathVariable("id") String id) {
        return businessService.paymentDetail(id);
    }

    @GetMapping("/demo/business/payments/{id}/raw")
    public DemoBusinessDataSource.PaymentVerification paymentRaw(@PathVariable("id") String id) {
        return businessService.paymentDetail(id);
    }

    @GetMapping("/demo/business/tickets")
    public Object tickets() {
        return businessService.tickets();
    }

    @GetMapping("/demo/business/tickets/{id}")
    public DemoBusinessDataSource.SupportTicket ticketDetail(@PathVariable("id") String id) {
        return businessService.supportTicketDetail(id);
    }

    @GetMapping("/demo/business/tickets/{id}/raw")
    public DemoBusinessDataSource.SupportTicket ticketRaw(@PathVariable("id") String id) {
        return businessService.supportTicketDetail(id);
    }

    @GetMapping("/demo/business/account")
    public DemoBusinessDataSource.DemoAccount account() {
        return businessService.account();
    }

    @GetMapping("/demo/business/accounts")
    public List<DemoBusinessDataSource.AccountSecurity> accounts() {
        return businessService.accounts();
    }

    @GetMapping("/demo/business/accounts/{id}")
    public DemoBusinessDataSource.AccountSecurity accountDetail(@PathVariable("id") String id) {
        return businessService.accountDetail(id);
    }

    @GetMapping("/demo/business/accounts/{id}/raw")
    public DemoBusinessDataSource.AccountSecurity accountRaw(@PathVariable("id") String id) {
        return businessService.accountDetail(id);
    }

    @GetMapping("/demo/business/legacy-plaintext")
    public DemoBusinessDataSource.DemoCustomer legacyPlaintext() {
        return businessService.legacyPlaintextCustomer();
    }
}

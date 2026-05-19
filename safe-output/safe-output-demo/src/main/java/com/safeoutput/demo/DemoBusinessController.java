package com.safeoutput.demo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoBusinessController {

    private static final Logger LOGGER = LogManager.getLogger(DemoBusinessController.class);

    private final DemoBusinessService businessService;

    public DemoBusinessController(DemoBusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping("/demo/workbench")
    public Map<String, Object> workbench() {
        logWorkbenchAccess();
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("summary", businessService.workbenchSummary());
        response.put("scenarios", businessService.scenarioMetadata());
        return response;
    }

    @GetMapping("/demo/business/customer")
    public DemoBusinessDataSource.DemoCustomer customer() {
        logCustomerAccess("customer");
        return businessService.customer();
    }

    @GetMapping("/demo/business/customers")
    public List<DemoBusinessDataSource.CustomerProfile> customers() {
        logCustomerAccess("customers");
        return businessService.customers();
    }

    @GetMapping("/demo/business/customers/{id}")
    public DemoBusinessDataSource.CustomerProfile customerDetail(@PathVariable("id") String id) {
        logCustomerAccess("customers-detail");
        return businessService.customerDetail(id);
    }

    @GetMapping("/demo/business/customers/{id}/raw")
    public DemoBusinessDataSource.CustomerProfile customerRaw(@PathVariable("id") String id) {
        logCustomerAccess("customers-raw");
        return businessService.customerDetail(id);
    }

    @GetMapping("/demo/business/order")
    public DemoBusinessDataSource.DemoOrder order() {
        logOrderAccess("order");
        return businessService.order();
    }

    @GetMapping("/demo/business/orders")
    public List<DemoBusinessDataSource.OrderFulfillment> orders() {
        logOrderAccess("orders");
        return businessService.orders();
    }

    @GetMapping("/demo/business/orders/{id}")
    public DemoBusinessDataSource.OrderFulfillment orderDetail(@PathVariable("id") String id) {
        logOrderDetailAccess("orders-detail");
        return businessService.orderDetail(id);
    }

    @GetMapping("/demo/business/orders/{id}/raw")
    public DemoBusinessDataSource.OrderFulfillment orderRaw(@PathVariable("id") String id) {
        logOrderAccess("orders-raw");
        return businessService.orderDetail(id);
    }

    @GetMapping("/demo/business/payment")
    public Map<String, Object> payment() {
        logPaymentAccess("payment");
        return businessService.payment();
    }

    @GetMapping("/demo/business/payments")
    public List<DemoBusinessDataSource.PaymentVerification> payments() {
        logPaymentAccess("payments");
        return businessService.payments();
    }

    @GetMapping("/demo/business/payments/{id}")
    public DemoBusinessDataSource.PaymentVerification paymentDetail(@PathVariable("id") String id) {
        logPaymentDetailAccess("payments-detail");
        return businessService.paymentDetail(id);
    }

    @GetMapping("/demo/business/payments/{id}/raw")
    public DemoBusinessDataSource.PaymentVerification paymentRaw(@PathVariable("id") String id) {
        logPaymentAccess("payments-raw");
        return businessService.paymentDetail(id);
    }

    @GetMapping("/demo/business/tickets")
    public Object tickets() {
        logTicketAccess("tickets");
        return businessService.tickets();
    }

    @GetMapping("/demo/business/tickets/{id}")
    public DemoBusinessDataSource.SupportTicket ticketDetail(@PathVariable("id") String id) {
        logTicketDetailAccess("tickets-detail");
        return businessService.supportTicketDetail(id);
    }

    @GetMapping("/demo/business/tickets/{id}/raw")
    public DemoBusinessDataSource.SupportTicket ticketRaw(@PathVariable("id") String id) {
        logTicketAccess("tickets-raw");
        return businessService.supportTicketDetail(id);
    }

    @GetMapping("/demo/business/account")
    public DemoBusinessDataSource.DemoAccount account() {
        logAccountAccess("account");
        return businessService.account();
    }

    @GetMapping("/demo/business/accounts")
    public List<DemoBusinessDataSource.AccountSecurity> accounts() {
        logAccountAccess("accounts");
        return businessService.accounts();
    }

    @GetMapping("/demo/business/accounts/{id}")
    public DemoBusinessDataSource.AccountSecurity accountDetail(@PathVariable("id") String id) {
        logAccountDetailAccess("accounts-detail");
        return businessService.accountDetail(id);
    }

    @GetMapping("/demo/business/accounts/{id}/raw")
    public DemoBusinessDataSource.AccountSecurity accountRaw(@PathVariable("id") String id) {
        logAccountAccess("accounts-raw");
        return businessService.accountDetail(id);
    }

    @GetMapping("/demo/business/legacy-plaintext")
    public DemoBusinessDataSource.DemoCustomer legacyPlaintext() {
        logCustomerAccess("legacy-plaintext");
        return businessService.legacyPlaintextCustomer();
    }

    private static void logWorkbenchAccess() {
        LOGGER.info("workbench overview mobile=13800138000 phoneNo=13900139000"
                + " email=workbench-log@example.com");
    }

    private static void logCustomerAccess(String route) {
        LOGGER.info("customer profile route=" + route
                + " mobile=13800138000 idCard=11010519491231002X email=customer-log@example.com");
    }

    private static void logOrderAccess(String route) {
        LOGGER.info("order fulfillment route=" + route
                + " mobile=13800138000 bankCard=6222021234567890123"
                + " shippingAddress=北京市海淀区中关村南大街27号");
    }

    private static void logOrderDetailAccess(String route) {
        LOGGER.info("order detail route=" + route
                + " {\"mobile\":\"13800138000\",\"bankCard\":\"6222021234567890123\"}"
                + " shippingAddress=北京市海淀区中关村南大街27号");
    }

    private static void logPaymentAccess(String route) {
        LOGGER.info("payment verification route=" + route
                + " mobile=13900138001 bankCard=6222029876543210987"
                + " email=payment-log@example.com securityAnswer=母亲生日是19900101");
    }

    private static void logPaymentDetailAccess(String route) {
        LOGGER.info("payment detail route=" + route
                + " {\"mobile\":\"13900138001\",\"email\":\"payment-detail@example.com\"}"
                + " bankCard=6222029876543210987 securityAnswer=母亲生日是19900101");
    }

    private static void logTicketAccess(String route) {
        LOGGER.info("support ticket route=" + route
                + " mobile=13700138002 email=ticket-log@example.com");
    }

    private static void logTicketDetailAccess(String route) {
        LOGGER.info("ticket detail route=" + route
                + " {\"mobile\":\"13700138002\",\"email\":\"ticket-detail@example.com\"}");
    }

    private static void logAccountAccess(String route) {
        LOGGER.info("account security route=" + route
                + " mobile=13500138000 email=account-log@example.com"
                + " password=Secret-12340 shippingAddress=安全城市风控街31号");
    }

    private static void logAccountDetailAccess(String route) {
        LOGGER.info("account detail route=" + route
                + " {\"mobile\":\"13500138000\",\"email\":\"account-detail@example.com\"}"
                + " password=Secret-12340 shippingAddress=安全城市风控街31号");
    }
}

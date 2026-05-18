package com.safeoutput.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/demo/business/order")
    public DemoBusinessDataSource.DemoOrder order() {
        return businessService.order();
    }

    @GetMapping("/demo/business/payment")
    public Map<String, Object> payment() {
        return businessService.payment();
    }

    @GetMapping("/demo/business/tickets")
    public Object tickets() {
        return businessService.tickets();
    }

    @GetMapping("/demo/business/account")
    public DemoBusinessDataSource.DemoAccount account() {
        return businessService.account();
    }

    @GetMapping("/demo/business/legacy-plaintext")
    public DemoBusinessDataSource.DemoCustomer legacyPlaintext() {
        return businessService.legacyPlaintextCustomer();
    }
}

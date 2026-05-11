package com.safeoutput.demo;

import com.safeoutput.core.Desensitize;
import com.safeoutput.core.MaskType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoResponseController {

    @GetMapping("/demo/bean")
    public CustomerResponse bean() {
        return customer("张三", "13800138000");
    }

    @GetMapping("/demo/map")
    public Map<String, Object> map() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("email", "foo@example.com");
        response.put("mobile", "13800138000");
        return response;
    }

    @GetMapping("/demo/list")
    public List<CustomerResponse> list() {
        return Arrays.asList(customer("李四", "13900138001"), customer("王五", "13700138002"));
    }

    @GetMapping("/demo/nested")
    public OrderResponse nested() {
        return new OrderResponse("ORD-001", customer("赵六", "13600138003"), "6222021234567890123");
    }

    @GetMapping("/demo/ignored")
    public CustomerResponse ignored() {
        return customer("张三", "13800138000");
    }

    private static CustomerResponse customer(String name, String mobile) {
        return new CustomerResponse(name, mobile, "demo note 13800138000");
    }

    public static final class CustomerResponse {

        @Desensitize(type = MaskType.CHINESE_NAME)
        private String name;

        private String mobile;

        private String plainNote;

        public CustomerResponse(String name, String mobile, String plainNote) {
            this.name = name;
            this.mobile = mobile;
            this.plainNote = plainNote;
        }

        public String getName() {
            return name;
        }

        public String getMobile() {
            return mobile;
        }

        public String getPlainNote() {
            return plainNote;
        }
    }

    public static final class OrderResponse {

        private String orderNo;

        private CustomerResponse customer;

        private String bankCard;

        public OrderResponse(String orderNo, CustomerResponse customer, String bankCard) {
            this.orderNo = orderNo;
            this.customer = customer;
            this.bankCard = bankCard;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public CustomerResponse getCustomer() {
            return customer;
        }

        public String getBankCard() {
            return bankCard;
        }
    }
}

package com.safeoutput.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class DemoBusinessDataSource {

    public DemoCustomer customer() {
        return new DemoCustomer("C-1001", "张三", "13800138000", "11010519491231002X",
                "zhangsan@example.com", "北京市朝阳区望京街道88号", "demo note 13800138000");
    }

    public DemoOrder order() {
        return new DemoOrder("ORD-20260518-001", customer(), "6222021234567890123",
                "北京市海淀区中关村南大街27号", "blue-widget");
    }

    public Map<String, Object> payment() {
        Map<String, Object> payment = new LinkedHashMap<String, Object>();
        payment.put("paymentNo", "PAY-8848");
        payment.put("mobile", "13900138001");
        payment.put("bankCard", "6222029876543210987");
        payment.put("securityAnswer", "母亲生日是19900101");
        payment.put("channelEmail", "payops@example.com");
        return payment;
    }

    public List<DemoTicket> tickets() {
        List<DemoTicket> tickets = new ArrayList<DemoTicket>();
        tickets.add(new DemoTicket("TK-01", "李四", "13700138002", "lisi@example.com", "账号解锁"));
        tickets.add(new DemoTicket("TK-02", "王五", "13600138003", "wangwu@example.com", "支付核验"));
        return tickets;
    }

    public DemoAccount account() {
        return new DemoAccount("AC-7788", "赵六", "13500138004", "zhaoliu@example.com",
                "Secret-12345", "北京市西城区金融大街11号");
    }

    public DemoCustomer legacyPlaintextCustomer() {
        return customer();
    }

    public static final class DemoCustomer {

        private String customerNo;

        @com.safeoutput.core.Desensitize(type = com.safeoutput.core.MaskTypes.CHINESE_NAME)
        private String displayName;

        private String mobile;

        private String idCard;

        private String email;

        private String shippingAddress;

        private String plainNote;

        DemoCustomer(String customerNo, String displayName, String mobile, String idCard, String email,
                String shippingAddress, String plainNote) {
            this.customerNo = customerNo;
            this.displayName = displayName;
            this.mobile = mobile;
            this.idCard = idCard;
            this.email = email;
            this.shippingAddress = shippingAddress;
            this.plainNote = plainNote;
        }

        public String getCustomerNo() {
            return customerNo;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getMobile() {
            return mobile;
        }

        public String getIdCard() {
            return idCard;
        }

        public String getEmail() {
            return email;
        }

        public String getShippingAddress() {
            return shippingAddress;
        }

        public String getPlainNote() {
            return plainNote;
        }
    }

    public static final class DemoOrder {

        private String orderNo;

        private DemoCustomer customer;

        private String bankCard;

        private String shippingAddress;

        private String productName;

        DemoOrder(String orderNo, DemoCustomer customer, String bankCard, String shippingAddress,
                String productName) {
            this.orderNo = orderNo;
            this.customer = customer;
            this.bankCard = bankCard;
            this.shippingAddress = shippingAddress;
            this.productName = productName;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public DemoCustomer getCustomer() {
            return customer;
        }

        public String getBankCard() {
            return bankCard;
        }

        public String getShippingAddress() {
            return shippingAddress;
        }

        public String getProductName() {
            return productName;
        }
    }

    public static final class DemoTicket {

        private String ticketNo;

        private String realName;

        private String mobile;

        private String email;

        private String title;

        DemoTicket(String ticketNo, String realName, String mobile, String email, String title) {
            this.ticketNo = ticketNo;
            this.realName = realName;
            this.mobile = mobile;
            this.email = email;
            this.title = title;
        }

        public String getTicketNo() {
            return ticketNo;
        }

        public String getRealName() {
            return realName;
        }

        public String getMobile() {
            return mobile;
        }

        public String getEmail() {
            return email;
        }

        public String getTitle() {
            return title;
        }
    }

    public static final class DemoAccount {

        private String accountNo;

        private String realName;

        private String mobile;

        private String email;

        private String password;

        private String shippingAddress;

        DemoAccount(String accountNo, String realName, String mobile, String email, String password,
                String shippingAddress) {
            this.accountNo = accountNo;
            this.realName = realName;
            this.mobile = mobile;
            this.email = email;
            this.password = password;
            this.shippingAddress = shippingAddress;
        }

        public String getAccountNo() {
            return accountNo;
        }

        public String getRealName() {
            return realName;
        }

        public String getMobile() {
            return mobile;
        }

        public String getEmail() {
            return email;
        }

        public String getPassword() {
            return password;
        }

        public String getShippingAddress() {
            return shippingAddress;
        }
    }
}

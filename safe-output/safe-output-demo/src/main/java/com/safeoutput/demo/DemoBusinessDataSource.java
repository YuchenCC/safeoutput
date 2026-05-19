package com.safeoutput.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class DemoBusinessDataSource {

    public DemoCustomer customer() {
        return customerProfiles().get(0).toDemoCustomer();
    }

    public DemoOrder order() {
        CustomerProfile customer = customerProfiles().get(0);
        return new DemoOrder("ORD-20260518-001", customer.toDemoCustomer(), "6222021234567890123",
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
        AccountSecurity account = accountSecurities().get(0);
        return new DemoAccount(account.getAccountNo(), account.getRealName(), account.getMobile(),
                account.getEmail(), account.getPassword(), account.getShippingAddress());
    }

    public DemoCustomer legacyPlaintextCustomer() {
        return customer();
    }

    public List<CustomerProfile> customerProfiles() {
        List<CustomerProfile> customers = new ArrayList<CustomerProfile>();
        String[] names = { "张三", "李明", "王芳", "赵一诺", "陈晨", "周嘉" };
        String[] levels = { "钻石", "黄金", "白银", "黄金", "铂金", "白银" };
        String[] cities = { "北京", "上海", "杭州", "深圳", "南京", "成都" };
        for (int i = 0; i < names.length; i++) {
            customers.add(new CustomerProfile("C-" + (1001 + i), names[i], "1380013800" + i,
                    "11010519491231002" + (i == 0 ? "X" : String.valueOf(i)), "customer" + (i + 1)
                            + "@example.com", cities[i] + "市核心区安全街" + (18 + i) + "号",
                    levels[i], "实名已核验", "demo note 1380013800" + i));
        }
        return customers;
    }

    public List<OrderFulfillment> orderFulfillments() {
        List<CustomerProfile> customers = customerProfiles();
        List<OrderFulfillment> orders = new ArrayList<OrderFulfillment>();
        String[] statuses = { "待出库", "运输中", "待签收", "已签收", "异常拦截", "待复核" };
        for (int i = 0; i < customers.size(); i++) {
            CustomerProfile customer = customers.get(i);
            orders.add(new OrderFulfillment("ORD-20260518-00" + (i + 1), customer.getCustomerNo(),
                    customer.getDisplayName(), customer.getMobile(), "62220212345678901" + (20 + i),
                    customer.getShippingAddress(), "SKU-" + (700 + i), statuses[i], 1 + i,
                    "履约节点-" + (i + 1)));
        }
        return orders;
    }

    public List<PaymentVerification> paymentVerifications() {
        List<PaymentVerification> payments = new ArrayList<PaymentVerification>();
        String[] channels = { "快捷支付", "网银", "企业转账", "余额", "代扣", "退款" };
        for (int i = 0; i < channels.length; i++) {
            payments.add(new PaymentVerification("PAY-884" + i, "C-" + (1001 + i), "付款人" + (i + 1),
                    "1390013800" + i, "62220298765432109" + (80 + i), "pay" + (i + 1) + "@example.com",
                    "母亲生日是1990010" + i, channels[i], i % 2 == 0 ? "待核验" : "已通过"));
        }
        return payments;
    }

    public List<SupportTicket> supportTickets() {
        List<SupportTicket> tickets = new ArrayList<SupportTicket>();
        String[] topics = { "账号解锁", "支付核验", "地址修改", "发票补开", "退款催办", "登录异常" };
        for (int i = 0; i < topics.length; i++) {
            tickets.add(new SupportTicket("TK-20260518-0" + (i + 1), "用户" + (i + 1), "1370013800" + i,
                    "ticket" + (i + 1) + "@example.com", topics[i], i % 3 == 0 ? "高" : "中",
                    "客户补充手机号 1370013800" + i));
        }
        return tickets;
    }

    public List<AccountSecurity> accountSecurities() {
        List<AccountSecurity> accounts = new ArrayList<AccountSecurity>();
        String[] states = { "正常", "异地登录", "密码过期", "设备变更", "风险拦截", "待实名" };
        for (int i = 0; i < states.length; i++) {
            accounts.add(new AccountSecurity("AC-778" + i, "账户人" + (i + 1), "1350013800" + i,
                    "account" + (i + 1) + "@example.com", "Secret-1234" + i,
                    "安全城市风控街" + (31 + i) + "号", states[i], "DEV-" + (9000 + i)));
        }
        return accounts;
    }

    public CustomerProfile customerProfile(String id) {
        for (CustomerProfile item : customerProfiles()) {
            if (item.getCustomerNo().equals(id)) {
                return item;
            }
        }
        return customerProfiles().get(0);
    }

    public OrderFulfillment orderFulfillment(String id) {
        for (OrderFulfillment item : orderFulfillments()) {
            if (item.getOrderNo().equals(id)) {
                return item;
            }
        }
        return orderFulfillments().get(0);
    }

    public PaymentVerification paymentVerification(String id) {
        for (PaymentVerification item : paymentVerifications()) {
            if (item.getPaymentNo().equals(id)) {
                return item;
            }
        }
        return paymentVerifications().get(0);
    }

    public SupportTicket supportTicket(String id) {
        for (SupportTicket item : supportTickets()) {
            if (item.getTicketNo().equals(id)) {
                return item;
            }
        }
        return supportTickets().get(0);
    }

    public AccountSecurity accountSecurity(String id) {
        for (AccountSecurity item : accountSecurities()) {
            if (item.getAccountNo().equals(id)) {
                return item;
            }
        }
        return accountSecurities().get(0);
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

    public static final class CustomerProfile {

        private String customerNo;

        @com.safeoutput.core.Desensitize(type = com.safeoutput.core.MaskTypes.CHINESE_NAME)
        private String displayName;

        private String mobile;

        private String idCard;

        private String email;

        private String shippingAddress;

        private String customerLevel;

        private String status;

        private String plainNote;

        CustomerProfile(String customerNo, String displayName, String mobile, String idCard, String email,
                String shippingAddress, String customerLevel, String status, String plainNote) {
            this.customerNo = customerNo;
            this.displayName = displayName;
            this.mobile = mobile;
            this.idCard = idCard;
            this.email = email;
            this.shippingAddress = shippingAddress;
            this.customerLevel = customerLevel;
            this.status = status;
            this.plainNote = plainNote;
        }

        DemoCustomer toDemoCustomer() {
            return new DemoCustomer(customerNo, displayName, mobile, idCard, email, shippingAddress, plainNote);
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

        public String getCustomerLevel() {
            return customerLevel;
        }

        public String getStatus() {
            return status;
        }

        public String getPlainNote() {
            return plainNote;
        }
    }

    public static final class OrderFulfillment {

        private String orderNo;

        private String customerNo;

        @com.safeoutput.core.Desensitize(type = com.safeoutput.core.MaskTypes.CHINESE_NAME)
        private String customerName;

        private String mobile;

        private String bankCard;

        private String shippingAddress;

        private String productSku;

        private String fulfillmentStatus;

        private int quantity;

        private String warehouseMemo;

        OrderFulfillment(String orderNo, String customerNo, String customerName, String mobile, String bankCard,
                String shippingAddress, String productSku, String fulfillmentStatus, int quantity,
                String warehouseMemo) {
            this.orderNo = orderNo;
            this.customerNo = customerNo;
            this.customerName = customerName;
            this.mobile = mobile;
            this.bankCard = bankCard;
            this.shippingAddress = shippingAddress;
            this.productSku = productSku;
            this.fulfillmentStatus = fulfillmentStatus;
            this.quantity = quantity;
            this.warehouseMemo = warehouseMemo;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public String getCustomerNo() {
            return customerNo;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getMobile() {
            return mobile;
        }

        public String getBankCard() {
            return bankCard;
        }

        public String getShippingAddress() {
            return shippingAddress;
        }

        public String getProductSku() {
            return productSku;
        }

        public String getFulfillmentStatus() {
            return fulfillmentStatus;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getWarehouseMemo() {
            return warehouseMemo;
        }
    }

    public static final class PaymentVerification {

        private String paymentNo;

        private String customerNo;

        @com.safeoutput.core.Desensitize(type = com.safeoutput.core.MaskTypes.CHINESE_NAME)
        private String payerName;

        private String mobile;

        private String bankCard;

        private String email;

        private String securityAnswer;

        private String channel;

        private String verifyStatus;

        PaymentVerification(String paymentNo, String customerNo, String payerName, String mobile, String bankCard,
                String email, String securityAnswer, String channel, String verifyStatus) {
            this.paymentNo = paymentNo;
            this.customerNo = customerNo;
            this.payerName = payerName;
            this.mobile = mobile;
            this.bankCard = bankCard;
            this.email = email;
            this.securityAnswer = securityAnswer;
            this.channel = channel;
            this.verifyStatus = verifyStatus;
        }

        public String getPaymentNo() {
            return paymentNo;
        }

        public String getCustomerNo() {
            return customerNo;
        }

        public String getPayerName() {
            return payerName;
        }

        public String getMobile() {
            return mobile;
        }

        public String getBankCard() {
            return bankCard;
        }

        public String getEmail() {
            return email;
        }

        public String getSecurityAnswer() {
            return securityAnswer;
        }

        public String getChannel() {
            return channel;
        }

        public String getVerifyStatus() {
            return verifyStatus;
        }
    }

    public static final class SupportTicket {

        private String ticketNo;

        @com.safeoutput.core.Desensitize(type = com.safeoutput.core.MaskTypes.CHINESE_NAME)
        private String requesterName;

        private String mobile;

        private String email;

        private String title;

        private String priority;

        private String plainNote;

        SupportTicket(String ticketNo, String requesterName, String mobile, String email, String title,
                String priority, String plainNote) {
            this.ticketNo = ticketNo;
            this.requesterName = requesterName;
            this.mobile = mobile;
            this.email = email;
            this.title = title;
            this.priority = priority;
            this.plainNote = plainNote;
        }

        public String getTicketNo() {
            return ticketNo;
        }

        public String getRequesterName() {
            return requesterName;
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

        public String getPriority() {
            return priority;
        }

        public String getPlainNote() {
            return plainNote;
        }
    }

    public static final class AccountSecurity {

        private String accountNo;

        @com.safeoutput.core.Desensitize(type = com.safeoutput.core.MaskTypes.CHINESE_NAME)
        private String realName;

        private String mobile;

        private String email;

        private String password;

        private String shippingAddress;

        private String securityState;

        private String deviceId;

        AccountSecurity(String accountNo, String realName, String mobile, String email, String password,
                String shippingAddress, String securityState, String deviceId) {
            this.accountNo = accountNo;
            this.realName = realName;
            this.mobile = mobile;
            this.email = email;
            this.password = password;
            this.shippingAddress = shippingAddress;
            this.securityState = securityState;
            this.deviceId = deviceId;
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

        public String getSecurityState() {
            return securityState;
        }

        public String getDeviceId() {
            return deviceId;
        }
    }
}

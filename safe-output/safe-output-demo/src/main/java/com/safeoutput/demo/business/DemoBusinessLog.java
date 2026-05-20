package com.safeoutput.demo.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class DemoBusinessLog {

    private static final Logger LOGGER = LogManager.getLogger(DemoBusinessController.class);

    private DemoBusinessLog() {
    }

    static void workbenchAccess() {
        LOGGER.info("workbench overview mobile=13800138000 phoneNo=13900139000"
                + " email=workbench-log@example.com");
    }

    static void customerAccess(String route) {
        LOGGER.info("customer profile route=" + route
                + " mobile=13800138000 idCard=11010519491231002X email=customer-log@example.com");
    }

    static void orderAccess(String route) {
        LOGGER.info("order fulfillment route=" + route
                + " mobile=13800138000 bankCard=6222021234567890123"
                + " shippingAddress=北京市海淀区中关村南大街27号");
    }

    static void orderDetailAccess(String route) {
        LOGGER.info("order detail route=" + route
                + " {\"mobile\":\"13800138000\",\"bankCard\":\"6222021234567890123\"}"
                + " shippingAddress=北京市海淀区中关村南大街27号");
    }

    static void paymentAccess(String route) {
        LOGGER.info("payment verification route=" + route
                + " mobile=13900138001 bankCard=6222029876543210987"
                + " email=payment-log@example.com securityAnswer=母亲生日是19900101");
    }

    static void paymentDetailAccess(String route) {
        LOGGER.info("payment detail route=" + route
                + " {\"mobile\":\"13900138001\",\"email\":\"payment-detail@example.com\"}"
                + " bankCard=6222029876543210987 securityAnswer=母亲生日是19900101");
    }

    static void ticketAccess(String route) {
        LOGGER.info("support ticket route=" + route
                + " mobile=13700138002 email=ticket-log@example.com");
    }

    static void ticketDetailAccess(String route) {
        LOGGER.info("ticket detail route=" + route
                + " {\"mobile\":\"13700138002\",\"email\":\"ticket-detail@example.com\"}");
    }

    static void accountAccess(String route) {
        LOGGER.info("account security route=" + route
                + " mobile=13500138000 email=account-log@example.com"
                + " password=Secret-12340 shippingAddress=安全城市风控街31号");
    }

    static void accountDetailAccess(String route) {
        LOGGER.info("account detail route=" + route
                + " {\"mobile\":\"13500138000\",\"email\":\"account-detail@example.com\"}"
                + " password=Secret-12340 shippingAddress=安全城市风控街31号");
    }
}

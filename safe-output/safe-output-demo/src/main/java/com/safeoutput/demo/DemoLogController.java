package com.safeoutput.demo;

import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoLogController {

    private static final Logger LOGGER = LogManager.getLogger(DemoLogController.class);

    @GetMapping("/demo/logs")
    public Map<String, String> logs() {
        // 日志 Demo 故意包含不同形态数据，用 %safeOutputMsg 验证日志输出侧脱敏。
        LOGGER.info("demo log mobile=13800138000 email=foo@example.com idCard=11010519491231002X"
                + " phoneNo=13900139000 certNum=11010519491231002X mailAddr=bar@example.com"
                + " flow=123456789012345678 bank=6222021234567890123");
        return Collections.singletonMap("status", "logged");
    }
}

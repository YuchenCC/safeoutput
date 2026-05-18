package com.safeoutput.demo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.safeoutput.report.LogRuleSuggestionAnalyzer;
import com.safeoutput.report.LogRuleSuggestionReport;
import com.safeoutput.report.MaskMetricsCollector;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputConfiguredKeys;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputProperties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoLogController {

    private static final Logger LOGGER = LogManager.getLogger(DemoLogController.class);

    private final MaskMetricsCollector metricsCollector;

    private final SafeOutputProperties properties;

    public DemoLogController(MaskMetricsCollector metricsCollector, SafeOutputProperties properties) {
        this.metricsCollector = metricsCollector;
        this.properties = properties;
    }

    @GetMapping("/demo/logs")
    public Map<String, String> logs() {
        // 日志 Demo 故意包含不同形态数据，用 %safeOutputMsg 验证日志输出侧脱敏。
        LOGGER.info("demo log mobile=13800138000 email=foo@example.com idCard=11010519491231002X"
                + " phoneNo=13900139000 certNum=11010519491231002X mailAddr=bar@example.com"
                + " flow=123456789012345678 bank=6222021234567890123");
        return Collections.singletonMap("status", "logged");
    }

    @GetMapping("/demo/logs/scenarios")
    public Map<String, Object> scenarios() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("scenarios", scenarioList());
        response.put("summary", logSummary());
        return response;
    }

    @GetMapping("/demo/logs/scenarios/{id}/trigger")
    public Map<String, Object> trigger(@PathVariable("id") String id) {
        long before = metricsCollector.snapshot().getLogCount();
        if ("json-like".equals(id)) {
            LOGGER.info("order audit {\"mobile\":\"13800138000\",\"email\":\"jsonuser@example.com\"}");
        } else if ("key-value".equals(id)) {
            LOGGER.info("payment audit mobile=13900138001 email=payuser@example.com phoneNo=13700138002");
        } else if ("regex-fallback".equals(id)) {
            LOGGER.info("free text contact 13600138003 and fallback@example.com for manual review");
        } else if ("configured-vs-missing".equals(id)) {
            LOGGER.info("compare phoneNo=13500138004 certNum=11010519491231002X mailAddr=missing@example.com");
        } else {
            LOGGER.info("demo log mobile=13800138000 certNum=11010519491231002X mailAddr=bar@example.com");
        }
        long after = metricsCollector.snapshot().getLogCount();
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("scenarioId", id);
        response.put("status", "logged");
        response.put("logCountBefore", before);
        response.put("logCountAfter", after);
        response.put("templateSummary", templateSummary(id));
        response.put("summary", logSummary());
        return response;
    }

    private Map<String, Object> logSummary() {
        LogRuleSuggestionReport suggestions = new LogRuleSuggestionAnalyzer().analyze(
                metricsCollector.snapshotSuggestions(), SafeOutputConfiguredKeys.from(properties));
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("logCount", metricsCollector.snapshot().getLogCount());
        summary.put("suggestionCount", suggestions.getLogRuleSuggestions().size());
        summary.put("logRuleSuggestions", suggestions.getLogRuleSuggestions());
        summary.put("configSnippet", suggestions.getConfigSnippet());
        return summary;
    }

    private static List<Map<String, Object>> scenarioList() {
        List<Map<String, Object>> scenarios = new ArrayList<Map<String, Object>>();
        scenarios.add(scenario("json-like", "JSON-like", "mobile/email key 命中默认规则"));
        scenarios.add(scenario("key-value", "key=value", "已配置 phoneNo 与默认 mobile/email 对照"));
        scenarios.add(scenario("regex-fallback", "regex fallback", "无 key 文本只展示模板摘要"));
        scenarios.add(scenario("configured-vs-missing", "配置 key 对比", "phoneNo 已配置，certNum/mailAddr 生成建议"));
        return scenarios;
    }

    private static Map<String, Object> scenario(String id, String title, String expectation) {
        Map<String, Object> scenario = new LinkedHashMap<String, Object>();
        scenario.put("id", id);
        scenario.put("title", title);
        scenario.put("templateSummary", expectation);
        scenario.put("triggerEndpoint", "/demo/logs/scenarios/" + id + "/trigger");
        return scenario;
    }

    private static String templateSummary(String id) {
        if ("json-like".equals(id)) {
            return "JSON-like 模板含 mobile/email";
        }
        if ("key-value".equals(id)) {
            return "key=value 模板含 mobile/email/phoneNo";
        }
        if ("regex-fallback".equals(id)) {
            return "自由文本模板含手机号和邮箱 fallback";
        }
        if ("configured-vs-missing".equals(id)) {
            return "已配置 phoneNo 与未配置 certNum/mailAddr 对照";
        }
        return "综合日志模板";
    }
}

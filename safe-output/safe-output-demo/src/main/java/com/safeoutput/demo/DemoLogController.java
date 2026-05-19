package com.safeoutput.demo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.safeoutput.report.LogRuleSuggestionAnalyzer;
import com.safeoutput.report.LogRuleSuggestionReport;
import com.safeoutput.report.MaskMetricsCollector;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputConfiguredKeys;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputProperties;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoLogController {

    private final MaskMetricsCollector metricsCollector;

    private final SafeOutputProperties properties;

    public DemoLogController(MaskMetricsCollector metricsCollector, SafeOutputProperties properties) {
        this.metricsCollector = metricsCollector;
        this.properties = properties;
    }

    @GetMapping("/demo/logs/scenarios")
    public Map<String, Object> scenarios() {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("scenarios", scenarioList());
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
        scenarios.add(scenario("json-like", "JSON-like",
                "业务详情日志中的 mobile/email key 命中默认规则"));
        scenarios.add(scenario("key-value", "key=value",
                "业务菜单和实验室日志中的 key=value 参数脱敏"));
        scenarios.add(scenario("regex-fallback", "regex fallback",
                "强文本扫描日志中的无 key 文本兜底脱敏"));
        return scenarios;
    }

    private static Map<String, Object> scenario(String id, String title, String expectation) {
        Map<String, Object> scenario = new LinkedHashMap<String, Object>();
        scenario.put("id", id);
        scenario.put("title", title);
        scenario.put("templateSummary", expectation);
        return scenario;
    }
}

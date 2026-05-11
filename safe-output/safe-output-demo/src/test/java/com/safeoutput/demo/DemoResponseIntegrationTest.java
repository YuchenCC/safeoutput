package com.safeoutput.demo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.safeoutput.report.ApiMaskMetrics;
import com.safeoutput.report.MaskMetricsCollector;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(classes = DemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoResponseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MaskMetricsCollector metricsCollector;

    @Test
    void demoEndpointsMaskCommonResponseShapesWithoutControllerCalls() {
        String bean = restTemplate.getForObject("/demo/bean", String.class);
        String map = restTemplate.getForObject("/demo/map", String.class);
        String list = restTemplate.getForObject("/demo/list", String.class);
        String nested = restTemplate.getForObject("/demo/nested", String.class);

        assertTrue(bean.contains("138****8000"));
        assertTrue(bean.contains("\"name\":\"张*\""));
        assertTrue(bean.contains("\"plainNote\":\"demo note 13800138000\""));
        assertFalse(bean.contains("\"mobile\":\"13800138000\""));

        assertTrue(map.contains("foo****@example.com"));
        assertFalse(map.contains("foo@example.com"));
        assertTrue(list.contains("139****8001"));
        assertTrue(nested.contains("622202*********0123"));
    }

    @Test
    void apiIgnoreKeepsPlaintextResponseAndRecordsRiskMetric() {
        String ignored = restTemplate.getForObject("/demo/ignored", String.class);

        assertTrue(ignored.contains("\"mobile\":\"13800138000\""));
        ApiMaskMetrics metric = metricsCollector.snapshot().getApiMetric("GET", "/demo/ignored");
        assertNotNull(metric);
        assertTrue(metric.isIgnored());
    }
}

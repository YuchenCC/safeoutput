package com.safeoutput.dashboard;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(classes = TestDashboardApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "safe-output.dashboard.enabled=true",
                "safe-output.dashboard.path-prefix=/ops/safe-dashboard"
        })
class DashboardCustomPrefixIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void pathPrefixMovesStaticEntryAndApi() {
        String page = restTemplate.getForObject("/ops/safe-dashboard/index.html", String.class);
        ResponseEntity<String> health = restTemplate.postForEntity("/ops/safe-dashboard/api/health",
                new LinkedHashMap<String, Object>(), String.class);

        assertTrue(page.contains("Safe Output Dashboard"));
        assertTrue(health.getStatusCode().is2xxSuccessful());
        assertTrue(health.getBody().contains("\"pathPrefix\":\"/ops/safe-dashboard\""));
    }
}

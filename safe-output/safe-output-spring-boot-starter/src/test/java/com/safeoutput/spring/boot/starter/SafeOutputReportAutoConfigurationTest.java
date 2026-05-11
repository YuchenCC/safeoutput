package com.safeoutput.spring.boot.starter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.safeoutput.report.MaskMetricsCollector;
import com.safeoutput.report.MaskReportExporter;
import com.safeoutput.spring.boot.autoconfigure.SafeOutputAutoConfiguration;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SafeOutputReportAutoConfigurationTest {

    @TempDir
    Path tempDir;

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SafeOutputAutoConfiguration.class);

    @Test
    void reportExporterIsDisabledByDefault() {
        contextRunner.run(context -> {
            assertFalse(context.containsBean("maskMetricsCollector"));
            assertFalse(context.containsBean("maskReportExporter"));
        });
    }

    @Test
    void reportExporterStartsWhenReportIsEnabled() {
        contextRunner
                .withPropertyValues(
                        "safe-output.report.enabled=true",
                        "safe-output.report.directory=" + tempDir.toString(),
                        "safe-output.report.interval-millis=10000")
                .run(context -> {
                    assertTrue(context.getBean(MaskMetricsCollector.class) != null);
                    assertTrue(context.getBean(MaskReportExporter.class) != null);
                });
    }
}

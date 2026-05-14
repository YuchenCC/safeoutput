package com.safeoutput.report;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.safeoutput.core.MaskScene;
import com.safeoutput.core.MaskType;
import com.safeoutput.core.MaskTypes;
import com.safeoutput.core.ResponseRiskEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MaskReportExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void exportsSnapshotAsJsonWithoutRawPayloadFields() throws Exception {
        MaskMetricsCollector collector = new MaskMetricsCollector(10);
        collector.recordMask(MaskScene.RESPONSE, MaskType.MOBILE, TimeUnit.MILLISECONDS.toNanos(2));
        collector.recordMask(MaskScene.MANUAL, MaskType.EMAIL, TimeUnit.MILLISECONDS.toNanos(1));
        collector.recordUnknownType("mobileM", MaskScene.RESPONSE);
        collector.recordApi(new ResponseRiskEvent("GET", "/customers", false, null,
                Collections.singletonMap(MaskTypes.MOBILE, 1), TimeUnit.MILLISECONDS.toNanos(2)));

        MaskReportExporter exporter = new MaskReportExporter(new MaskReportExportOptions(tempDir, "safe-output",
                1000, 3), collector);

        Path written = exporter.exportNow();
        String json = new String(Files.readAllBytes(written), StandardCharsets.UTF_8);

        assertTrue(json.contains("\"totalCount\":2"));
        assertTrue(json.contains("\"responseCount\":1"));
        assertTrue(json.contains("\"manualCount\":1"));
        assertTrue(json.contains("\"mobile\":1"));
        assertTrue(json.contains("\"unknownTypeCounts\":{\"mobilem\":1}"));
        assertTrue(json.contains("\"method\":\"GET\""));
        assertTrue(json.contains("\"path\":\"/customers\""));
        assertTrue(json.contains("\"responseRiskSummary\""));
        assertTrue(json.contains("\"topRiskApis\""));
        assertTrue(json.contains("\"riskScore\""));
        assertTrue(json.contains("\"riskReasons\""));
        assertTrue(json.contains("\"governanceAdvice\""));
        assertTrue(json.contains("\"performanceProfile\""));
        assertFalse(json.contains("13800138000"));
        assertFalse(json.contains("responseBody"));
        assertFalse(json.contains("logMessage"));
    }

    @Test
    void scheduledExporterWritesSnapshotsAndRetainsNewestFiles() throws Exception {
        MaskMetricsCollector collector = new MaskMetricsCollector(10);
        collector.recordMask(MaskScene.LOG, MaskType.EMAIL, 1);
        MaskReportExporter exporter = new MaskReportExporter(new MaskReportExportOptions(tempDir, "snapshot",
                20, 2), collector);

        exporter.start();
        try {
            waitForAtLeastFiles(2);
            Thread.sleep(80);
        } finally {
            exporter.stop();
        }

        List<Path> files = reportFiles();
        assertEquals(2, files.size());
        assertTrue(files.get(0).getFileName().toString().startsWith("snapshot-"));
    }

    @Test
    void writeFailureIncrementsMetricsAndDoesNotThrow() throws Exception {
        Path fileAsDirectory = tempDir.resolve("not-a-directory");
        Files.write(fileAsDirectory, new byte[] {1});
        MaskMetricsCollector collector = new MaskMetricsCollector(10);
        MaskReportExporter exporter = new MaskReportExporter(new MaskReportExportOptions(fileAsDirectory, "snapshot",
                1000, 3), collector);

        assertDoesNotThrow(new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                exporter.exportNow();
            }
        });
        assertEquals(1, collector.snapshot().getFailureCount());
    }

    private void waitForAtLeastFiles(int count) throws Exception {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (reportFiles().size() >= count) {
                return;
            }
            Thread.sleep(20);
        }
    }

    private List<Path> reportFiles() throws Exception {
        try (Stream<Path> stream = Files.list(tempDir)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }
}

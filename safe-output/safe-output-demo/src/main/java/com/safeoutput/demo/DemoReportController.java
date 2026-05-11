package com.safeoutput.demo;

import com.safeoutput.report.MaskReportExporter;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoReportController {

    private final MaskReportExporter exporter;

    public DemoReportController(MaskReportExporter exporter) {
        this.exporter = exporter;
    }

    @GetMapping("/demo/report/export")
    public Map<String, String> export() {
        Path path = exporter.exportNow();
        return Collections.singletonMap("path", path == null ? "" : path.toString());
    }
}

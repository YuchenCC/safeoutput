package com.safeoutput.report;

import com.safeoutput.core.MaskType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class MaskReportExporter {

    private static final Logger LOGGER = Logger.getLogger(MaskReportExporter.class.getName());

    private final MaskReportExportOptions options;
    private final MaskMetricsCollector collector;
    private final AtomicLong sequence = new AtomicLong();

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;

    public MaskReportExporter(MaskReportExportOptions options, MaskMetricsCollector collector) {
        this.options = options;
        this.collector = collector;
    }

    public synchronized void start() {
        if (future != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "safe-output-report-exporter");
            thread.setDaemon(true);
            return thread;
        });
        future = executor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                exportNow();
            }
        }, options.getIntervalMillis(), options.getIntervalMillis(), TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public Path exportNow() {
        try {
            Files.createDirectories(options.getDirectory());
            Path target = nextFile();
            // 报告只写聚合快照，不写原始响应、原始日志或敏感字段值。
            Files.write(target, toJson(collector.snapshot()).getBytes(StandardCharsets.UTF_8));
            retainNewestFiles();
            return target;
        } catch (RuntimeException ex) {
            recordFailure(ex);
        } catch (IOException ex) {
            recordFailure(ex);
        }
        return null;
    }

    private void recordFailure(Exception ex) {
        collector.recordFailure();
        LOGGER.log(Level.WARNING, "Failed to export safe-output report snapshot: {0}", ex.toString());
    }

    private Path nextFile() {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String filename = options.getFilePrefix() + "-" + timestamp + "-" + sequence.incrementAndGet() + ".json";
        return options.getDirectory().resolve(filename);
    }

    private void retainNewestFiles() throws IOException {
        List<Path> files = reportFiles();
        int deleteCount = files.size() - options.getRetainFiles();
        // 文件名包含递增时间戳，字典序排序后前面的就是最旧快照。
        for (int i = 0; i < deleteCount; i++) {
            Files.deleteIfExists(files.get(i));
        }
    }

    private List<Path> reportFiles() throws IOException {
        if (!Files.isDirectory(options.getDirectory())) {
            return new ArrayList<Path>();
        }
        try (Stream<Path> stream = Files.list(options.getDirectory())) {
            return stream
                    .filter(path -> path.getFileName().toString().startsWith(options.getFilePrefix() + "-"))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static String toJson(MaskReport report) {
        StringBuilder json = new StringBuilder();
        json.append('{');
        field(json, "totalCount", report.getTotalCount()).append(',');
        field(json, "responseCount", report.getResponseCount()).append(',');
        field(json, "logCount", report.getLogCount()).append(',');
        field(json, "failureCount", report.getFailureCount()).append(',');
        field(json, "averageElapsedNanos", report.getAverageElapsedNanos()).append(',');
        field(json, "maxElapsedNanos", report.getMaxElapsedNanos()).append(',');
        json.append("\"maskTypeCounts\":");
        maskTypeCounts(json, report.getMaskTypeCounts()).append(',');
        json.append("\"apiMetrics\":");
        apiMetrics(json, report.getApiMetrics());
        json.append('}');
        return json.toString();
    }

    private static StringBuilder apiMetrics(StringBuilder json, List<ApiMaskMetrics> metrics) {
        json.append('[');
        for (int i = 0; i < metrics.size(); i++) {
            if (i > 0) {
                json.append(',');
            }
            ApiMaskMetrics metric = metrics.get(i);
            json.append('{');
            stringField(json, "method", metric.getMethod()).append(',');
            stringField(json, "path", metric.getPath()).append(',');
            field(json, "hitCount", metric.getHitCount()).append(',');
            booleanField(json, "ignored", metric.isIgnored()).append(',');
            stringField(json, "ignoreReason", metric.getIgnoreReason()).append(',');
            field(json, "averageElapsedNanos", metric.getAverageElapsedNanos()).append(',');
            field(json, "maxElapsedNanos", metric.getMaxElapsedNanos()).append(',');
            stringField(json, "riskLevel", metric.getRiskLevel().name()).append(',');
            json.append("\"maskTypeCounts\":");
            maskTypeCounts(json, metric.getMaskTypeCounts());
            json.append('}');
        }
        json.append(']');
        return json;
    }

    private static StringBuilder maskTypeCounts(StringBuilder json, Map<MaskType, Long> counts) {
        json.append('{');
        int index = 0;
        for (Map.Entry<MaskType, Long> entry : counts.entrySet()) {
            if (index > 0) {
                json.append(',');
            }
            string(json, entry.getKey().name()).append(':').append(entry.getValue());
            index++;
        }
        json.append('}');
        return json;
    }

    private static StringBuilder field(StringBuilder json, String name, long value) {
        return string(json, name).append(':').append(value);
    }

    private static StringBuilder booleanField(StringBuilder json, String name, boolean value) {
        return string(json, name).append(':').append(value);
    }

    private static StringBuilder stringField(StringBuilder json, String name, String value) {
        string(json, name).append(':');
        if (value == null) {
            return json.append("null");
        }
        return string(json, value);
    }

    private static StringBuilder string(StringBuilder json, String value) {
        json.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '"' || ch == '\\') {
                json.append('\\').append(ch);
            } else if (ch == '\n') {
                json.append("\\n");
            } else if (ch == '\r') {
                json.append("\\r");
            } else if (ch == '\t') {
                json.append("\\t");
            } else {
                json.append(ch);
            }
        }
        return json.append('"');
    }
}

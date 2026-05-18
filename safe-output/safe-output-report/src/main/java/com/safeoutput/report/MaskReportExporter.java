package com.safeoutput.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
    private final List<String> configuredKeys;
    private final MaskReportJsonWriter jsonWriter = new MaskReportJsonWriter();
    private final AtomicLong sequence = new AtomicLong();

    private ScheduledExecutorService executor;
    private ScheduledFuture<?> future;

    public MaskReportExporter(MaskReportExportOptions options, MaskMetricsCollector collector) {
        this(options, collector, Collections.<String>emptyList());
    }

    public MaskReportExporter(MaskReportExportOptions options, MaskMetricsCollector collector,
            List<String> configuredKeys) {
        this.options = options;
        this.collector = collector;
        this.configuredKeys = configuredKeys == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(configuredKeys));
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
            LogRuleSuggestionReport suggestions = new LogRuleSuggestionAnalyzer()
                    .analyze(collector.snapshotSuggestions(), configuredKeys);
            Files.write(target, jsonWriter.write(collector.snapshot(), suggestions).getBytes(StandardCharsets.UTF_8));
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
}

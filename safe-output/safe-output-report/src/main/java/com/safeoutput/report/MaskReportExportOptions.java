package com.safeoutput.report;

import java.nio.file.Path;

public final class MaskReportExportOptions {

    private final Path directory;

    private final String filePrefix;

    private final long intervalMillis;

    private final int retainFiles;

    public MaskReportExportOptions(Path directory, String filePrefix, long intervalMillis, int retainFiles) {
        this.directory = directory;
        this.filePrefix = normalizePrefix(filePrefix);
        this.intervalMillis = Math.max(1, intervalMillis);
        this.retainFiles = Math.max(1, retainFiles);
    }

    public Path getDirectory() {
        return directory;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public long getIntervalMillis() {
        return intervalMillis;
    }

    public int getRetainFiles() {
        return retainFiles;
    }

    private static String normalizePrefix(String filePrefix) {
        if (filePrefix == null || filePrefix.trim().isEmpty()) {
            return "safe-output-report";
        }
        return filePrefix.trim();
    }
}

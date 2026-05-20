package com.safeoutput.dashboard.service;

import com.safeoutput.spring.boot.autoconfigure.SafeOutputProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.ObjectProvider;

public class SafeOutputDashboardReportFileStore {

    private final ObjectProvider<SafeOutputProperties> safeOutputProperties;

    public SafeOutputDashboardReportFileStore(ObjectProvider<SafeOutputProperties> safeOutputProperties) {
        this.safeOutputProperties = safeOutputProperties;
    }

    public List<Map<String, Object>> list() throws IOException {
        Path directory = reportDirectory();
        if (!Files.isDirectory(directory)) {
            return new ArrayList<Map<String, Object>>();
        }
        try (Stream<Path> stream = Files.list(directory)) {
            List<Path> paths = stream
                    .filter(path -> path.getFileName().toString().startsWith(filePrefix() + "-"))
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
            List<Map<String, Object>> files = new ArrayList<Map<String, Object>>();
            for (Path path : paths) {
                files.add(fileItem(path));
            }
            return files;
        }
    }

    public Path find(String name) throws IOException {
        if (!isSafeReportName(name)) {
            return null;
        }
        Path directory = reportDirectory();
        Path resolved = directory.resolve(name).normalize();
        if (!resolved.startsWith(directory) || !Files.isRegularFile(resolved)) {
            return null;
        }
        return resolved;
    }

    public String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private Map<String, Object> fileItem(Path path) throws IOException {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("name", path.getFileName().toString());
        item.put("size", Files.size(path));
        item.put("modifiedAt", Files.getLastModifiedTime(path).toMillis());
        item.put("viewable", Boolean.TRUE);
        return item;
    }

    private boolean isSafeReportName(String name) {
        return name != null && name.indexOf('/') < 0 && name.indexOf('\\') < 0
                && name.startsWith(filePrefix() + "-") && name.endsWith(".json");
    }

    private Path reportDirectory() {
        return Paths.get(properties().getReport().getDirectory()).toAbsolutePath().normalize();
    }

    private String filePrefix() {
        return properties().getReport().getFilePrefix();
    }

    private SafeOutputProperties properties() {
        SafeOutputProperties properties = safeOutputProperties.getIfAvailable();
        return properties == null ? new SafeOutputProperties() : properties;
    }
}

package com.safeoutput.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InMemoryLogRuleSuggestionCollector implements LogRuleSuggestionCollector {

    private final Map<String, MutableMetric> metrics = new LinkedHashMap<String, MutableMetric>();

    @Override
    public synchronized void record(LogRuleSuggestionEvent event) {
        try {
            if (event == null || event.getKey() == null || event.getKey().trim().isEmpty()) {
                return;
            }
            String normalizedKey = event.getKey().trim().toLowerCase(Locale.ENGLISH);
            String normalizedType = MaskTypes.normalize(event.getType());
            String key = normalizedKey + ":" + normalizedType;
            MutableMetric metric = metrics.get(key);
            if (metric == null) {
                metric = new MutableMetric(normalizedKey, normalizedType, event.getSeenTimeMillis(),
                        event.getEvidence());
                metrics.put(key, metric);
            }
            metric.record(event.getSeenTimeMillis(), event.getEvidence());
        } catch (RuntimeException ex) {
            // 日志线索采集不能影响日志输出链路。
        }
    }

    @Override
    public synchronized List<LogRuleSuggestionMetric> snapshot() {
        List<LogRuleSuggestionMetric> snapshot = new ArrayList<LogRuleSuggestionMetric>();
        for (MutableMetric metric : metrics.values()) {
            snapshot.add(metric.snapshot());
        }
        return snapshot;
    }

    private static final class MutableMetric {

        private final String key;

        private final String type;

        private long hitCount;

        private final long firstSeenTimeMillis;

        private long lastSeenTimeMillis;

        private String evidence;

        private MutableMetric(String key, String type, long seenTimeMillis, String evidence) {
            this.key = key;
            this.type = type;
            this.firstSeenTimeMillis = seenTimeMillis;
            this.lastSeenTimeMillis = seenTimeMillis;
            this.evidence = evidence;
        }

        private void record(long seenTimeMillis, String value) {
            hitCount++;
            lastSeenTimeMillis = Math.max(lastSeenTimeMillis, seenTimeMillis);
            if (value != null && !value.trim().isEmpty()) {
                evidence = value;
            }
        }

        private LogRuleSuggestionMetric snapshot() {
            return new LogRuleSuggestionMetric(key, type, hitCount, firstSeenTimeMillis, lastSeenTimeMillis, evidence);
        }
    }
}

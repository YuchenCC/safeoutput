package com.safeoutput.report;

import java.util.Collections;
import java.util.List;

public final class PerformanceProfile {

    private final long averageElapsedNanos;

    private final long maxElapsedNanos;

    private final long slowMaskCount;

    private final List<String> warnings;

    PerformanceProfile(long averageElapsedNanos, long maxElapsedNanos, long slowMaskCount, List<String> warnings) {
        this.averageElapsedNanos = averageElapsedNanos;
        this.maxElapsedNanos = maxElapsedNanos;
        this.slowMaskCount = slowMaskCount;
        this.warnings = Collections.unmodifiableList(warnings);
    }

    public long getAverageElapsedNanos() {
        return averageElapsedNanos;
    }

    public long getMaxElapsedNanos() {
        return maxElapsedNanos;
    }

    public long getSlowMaskCount() {
        return slowMaskCount;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}

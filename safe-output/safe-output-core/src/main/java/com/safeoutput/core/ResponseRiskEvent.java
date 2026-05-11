package com.safeoutput.core;

public final class ResponseRiskEvent {

    private final String method;

    private final String path;

    private final boolean ignored;

    private final String ignoreReason;

    public ResponseRiskEvent(String method, String path, boolean ignored, String ignoreReason) {
        this.method = method;
        this.path = path;
        this.ignored = ignored;
        this.ignoreReason = ignoreReason;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public String getIgnoreReason() {
        return ignoreReason;
    }
}

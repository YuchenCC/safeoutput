package com.safeoutput.core;

public interface ResponseRiskRecorder {

    void record(ResponseRiskEvent event);
}

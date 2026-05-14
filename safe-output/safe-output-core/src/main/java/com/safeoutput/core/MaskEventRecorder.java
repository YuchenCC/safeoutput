package com.safeoutput.core;

public interface MaskEventRecorder {

    void recordMask(MaskScene scene, String type, long elapsedNanos);
}

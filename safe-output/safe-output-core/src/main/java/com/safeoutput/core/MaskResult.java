package com.safeoutput.core;

import java.util.Objects;

public final class MaskResult {

    private final MaskContext context;
    private final String value;
    private final boolean masked;

    private MaskResult(MaskContext context, String value, boolean masked) {
        this.context = Objects.requireNonNull(context, "context");
        this.value = value;
        this.masked = masked;
    }

    public static MaskResult unchanged(MaskContext context) {
        return new MaskResult(context, context.getRawValue(), false);
    }

    public static MaskResult masked(MaskContext context, String value) {
        return new MaskResult(context, value, true);
    }

    public MaskContext getContext() {
        return context;
    }

    public String getValue() {
        return value;
    }

    public boolean isMasked() {
        return masked;
    }
}

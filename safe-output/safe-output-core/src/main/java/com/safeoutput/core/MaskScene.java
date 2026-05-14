package com.safeoutput.core;

import java.util.Locale;

public enum MaskScene {

    UNKNOWN("unknown"),
    RESPONSE("response"),
    LOG("log"),
    MANUAL("manual"),
    REPORT("report");

    private final String code;

    MaskScene(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MaskScene fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return UNKNOWN;
        }

        String normalized = code.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ENGLISH);
        for (MaskScene scene : values()) {
            if (scene.name().equals(normalized) || scene.code.equalsIgnoreCase(code.trim())) {
                return scene;
            }
        }
        return UNKNOWN;
    }
}

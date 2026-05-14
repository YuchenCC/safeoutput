package com.safeoutput.core;

import java.util.Locale;

public final class MaskTypes {

    public static final String UNKNOWN = "unknown";
    public static final String MOBILE = "mobile";
    public static final String EMAIL = "email";
    public static final String ID_CARD = "id_card";
    public static final String BANK_CARD = "bank_card";
    public static final String CHINESE_NAME = "chinese_name";
    public static final String ADDRESS = "address";
    public static final String PASSWORD = "password";
    public static final String DEFAULT = "default";

    private MaskTypes() {
    }

    public static String normalize(String type) {
        if (type == null || type.trim().isEmpty()) {
            return UNKNOWN;
        }
        String normalized = type.trim()
                .replace('-', '_')
                .toLowerCase(Locale.ENGLISH);
        if ("phone".equals(normalized)) {
            return MOBILE;
        }
        return normalized;
    }

    public static String from(MaskType type) {
        return type == null ? UNKNOWN : normalize(type.getCode());
    }

    public static boolean isUnknown(String type) {
        return UNKNOWN.equals(normalize(type));
    }

    public static boolean same(String left, String right) {
        return normalize(left).equals(normalize(right));
    }
}

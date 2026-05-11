package com.safeoutput.core;

import java.util.Locale;

public enum MaskType {

    UNKNOWN("unknown"),
    MOBILE("mobile"),
    EMAIL("email"),
    ID_CARD("id_card"),
    BANK_CARD("bank_card"),
    CHINESE_NAME("chinese_name"),
    ADDRESS("address"),
    PASSWORD("password"),
    DEFAULT("default");

    private final String code;

    MaskType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MaskType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return UNKNOWN;
        }

        String normalized = code.trim()
                .replace('-', '_')
                .toUpperCase(Locale.ENGLISH);
        if ("PHONE".equals(normalized)) {
            return MOBILE;
        }
        for (MaskType type : values()) {
            if (type.name().equals(normalized) || type.code.equalsIgnoreCase(code.trim())) {
                return type;
            }
        }
        return UNKNOWN;
    }
}

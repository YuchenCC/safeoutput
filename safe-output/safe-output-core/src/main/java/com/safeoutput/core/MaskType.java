package com.safeoutput.core;

import java.util.Locale;

public enum MaskType {

    UNKNOWN("unknown"),
    MOBILE(MaskTypes.MOBILE),
    EMAIL(MaskTypes.EMAIL),
    ID_CARD(MaskTypes.ID_CARD),
    BANK_CARD(MaskTypes.BANK_CARD),
    CHINESE_NAME(MaskTypes.CHINESE_NAME),
    ADDRESS(MaskTypes.ADDRESS),
    PASSWORD(MaskTypes.PASSWORD),
    DEFAULT(MaskTypes.DEFAULT);

    private final String code;

    MaskType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MaskType fromCode(String code) {
        String normalized = MaskTypes.normalize(code);
        if (MaskTypes.UNKNOWN.equals(normalized)) {
            return UNKNOWN;
        }
        for (MaskType type : values()) {
            if (type.name().equals(normalized.toUpperCase(Locale.ENGLISH))
                    || type.code.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}

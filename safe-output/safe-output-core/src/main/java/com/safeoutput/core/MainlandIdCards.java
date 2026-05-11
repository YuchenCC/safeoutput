package com.safeoutput.core;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.regex.Pattern;

final class MainlandIdCards {

    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[\\dXx]");
    private static final int[] WEIGHTS = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    private static final char[] CHECK_CODES = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private MainlandIdCards() {
    }

    static boolean isValid(String value) {
        if (value == null || !ID_CARD_PATTERN.matcher(value).matches()) {
            return false;
        }
        if (!hasValidBirthDate(value)) {
            return false;
        }
        return hasValidCheckCode(value);
    }

    private static boolean hasValidBirthDate(String value) {
        int year = Integer.parseInt(value.substring(6, 10));
        int month = Integer.parseInt(value.substring(10, 12));
        int day = Integer.parseInt(value.substring(12, 14));
        if (year < 1900 || year > LocalDate.now().getYear()) {
            return false;
        }
        try {
            LocalDate.of(year, month, day);
            return true;
        } catch (DateTimeException ex) {
            return false;
        }
    }

    private static boolean hasValidCheckCode(String value) {
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += Character.digit(value.charAt(i), 10) * WEIGHTS[i];
        }
        char expected = CHECK_CODES[sum % 11];
        return expected == Character.toUpperCase(value.charAt(17));
    }
}

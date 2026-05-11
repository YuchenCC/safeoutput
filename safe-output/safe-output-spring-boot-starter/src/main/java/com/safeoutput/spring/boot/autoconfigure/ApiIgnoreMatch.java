package com.safeoutput.spring.boot.autoconfigure;

final class ApiIgnoreMatch {

    private final String reason;

    ApiIgnoreMatch(String reason) {
        this.reason = reason;
    }

    String getReason() {
        return reason;
    }
}

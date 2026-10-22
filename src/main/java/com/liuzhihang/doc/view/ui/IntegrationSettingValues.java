package com.liuzhihang.doc.view.ui;

final class IntegrationSettingValues {

    private IntegrationSettingValues() {
    }

    static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    static String trimmedOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    static String normalizedUrl(String value) {
        String normalized = trimmedOrEmpty(value);
        while (normalized.endsWith("/") && !normalized.endsWith("://")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

package com.liuzhihang.doc.view.service.http;

/** Immutable text produced from one Java endpoint method. */
public record HttpRequestSnapshot(String text) {
    public HttpRequestSnapshot {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("HTTP request text must not be blank");
        }
    }
}

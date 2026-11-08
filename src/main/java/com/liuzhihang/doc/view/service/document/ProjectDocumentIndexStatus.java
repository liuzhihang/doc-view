package com.liuzhihang.doc.view.service.document;

import org.jetbrains.annotations.Nullable;

/** A read-only view of the current on-disk index and this IDE session's freshness evidence. */
public record ProjectDocumentIndexStatus(IndexState indexState,
                                         Freshness freshness,
                                         @Nullable String snapshotId,
                                         @Nullable Integer entryCount,
                                         @Nullable String generatedAt,
                                         @Nullable Boolean hasUnsavedDocuments,
                                         @Nullable String errorCode) {

    public enum IndexState {
        NOT_GENERATED("not_generated"),
        READY("ready"),
        INVALID("invalid"),
        UNSUPPORTED_SCHEMA("unsupported_version"),
        INCONSISTENT("inconsistent");

        private final String value;

        IndexState(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum Freshness {
        CURRENT("current"),
        STALE("stale"),
        UNKNOWN("unknown");

        private final String value;

        Freshness(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}

package com.liuzhihang.doc.view.service.document;

import java.util.List;
import java.util.Objects;

/** Immutable, PSI-free values stored in the project document snapshot. */
public final class ProjectDocumentData {

    private ProjectDocumentData() {
    }

    public record ApiData(
            String apiId,
            IdentityData identity,
            String name,
            String description,
            String framework,
            String httpMethod,
            String httpPath,
            List<String> tags,
            SourceData source,
            List<HeaderData> headers,
            List<FieldData> requestParameters,
            BodyData requestBody,
            BodyData responseBody,
            ExamplesData examples,
            String remark) {
        public ApiData {
            Objects.requireNonNull(apiId, "apiId");
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(framework, "framework");
            tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
            Objects.requireNonNull(source, "source");
            headers = List.copyOf(Objects.requireNonNull(headers, "headers"));
            requestParameters = List.copyOf(Objects.requireNonNull(requestParameters, "requestParameters"));
            Objects.requireNonNull(examples, "examples");
            Objects.requireNonNull(remark, "remark");
        }
    }

    public record IdentityData(
            String module,
            String qualifiedClass,
            String methodName,
            List<String> parameterTypes,
            String fullSignature,
            String canonicalForm) {
        public IdentityData {
            Objects.requireNonNull(module, "module");
            Objects.requireNonNull(qualifiedClass, "qualifiedClass");
            Objects.requireNonNull(methodName, "methodName");
            parameterTypes = List.copyOf(Objects.requireNonNull(parameterTypes, "parameterTypes"));
            Objects.requireNonNull(fullSignature, "fullSignature");
            Objects.requireNonNull(canonicalForm, "canonicalForm");
        }
    }

    public record SourceData(String path, Integer line, String capturedContentSha256, String basis) {
        public SourceData {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(capturedContentSha256, "capturedContentSha256");
            Objects.requireNonNull(basis, "basis");
        }
    }

    public record FileData(String path, String sha256) {
        public FileData {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(sha256, "sha256");
        }
    }

    public record HeaderData(String name, String description, Boolean required, String example) {
        public HeaderData {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(description, "description");
        }
    }

    public record FieldData(
            String name,
            String type,
            String description,
            Boolean required,
            String example,
            String since,
            String version,
            boolean isCollection,
            boolean isMap,
            List<FieldData> children) {
        public FieldData {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(description, "description");
            children = List.copyOf(Objects.requireNonNull(children, "children"));
        }
    }

    public record BodyData(String type, List<FieldData> fields) {
        public BodyData {
            Objects.requireNonNull(type, "type");
            fields = List.copyOf(Objects.requireNonNull(fields, "fields"));
        }
    }

    public record ExamplesData(String request, String response) {
    }

    public record EntryData(
            String apiId,
            IdentityData identity,
            String name,
            String description,
            String framework,
            String httpMethod,
            String httpPath,
            List<String> tags,
            SourceData source,
            FileData markdown,
            FileData detail) {
        public EntryData {
            Objects.requireNonNull(apiId, "apiId");
            Objects.requireNonNull(identity, "identity");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(framework, "framework");
            tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(markdown, "markdown");
            Objects.requireNonNull(detail, "detail");
        }
    }

    public record IndexData(
            String snapshotId,
            String generatedAt,
            String producerVersion,
            String generatorFingerprint,
            boolean hasUnsavedDocuments,
            FileData markdownIndex,
            List<EntryData> entries) {
        public IndexData {
            Objects.requireNonNull(snapshotId, "snapshotId");
            Objects.requireNonNull(generatedAt, "generatedAt");
            Objects.requireNonNull(producerVersion, "producerVersion");
            Objects.requireNonNull(generatorFingerprint, "generatorFingerprint");
            Objects.requireNonNull(markdownIndex, "markdownIndex");
            entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        }
    }
}

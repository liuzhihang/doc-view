package com.liuzhihang.doc.view.service.document;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIdentity;

import java.io.ByteArrayInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Strict reader and deterministic writer for the project document JSON v1 format. */
public final class ProjectDocumentFormat {

    public static final String OWNER = "com.liuzhihang.doc.view";
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_INDEX_BYTES = 32 * 1024 * 1024;
    public static final int MAX_DOCUMENT_BYTES = 1024 * 1024;
    public static final int MAX_ENTRIES = 10_000;
    public static final int MAX_FIELDS_PER_API = 10_000;
    public static final int MAX_FIELD_DEPTH = 32;

    private static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;
    private static final int MAX_JSON_DEPTH = 80;
    private static final Pattern SHA256 = Pattern.compile("[a-f0-9]{64}");
    private static final Pattern CANONICAL_FORM = Pattern.compile("[A-Za-z0-9_-]{1,65536}");
    private static final Pattern INTEGER = Pattern.compile("-?(0|[1-9][0-9]*)");
    private static final Set<String> FRAMEWORKS = Set.of("spring", "feign", "dubbo", "interface");
    private static final Set<String> HTTP_METHODS = Set.of(
            "GET", "HEAD", "POST", "PUT", "DELETE", "CONNECT", "OPTIONS", "TRACE", "PATCH");
    private static final Runnable NO_CANCELLATION_CHECK = () -> { };

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private ProjectDocumentFormat() {
    }

    public static byte[] writeApi(ProjectDocumentData.ApiData api) {
        Objects.requireNonNull(api, "api");
        JsonElement payload = GSON.toJsonTree(api);
        validateApiPayload(payload, Reason.INVALID);
        return writeEnvelope("api", payload, MAX_DOCUMENT_BYTES);
    }

    public static ProjectDocumentData.ApiData readApi(byte[] bytes) {
        JsonObject envelope = readEnvelope(bytes, "api", MAX_DOCUMENT_BYTES, Reason.INCONSISTENT);
        requireSchemaVersion(envelope, Reason.INCONSISTENT);
        JsonElement payload = envelope.get("payload");
        validateApiPayload(payload, Reason.INCONSISTENT);
        try {
            return GSON.fromJson(payload, ProjectDocumentData.ApiData.class);
        } catch (RuntimeException exception) {
            throw failure(Reason.INCONSISTENT, "API detail could not be decoded", exception);
        }
    }

    public static byte[] writeIndex(ProjectDocumentData.IndexData index) {
        Objects.requireNonNull(index, "index");
        JsonElement payload = GSON.toJsonTree(index);
        validateIndexPayload(payload, Reason.INVALID);
        String expectedSnapshotId = computeSnapshotId(index);
        if (!expectedSnapshotId.equals(index.snapshotId())) {
            throw failure(Reason.INVALID, "Index snapshotId does not match its payload");
        }
        return writeEnvelope("index", payload, MAX_INDEX_BYTES);
    }

    public static ProjectDocumentData.IndexData readIndex(byte[] bytes) {
        return readIndex(bytes, NO_CANCELLATION_CHECK);
    }

    public static ProjectDocumentData.IndexData readIndex(byte[] bytes, Runnable cancellationCheck) {
        Objects.requireNonNull(cancellationCheck, "cancellationCheck");
        checkCancellation(cancellationCheck);
        JsonObject envelope = readEnvelope(bytes, "index", MAX_INDEX_BYTES, Reason.INVALID, cancellationCheck);
        requireSchemaVersion(envelope, Reason.INVALID);
        JsonElement payload = envelope.get("payload");
        validateIndexPayload(payload, Reason.INVALID, cancellationCheck);
        checkCancellation(cancellationCheck);
        ProjectDocumentData.IndexData index;
        try {
            index = GSON.fromJson(payload, ProjectDocumentData.IndexData.class);
        } catch (RuntimeException exception) {
            throw failure(Reason.INVALID, "Index payload could not be decoded", exception);
        }
        checkCancellation(cancellationCheck);
        if (!computeSnapshotId(index, cancellationCheck).equals(index.snapshotId())) {
            throw failure(Reason.INVALID, "Index snapshotId does not match its payload");
        }
        checkCancellation(cancellationCheck);
        return index;
    }

    /** Computes the content generation id, excluding its own id and publication time. */
    public static String computeSnapshotId(ProjectDocumentData.IndexData index) {
        return computeSnapshotId(index, NO_CANCELLATION_CHECK);
    }

    private static String computeSnapshotId(ProjectDocumentData.IndexData index, Runnable cancellationCheck) {
        Objects.requireNonNull(index, "index");
        checkCancellation(cancellationCheck);
        JsonElement tree = GSON.toJsonTree(index);
        checkCancellation(cancellationCheck);
        JsonObject payload = requireObject(tree, Reason.INVALID, "index payload");
        payload.remove("snapshotId");
        payload.remove("generatedAt");
        return sha256(canonicalBytes(payload, MAX_INDEX_BYTES, cancellationCheck), cancellationCheck);
    }

    /** SHA-256 over exact file bytes, returned as lower-case hexadecimal. */
    public static String sha256(byte[] bytes) {
        Objects.requireNonNull(bytes, "bytes");
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    static String sha256(byte[] bytes, Runnable cancellationCheck) {
        Objects.requireNonNull(bytes, "bytes");
        if (cancellationCheck == NO_CANCELLATION_CHECK) {
            return sha256(bytes);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int offset = 0; offset < bytes.length; offset += 8_192) {
                checkCancellation(cancellationCheck);
                digest.update(bytes, offset, Math.min(8_192, bytes.length - offset));
            }
            checkCancellation(cancellationCheck);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public static ProjectDocumentFormatException limitExceeded(String message) {
        return failure(Reason.LIMIT_EXCEEDED, message);
    }

    /**
     * Checks that bytes are a self-consistent Doc View-owned JSON file. Unknown schema versions
     * are recognized from their common envelope and self-hash only; their payload is not parsed.
     */
    public static void validateOwnedJson(byte[] bytes, String kind, ProjectDocumentIdentity expectedIdentity) {
        if (!"api".equals(kind) && !"index".equals(kind)) {
            throw failure(Reason.INCONSISTENT, "Unsupported owned JSON kind");
        }
        JsonObject envelope = readEnvelope(bytes, kind,
                "index".equals(kind) ? MAX_INDEX_BYTES : MAX_DOCUMENT_BYTES, Reason.INCONSISTENT);
        int maximumBytes = "index".equals(kind) ? MAX_INDEX_BYTES : MAX_DOCUMENT_BYTES;
        if (!Arrays.equals(bytes, canonicalFileBytes(envelope, maximumBytes))) {
            throw failure(Reason.INCONSISTENT, "Owned JSON is not in canonical JCS+LF form");
        }
        if (schemaVersion(envelope, Reason.INCONSISTENT) != SCHEMA_VERSION) {
            return;
        }
        if ("api".equals(kind)) {
            ProjectDocumentData.ApiData api = readApi(bytes);
            if (expectedIdentity != null && !expectedIdentity.canonicalForm().equals(api.identity().canonicalForm())) {
                throw failure(Reason.INCONSISTENT, "API detail identity does not match its target");
            }
        } else {
            readIndex(bytes);
        }
    }

    private static byte[] writeEnvelope(String kind, JsonElement payload, int maximumBytes) {
        JsonObject envelope = new JsonObject();
        envelope.addProperty("schemaVersion", SCHEMA_VERSION);
        envelope.addProperty("owner", OWNER);
        envelope.addProperty("kind", kind);
        envelope.add("payload", payload);

        JsonObject integrity = new JsonObject();
        integrity.addProperty("algorithm", "sha256-jcs");
        integrity.addProperty("value", sha256(canonicalBytes(envelope, maximumBytes)));
        envelope.add("integrity", integrity);

        return canonicalFileBytes(envelope, maximumBytes);
    }

    private static byte[] canonicalFileBytes(JsonElement envelope, int maximumBytes) {
        byte[] canonical = canonicalBytes(envelope, maximumBytes - 1);
        byte[] result = new byte[canonical.length + 1];
        System.arraycopy(canonical, 0, result, 0, canonical.length);
        result[result.length - 1] = '\n';
        return result;
    }

    private static JsonObject readEnvelope(byte[] bytes, String expectedKind, int maximumBytes, Reason invalidReason) {
        return readEnvelope(bytes, expectedKind, maximumBytes, invalidReason, NO_CANCELLATION_CHECK);
    }

    private static JsonObject readEnvelope(byte[] bytes,
                                           String expectedKind,
                                           int maximumBytes,
                                           Reason invalidReason,
                                           Runnable cancellationCheck) {
        checkCancellation(cancellationCheck);
        if (bytes == null) {
            throw failure(invalidReason, "JSON document is missing");
        }
        if (bytes.length > maximumBytes) {
            throw failure(Reason.LIMIT_EXCEEDED, "JSON document exceeds its byte limit");
        }
        JsonElement parsed = parseStrictJson(bytes, invalidReason, cancellationCheck);
        checkCancellation(cancellationCheck);
        JsonObject envelope = requireObject(parsed, invalidReason, "envelope");
        requireKeys(envelope, invalidReason, "schemaVersion", "owner", "kind", "payload", "integrity");
        long version = schemaVersion(envelope, invalidReason);
        if (version < 1) {
            throw failure(invalidReason, "Unsupported schema version");
        }
        requireString(envelope, "owner", OWNER, invalidReason);
        requireString(envelope, "kind", expectedKind, invalidReason);
        requireObject(envelope.get("payload"), invalidReason, "payload");

        JsonObject integrity = requireObject(envelope.get("integrity"), invalidReason, "integrity");
        requireKeys(integrity, invalidReason, "algorithm", "value");
        requireString(integrity, "algorithm", "sha256-jcs", invalidReason);
        String actualHash = requireHash(integrity.get("value"), invalidReason, "integrity.value");
        JsonObject unsigned = envelope.deepCopy();
        unsigned.remove("integrity");
        byte[] canonicalUnsigned = canonicalBytes(unsigned, maximumBytes, cancellationCheck);
        if (!sha256(canonicalUnsigned, cancellationCheck).equals(actualHash)) {
            throw failure(Reason.INTEGRITY_MISMATCH, "JSON self-integrity does not match");
        }
        return envelope;
    }

    private static void requireSchemaVersion(JsonObject envelope, Reason reason) {
        if (schemaVersion(envelope, reason) != SCHEMA_VERSION) {
            throw failure(Reason.UNSUPPORTED_SCHEMA, "Unsupported project document schema version");
        }
    }

    private static long schemaVersion(JsonObject envelope, Reason reason) {
        return requireInteger(envelope.get("schemaVersion"), reason, "schemaVersion", 1, MAX_SAFE_INTEGER);
    }

    private static JsonElement parseStrictJson(byte[] bytes, Reason reason, Runnable cancellationCheck) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try (InputStreamReader decoded = new InputStreamReader(new ByteArrayInputStream(bytes), decoder);
             JsonReader reader = new JsonReader(new CancellationAwareReader(decoded, cancellationCheck))) {
            checkCancellation(cancellationCheck);
            reader.setStrictness(Strictness.LEGACY_STRICT);
            JsonElement value = readElement(reader, 0, reason);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw failure(reason, "JSON contains trailing data");
            }
            return value;
        } catch (ProjectDocumentFormatException exception) {
            throw exception;
        } catch (CancellationCheckException exception) {
            throw exception;
        } catch (IOException exception) {
            throw failure(reason, "JSON is not valid UTF-8 or is malformed", exception);
        } catch (IllegalStateException | NumberFormatException exception) {
            throw failure(reason, "JSON is malformed", exception);
        }
    }

    private static JsonElement readElement(JsonReader reader, int depth, Reason reason) throws IOException {
        if (depth > MAX_JSON_DEPTH) {
            throw failure(Reason.LIMIT_EXCEEDED, "JSON nesting exceeds the parser limit");
        }
        JsonToken token = reader.peek();
        return switch (token) {
            case BEGIN_OBJECT -> readObject(reader, depth, reason);
            case BEGIN_ARRAY -> readArray(reader, depth, reason);
            case STRING -> stringPrimitive(reader.nextString(), reason);
            case NUMBER -> numberPrimitive(reader.nextString(), reason);
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> {
                reader.nextNull();
                yield JsonNull.INSTANCE;
            }
            default -> throw failure(reason, "JSON token is not permitted");
        };
    }

    private static JsonObject readObject(JsonReader reader, int depth, Reason reason) throws IOException {
        JsonObject object = new JsonObject();
        Set<String> seen = new HashSet<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            requireWellFormedUnicode(name, reason, "object key");
            if (!seen.add(name)) {
                throw failure(reason, "JSON contains a duplicate object key");
            }
            object.add(name, readElement(reader, depth + 1, reason));
        }
        reader.endObject();
        return object;
    }

    private static JsonArray readArray(JsonReader reader, int depth, Reason reason) throws IOException {
        JsonArray array = new JsonArray();
        reader.beginArray();
        while (reader.hasNext()) {
            array.add(readElement(reader, depth + 1, reason));
        }
        reader.endArray();
        return array;
    }

    private static JsonPrimitive stringPrimitive(String value, Reason reason) {
        requireWellFormedUnicode(value, reason, "string");
        return new JsonPrimitive(value);
    }

    private static JsonPrimitive numberPrimitive(String value, Reason reason) {
        if (!INTEGER.matcher(value).matches()) {
            throw failure(reason, "Only integer JSON numbers are supported");
        }
        try {
            BigInteger number = new BigInteger(value);
            if (number.abs().compareTo(BigInteger.valueOf(MAX_SAFE_INTEGER)) > 0) {
                throw failure(Reason.LIMIT_EXCEEDED, "JSON integer exceeds the safe integer range");
            }
            return new JsonPrimitive(number);
        } catch (NumberFormatException exception) {
            throw failure(reason, "JSON integer is malformed", exception);
        }
    }

    private static byte[] canonicalBytes(JsonElement value, int maximumBytes) {
        return canonicalBytes(value, maximumBytes, NO_CANCELLATION_CHECK);
    }

    private static byte[] canonicalBytes(JsonElement value, int maximumBytes, Runnable cancellationCheck) {
        checkCancellation(cancellationCheck);
        CanonicalBuffer builder = new CanonicalBuffer(maximumBytes, cancellationCheck);
        appendCanonical(value, builder);
        checkCancellation(cancellationCheck);
        byte[] result = builder.toString().getBytes(StandardCharsets.UTF_8);
        if (result.length > maximumBytes) {
            throw failure(Reason.LIMIT_EXCEEDED, "Serialized document exceeds its byte limit");
        }
        return result;
    }

    private static void appendCanonical(JsonElement value, CanonicalBuffer output) {
        if (value == null || value.isJsonNull()) {
            output.append("null");
        } else if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            TreeMap<String, JsonElement> sorted = new TreeMap<>(Comparator.naturalOrder());
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                sorted.put(entry.getKey(), entry.getValue());
            }
            output.append('{');
            boolean first = true;
            for (Map.Entry<String, JsonElement> entry : sorted.entrySet()) {
                if (!first) output.append(',');
                first = false;
                appendString(entry.getKey(), output);
                output.append(':');
                appendCanonical(entry.getValue(), output);
            }
            output.append('}');
        } else if (value.isJsonArray()) {
            output.append('[');
            boolean first = true;
            for (JsonElement element : value.getAsJsonArray()) {
                if (!first) output.append(',');
                first = false;
                appendCanonical(element, output);
            }
            output.append(']');
        } else {
            JsonPrimitive primitive = value.getAsJsonPrimitive();
            if (primitive.isString()) {
                appendString(primitive.getAsString(), output);
            } else if (primitive.isBoolean()) {
                output.append(Boolean.toString(primitive.getAsBoolean()));
            } else if (primitive.isNumber()) {
                String number = primitive.getAsString();
                if (!INTEGER.matcher(number).matches()) {
                    throw failure(Reason.INVALID, "Only integer JSON numbers are supported");
                }
                BigInteger integer = new BigInteger(number);
                if (integer.abs().compareTo(BigInteger.valueOf(MAX_SAFE_INTEGER)) > 0) {
                    throw failure(Reason.LIMIT_EXCEEDED, "JSON integer exceeds the safe integer range");
                }
                output.append(integer.toString());
            } else {
                throw failure(Reason.INVALID, "JSON primitive is not permitted");
            }
        }
    }

    private static void appendString(String value, CanonicalBuffer output) {
        requireWellFormedUnicode(value, Reason.INVALID, "string");
        output.append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\t' -> output.append("\\t");
                case '\n' -> output.append("\\n");
                case '\f' -> output.append("\\f");
                case '\r' -> output.append("\\r");
                default -> {
                    if (character < 0x20) {
                        output.append("\\u00");
                        output.append(Character.forDigit((character >>> 4) & 0xF, 16));
                        output.append(Character.forDigit(character & 0xF, 16));
                    } else {
                        output.append(character);
                    }
                }
            }
        }
        output.append('"');
    }

    private static void validateApiPayload(JsonElement element, Reason reason) {
        JsonObject api = requireObject(element, reason, "api payload");
        requireKeys(api, reason, "apiId", "identity", "name", "description", "framework", "httpMethod",
                "httpPath", "tags", "source", "headers", "requestParameters", "requestBody", "responseBody",
                "examples", "remark");
        Identity identity = validateIdentity(api.get("identity"), api.get("source"), api.get("apiId"), reason);
        validateCommonSummary(api, reason);
        validateSource(api.get("source"), reason);
        JsonArray tags = requireArray(api.get("tags"), reason, "tags");
        if (tags.size() > 1_000) limit(reason, "tags exceed the item limit");
        for (JsonElement tag : tags) requireNonBlankString(tag, reason, "tag", 4096);
        JsonArray headers = requireArray(api.get("headers"), reason, "headers");
        if (headers.size() > MAX_FIELDS_PER_API) limit(reason, "headers exceed the item limit");
        for (JsonElement headerElement : headers) validateHeader(headerElement, reason);
        Counter counter = new Counter();
        validateFieldList(api.get("requestParameters"), reason, counter, 1);
        validateBody(api.get("requestBody"), reason, counter);
        validateBody(api.get("responseBody"), reason, counter);
        validateExamples(api.get("examples"), reason);
        requireString(api, "remark", null, reason, 1_048_576);
        validateIdentityMatches(identity, api.get("source"), reason);
    }

    private static void validateIndexPayload(JsonElement element, Reason reason) {
        validateIndexPayload(element, reason, NO_CANCELLATION_CHECK);
    }

    private static void validateIndexPayload(JsonElement element, Reason reason, Runnable cancellationCheck) {
        checkCancellation(cancellationCheck);
        JsonObject index = requireObject(element, reason, "index payload");
        requireKeys(index, reason, "snapshotId", "generatedAt", "producerVersion", "generatorFingerprint",
                "hasUnsavedDocuments", "markdownIndex", "entries");
        requireHash(index.get("snapshotId"), reason, "snapshotId");
        String generatedAt = requireNonBlankString(index.get("generatedAt"), reason, "generatedAt", 128);
        try {
            if (!generatedAt.endsWith("Z")) throw new DateTimeParseException("UTC timestamp must end with Z", generatedAt, 0);
            Instant.parse(generatedAt);
        } catch (DateTimeParseException exception) {
            throw failure(reason, "generatedAt must be a UTC RFC 3339 timestamp", exception);
        }
        requireNonBlankString(index.get("producerVersion"), reason, "producerVersion", 4096);
        requireHash(index.get("generatorFingerprint"), reason, "generatorFingerprint");
        requireBoolean(index.get("hasUnsavedDocuments"), reason, "hasUnsavedDocuments");
        JsonObject markdownIndex = validateFile(index.get("markdownIndex"), reason, "markdownIndex");
        if (!"index.md".equals(stringValue(markdownIndex.get("path")))) {
            invalid(reason, "markdownIndex path must be index.md");
        }
        JsonArray entries = requireArray(index.get("entries"), reason, "entries");
        if (entries.size() > MAX_ENTRIES) limit(reason, "index entries exceed the item limit");
        Set<String> seenIdentity = new HashSet<>();
        String previousIdentity = null;
        for (JsonElement entryElement : entries) {
            checkCancellation(cancellationCheck);
            EntryIdentity entry = validateEntry(entryElement, reason);
            if (!seenIdentity.add(entry.canonicalForm())) invalid(reason, "index contains duplicate identities");
            if (previousIdentity != null && previousIdentity.compareTo(entry.canonicalForm()) >= 0) {
                invalid(reason, "index entries are not sorted by canonical identity");
            }
            previousIdentity = entry.canonicalForm();
        }
    }

    private static EntryIdentity validateEntry(JsonElement element, Reason reason) {
        JsonObject entry = requireObject(element, reason, "entry");
        requireKeys(entry, reason, "apiId", "identity", "name", "description", "framework", "httpMethod",
                "httpPath", "tags", "source", "markdown", "detail");
        requireHash(entry.get("apiId"), reason, "apiId");
        Identity identity = validateIdentity(entry.get("identity"), entry.get("source"), entry.get("apiId"), reason);
        validateCommonSummary(entry, reason);
        validateSource(entry.get("source"), reason);
        JsonArray tags = requireArray(entry.get("tags"), reason, "tags");
        if (tags.size() > 1_000) limit(reason, "entry tags exceed the item limit");
        for (JsonElement tag : tags) requireNonBlankString(tag, reason, "tag", 4096);
        JsonObject markdown = validateFile(entry.get("markdown"), reason, "markdown");
        JsonObject detail = validateFile(entry.get("detail"), reason, "detail");
        String markdownPath = stringValue(markdown.get("path"));
        String detailPath = stringValue(detail.get("path"));
        String expectedMarkdown = "apis/" + identity.identity().pageFileName();
        String expectedDetail = expectedMarkdown.substring(0, expectedMarkdown.length() - 3) + ".json";
        if (!expectedMarkdown.equals(markdownPath) || !expectedDetail.equals(detailPath)) {
            invalid(reason, "entry file paths do not match its stable identity stem");
        }
        return new EntryIdentity(identity.canonicalForm());
    }

    private static void validateCommonSummary(JsonObject value, Reason reason) {
        requireHash(value.get("apiId"), reason, "apiId");
        requireNonBlankString(value.get("name"), reason, "name", 1_048_576);
        requireString(value, "description", null, reason, 1_048_576);
        String framework = requireNonBlankString(value.get("framework"), reason, "framework", 32);
        if (!FRAMEWORKS.contains(framework)) invalid(reason, "framework is not a supported value");
        JsonElement httpMethod = value.get("httpMethod");
        if (!httpMethod.isJsonNull()) {
            String method = requireNonBlankString(httpMethod, reason, "httpMethod", 16);
            if (!HTTP_METHODS.contains(method)) invalid(reason, "httpMethod is not a supported value");
        }
        JsonElement httpPath = value.get("httpPath");
        if (!httpPath.isJsonNull()) requireStringValue(httpPath, reason, "httpPath", 1_048_576);
    }

    private static Identity validateIdentity(JsonElement identityElement,
                                             JsonElement sourceElement,
                                             JsonElement apiIdElement,
                                             Reason reason) {
        JsonObject identity = requireObject(identityElement, reason, "identity");
        requireKeys(identity, reason, "module", "qualifiedClass", "methodName", "parameterTypes",
                "fullSignature", "canonicalForm");
        String module = requireNonBlankString(identity.get("module"), reason, "identity.module", 4096);
        String qualifiedClass = requireNonBlankString(identity.get("qualifiedClass"), reason, "identity.qualifiedClass", 4096);
        String methodName = requireNonBlankString(identity.get("methodName"), reason, "identity.methodName", 4096);
        JsonArray parameterTypes = requireArray(identity.get("parameterTypes"), reason, "identity.parameterTypes");
        if (parameterTypes.size() > 255) limit(reason, "identity parameter count exceeds the limit");
        List<String> types = new ArrayList<>(parameterTypes.size());
        for (JsonElement parameterType : parameterTypes) {
            types.add(requireNonBlankString(parameterType, reason, "identity parameter type", 4096));
        }
        String fullSignature = requireNonBlankString(identity.get("fullSignature"), reason, "identity.fullSignature", 4096);
        String canonicalForm = requireNonBlankString(identity.get("canonicalForm"), reason, "identity.canonicalForm", 65536);
        if (!CANONICAL_FORM.matcher(canonicalForm).matches()) invalid(reason, "identity.canonicalForm is malformed");
        JsonObject source = requireObject(sourceElement, reason, "source");
        String sourcePath = requireNonBlankString(source.get("path"), reason, "source.path", 4096);
        validateRelativePath(sourcePath, reason, "source.path");
        ProjectDocumentIdentity computed;
        try {
            computed = ProjectDocumentIdentity.of(module, sourcePath, qualifiedClass, methodName, types);
        } catch (RuntimeException exception) {
            throw failure(reason, "identity fields are invalid", exception);
        }
        if (!computed.canonicalForm().equals(canonicalForm)) invalid(reason, "identity canonicalForm does not match its fields");
        if (!computed.displaySignature().equals(fullSignature)) invalid(reason, "identity fullSignature does not match its fields");
        String apiId = requireHash(apiIdElement, reason, "apiId");
        if (!computed.sha256().equals(apiId)) invalid(reason, "apiId does not match identity");
        return new Identity(computed, canonicalForm);
    }

    private static void validateIdentityMatches(Identity identity, JsonElement sourceElement, Reason reason) {
        JsonObject source = requireObject(sourceElement, reason, "source");
        if (!identity.identity().sourcePath().equals(stringValue(source.get("path")))) {
            invalid(reason, "identity source path does not match source metadata");
        }
    }

    private static void validateSource(JsonElement element, Reason reason) {
        JsonObject source = requireObject(element, reason, "source");
        requireKeys(source, reason, "path", "line", "capturedContentSha256", "basis");
        String path = requireNonBlankString(source.get("path"), reason, "source.path", 4096);
        validateRelativePath(path, reason, "source.path");
        JsonElement line = source.get("line");
        if (!line.isJsonNull()) requireInteger(line, reason, "source.line", 1, Integer.MAX_VALUE);
        requireHash(source.get("capturedContentSha256"), reason, "source.capturedContentSha256");
        requireString(source, "basis", "ide-document-text", reason);
    }

    private static JsonObject validateFile(JsonElement element, Reason reason, String field) {
        JsonObject file = requireObject(element, reason, field);
        requireKeys(file, reason, "path", "sha256");
        String path = requireNonBlankString(file.get("path"), reason, field + ".path", 4096);
        validateRelativePath(path, reason, field + ".path");
        requireHash(file.get("sha256"), reason, field + ".sha256");
        return file;
    }

    private static void validateHeader(JsonElement element, Reason reason) {
        JsonObject header = requireObject(element, reason, "header");
        requireKeys(header, reason, "name", "description", "required", "example");
        requireNonBlankString(header.get("name"), reason, "header.name", 4096);
        requireString(header, "description", null, reason, 1_048_576);
        requireNullableBoolean(header.get("required"), reason, "header.required");
        requireNullableString(header.get("example"), reason, "header.example", 1_048_576);
    }

    private static void validateBody(JsonElement element, Reason reason, Counter counter) {
        if (element.isJsonNull()) return;
        JsonObject body = requireObject(element, reason, "body");
        requireKeys(body, reason, "type", "fields");
        requireNonBlankString(body.get("type"), reason, "body.type", 4096);
        validateFieldList(body.get("fields"), reason, counter, 1);
    }

    private static void validateFieldList(JsonElement element, Reason reason, Counter counter, int depth) {
        JsonArray fields = requireArray(element, reason, "fields");
        if (fields.size() > MAX_FIELDS_PER_API) limit(reason, "field list exceeds the item limit");
        if (!fields.isEmpty() && depth > MAX_FIELD_DEPTH) limit(reason, "field depth exceeds the limit");
        for (JsonElement fieldElement : fields) {
            counter.count++;
            if (counter.count > MAX_FIELDS_PER_API) limit(reason, "API field count exceeds the limit");
            JsonObject field = requireObject(fieldElement, reason, "field");
            requireKeys(field, reason, "name", "type", "description", "required", "example", "since", "version",
                    "isCollection", "isMap", "children");
            requireString(field, "name", null, reason, 4096);
            requireNonBlankString(field.get("type"), reason, "field.type", 4096);
            requireString(field, "description", null, reason, 1_048_576);
            requireNullableBoolean(field.get("required"), reason, "field.required");
            requireNullableString(field.get("example"), reason, "field.example", 1_048_576);
            requireNullableString(field.get("since"), reason, "field.since", 1_048_576);
            requireNullableString(field.get("version"), reason, "field.version", 1_048_576);
            requireBoolean(field.get("isCollection"), reason, "field.isCollection");
            requireBoolean(field.get("isMap"), reason, "field.isMap");
            validateFieldList(field.get("children"), reason, counter, depth + 1);
        }
    }

    private static void validateExamples(JsonElement element, Reason reason) {
        JsonObject examples = requireObject(element, reason, "examples");
        requireKeys(examples, reason, "request", "response");
        requireNullableString(examples.get("request"), reason, "examples.request", 1_048_576);
        requireNullableString(examples.get("response"), reason, "examples.response", 1_048_576);
    }

    private static void validateRelativePath(String path, Reason reason, String field) {
        if (path.isEmpty() || path.startsWith("/") || path.endsWith("/") || path.indexOf('\\') >= 0
                || path.indexOf('\0') >= 0 || path.matches("^[A-Za-z]:.*")) {
            invalid(reason, field + " must be a normalized relative path");
        }
        for (String component : path.split("/", -1)) {
            if (component.isEmpty() || component.equals(".") || component.equals("..")) {
                invalid(reason, field + " must not contain empty or traversal components");
            }
        }
    }

    private static void requireKeys(JsonObject object, Reason reason, String... keys) {
        Set<String> expected = Set.of(keys);
        if (object.size() != expected.size() || !object.keySet().equals(expected)) {
            invalid(reason, "JSON object fields do not match the v1 schema");
        }
    }

    private static JsonObject requireObject(JsonElement value, Reason reason, String field) {
        if (value == null || !value.isJsonObject()) invalid(reason, field + " must be an object");
        return value.getAsJsonObject();
    }

    private static JsonArray requireArray(JsonElement value, Reason reason, String field) {
        if (value == null || !value.isJsonArray()) invalid(reason, field + " must be an array");
        return value.getAsJsonArray();
    }

    private static String requireHash(JsonElement value, Reason reason, String field) {
        String hash = requireNonBlankString(value, reason, field, 64);
        if (!SHA256.matcher(hash).matches()) invalid(reason, field + " must be a lower-case SHA-256 value");
        return hash;
    }

    private static String requireNonBlankString(JsonElement value, Reason reason, String field, int maxCodePoints) {
        String text = requireStringValue(value, reason, field, maxCodePoints);
        if (text.isBlank()) invalid(reason, field + " must not be blank");
        return text;
    }

    private static String requireStringValue(JsonElement value, Reason reason, String field, int maxCodePoints) {
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            invalid(reason, field + " must be a string");
        }
        String text = value.getAsString();
        requireWellFormedUnicode(text, reason, field);
        if (text.codePointCount(0, text.length()) > maxCodePoints) limit(reason, field + " exceeds the string length limit");
        return text;
    }

    private static String requireString(JsonObject object, String field, String expected, Reason reason) {
        return requireString(object, field, expected, reason, 4096);
    }

    private static String requireString(JsonObject object, String field, String expected, Reason reason, int maxCodePoints) {
        String value = requireStringValue(object.get(field), reason, field, maxCodePoints);
        if (expected != null && !expected.equals(value)) invalid(reason, field + " has an unexpected value");
        return value;
    }

    private static void requireNullableString(JsonElement value, Reason reason, String field, int maxCodePoints) {
        if (value != null && !value.isJsonNull()) requireStringValue(value, reason, field, maxCodePoints);
    }

    private static void requireNullableBoolean(JsonElement value, Reason reason, String field) {
        if (value != null && !value.isJsonNull()) requireBoolean(value, reason, field);
    }

    private static void requireBoolean(JsonElement value, Reason reason, String field) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            invalid(reason, field + " must be a boolean");
        }
    }

    private static long requireInteger(JsonElement value, Reason reason, String field, long minimum, long maximum) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            invalid(reason, field + " must be an integer");
        }
        String text = value.getAsString();
        if (!INTEGER.matcher(text).matches()) invalid(reason, field + " must be an integer");
        BigInteger integer = new BigInteger(text);
        if (integer.abs().compareTo(BigInteger.valueOf(MAX_SAFE_INTEGER)) > 0) limit(reason, field + " exceeds the safe integer range");
        if (integer.compareTo(BigInteger.valueOf(minimum)) < 0 || integer.compareTo(BigInteger.valueOf(maximum)) > 0) {
            invalid(reason, field + " is out of range");
        }
        return integer.longValue();
    }

    private static String stringValue(JsonElement value) {
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static void requireWellFormedUnicode(String value, Reason reason, String field) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (Character.isHighSurrogate(character)) {
                if (i + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(i + 1))) {
                    invalid(reason, field + " contains invalid Unicode");
                }
                i++;
            } else if (Character.isLowSurrogate(character)) {
                invalid(reason, field + " contains invalid Unicode");
            }
        }
    }

    private static void checkCancellation(Runnable cancellationCheck) {
        if (cancellationCheck == NO_CANCELLATION_CHECK) {
            return;
        }
        try {
            cancellationCheck.run();
        } catch (RuntimeException cancellation) {
            throw new CancellationCheckException(cancellation);
        }
    }

    private static void invalid(Reason reason, String message) {
        throw failure(reason, message);
    }

    private static void limit(Reason reason, String message) {
        throw failure(Reason.LIMIT_EXCEEDED, message);
    }

    private static ProjectDocumentFormatException failure(Reason reason, String message) {
        return new ProjectDocumentFormatException(reason, message);
    }

    private static ProjectDocumentFormatException failure(Reason reason, String message, Throwable cause) {
        return new ProjectDocumentFormatException(reason, message, cause);
    }

    public enum Reason {
        UNSUPPORTED_SCHEMA,
        INVALID,
        INCONSISTENT,
        INTEGRITY_MISMATCH,
        LIMIT_EXCEEDED
    }

    public static final class ProjectDocumentFormatException extends IllegalArgumentException {
        private final Reason reason;

        private ProjectDocumentFormatException(Reason reason, String message) {
            super(message);
            this.reason = reason;
        }

        private ProjectDocumentFormatException(Reason reason, String message, Throwable cause) {
            super(message, cause);
            this.reason = reason;
        }

        public Reason reason() {
            return reason;
        }
    }

    public static final class CancellationCheckException extends RuntimeException {
        private CancellationCheckException(RuntimeException cause) {
            super(cause);
        }
    }

    private record Identity(ProjectDocumentIdentity identity, String canonicalForm) {
    }

    private record EntryIdentity(String canonicalForm) {
    }

    private static final class Counter {
        private int count;
    }

    private static final class CanonicalBuffer {
        private final StringBuilder value;
        private final int maximumChars;
        private final Runnable cancellationCheck;
        private int charsUntilCancellationCheck = 8_192;

        private CanonicalBuffer(int maximumChars, Runnable cancellationCheck) {
            this.maximumChars = maximumChars;
            this.cancellationCheck = cancellationCheck;
            this.value = new StringBuilder(Math.min(maximumChars, 4096));
        }

        private CanonicalBuffer append(char character) {
            ensureCapacity(1);
            value.append(character);
            if (cancellationCheck != NO_CANCELLATION_CHECK && --charsUntilCancellationCheck == 0) {
                checkCancellation(cancellationCheck);
                charsUntilCancellationCheck = 8_192;
            }
            return this;
        }

        private CanonicalBuffer append(String text) {
            ensureCapacity(text.length());
            if (cancellationCheck == NO_CANCELLATION_CHECK) {
                value.append(text);
                return this;
            }
            int offset = 0;
            while (offset < text.length()) {
                int count = Math.min(charsUntilCancellationCheck, text.length() - offset);
                value.append(text, offset, offset + count);
                offset += count;
                charsUntilCancellationCheck -= count;
                if (charsUntilCancellationCheck == 0) {
                    checkCancellation(cancellationCheck);
                    charsUntilCancellationCheck = 8_192;
                }
            }
            return this;
        }

        private void ensureCapacity(int additionalChars) {
            if (additionalChars < 0 || additionalChars > maximumChars - value.length()) {
                throw failure(Reason.LIMIT_EXCEEDED, "Serialized document exceeds its byte limit");
            }
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    private static final class CancellationAwareReader extends FilterReader {
        private final Runnable cancellationCheck;

        private CancellationAwareReader(Reader delegate, Runnable cancellationCheck) {
            super(delegate);
            this.cancellationCheck = cancellationCheck;
        }

        @Override
        public int read(char[] buffer, int offset, int length) throws IOException {
            checkCancellation(cancellationCheck);
            return super.read(buffer, offset, length);
        }

        @Override
        public int read() throws IOException {
            checkCancellation(cancellationCheck);
            return super.read();
        }
    }
}

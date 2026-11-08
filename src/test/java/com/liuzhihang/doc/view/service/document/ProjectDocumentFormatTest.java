package com.liuzhihang.doc.view.service.document;

import com.liuzhihang.doc.view.service.export.ProjectDocumentIdentity;
import junit.framework.TestCase;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProjectDocumentFormatTest extends TestCase {

    private static final Path EXAMPLES = Path.of("openspec/changes/project-document-store/examples");
    private static final String API_FILE = "apis/GreetingController-greet-b57b3f3f36aeb624a3bdc7d4fbbef8e0ae7f8278da113e0f313eefb884ef28b4.json";

    public void testCrossLanguageApiAndIndexGoldensRoundTripByteForByte() throws Exception {
        byte[] apiBytes = Files.readAllBytes(EXAMPLES.resolve(API_FILE));
        byte[] indexBytes = Files.readAllBytes(EXAMPLES.resolve("index.json"));

        assertTrue(java.util.Arrays.equals(apiBytes, ProjectDocumentFormat.writeApi(ProjectDocumentFormat.readApi(apiBytes))));
        assertTrue(java.util.Arrays.equals(indexBytes, ProjectDocumentFormat.writeIndex(ProjectDocumentFormat.readIndex(indexBytes))));
    }

    public void testIndexSnapshotIdExcludesPublicationTimeAndItsOwnId() {
        ProjectDocumentData.ApiData api = ProjectDocumentFormat.readApi(readExampleApi());
        ProjectDocumentData.EntryData entry = entry(api);
        ProjectDocumentData.IndexData first = index(entry, "2026-10-24T00:00:00Z", "0".repeat(64));
        ProjectDocumentData.IndexData second = index(entry, "2026-10-25T12:30:00Z", "1".repeat(64));

        assertEquals(ProjectDocumentFormat.computeSnapshotId(first), ProjectDocumentFormat.computeSnapshotId(second));
        ProjectDocumentData.IndexData committed = index(entry, first.generatedAt(), ProjectDocumentFormat.computeSnapshotId(first));
        assertEquals(committed, ProjectDocumentFormat.readIndex(ProjectDocumentFormat.writeIndex(committed)));
    }

    public void testDuplicateKeysAndTamperedSelfHashAreRejected() throws Exception {
        byte[] sample = readExampleApi();
        String json = new String(sample, StandardCharsets.UTF_8);
        byte[] duplicate = json.replaceFirst("\\\"kind\\\":\\\"api\\\"", "\\\"kind\\\":\\\"api\\\",\\\"kind\\\":\\\"api\\\"")
                .getBytes(StandardCharsets.UTF_8);
        assertReason(ProjectDocumentFormat.Reason.INCONSISTENT, () -> ProjectDocumentFormat.readApi(duplicate));

        byte[] tampered = json.replace("查询问候", "已修改") .getBytes(StandardCharsets.UTF_8);
        assertReason(ProjectDocumentFormat.Reason.INTEGRITY_MISMATCH, () -> ProjectDocumentFormat.readApi(tampered));
    }

    public void testStrictJsonRejectsComments() throws Exception {
        String json = new String(readExampleApi(), StandardCharsets.UTF_8);
        byte[] commented = json.replaceFirst("\\{", "{/* comment */").getBytes(StandardCharsets.UTF_8);

        assertReason(ProjectDocumentFormat.Reason.INCONSISTENT, () -> ProjectDocumentFormat.readApi(commented));
    }

    public void testIndexReadChecksCancellationWhileConsumingLargeStrings() {
        byte[] bytes = indexWithDescription("x".repeat(5_000));
        AtomicInteger checks = new AtomicInteger();
        RuntimeException cancellation = new RuntimeException("cancel fixture");
        try {
            ProjectDocumentFormat.readIndex(bytes, () -> {
                if (checks.incrementAndGet() == 5) {
                    throw cancellation;
                }
            });
            fail("Index parsing must poll the caller's cancellation check while reading JSON");
        } catch (ProjectDocumentFormat.CancellationCheckException exception) {
            assertSame(cancellation, exception.getCause());
            assertEquals("The cancellation must be observed at the deterministic fifth poll", 5, checks.get());
        }
    }

    public void testIndexReadChecksCancellationWhileRecomputingIntegrity() {
        byte[] bytes = indexWithDescription("x".repeat(12_000));
        AtomicBoolean cancelledInCanonicalization = new AtomicBoolean();
        RuntimeException cancellation = new RuntimeException("cancel during canonicalization");
        try {
            ProjectDocumentFormat.readIndex(bytes, () -> {
                boolean canonicalizing = Arrays.stream(Thread.currentThread().getStackTrace())
                        .anyMatch(frame -> frame.getClassName().equals(ProjectDocumentFormat.class.getName())
                                && frame.getMethodName().equals("canonicalBytes"));
                if (canonicalizing && cancelledInCanonicalization.compareAndSet(false, true)) {
                    throw cancellation;
                }
            });
            fail("Index integrity recomputation must use the query cancellation check");
        } catch (ProjectDocumentFormat.CancellationCheckException exception) {
            assertSame(cancellation, exception.getCause());
        }
        assertTrue("The cancellation must happen after JSON parsing enters canonical integrity validation",
                cancelledInCanonicalization.get());
    }

    public void testIndexReadChecksCancellationWhileValidatingEntries() {
        byte[] bytes = indexWithDescription("x".repeat(12_000));
        RuntimeException cancellation = new RuntimeException("cancel during payload validation");
        try {
            ProjectDocumentFormat.readIndex(bytes, () -> {
                boolean validating = Arrays.stream(Thread.currentThread().getStackTrace())
                        .anyMatch(frame -> frame.getClassName().equals(ProjectDocumentFormat.class.getName())
                                && frame.getMethodName().equals("validateIndexPayload"));
                if (validating) throw cancellation;
            });
            fail("Index entry validation must observe cancellation before decoding the DTO");
        } catch (ProjectDocumentFormat.CancellationCheckException exception) {
            assertSame(cancellation, exception.getCause());
        }
    }

    public void testApiOwnershipRequiresExpectedIdentity() throws Exception {
        byte[] sample = readExampleApi();
        ProjectDocumentIdentity expected = ProjectDocumentIdentity.of("demo",
                "demo/src/main/java/example/GreetingController.java", "example.GreetingController", "greet",
                List.of("java.lang.String"));
        ProjectDocumentFormat.validateOwnedJson(sample, "api", expected);

        ProjectDocumentIdentity wrong = ProjectDocumentIdentity.of("demo",
                "demo/src/main/java/example/GreetingController.java", "example.GreetingController", "other",
                List.of("java.lang.String"));
        assertReason(ProjectDocumentFormat.Reason.INCONSISTENT,
                () -> ProjectDocumentFormat.validateOwnedJson(sample, "api", wrong));

        String json = new String(sample, StandardCharsets.UTF_8);
        byte[] reformatted = json.replaceFirst("\\{", "{ ").getBytes(StandardCharsets.UTF_8);
        assertNotNull("Readers accept legal whitespace", ProjectDocumentFormat.readApi(reformatted));
        assertReason(ProjectDocumentFormat.Reason.INCONSISTENT,
                () -> ProjectDocumentFormat.validateOwnedJson(reformatted, "api", expected));
    }

    public void testFutureSchemaOwnershipChecksCommonEnvelopeWithoutParsingPayload() {
        String unsignedEnvelope = "{\"kind\":\"api\",\"owner\":\""
                + ProjectDocumentFormat.OWNER + "\",\"payload\":{},\"schemaVersion\":2}";
        String integrityHash = ProjectDocumentFormat.sha256(unsignedEnvelope.getBytes(StandardCharsets.UTF_8));
        String futureJson = "{\"integrity\":{\"algorithm\":\"sha256-jcs\",\"value\":\""
                + integrityHash + "\"},\"kind\":\"api\",\"owner\":\""
                + ProjectDocumentFormat.OWNER + "\",\"payload\":{},\"schemaVersion\":2}\n";
        ProjectDocumentIdentity expected = ProjectDocumentIdentity.of("demo", "src/Api.java", "example.Api",
                "method", List.of());

        ProjectDocumentFormat.validateOwnedJson(futureJson.getBytes(StandardCharsets.UTF_8), "api", expected);
        assertReason(ProjectDocumentFormat.Reason.UNSUPPORTED_SCHEMA,
                () -> ProjectDocumentFormat.readApi(futureJson.getBytes(StandardCharsets.UTF_8)));
    }

    public void testSerializedDetailLimitIsCheckedBeforeBuildingAnUnboundedJsonString() {
        ProjectDocumentData.ApiData api = ProjectDocumentFormat.readApi(readExampleApi());
        ProjectDocumentData.ApiData oversized = new ProjectDocumentData.ApiData(api.apiId(), api.identity(),
                "x".repeat(ProjectDocumentFormat.MAX_DOCUMENT_BYTES), api.description(), api.framework(),
                api.httpMethod(), api.httpPath(), api.tags(), api.source(), api.headers(), api.requestParameters(),
                api.requestBody(), api.responseBody(), api.examples(), api.remark());

        assertReason(ProjectDocumentFormat.Reason.LIMIT_EXCEEDED, () -> ProjectDocumentFormat.writeApi(oversized));
    }

    public void testUnsafeSourceAndMismatchedStableStemAreRejectedBeforeWriting() {
        ProjectDocumentData.ApiData api = ProjectDocumentFormat.readApi(readExampleApi());
        ProjectDocumentData.SourceData unsafeSource = new ProjectDocumentData.SourceData("../outside/Api.java", 1,
                api.source().capturedContentSha256(), "ide-document-text");
        ProjectDocumentData.ApiData unsafeApi = new ProjectDocumentData.ApiData(api.apiId(), api.identity(), api.name(),
                api.description(), api.framework(), api.httpMethod(), api.httpPath(), api.tags(), unsafeSource,
                api.headers(), api.requestParameters(), api.requestBody(), api.responseBody(), api.examples(), api.remark());
        assertReason(ProjectDocumentFormat.Reason.INVALID, () -> ProjectDocumentFormat.writeApi(unsafeApi));

        ProjectDocumentData.EntryData entry = entry(api);
        ProjectDocumentData.EntryData wrongStem = new ProjectDocumentData.EntryData(entry.apiId(), entry.identity(),
                entry.name(), entry.description(), entry.framework(), entry.httpMethod(), entry.httpPath(), entry.tags(),
                entry.source(), new ProjectDocumentData.FileData("apis/other.md", entry.markdown().sha256()), entry.detail());
        ProjectDocumentData.IndexData draft = index(wrongStem, "2026-10-24T00:00:00Z", "0".repeat(64));
        ProjectDocumentData.IndexData inconsistent = index(wrongStem, draft.generatedAt(), ProjectDocumentFormat.computeSnapshotId(draft));
        assertReason(ProjectDocumentFormat.Reason.INVALID, () -> ProjectDocumentFormat.writeIndex(inconsistent));
    }

    public void testInterfaceFrameworkIsRepresentedAsItsOwnCanonicalValue() {
        ProjectDocumentData.ApiData api = ProjectDocumentFormat.readApi(readExampleApi());
        ProjectDocumentData.ApiData interfaceApi = new ProjectDocumentData.ApiData(api.apiId(), api.identity(), api.name(),
                api.description(), "interface", null, null, api.tags(), api.source(), api.headers(), api.requestParameters(),
                api.requestBody(), api.responseBody(), api.examples(), api.remark());
        assertEquals("interface", ProjectDocumentFormat.readApi(ProjectDocumentFormat.writeApi(interfaceApi)).framework());
    }

    private static byte[] readExampleApi() {
        try {
            return Files.readAllBytes(EXAMPLES.resolve(API_FILE));
        } catch (Exception exception) {
            throw new AssertionError("Could not read the committed fictional API example", exception);
        }
    }

    private static byte[] indexWithDescription(String description) {
        ProjectDocumentData.ApiData api = ProjectDocumentFormat.readApi(readExampleApi());
        ProjectDocumentData.EntryData original = entry(api);
        ProjectDocumentData.EntryData updated = new ProjectDocumentData.EntryData(
                original.apiId(), original.identity(), original.name(), description, original.framework(),
                original.httpMethod(), original.httpPath(), original.tags(), original.source(),
                original.markdown(), original.detail());
        ProjectDocumentData.IndexData draft = index(updated, "2026-11-07T00:00:00Z", "0".repeat(64));
        ProjectDocumentData.IndexData committed = index(updated, draft.generatedAt(),
                ProjectDocumentFormat.computeSnapshotId(draft));
        return ProjectDocumentFormat.writeIndex(committed);
    }

    private static ProjectDocumentData.EntryData entry(ProjectDocumentData.ApiData api) {
        String stem = api.identity().qualifiedClass().substring(api.identity().qualifiedClass().lastIndexOf('.') + 1)
                + "-" + api.identity().methodName() + "-" + api.apiId();
        byte[] markdown = "# API\n".getBytes(StandardCharsets.UTF_8);
        byte[] detail = ProjectDocumentFormat.writeApi(api);
        return new ProjectDocumentData.EntryData(api.apiId(), api.identity(), api.name(), api.description(), api.framework(),
                api.httpMethod(), api.httpPath(), api.tags(), api.source(),
                new ProjectDocumentData.FileData("apis/" + stem + ".md", ProjectDocumentFormat.sha256(markdown)),
                new ProjectDocumentData.FileData("apis/" + stem + ".json", ProjectDocumentFormat.sha256(detail)));
    }

    private static ProjectDocumentData.IndexData index(ProjectDocumentData.EntryData entry, String generatedAt, String snapshotId) {
        byte[] markdownIndex = "# Index\n".getBytes(StandardCharsets.UTF_8);
        return new ProjectDocumentData.IndexData(snapshotId, generatedAt, "1.4.0", "a".repeat(64), false,
                new ProjectDocumentData.FileData("index.md", ProjectDocumentFormat.sha256(markdownIndex)), List.of(entry));
    }

    private static void assertReason(ProjectDocumentFormat.Reason reason, Runnable operation) {
        try {
            operation.run();
            fail("Expected format failure " + reason);
        } catch (ProjectDocumentFormat.ProjectDocumentFormatException exception) {
            assertEquals(reason, exception.reason());
        }
    }
}

package com.liuzhihang.doc.view.service.mcp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.testFramework.DumbModeTestUtils;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.ApiData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.EntryData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.ExamplesData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.FileData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.IdentityData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.IndexData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.SourceData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentFormat;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIdentity;
import com.liuzhihang.doc.view.testsupport.LocalProjectJavaSources;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public final class DocViewMcpToolContractTest extends LightJavaCodeInsightFixtureTestCase {
    private Path projectRoot;
    private Path documentRoot;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Path basePath = Path.of(getProject().getBasePath()).toAbsolutePath().normalize();
        Files.createDirectories(basePath);
        projectRoot = basePath.toRealPath();
        documentRoot = projectRoot.resolve("docs/doc-view");
        deleteTree(documentRoot);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            deleteTree(documentRoot);
        } finally {
            super.tearDown();
        }
    }

    public void testSchemasAndExamplesUseOneStrictInputContract() throws Exception {
        Path schemaFile = Path.of("src/main/resources/mcp/tool-contracts.json");
        Path proposalSchema = Path.of("openspec/changes/doc-view-mcp-tools/tool-contracts.json");
        assertTrue("The packaged contract schema must be present", Files.isRegularFile(schemaFile));
        assertEquals("Packaged MCP schema must be an exact copy of the OpenSpec contract",
                Files.readString(proposalSchema), Files.readString(schemaFile));
        JsonObject resource = JsonParser.parseString(Files.readString(schemaFile)).getAsJsonObject();
        assertEquals(1, resource.get("contractVersion").getAsInt());
        assertEquals(4, resource.getAsJsonArray("tools").size());

        JsonObject status = object("projectPath", projectRoot.toString());
        DocViewMcpExecutor.validateArguments(DocViewMcpExecutor.STATUS, status);
        DocViewMcpExecutor.validateArguments(DocViewMcpExecutor.SEARCH,
                object("projectPath", projectRoot.toString(), "query", ""));
        assertInvalid(() -> DocViewMcpExecutor.validateArguments(DocViewMcpExecutor.STATUS,
                object("projectPath", projectRoot.toString(), "arbitrary", true)));
        assertInvalid(() -> DocViewMcpExecutor.validateArguments(DocViewMcpExecutor.SEARCH,
                object("projectPath", projectRoot.toString(), "query", "ok", "limit", 101)));
        assertInvalid(() -> DocViewMcpExecutor.validateArguments(DocViewMcpExecutor.SEARCH,
                object("projectPath", projectRoot.toString(), "query", "x".repeat(257))));
        assertInvalid(() -> DocViewMcpExecutor.validateArguments(DocViewMcpExecutor.SEARCH,
                object("projectPath", projectRoot.toString(), "query", "one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen sixteen seventeen")));
        assertInvalid(() -> DocViewMcpExecutor.validateArguments(DocViewMcpExecutor.GET,
                object("projectPath", projectRoot.toString(), "apiId", "not-a-hash", "snapshotId", "0".repeat(64))));
        assertInvalid(() -> DocViewMcpExecutor.validateArguments(DocViewMcpExecutor.SEARCH,
                object("projectPath", projectRoot.toString(), "query", "greeting", "cursor", "")));
        assertInvalid(() -> DocViewMcpExecutor.validateArguments("doc_view_unknown",
                object("projectPath", projectRoot.toString())));

        JsonObject invalidArguments = DocViewMcpExecutor.error(null, "INVALID_ARGUMENT");
        assertEquals("INVALID_ARGUMENT", invalidArguments.getAsJsonObject("error").get("code").getAsString());
        assertTrue("Unresolved project routing must not echo malformed projectPath",
                invalidArguments.get("projectPath").isJsonNull());
    }

    public void testStatusAndSearchOnMissingStoreAreReadOnlyAndVersioned() throws Exception {
        DocViewMcpExecutor executor = new DocViewMcpExecutor(() -> false);
        JsonObject status = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.STATUS,
                object("projectPath", projectRoot.toString()), new EmptyProgressIndicator()));
        assertEquals(1, status.get("schemaVersion").getAsInt());
        assertEquals("ok", status.get("status").getAsString());
        assertEquals("unknown", status.get("freshness").getAsString());
        assertTrue(status.get("hasUnsavedDocuments").isJsonNull());
        assertEquals("not_generated", status.getAsJsonObject("data").get("indexState").getAsString());
        assertEquals(0, status.getAsJsonObject("data").get("entryCount").getAsInt());
        assertFalse(status.getAsJsonObject("data").get("generating").getAsBoolean());
        assertEquals(1, status.getAsJsonObject("data").get("toolContractVersion").getAsInt());
        java.util.Properties buildProperties = new java.util.Properties();
        try (var reader = Files.newBufferedReader(Path.of("gradle.properties"))) {
            buildProperties.load(reader);
        }
        String expectedPluginVersion = buildProperties.getProperty("pluginVersion");
        assertNotNull("Build must declare the plugin version", expectedPluginVersion);
        assertEquals(expectedPluginVersion, status.getAsJsonObject("data").get("pluginVersion").getAsString());
        assertFalse("Status must not create the document library", Files.exists(documentRoot));

        JsonObject search = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.SEARCH,
                object("projectPath", projectRoot.toString(), "query", "missing"), new EmptyProgressIndicator()));
        assertError(search, "INDEX_NOT_FOUND");
        assertFalse("A missing-store search must not create files", Files.exists(documentRoot));
    }

    public void testSearchGetAndOptionalMarkdownUseTheSameSnapshot() throws Exception {
        IndexData index = writeStore();
        Map<String, String> before = fileHashes(documentRoot);
        DocViewMcpExecutor executor = new DocViewMcpExecutor(() -> false);

        JsonObject search = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.SEARCH,
                object("projectPath", projectRoot.toString(), "query", "greeting", "limit", 1),
                new EmptyProgressIndicator()));
        assertEquals("ok", search.get("status").getAsString());
        assertEquals(index.snapshotId(), search.get("snapshotId").getAsString());
        JsonObject item = search.getAsJsonObject("data").getAsJsonArray("items").get(0).getAsJsonObject();
        assertEquals("demo", item.getAsJsonObject("identity").get("module").getAsString());
        assertEquals("greet(java.lang.String)", item.getAsJsonObject("identity").get("fullSignature").getAsString());

        JsonObject detail = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.GET,
                object("projectPath", projectRoot.toString(), "apiId", index.entries().get(0).apiId(),
                        "snapshotId", index.snapshotId()), new EmptyProgressIndicator()));
        assertEquals("ok", detail.get("status").getAsString());
        assertEquals(index.snapshotId(), detail.get("snapshotId").getAsString());
        assertEquals("Greeting detail", detail.getAsJsonObject("data").getAsJsonObject("api")
                .get("description").getAsString());
        assertFalse("Markdown is omitted unless requested", detail.getAsJsonObject("data").has("markdown"));

        JsonObject withMarkdown = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.GET,
                object("projectPath", projectRoot.toString(), "apiId", index.entries().get(0).apiId(),
                        "snapshotId", index.snapshotId(), "includeMarkdown", true), new EmptyProgressIndicator()));
        assertEquals("# Greeting API\n\nGreeting detail\n",
                withMarkdown.getAsJsonObject("data").get("markdown").getAsString());
        assertEquals("Read-only MCP queries must preserve every document byte", before, fileHashes(documentRoot));
    }

    public void testWrongSnapshotAndUnknownProjectFailWithoutChangingStore() throws Exception {
        IndexData index = writeStore();
        Map<String, String> before = fileHashes(documentRoot);
        DocViewMcpExecutor executor = new DocViewMcpExecutor(() -> false);
        JsonObject wrongSnapshot = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.GET,
                object("projectPath", projectRoot.toString(), "apiId", index.entries().get(0).apiId(),
                        "snapshotId", "0".repeat(64)), new EmptyProgressIndicator()));
        assertError(wrongSnapshot, "SNAPSHOT_CHANGED");

        JsonObject wrongProject = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.STATUS,
                object("projectPath", projectRoot.resolve("child").toString()), new EmptyProgressIndicator()));
        assertError(wrongProject, "PROJECT_NOT_OPEN");
        assertEquals("Failed reads must preserve every document byte", before, fileHashes(documentRoot));
        assertEquals(index.snapshotId(), JsonParser.parseString(Files.readString(documentRoot.resolve("index.json")))
                .getAsJsonObject().getAsJsonObject("payload").get("snapshotId").getAsString());
    }

    public void testGenerateUsesSharedWriterAndReportsInterfaceCounts() throws Exception {
        LocalProjectJavaSources sources = new LocalProjectJavaSources(getProject(), getModule());
        try {
            sources.installApiAnnotationStubs();
            sources.addClass("demo/GreetingController.java", """
                    package demo;

                    @org.springframework.web.bind.annotation.RestController
                    public class GreetingController {
                        @org.springframework.web.bind.annotation.GetMapping("/greeting")
                        public String greet(@org.springframework.web.bind.annotation.RequestParam("name") String name) {
                            return name;
                        }
                    }
                    """);
            IndexingTestUtil.waitUntilIndexesAreReady(getProject());
            DocViewMcpExecutor executor = new DocViewMcpExecutor(() -> false);
            JsonObject arguments = object("projectPath", projectRoot.toString());

            JsonObject first = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.GENERATE,
                    arguments, new EmptyProgressIndicator()));
            assertEquals("ok", first.get("status").getAsString());
            JsonObject firstData = first.getAsJsonObject("data");
            assertEquals("generated", firstData.get("outcome").getAsString());
            assertEquals(1, firstData.get("scanned").getAsInt());
            assertEquals(1, firstData.get("created").getAsInt());
            assertEquals(0, firstData.get("updated").getAsInt());
            assertEquals(0, firstData.get("unchanged").getAsInt());
            assertTrue(Files.isRegularFile(documentRoot.resolve("index.json")));
            assertTrue(firstData.get("publishedPaths").getAsJsonArray().size() >= 4);

            JsonObject second = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.GENERATE,
                    arguments, new EmptyProgressIndicator()));
            assertEquals("ok", second.get("status").getAsString());
            JsonObject secondData = second.getAsJsonObject("data");
            assertEquals("unchanged", secondData.get("outcome").getAsString());
            assertEquals(0, secondData.get("created").getAsInt());
            assertEquals(0, secondData.get("updated").getAsInt());
            assertEquals(1, secondData.get("unchanged").getAsInt());
            assertEquals(0, secondData.get("publishedPaths").getAsJsonArray().size());
        } finally {
            sources.cleanup();
        }
    }

    public void testInvalidAndFutureSchemasRemainReadOnlyAndDoNotLeakIndexContent() throws Exception {
        Files.createDirectories(documentRoot);
        Path indexPath = documentRoot.resolve("index.json");
        String privateMarker = "private-index-content-should-not-appear-in-errors";
        Files.writeString(indexPath, "{\"secret\":\"" + privateMarker + "\",broken\n", StandardCharsets.UTF_8);
        DocViewMcpExecutor executor = new DocViewMcpExecutor(() -> false);
        Map<String, String> malformedBefore = fileHashes(documentRoot);

        JsonObject malformedStatus = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.STATUS,
                object("projectPath", projectRoot.toString()), new EmptyProgressIndicator()));
        assertEquals("ok", malformedStatus.get("status").getAsString());
        assertEquals("invalid", malformedStatus.getAsJsonObject("data").get("indexState").getAsString());
        assertTrue(malformedStatus.getAsJsonObject("data").get("entryCount").isJsonNull());
        JsonObject malformedSearch = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.SEARCH,
                object("projectPath", projectRoot.toString(), "query", ""), new EmptyProgressIndicator()));
        assertError(malformedSearch, "INDEX_INVALID");
        assertFalse(malformedSearch.getAsJsonObject("error").toString().contains(privateMarker));
        assertFalse(malformedSearch.getAsJsonObject("error").toString().contains(projectRoot.toString()));
        assertEquals("Malformed-index reads must not rewrite any document bytes",
                malformedBefore, fileHashes(documentRoot));

        Files.write(indexPath, futureSchemaIndex());
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(documentRoot);
        Map<String, String> futureBefore = fileHashes(documentRoot);
        JsonObject futureStatus = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.STATUS,
                object("projectPath", projectRoot.toString()), new EmptyProgressIndicator()));
        assertEquals("unsupported_version", futureStatus.getAsJsonObject("data").get("indexState").getAsString());
        assertTrue(futureStatus.getAsJsonObject("data").get("entryCount").isJsonNull());
        JsonObject futureSearch = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.SEARCH,
                object("projectPath", projectRoot.toString(), "query", ""), new EmptyProgressIndicator()));
        assertError(futureSearch, "UNSUPPORTED_SCHEMA");
        assertEquals("Future-schema reads must not rewrite any document bytes", futureBefore,
                fileHashes(documentRoot));
    }

    public void testDumbModeReadsCommittedSnapshotButRejectsGenerationWithoutWrites() throws Exception {
        writeStore();
        Map<String, String> before = fileHashes(documentRoot);
        DocViewMcpExecutor executor = new DocViewMcpExecutor(() -> false);

        DumbModeTestUtils.runInDumbModeSynchronously(getProject(), () -> {
            assertTrue(com.intellij.openapi.project.DumbService.getInstance(getProject()).isDumb());
            try {
                JsonObject search = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.SEARCH,
                        object("projectPath", projectRoot.toString(), "query", "greeting"),
                        new EmptyProgressIndicator()));
                assertEquals("ok", search.get("status").getAsString());
                assertEquals(1, search.getAsJsonObject("data").getAsJsonArray("items").size());

                JsonObject generate = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.GENERATE,
                        object("projectPath", projectRoot.toString()), new EmptyProgressIndicator()));
                assertError(generate, "INDEXING");
            } catch (Exception failure) {
                throw new AssertionError("MCP should keep committed reads available during Dumb Mode", failure);
            }
        });

        assertEquals("Dumb-mode reads and rejected generation must preserve the library bytes",
                before, fileHashes(documentRoot));
    }

    public void testOversizedSearchEnvelopeReturnsSafeErrorWithoutTruncation() throws Exception {
        writeOversizedSearchStore();
        Map<String, String> before = fileHashes(documentRoot);
        DocViewMcpExecutor executor = new DocViewMcpExecutor(() -> false);
        JsonObject result = runInBackground(() -> executor.execute(getProject(), DocViewMcpExecutor.SEARCH,
                object("projectPath", projectRoot.toString(), "query", ""), new EmptyProgressIndicator()));

        assertError(result, "RESULT_TOO_LARGE");
        assertFalse(result.toString().contains("private-search-content-marker"));
        assertEquals("Oversized results must not truncate or rewrite indexed files", before,
                fileHashes(documentRoot));
    }

    public void testCancellationExceptionIsNotConvertedToIoErrorOrFalseGeneratingState() throws Exception {
        DocViewMcpExecutor executor = new DocViewMcpExecutor(() -> {
            throw new CancellationException("test cancellation");
        });
        CompletableFuture<Throwable> completed = new CompletableFuture<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                executor.execute(getProject(), DocViewMcpExecutor.STATUS,
                        object("projectPath", projectRoot.toString()), new EmptyProgressIndicator());
                completed.complete(null);
            } catch (Throwable failure) {
                completed.complete(failure);
            }
        });

        Throwable failure = PlatformTestUtil.waitForFuture(completed, 10_000);
        assertTrue("CancellationException must reach the provider task boundary",
                failure instanceof CancellationException);
        assertFalse("A status query cancelled by the provider must not create the library", Files.exists(documentRoot));
    }

    private IndexData writeStore() throws Exception {
        Files.createDirectories(documentRoot.resolve("apis"));
        ProjectDocumentIdentity identity = ProjectDocumentIdentity.of("demo",
                "src/main/java/example/GreetingController.java", "example.GreetingController", "greet",
                List.of("java.lang.String"));
        SourceData source = new SourceData("src/main/java/example/GreetingController.java", 3,
                ProjectDocumentFormat.sha256("source".getBytes(StandardCharsets.UTF_8)), "ide-document-text");
        IdentityData identityData = new IdentityData(identity.moduleName(), identity.qualifiedClassName(),
                identity.methodName(), identity.parameterTypes(), identity.displaySignature(), identity.canonicalForm());
        ApiData api = new ApiData(identity.sha256(), identityData, "Greeting API", "Greeting detail", "spring", "GET",
                "/greeting", List.of("example"), source, List.of(), List.of(), null, null,
                new ExamplesData(null, null), "");
        byte[] detail = ProjectDocumentFormat.writeApi(api);
        byte[] markdown = "# Greeting API\n\nGreeting detail\n".getBytes(StandardCharsets.UTF_8);
        String markdownPath = "apis/" + identity.pageFileName();
        String detailPath = markdownPath.substring(0, markdownPath.length() - 3) + ".json";
        Files.write(documentRoot.resolve(markdownPath), markdown);
        Files.write(documentRoot.resolve(detailPath), detail);
        EntryData entry = new EntryData(identity.sha256(), identityData, api.name(), api.description(), "spring", "GET",
                "/greeting", api.tags(), source, new FileData(markdownPath, ProjectDocumentFormat.sha256(markdown)),
                new FileData(detailPath, ProjectDocumentFormat.sha256(detail)));
        byte[] indexMarkdown = "# Project API documents\n".getBytes(StandardCharsets.UTF_8);
        FileData indexFile = new FileData("index.md", ProjectDocumentFormat.sha256(indexMarkdown));
        IndexData unsigned = new IndexData("0".repeat(64), "2026-11-12T00:00:00Z", "test", "1".repeat(64), false,
                indexFile, List.of(entry));
        IndexData index = new IndexData(ProjectDocumentFormat.computeSnapshotId(unsigned), unsigned.generatedAt(),
                unsigned.producerVersion(), unsigned.generatorFingerprint(), false, indexFile, List.of(entry));
        Files.write(documentRoot.resolve("index.md"), indexMarkdown);
        Files.write(documentRoot.resolve("index.json"), ProjectDocumentFormat.writeIndex(index));
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(documentRoot);
        return index;
    }

    private void writeOversizedSearchStore() throws Exception {
        Files.createDirectories(documentRoot);
        Files.writeString(documentRoot.resolve("index.md"), "# Test index\n", StandardCharsets.UTF_8);
        String longText = "private-search-content-marker-" + "x".repeat(130_000);
        List<EntryData> entries = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            ProjectDocumentIdentity identity = ProjectDocumentIdentity.of("demo",
                    "src/main/java/example/GreetingController.java", "example.GreetingController",
                    "method" + String.format("%03d", index), List.of());
            IdentityData identityData = new IdentityData(identity.moduleName(), identity.qualifiedClassName(),
                    identity.methodName(), identity.parameterTypes(), identity.displaySignature(), identity.canonicalForm());
            SourceData source = new SourceData("src/main/java/example/GreetingController.java", index + 1,
                    ProjectDocumentFormat.sha256(("source-" + index).getBytes(StandardCharsets.UTF_8)),
                    "ide-document-text");
            String markdownPath = "apis/" + identity.pageFileName();
            String detailPath = markdownPath.substring(0, markdownPath.length() - 3) + ".json";
            entries.add(new EntryData(identity.sha256(), identityData, longText, longText, "spring", "GET",
                    "/greeting/" + index, List.of("example"), source,
                    new FileData(markdownPath, "a".repeat(64)), new FileData(detailPath, "b".repeat(64))));
        }
        entries.sort(Comparator.comparing(entry -> entry.identity().canonicalForm()));
        byte[] markdownIndex = Files.readAllBytes(documentRoot.resolve("index.md"));
        FileData indexFile = new FileData("index.md", ProjectDocumentFormat.sha256(markdownIndex));
        IndexData unsigned = new IndexData("0".repeat(64), "2026-11-12T00:00:00Z", "test", "1".repeat(64),
                false, indexFile, entries);
        IndexData index = new IndexData(ProjectDocumentFormat.computeSnapshotId(unsigned), unsigned.generatedAt(),
                unsigned.producerVersion(), unsigned.generatorFingerprint(), false, indexFile, entries);
        Files.write(documentRoot.resolve("index.json"), ProjectDocumentFormat.writeIndex(index));
        LocalFileSystem.getInstance().refreshAndFindFileByNioFile(documentRoot);
    }

    private static byte[] futureSchemaIndex() {
        String unsignedEnvelope = "{\"kind\":\"index\",\"owner\":\""
                + ProjectDocumentFormat.OWNER + "\",\"payload\":{},\"schemaVersion\":2}";
        String integrityHash = ProjectDocumentFormat.sha256(unsignedEnvelope.getBytes(StandardCharsets.UTF_8));
        String futureJson = "{\"integrity\":{\"algorithm\":\"sha256-jcs\",\"value\":\""
                + integrityHash + "\"},\"kind\":\"index\",\"owner\":\""
                + ProjectDocumentFormat.OWNER + "\",\"payload\":{},\"schemaVersion\":2}\n";
        return futureJson.getBytes(StandardCharsets.UTF_8);
    }

    private static JsonObject object(Object... values) {
        JsonObject result = new JsonObject();
        for (int index = 0; index < values.length; index += 2) {
            Object value = values[index + 1];
            if (value instanceof String text) result.addProperty((String) values[index], text);
            else if (value instanceof Number number) result.addProperty((String) values[index], number);
            else if (value instanceof Boolean bool) result.addProperty((String) values[index], bool);
            else throw new IllegalArgumentException("Unsupported fixture value");
        }
        return result;
    }

    private static void assertInvalid(Runnable action) {
        try {
            action.run();
            fail("Invalid input must be rejected by the MCP contract validator");
        } catch (IllegalArgumentException expected) {
            // The provider translates this to an MCP InvalidParams error.
        }
    }

    private static void assertError(JsonObject result, String code) {
        assertEquals(1, result.get("schemaVersion").getAsInt());
        assertEquals("error", result.get("status").getAsString());
        assertTrue(result.get("data").isJsonNull());
        JsonObject error = result.getAsJsonObject("error");
        assertEquals(code, error.get("code").getAsString());
        assertNotNull(error.get("message").getAsString());
        assertTrue(error.get("details").getAsJsonObject().has("indexCommitted"));
        assertTrue(error.get("relativePathsTotal").getAsInt() >= error.getAsJsonArray("relativePaths").size());
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static Map<String, String> fileHashes(Path root) throws Exception {
        if (!Files.exists(root)) return Map.of();
        Map<String, String> hashes = new LinkedHashMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                hashes.put(root.relativize(path).toString().replace('\\', '/'),
                        ProjectDocumentFormat.sha256(Files.readAllBytes(path)));
            }
        }
        return Map.copyOf(hashes);
    }

    private <T> T runInBackground(Callable<T> operation) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                result.complete(operation.call());
            } catch (Throwable failure) {
                result.completeExceptionally(failure);
            }
        });
        return PlatformTestUtil.waitForFuture(result, 30_000);
    }
}

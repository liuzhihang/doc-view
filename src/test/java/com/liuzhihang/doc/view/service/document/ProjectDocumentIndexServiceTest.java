package com.liuzhihang.doc.view.service.document;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.project.TestProjectManager;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.util.messages.MessageBusConnection;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.ApiData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.EntryData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.ExamplesData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.FileData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.IdentityData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.IndexData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.SourceData;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIdentity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ProjectDocumentIndexServiceTest extends LightJavaCodeInsightFixtureTestCase {

    private Path documentRoot;
    private ProjectDocumentIndexService service;
    private Boolean originalIncludeNormalInterface;
    private String originalSpringTemplate;

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Path projectRoot = Path.of(getProject().getBasePath()).toAbsolutePath().normalize();
        Files.createDirectories(projectRoot);
        projectRoot = projectRoot.toRealPath();
        documentRoot = projectRoot.resolve(ProjectDocumentIndexService.DOCUMENT_ROOT);
        deleteTree(documentRoot);
        service = ProjectDocumentIndexService.getInstance(getProject());
        originalIncludeNormalInterface = Settings.getInstance(getProject()).getIncludeNormalInterface();
        originalSpringTemplate = TemplateSettings.getInstance(getProject()).getSpringTemplate();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Settings.getInstance(getProject()).setIncludeNormalInterface(originalIncludeNormalInterface);
            TemplateSettings.getInstance(getProject()).setSpringTemplate(originalSpringTemplate);
            deleteTree(documentRoot);
        } finally {
            super.tearDown();
        }
    }

    public void testSearchNormalizesTermsAndCursorBindsQueryAndSnapshot() throws Exception {
        IndexData index = writeStore(List.of(
                api("core", "src/main/java/example/GreetingApi.java", "example.GreetingApi", "findGreeting",
                        "API greeting", "Returns a greeting", "/greeting"),
                api("web", "web/src/main/java/example/WelcomeApi.java", "example.WelcomeApi", "showWelcome",
                        "API greeting", "Returns a welcome", "/hello")));

        ProjectDocumentQueryResult first = background(() -> service.search(
                new ProjectDocumentQuery("ＡＰＩ   greeting", null, null, null, 1, null), indicator()));
        assertEquals(2, first.totalMatched());
        assertEquals(1, first.items().size());
        assertNotNull(first.nextCursor());
        assertEquals(index.snapshotId(), first.snapshotId());

        ProjectDocumentQueryResult second = background(() -> service.search(
                new ProjectDocumentQuery("api greeting", null, null, null, 1, first.nextCursor()), indicator()));
        assertEquals(1, second.items().size());
        assertFalse(first.items().get(0).apiId().equals(second.items().get(0).apiId()));
        assertNull(second.nextCursor());

        ProjectDocumentQueryResult byHttpPath = background(() -> service.search(
                new ProjectDocumentQuery("greeting /hello", null, null, null, null, null), indicator()));
        assertEquals(1, byHttpPath.totalMatched());
        assertEquals("web", byHttpPath.items().get(0).identity().module());

        ProjectDocumentQueryResult exactFilter = background(() -> service.search(
                new ProjectDocumentQuery("", "Core", null, null, null, null), indicator()));
        assertEquals(0, exactFilter.totalMatched());

        try {
            background(() -> service.search(new ProjectDocumentQuery("different", null, null, null, 1,
                    first.nextCursor()), indicator()));
            fail("A cursor must not be reused with another normalized query");
        } catch (ProjectDocumentQueryException exception) {
            assertEquals(ProjectDocumentQueryException.Code.CURSOR_STALE, exception.code());
        }

        try {
            background(() -> service.search(new ProjectDocumentQuery("api greeting", "core", null, null, 1,
                    first.nextCursor()), indicator()));
            fail("A cursor must not be reused with another exact filter");
        } catch (ProjectDocumentQueryException exception) {
            assertEquals(ProjectDocumentQueryException.Code.CURSOR_STALE, exception.code());
        }

        writeStore(List.of(
                api("core", "src/main/java/example/GreetingApi.java", "example.GreetingApi", "findGreeting",
                        "API greeting", "Returns a greeting", "/greeting"),
                api("web", "web/src/main/java/example/WelcomeApi.java", "example.WelcomeApi", "showWelcome",
                        "API greeting", "Returns a welcome", "/hello"),
                api("admin", "admin/src/main/java/example/AdminApi.java", "example.AdminApi", "listEntries",
                        "API administration", "Lists entries", "/entries")));
        try {
            background(() -> service.search(new ProjectDocumentQuery("api greeting", null, null, null, 1,
                    first.nextCursor()), indicator()));
            fail("A cursor must not be reused with a later disk snapshot");
        } catch (ProjectDocumentQueryException exception) {
            assertEquals(ProjectDocumentQueryException.Code.CURSOR_STALE, exception.code());
        }
    }

    public void testStatusDistinguishesMissingInvalidAndUnsupportedIndexes() throws Exception {
        assertFalse("A read-only status check must not create the project document root",
                Files.exists(documentRoot));
        ProjectDocumentIndexStatus missing = background(() -> service.status(indicator()));
        assertEquals(ProjectDocumentIndexStatus.IndexState.NOT_GENERATED, missing.indexState());
        assertEquals(ProjectDocumentIndexStatus.Freshness.UNKNOWN, missing.freshness());
        assertEquals("INDEX_NOT_FOUND", missing.errorCode());
        ProjectDocumentQueryException missingSearch = queryFailure(() -> background(() -> service.search(
                ProjectDocumentQuery.all(), indicator())));
        assertEquals(ProjectDocumentQueryException.Code.INDEX_NOT_FOUND, missingSearch.code());
        assertFalse("A missing-index query must remain read-only", Files.exists(documentRoot));

        Files.createDirectories(documentRoot);
        Files.writeString(documentRoot.resolve("index.json"), "{broken\n", StandardCharsets.UTF_8);
        ProjectDocumentIndexStatus invalid = background(() -> service.status(indicator()));
        assertEquals(ProjectDocumentIndexStatus.IndexState.INVALID, invalid.indexState());
        assertEquals("INDEX_INVALID", invalid.errorCode());

        Files.write(documentRoot.resolve("index.json"), futureSchemaIndex());
        ProjectDocumentIndexStatus unsupported = background(() -> service.status(indicator()));
        assertEquals(ProjectDocumentIndexStatus.IndexState.UNSUPPORTED_SCHEMA, unsupported.indexState());
        assertEquals("UNSUPPORTED_SCHEMA", unsupported.errorCode());
        assertEquals(ProjectDocumentIndexStatus.Freshness.UNKNOWN, unsupported.freshness());
    }

    public void testSearchHonorsExactFiltersAndDefaultMaximumPageLimits() throws Exception {
        List<ApiFixture> fixtures = new ArrayList<>();
        for (int index = 0; index < 101; index++) {
            String suffix = String.format("%03d", index);
            boolean spring = index % 2 == 0;
            fixtures.add(api(index % 3 == 0 ? "core" : "web",
                    "src/main/java/example/Api" + suffix + ".java", "example.Api" + suffix, "call" + suffix,
                    "Endpoint " + suffix, "Description " + suffix, "/api/" + suffix,
                    spring ? "spring" : "dubbo", spring ? "GET" : "POST"));
        }
        writeStore(fixtures);

        ProjectDocumentQueryResult defaultPage = background(() -> service.search(ProjectDocumentQuery.all(), indicator()));
        assertEquals(101, defaultPage.totalMatched());
        assertEquals(ProjectDocumentQuery.DEFAULT_LIMIT, defaultPage.items().size());
        assertNotNull(defaultPage.nextCursor());
        List<String> firstPageIdentities = defaultPage.items().stream()
                .map(item -> item.identity().canonicalForm()).toList();
        List<String> sortedFirstPageIdentities = new ArrayList<>(firstPageIdentities);
        sortedFirstPageIdentities.sort(String::compareTo);
        assertEquals("Results must preserve canonical identity order", sortedFirstPageIdentities,
                firstPageIdentities);

        ProjectDocumentQueryResult maximumPage = background(() -> service.search(
                new ProjectDocumentQuery("", null, null, null, ProjectDocumentQuery.MAX_LIMIT, null), indicator()));
        assertEquals(101, maximumPage.totalMatched());
        assertEquals(ProjectDocumentQuery.MAX_LIMIT, maximumPage.items().size());
        assertNotNull(maximumPage.nextCursor());
        ProjectDocumentQueryResult finalPage = background(() -> service.search(
                new ProjectDocumentQuery("", null, null, null, ProjectDocumentQuery.MAX_LIMIT,
                        maximumPage.nextCursor()), indicator()));
        assertEquals(1, finalPage.items().size());
        assertNull(finalPage.nextCursor());

        ProjectDocumentQueryResult filtered = background(() -> service.search(
                new ProjectDocumentQuery("", "core", "spring", "GET", 100, null), indicator()));
        assertEquals(fixtures.stream().filter(item -> "core".equals(item.module())
                && "spring".equals(item.framework()) && "GET".equals(item.httpMethod())).count(),
                filtered.totalMatched());
        assertTrue(filtered.items().stream().allMatch(item -> "core".equals(item.identity().module())
                && "spring".equals(item.framework()) && "GET".equals(item.httpMethod())));
        ProjectDocumentQueryResult caseSensitiveHttpMethod = background(() -> service.search(
                new ProjectDocumentQuery("", "core", "spring", "get", null, null), indicator()));
        assertEquals("HTTP method filters are exact canonical values", 0, caseSensitiveHttpMethod.totalMatched());

        ProjectDocumentQueryResult caseSensitiveModule = background(() -> service.search(
                new ProjectDocumentQuery("", "Core", null, null, null, null), indicator()));
        assertEquals("Module filters are exact values", 0, caseSensitiveModule.totalMatched());
        ProjectDocumentQueryException invalidLimit = queryFailure(() -> background(() -> service.search(
                new ProjectDocumentQuery("", null, null, null, 101, null), indicator())));
        assertEquals(ProjectDocumentQueryException.Code.INVALID_ARGUMENT, invalidLimit.code());
        ProjectDocumentQueryException zeroLimit = queryFailure(() -> background(() -> service.search(
                new ProjectDocumentQuery("", null, null, null, 0, null), indicator())));
        assertEquals(ProjectDocumentQueryException.Code.INVALID_ARGUMENT, zeroLimit.code());
        ProjectDocumentQueryException invalidFramework = queryFailure(() -> background(() -> service.search(
                new ProjectDocumentQuery("", null, "Spring", null, null, null), indicator())));
        assertEquals(ProjectDocumentQueryException.Code.INVALID_ARGUMENT, invalidFramework.code());
    }

    public void testSearchRejectsOversizedQueriesAndReportsCancellationAndTimeout() throws Exception {
        writeStore(List.of(api("core", "src/main/java/example/Greeter.java", "example.Greeter", "greet",
                "Greeting", "Description", "/greeting")));

        ProjectDocumentQueryException longQuery = queryFailure(() -> background(() -> service.search(
                new ProjectDocumentQuery("x".repeat(257), null, null, null, null, null), indicator())));
        assertEquals(ProjectDocumentQueryException.Code.LIMIT_EXCEEDED, longQuery.code());
        ProjectDocumentQueryException manyTerms = queryFailure(() -> background(() -> service.search(
                new ProjectDocumentQuery("one two three four five six seven eight nine ten eleven twelve "
                        + "thirteen fourteen fifteen sixteen seventeen", null, null, null, null, null), indicator())));
        assertEquals(ProjectDocumentQueryException.Code.LIMIT_EXCEEDED, manyTerms.code());

        EmptyProgressIndicator canceled = new EmptyProgressIndicator();
        canceled.cancel();
        ProjectDocumentQueryException cancellation = queryFailure(() -> background(() -> service.search(
                ProjectDocumentQuery.all(), canceled)));
        assertEquals(ProjectDocumentQueryException.Code.CANCELLED, cancellation.code());

        ProjectDocumentQueryException timeout = queryFailure(() -> background(() -> service.search(
                ProjectDocumentQuery.all(), timeoutIndicator())));
        assertEquals(ProjectDocumentQueryException.Code.TIMEOUT, timeout.code());
    }

    public void testIdenticalApiIdentityReadsFromEachProjectsOwnIndex() throws Exception {
        String source = "src/main/java/example/SharedApi.java";
        String qualifiedClass = "example.SharedApi";
        String methodName = "lookup";
        IndexData firstIndex = writeStore(List.of(api("core", source, qualifiedClass, methodName,
                "Project A name", "A description", "/shared")));

        Path secondProjectRoot = Files.createTempDirectory("doc-view-index-project-b-");
        Project[] secondProjectRef = new Project[1];
        try {
            ApplicationManager.getApplication().invokeAndWait(() -> {
                TestProjectManager manager = (TestProjectManager) ProjectManager.getInstance();
                Project secondProject = manager.newProject(secondProjectRoot.resolve("project-b.ipr"),
                        getProjectDescriptor().getOpenProjectOptions());
                assertNotNull("The second real project must be created", secondProject);
                secondProjectRef[0] = secondProject;
                assertTrue("The second real project must open", manager.openProject(secondProject));
            });
            Project secondProject = secondProjectRef[0];
            assertNotNull(secondProject);
            assertNotNull("The second project must have its own base path", secondProject.getBasePath());
            Path secondDocumentRoot = Path.of(secondProject.getBasePath()).resolve(ProjectDocumentIndexService.DOCUMENT_ROOT);
            IndexData secondIndex = writeStoreAt(secondDocumentRoot, List.of(api("core", source, qualifiedClass,
                    methodName, "Project B name", "B description", "/shared")));
            assertEquals("The two projects deliberately share one API identity",
                    firstIndex.entries().get(0).apiId(), secondIndex.entries().get(0).apiId());

            ProjectDocumentQueryResult firstSearch = background(() -> service.search(ProjectDocumentQuery.all(), indicator()));
            ProjectDocumentQueryResult secondSearch = background(() -> ProjectDocumentIndexService.getInstance(secondProject)
                    .search(ProjectDocumentQuery.all(), indicator()));
            assertEquals("Project A name", firstSearch.items().get(0).name());
            assertEquals("Project B name", secondSearch.items().get(0).name());
            assertEquals("Project A name", background(() -> service.getDetail(firstIndex.entries().get(0).apiId(),
                    firstIndex.snapshotId(), indicator())).detail().name());
            assertEquals("Project B name", background(() -> ProjectDocumentIndexService.getInstance(secondProject)
                    .getDetail(secondIndex.entries().get(0).apiId(), secondIndex.snapshotId(), indicator()))
                    .detail().name());
        } finally {
            Project secondProject = secondProjectRef[0];
            if (secondProject != null && !secondProject.isDisposed()) {
                ApplicationManager.getApplication().invokeAndWait(() ->
                        PlatformTestUtil.forceCloseProjectWithoutSaving(secondProject));
            }
            deleteTree(secondProjectRoot);
        }
    }

    public void testPartialOldIndexRemainsSearchableWhileTamperedDetailFailsClosed() throws Exception {
        IndexData index = writeStore(List.of(api("core", "src/main/java/example/Greeter.java",
                "example.Greeter", "greet", "Greeting", "Description", "/greeting")));
        EntryData entry = index.entries().get(0);
        Path detail = documentRoot.resolve(entry.detail().path());
        byte[] originalBytes = Files.readAllBytes(detail);
        String originalJson = new String(originalBytes, StandardCharsets.UTF_8);
        assertTrue("The test edit must target the indexed API name", originalJson.contains("\"name\":\"Greeting\""));
        byte[] editedBytes = originalJson.replace("\"name\":\"Greeting\"", "\"name\":\"Freeting\"")
                .getBytes(StandardCharsets.UTF_8);
        assertEquals("The tampered detail intentionally keeps the original byte length",
                originalBytes.length, editedBytes.length);
        Files.write(detail, editedBytes);

        ProjectDocumentQueryResult search = background(() -> service.search(ProjectDocumentQuery.all(), indicator()));
        assertEquals("A valid old index remains searchable after partial publication", 1, search.totalMatched());
        try {
            background(() -> service.getDetail(entry.apiId(), index.snapshotId(), indicator()));
            fail("A detail whose raw bytes no longer match the index must be rejected");
        } catch (ProjectDocumentQueryException exception) {
            assertEquals(ProjectDocumentQueryException.Code.INDEX_INCONSISTENT, exception.code());
            assertEquals(entry.detail().path(), exception.relativePath());
        }
        assertEquals(ProjectDocumentIndexStatus.IndexState.INCONSISTENT,
                background(() -> service.status(indicator())).indexState());
    }

    public void testDetailRequiresSnapshotAndChecksSymlinkBoundaries() throws Exception {
        IndexData index = writeStore(List.of(api("core", "src/main/java/example/Greeter.java",
                "example.Greeter", "greet", "Greeting", "Description", "/greeting")));
        EntryData entry = index.entries().get(0);
        try {
            background(() -> service.getDetail(entry.apiId(), "0".repeat(64), indicator()));
            fail("Details must be bound to the result snapshot");
        } catch (ProjectDocumentQueryException exception) {
            assertEquals(ProjectDocumentQueryException.Code.SNAPSHOT_CHANGED, exception.code());
        }

        ProjectDocumentDetailResult detail = background(() -> service.getDetail(entry.apiId(), index.snapshotId(), indicator()));
        assertEquals(entry.apiId(), detail.detail().apiId());
        assertEquals(index.snapshotId(), detail.snapshotId());
        ProjectDocumentQueryException absentApi = queryFailure(() -> background(() -> service.getDetail(
                "f".repeat(64), index.snapshotId(), indicator())));
        assertEquals(ProjectDocumentQueryException.Code.API_NOT_FOUND, absentApi.code());

        Path external = Files.createTempFile("doc-view-outside-detail-", ".json");
        try {
            Files.writeString(external, "external data\n", StandardCharsets.UTF_8);
            Path detailPath = documentRoot.resolve(entry.detail().path());
            Files.delete(detailPath);
            Files.createSymbolicLink(detailPath, external);
            try {
                background(() -> service.getDetail(entry.apiId(), index.snapshotId(), indicator()));
                fail("The detail reader must not follow a symlink out of the document root");
            } catch (ProjectDocumentQueryException exception) {
                assertEquals(ProjectDocumentQueryException.Code.INDEX_INCONSISTENT, exception.code());
            }
        } finally {
            Files.deleteIfExists(external);
        }
    }

    public void testIndexMarkdownRequiresSnapshotAndRejectsChangedBytes() throws Exception {
        IndexData index = writeStore(List.of(api("core", "src/main/java/example/Greeter.java",
                "example.Greeter", "greet", "Greeting", "Description", "/greeting")));
        try {
            background(() -> service.getIndexMarkdown("0".repeat(64), indicator()));
            fail("The index Markdown read must be bound to the selected snapshot");
        } catch (ProjectDocumentQueryException exception) {
            assertEquals(ProjectDocumentQueryException.Code.SNAPSHOT_CHANGED, exception.code());
        }

        ProjectDocumentIndexMarkdownResult result = background(
                () -> service.getIndexMarkdown(index.snapshotId(), indicator()));
        assertEquals("index.md", result.relativePath());
        assertEquals(index.snapshotId(), result.snapshotId());
        assertTrue(result.markdown().contains("Project API documents"));

        byte[] original = Files.readAllBytes(documentRoot.resolve("index.md"));
        byte[] sameSizeEdit = "# Project XYZ documents\n".getBytes(StandardCharsets.UTF_8);
        assertEquals("The index page edit must keep the original byte length", original.length, sameSizeEdit.length);
        Files.write(documentRoot.resolve("index.md"), sameSizeEdit);
        try {
            background(() -> service.getIndexMarkdown(index.snapshotId(), indicator()));
            fail("Changed index Markdown must be rejected by its recorded raw-byte hash");
        } catch (ProjectDocumentQueryException exception) {
            assertEquals(ProjectDocumentQueryException.Code.INDEX_INCONSISTENT, exception.code());
            assertEquals("index.md", exception.relativePath());
        }
        assertEquals(ProjectDocumentIndexStatus.IndexState.INCONSISTENT,
                background(() -> service.status(indicator())).indexState());
    }

    public void testMissingApiDetailAndMarkdownFailClosed() throws Exception {
        IndexData index = writeStore(List.of(
                api("core", "src/main/java/example/DetailApi.java", "example.DetailApi", "detail",
                        "Detail page", "A detail", "/detail"),
                api("web", "web/src/main/java/example/MarkdownApi.java", "example.MarkdownApi", "markdown",
                        "Markdown page", "A page", "/markdown")));
        EntryData detailEntry = index.entries().get(0);
        EntryData markdownEntry = index.entries().get(1);
        ProjectDocumentQueryException staleMarkdown = queryFailure(() -> background(() -> service.getMarkdown(
                markdownEntry.apiId(), "0".repeat(64), indicator())));
        assertEquals(ProjectDocumentQueryException.Code.SNAPSHOT_CHANGED, staleMarkdown.code());
        ProjectDocumentMarkdownResult validMarkdown = background(() -> service.getMarkdown(
                markdownEntry.apiId(), index.snapshotId(), indicator()));
        assertEquals(markdownEntry.markdown().path(), validMarkdown.relativePath());
        assertTrue(validMarkdown.markdown().contains(markdownEntry.name()));

        Files.delete(documentRoot.resolve(detailEntry.detail().path()));
        ProjectDocumentQueryException missingDetail = queryFailure(() -> background(() -> service.getDetail(
                detailEntry.apiId(), index.snapshotId(), indicator())));
        assertEquals(ProjectDocumentQueryException.Code.INDEX_INCONSISTENT, missingDetail.code());
        assertEquals(detailEntry.detail().path(), missingDetail.relativePath());

        Files.delete(documentRoot.resolve(markdownEntry.markdown().path()));
        ProjectDocumentQueryException missingMarkdown = queryFailure(() -> background(() -> service.getMarkdown(
                markdownEntry.apiId(), index.snapshotId(), indicator())));
        assertEquals(ProjectDocumentQueryException.Code.INDEX_INCONSISTENT, missingMarkdown.code());
        assertEquals(markdownEntry.markdown().path(), missingMarkdown.relativePath());
    }

    public void testRestoredDocumentResourcesClearOnlyTheirOwnIntegrityErrors() throws Exception {
        IndexData index = writeStore(List.of(api("core", "src/main/java/example/Greeter.java",
                "example.Greeter", "greet", "Greeting", "Description", "/greeting")));
        service.installCommittedIndex(index, service.captureInputEpoch());
        EntryData entry = index.entries().get(0);

        Path markdownPath = documentRoot.resolve(entry.markdown().path());
        byte[] originalMarkdown = Files.readAllBytes(markdownPath);
        String originalMarkdownText = new String(originalMarkdown, StandardCharsets.UTF_8);
        String changedMarkdownText = originalMarkdownText.replace("Greeting", "Freeting");
        assertFalse("The test edit must change the indexed Markdown", originalMarkdownText.equals(changedMarkdownText));
        Files.writeString(markdownPath, changedMarkdownText, StandardCharsets.UTF_8);

        Path detailPath = documentRoot.resolve(entry.detail().path());
        byte[] originalDetail = Files.readAllBytes(detailPath);
        String originalDetailText = new String(originalDetail, StandardCharsets.UTF_8);
        String changedDetailText = originalDetailText.replace("\"name\":\"Greeting\"",
                "\"name\":\"Freeting\"");
        assertFalse("The test edit must change the indexed detail", originalDetailText.equals(changedDetailText));
        assertEquals("The detail edit intentionally preserves its byte length", originalDetail.length,
                changedDetailText.getBytes(StandardCharsets.UTF_8).length);
        Files.writeString(detailPath, changedDetailText, StandardCharsets.UTF_8);

        ProjectDocumentQueryException changedMarkdownFailure = queryFailure(() -> background(
                () -> service.getMarkdown(entry.apiId(), index.snapshotId(), indicator())));
        assertEquals(ProjectDocumentQueryException.Code.INDEX_INCONSISTENT, changedMarkdownFailure.code());
        assertEquals(entry.markdown().path(), changedMarkdownFailure.relativePath());
        ProjectDocumentQueryException changedDetailFailure = queryFailure(() -> background(
                () -> service.getDetail(entry.apiId(), index.snapshotId(), indicator())));
        assertEquals(ProjectDocumentQueryException.Code.INDEX_INCONSISTENT, changedDetailFailure.code());
        assertEquals(entry.detail().path(), changedDetailFailure.relativePath());
        assertEquals(ProjectDocumentIndexStatus.IndexState.INCONSISTENT,
                background(() -> service.status(indicator())).indexState());

        Files.write(markdownPath, originalMarkdown);
        ProjectDocumentMarkdownResult restoredMarkdown = background(
                () -> service.getMarkdown(entry.apiId(), index.snapshotId(), indicator()));
        assertTrue(restoredMarkdown.markdown().contains("Greeting"));
        ProjectDocumentIndexStatus detailStillInconsistent = background(() -> service.status(indicator()));
        assertEquals("A successful Markdown read must not clear the separate detail error",
                ProjectDocumentIndexStatus.IndexState.INCONSISTENT, detailStillInconsistent.indexState());
        assertEquals(ProjectDocumentIndexStatus.Freshness.CURRENT, detailStillInconsistent.freshness());

        Files.write(detailPath, originalDetail);
        ProjectDocumentDetailResult restoredDetail = background(
                () -> service.getDetail(entry.apiId(), index.snapshotId(), indicator()));
        assertEquals(entry.apiId(), restoredDetail.detail().apiId());
        ProjectDocumentIndexStatus apiResourcesRestored = background(() -> service.status(indicator()));
        assertEquals(ProjectDocumentIndexStatus.IndexState.READY, apiResourcesRestored.indexState());
        assertEquals(ProjectDocumentIndexStatus.Freshness.CURRENT, apiResourcesRestored.freshness());

        Path indexMarkdownPath = documentRoot.resolve("index.md");
        byte[] originalIndexMarkdown = Files.readAllBytes(indexMarkdownPath);
        String originalIndexMarkdownText = new String(originalIndexMarkdown, StandardCharsets.UTF_8);
        String changedIndexMarkdownText = originalIndexMarkdownText.replace("API", "XYZ");
        assertFalse("The test edit must change index.md", originalIndexMarkdownText.equals(changedIndexMarkdownText));
        assertEquals("The index Markdown edit intentionally preserves its byte length", originalIndexMarkdown.length,
                changedIndexMarkdownText.getBytes(StandardCharsets.UTF_8).length);
        Files.writeString(indexMarkdownPath, changedIndexMarkdownText, StandardCharsets.UTF_8);
        ProjectDocumentQueryException changedIndexMarkdownFailure = queryFailure(() -> background(
                () -> service.getIndexMarkdown(index.snapshotId(), indicator())));
        assertEquals(ProjectDocumentQueryException.Code.INDEX_INCONSISTENT, changedIndexMarkdownFailure.code());
        assertEquals("index.md", changedIndexMarkdownFailure.relativePath());

        Files.write(indexMarkdownPath, originalIndexMarkdown);
        ProjectDocumentIndexMarkdownResult restoredIndexMarkdown = background(
                () -> service.getIndexMarkdown(index.snapshotId(), indicator()));
        assertTrue(restoredIndexMarkdown.markdown().contains("Project API documents"));
        ProjectDocumentIndexStatus allResourcesRestored = background(() -> service.status(indicator()));
        assertEquals(ProjectDocumentIndexStatus.IndexState.READY, allResourcesRestored.indexState());
        assertEquals(ProjectDocumentIndexStatus.Freshness.CURRENT, allResourcesRestored.freshness());
    }

    public void testOlderSuccessfulReadCannotClearANewerFailureForTheSameResource() throws Exception {
        IndexData index = writeStore(List.of(api("core", "src/main/java/example/Greeter.java",
                "example.Greeter", "greet", "Greeting", "Description", "/greeting")));
        long capturedEpoch = service.captureInputEpoch();
        service.installCommittedIndex(index, capturedEpoch);
        String snapshotId = index.snapshotId();
        String resourcePath = index.entries().get(0).markdown().path();
        Method markInconsistent = ProjectDocumentIndexService.class.getDeclaredMethod(
                "markInconsistent", String.class, String.class);
        Method inconsistencyToken = ProjectDocumentIndexService.class.getDeclaredMethod(
                "inconsistencyToken", String.class, String.class);
        Method clearInconsistent = ProjectDocumentIndexService.class.getDeclaredMethod(
                "clearInconsistent", String.class, String.class, long.class);
        Method hasKnownInconsistency = ProjectDocumentIndexService.class.getDeclaredMethod(
                "hasKnownInconsistency", String.class);
        markInconsistent.setAccessible(true);
        inconsistencyToken.setAccessible(true);
        clearInconsistent.setAccessible(true);
        hasKnownInconsistency.setAccessible(true);

        markInconsistent.invoke(service, snapshotId, resourcePath);
        long tokenCapturedWhenReadStarted = (long) inconsistencyToken.invoke(service, snapshotId, resourcePath);
        markInconsistent.invoke(service, snapshotId, resourcePath);

        // Model a read that began before the newer failure but completed successfully afterward.
        clearInconsistent.invoke(service, snapshotId, resourcePath, tokenCapturedWhenReadStarted);
        assertTrue("A late successful read must not clear a newer failure for the same path",
                (boolean) hasKnownInconsistency.invoke(service, snapshotId));

        long latestFailureToken = (long) inconsistencyToken.invoke(service, snapshotId, resourcePath);
        clearInconsistent.invoke(service, snapshotId, resourcePath, latestFailureToken);
        assertFalse("The matching successful read may clear the latest failure",
                (boolean) hasKnownInconsistency.invoke(service, snapshotId));

        markInconsistent.invoke(service, snapshotId, resourcePath);
        long tokenBeforeCacheClear = (long) inconsistencyToken.invoke(service, snapshotId, resourcePath);
        service.clearDerivedCache();
        markInconsistent.invoke(service, snapshotId, resourcePath);
        clearInconsistent.invoke(service, snapshotId, resourcePath, tokenBeforeCacheClear);
        assertTrue("A pre-clear read token must not erase a later failure after Clear Cache",
                (boolean) hasKnownInconsistency.invoke(service, snapshotId));
        long tokenAfterCacheClear = (long) inconsistencyToken.invoke(service, snapshotId, resourcePath);
        clearInconsistent.invoke(service, snapshotId, resourcePath, tokenAfterCacheClear);

        markInconsistent.invoke(service, snapshotId, resourcePath);
        long tokenBeforeCommit = (long) inconsistencyToken.invoke(service, snapshotId, resourcePath);
        service.installCommittedIndex(index, capturedEpoch);
        markInconsistent.invoke(service, snapshotId, resourcePath);
        clearInconsistent.invoke(service, snapshotId, resourcePath, tokenBeforeCommit);
        assertTrue("A pre-commit read token must not erase a later failure for the same snapshot",
                (boolean) hasKnownInconsistency.invoke(service, snapshotId));
        long tokenAfterCommit = (long) inconsistencyToken.invoke(service, snapshotId, resourcePath);
        clearInconsistent.invoke(service, snapshotId, resourcePath, tokenAfterCommit);
        service.clearDerivedCache();
    }

    public void testPublishesProjectScopedStateEvents() throws Exception {
        List<ObservedState> events = new ArrayList<>();
        MessageBusConnection connection = getProject().getMessageBus().connect();
        connection.subscribe(ProjectDocumentIndexListener.TOPIC,
                (epoch, snapshotId) -> events.add(new ObservedState(epoch, snapshotId)));
        try {
            IndexData index = writeStore(List.of(api("core", "src/main/java/example/Greeter.java",
                    "example.Greeter", "greet", "Greeting", "Description", "/greeting")));
            long initialEpoch = service.captureInputEpoch();
            service.installCommittedIndex(index, initialEpoch);
            events.clear();
            long currentEpoch = service.captureInputEpoch();
            service.markObservedInputsChanged();
            assertEquals(currentEpoch + 1, events.get(0).inputEpoch());
            assertEquals("A stale event retains the previously committed generation", index.snapshotId(),
                    events.get(0).snapshotId());

            int beforeClear = events.size();
            service.clearDerivedCache();
            assertEquals(beforeClear + 1, events.size());
            assertEquals(index.snapshotId(), events.get(events.size() - 1).snapshotId());
        } finally {
            connection.disconnect();
        }
    }

    public void testFreshnessTracksSettingsAndClearCachePreservesCommittedGeneration() throws Exception {
        IndexData index = writeStore(List.of(api("core", "src/main/java/example/FreshnessProbe.java",
                "example.FreshnessProbe", "captureFreshness", "Freshness baseline", "Freshness description",
                "/freshness-baseline")));
        assertEquals(ProjectDocumentIndexStatus.Freshness.UNKNOWN,
                background(() -> service.status(indicator())).freshness());

        long capturedEpoch = service.captureInputEpoch();
        service.installCommittedIndex(index, capturedEpoch);
        assertEquals(ProjectDocumentIndexStatus.Freshness.CURRENT,
                background(() -> service.status(indicator())).freshness());
        service.clearDerivedCache();
        assertEquals("Clear Cache retains the current-generation baseline",
                ProjectDocumentIndexStatus.Freshness.CURRENT,
                background(() -> service.status(indicator())).freshness());

        Settings settings = Settings.getInstance(getProject());
        settings.setIncludeNormalInterface(!Boolean.TRUE.equals(originalIncludeNormalInterface));
        assertEquals(ProjectDocumentIndexStatus.Freshness.STALE,
                background(() -> service.status(indicator())).freshness());
        service.clearDerivedCache();
        assertEquals(ProjectDocumentIndexStatus.Freshness.STALE,
                background(() -> service.status(indicator())).freshness());

        long settingsEpoch = service.captureInputEpoch();
        service.installCommittedIndex(index, settingsEpoch);
        TemplateSettings templates = TemplateSettings.getInstance(getProject());
        templates.setSpringTemplate(templates.getSpringTemplate() + "\n<!-- freshness-test -->");
        assertEquals("Template edits must make the installed generation stale",
                ProjectDocumentIndexStatus.Freshness.STALE,
                background(() -> service.status(indicator())).freshness());

        long templateEpoch = service.captureInputEpoch();
        service.installCommittedIndex(index, templateEpoch);
        Path externalContentRoot = Files.createTempDirectory("doc-view-external-content-");
        Path sourcePath = externalContentRoot.resolve("SourceChange.java");
        Files.writeString(sourcePath, "class SourceChange { String value() { return \"before\"; } }\n");
        VirtualFile externalRoot = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(externalContentRoot);
        VirtualFile sourceFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(sourcePath);
        String externalRootUrl = externalRoot == null ? null : externalRoot.getUrl();
        try {
            assertNotNull("The external content root must be visible to the virtual file system", externalRoot);
            assertNotNull("The external Java file must be visible to the virtual file system", sourceFile);
            WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                ModifiableRootModel model = ModuleRootManager.getInstance(getModule()).getModifiableModel();
                try {
                    model.addContentEntry(externalRoot);
                    model.commit();
                } catch (RuntimeException | Error failure) {
                    model.dispose();
                    throw failure;
                }
            });
            assertTrue("The test file must be inside a configured content root",
                    ReadAction.compute(() -> ProjectFileIndex.getInstance(getProject()).isInContent(sourceFile)));
            assertFalse("The content root must be outside the project base path",
                    Path.of(sourceFile.getPath()).toAbsolutePath().normalize().startsWith(
                            Path.of(getProject().getBasePath()).toAbsolutePath().normalize()));

            // Root changes themselves are observed; install a baseline after that event to isolate the file edit.
            service.installCommittedIndex(index, service.captureInputEpoch());
            Document document = FileDocumentManager.getInstance().getDocument(sourceFile);
            assertNotNull("The external Java source must have a Document", document);
            WriteCommandAction.runWriteCommandAction(getProject(), () ->
                    document.setText("class SourceChange { String value() { return \"after\"; } }\n"));
            assertEquals("An edited Java file in any project content root must make the generation stale",
                    ProjectDocumentIndexStatus.Freshness.STALE,
                    background(() -> service.status(indicator())).freshness());
        } finally {
            try {
                WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                    ModifiableRootModel model = ModuleRootManager.getInstance(getModule()).getModifiableModel();
                    try {
                        if (externalRootUrl != null) {
                            for (ContentEntry entry : model.getContentEntries()) {
                                if (externalRootUrl.equals(entry.getUrl())) {
                                    model.removeContentEntry(entry);
                                }
                            }
                        }
                        model.commit();
                    } catch (RuntimeException | Error failure) {
                        model.dispose();
                        throw failure;
                    }
                });
            } finally {
                deleteTree(externalContentRoot);
            }
        }
    }

    public void testXmlDocumentChangeInExternalContentRootMakesCommittedGenerationStale() throws Exception {
        IndexData index = writeStore(List.of(api("core", "src/main/java/example/XmlFreshnessProbe.java",
                "example.XmlFreshnessProbe", "lookup", "XML freshness baseline", "Description", "/xml-freshness")));
        Path externalRoot = Files.createTempDirectory("doc-view-external-xml-");
        Path xmlPath = externalRoot.resolve("dubbo-provider.xml");
        String originalText = "<beans xmlns=\"http://www.springframework.org/schema/beans\"/>\n";
        String editedText = "<beans xmlns=\"http://www.springframework.org/schema/beans\" "
                + "xmlns:dubbo=\"http://dubbo.apache.org/schema/dubbo\">"
                + "<dubbo:service interface=\"example.XmlFreshnessProbe\" ref=\"provider\"/>"
                + "</beans>\n";
        Files.writeString(xmlPath, originalText, StandardCharsets.UTF_8);
        VirtualFile externalRootVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(externalRoot);
        VirtualFile xmlFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(xmlPath);
        String externalRootUrl = externalRootVf == null ? null : externalRootVf.getUrl();
        Document xmlDocument = xmlFile == null ? null : FileDocumentManager.getInstance().getDocument(xmlFile);
        try {
            assertNotNull("The external XML content root must be visible to the virtual file system", externalRootVf);
            assertNotNull("The external Dubbo XML file must be visible to the virtual file system", xmlFile);
            assertNotNull("The external Dubbo XML file must have an IDE Document", xmlDocument);
            WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                ModifiableRootModel model = ModuleRootManager.getInstance(getModule()).getModifiableModel();
                try {
                    model.addContentEntry(externalRootVf);
                    model.commit();
                } catch (RuntimeException | Error failure) {
                    model.dispose();
                    throw failure;
                }
            });
            assertTrue("The external XML file must be inside a configured content root",
                    ReadAction.compute(() -> ProjectFileIndex.getInstance(getProject()).isInContent(xmlFile)));
            assertFalse("The XML content root must be outside the project base path",
                    Path.of(xmlFile.getPath()).toAbsolutePath().normalize().startsWith(
                            Path.of(getProject().getBasePath()).toAbsolutePath().normalize()));

            // Exclude the root-change event from this assertion; only the following XML edit is under test.
            service.installCommittedIndex(index, service.captureInputEpoch());
            assertEquals(ProjectDocumentIndexStatus.Freshness.CURRENT,
                    background(() -> service.status(indicator())).freshness());
            byte[] diskBefore = Files.readAllBytes(xmlPath);

            WriteCommandAction.runWriteCommandAction(getProject(), () -> xmlDocument.setText(editedText));

            assertTrue("An unsaved XML edit must remain an IDE Document change", FileDocumentManager.getInstance()
                    .isDocumentUnsaved(xmlDocument));
            assertTrue("Freshness tracking must not save the user's XML edit",
                    java.util.Arrays.equals(diskBefore, Files.readAllBytes(xmlPath)));
            assertEquals("An XML declaration change in any project content root makes the old snapshot stale",
                    ProjectDocumentIndexStatus.Freshness.STALE,
                    background(() -> service.status(indicator())).freshness());
        } finally {
            try {
                if (xmlDocument != null) {
                    Document documentToRestore = xmlDocument;
                    WriteCommandAction.runWriteCommandAction(getProject(), () -> documentToRestore.setText(originalText));
                    com.intellij.psi.PsiDocumentManager.getInstance(getProject()).commitDocument(xmlDocument);
                    FileDocumentManager.getInstance().saveDocument(xmlDocument);
                }
            } finally {
                try {
                    if (externalRootUrl != null && !getModule().isDisposed()) {
                        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                            ModifiableRootModel model = ModuleRootManager.getInstance(getModule()).getModifiableModel();
                            try {
                                for (ContentEntry entry : model.getContentEntries()) {
                                    if (externalRootUrl.equals(entry.getUrl())) {
                                        model.removeContentEntry(entry);
                                    }
                                }
                                model.commit();
                            } catch (RuntimeException | Error failure) {
                                model.dispose();
                                throw failure;
                            }
                        });
                    }
                } finally {
                    deleteTree(externalRoot);
                }
            }
        }
    }

    public void testModuleRootChangeMakesCommittedGenerationStale() throws Exception {
        IndexData index = writeStore(List.of(api("core", "src/main/java/example/Greeter.java",
                "example.Greeter", "greet", "Greeting", "Description", "/greeting")));
        service.installCommittedIndex(index, service.captureInputEpoch());
        Path additionalRoot = Files.createTempDirectory("doc-view-module-root-change-");
        VirtualFile additionalRootVf = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(additionalRoot);
        String additionalRootUrl = additionalRootVf == null ? "" : additionalRootVf.getUrl();
        try {
            assertNotNull("The temporary module root must be visible to the virtual file system", additionalRootVf);
            WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                ModifiableRootModel model = ModuleRootManager.getInstance(getModule()).getModifiableModel();
                try {
                    model.addContentEntry(additionalRootVf);
                    model.commit();
                } catch (RuntimeException | Error failure) {
                    model.dispose();
                    throw failure;
                }
            });
            assertEquals("A committed module-root change must invalidate the observed source generation",
                    ProjectDocumentIndexStatus.Freshness.STALE,
                    background(() -> service.status(indicator())).freshness());
        } finally {
            try {
                WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                    ModifiableRootModel model = ModuleRootManager.getInstance(getModule()).getModifiableModel();
                    try {
                        for (ContentEntry entry : model.getContentEntries()) {
                            if (additionalRootUrl.equals(entry.getUrl())) {
                                model.removeContentEntry(entry);
                            }
                        }
                        model.commit();
                    } catch (RuntimeException | Error failure) {
                        model.dispose();
                        throw failure;
                    }
                });
            } finally {
                deleteTree(additionalRoot);
            }
        }
    }

    private IndexData writeStore(List<ApiFixture> fixtures) throws Exception {
        return writeStoreAt(documentRoot, fixtures);
    }

    private static IndexData writeStoreAt(Path storeRoot, List<ApiFixture> fixtures) throws Exception {
        Path apiDirectory = storeRoot.resolve("apis");
        Files.createDirectories(apiDirectory);
        List<EntryData> entries = new ArrayList<>();
        for (ApiFixture fixture : fixtures) {
            ProjectDocumentIdentity identity = ProjectDocumentIdentity.of(fixture.module(), fixture.sourcePath(),
                    fixture.qualifiedClass(), fixture.methodName(), List.of());
            SourceData source = new SourceData(fixture.sourcePath(), 4,
                    ProjectDocumentFormat.sha256("source".getBytes(StandardCharsets.UTF_8)), "ide-document-text");
            IdentityData identityData = new IdentityData(identity.moduleName(), identity.qualifiedClassName(),
                    identity.methodName(), identity.parameterTypes(), identity.displaySignature(), identity.canonicalForm());
            ApiData api = new ApiData(identity.sha256(), identityData, fixture.name(), fixture.description(),
                    fixture.framework(), fixture.httpMethod(), fixture.httpPath(), List.of("sample"), source,
                    List.of(), List.of(), null, null, new ExamplesData(null, null), "");
            byte[] detailBytes = ProjectDocumentFormat.writeApi(api);
            byte[] markdownBytes = ("# " + fixture.name() + "\n\n" + fixture.description() + "\n")
                    .getBytes(StandardCharsets.UTF_8);
            String markdownPath = "apis/" + identity.pageFileName();
            String detailPath = markdownPath.substring(0, markdownPath.length() - 3) + ".json";
            Files.write(storeRoot.resolve(Path.of(markdownPath)), markdownBytes);
            Files.write(storeRoot.resolve(Path.of(detailPath)), detailBytes);
            entries.add(new EntryData(identity.sha256(), identityData, fixture.name(), fixture.description(),
                    fixture.framework(), fixture.httpMethod(), fixture.httpPath(), List.of("sample"), source,
                    new FileData(markdownPath, ProjectDocumentFormat.sha256(markdownBytes)),
                    new FileData(detailPath, ProjectDocumentFormat.sha256(detailBytes))));
        }
        entries.sort(Comparator.comparing(entry -> entry.identity().canonicalForm()));
        byte[] markdownIndex = "# Project API documents\n".getBytes(StandardCharsets.UTF_8);
        IndexData unsigned = new IndexData("0".repeat(64), "2026-10-24T00:00:00Z", "test", "1".repeat(64), false,
                new FileData("index.md", ProjectDocumentFormat.sha256(markdownIndex)), entries);
        IndexData index = new IndexData(ProjectDocumentFormat.computeSnapshotId(unsigned), unsigned.generatedAt(),
                unsigned.producerVersion(), unsigned.generatorFingerprint(), unsigned.hasUnsavedDocuments(),
                unsigned.markdownIndex(), unsigned.entries());
        Files.createDirectories(storeRoot);
        Files.write(storeRoot.resolve("index.md"), markdownIndex);
        Files.write(storeRoot.resolve("index.json"), ProjectDocumentFormat.writeIndex(index));
        return index;
    }

    private static ApiFixture api(String module, String sourcePath, String qualifiedClass, String methodName,
                                  String name, String description, String httpPath) {
        return api(module, sourcePath, qualifiedClass, methodName, name, description, httpPath, "spring", "GET");
    }

    private static ApiFixture api(String module, String sourcePath, String qualifiedClass, String methodName,
                                  String name, String description, String httpPath,
                                  String framework, String httpMethod) {
        return new ApiFixture(module, sourcePath, qualifiedClass, methodName, name, description, httpPath,
                framework, httpMethod);
    }

    private static ProgressIndicator indicator() {
        return new EmptyProgressIndicator();
    }

    private static ProgressIndicator timeoutIndicator() {
        EmptyProgressIndicator delegate = new EmptyProgressIndicator();
        AtomicBoolean advancedPastBudget = new AtomicBoolean();
        return (ProgressIndicator) Proxy.newProxyInstance(ProgressIndicator.class.getClassLoader(),
                new Class<?>[]{ProgressIndicator.class}, (proxy, method, arguments) -> {
                    if ("checkCanceled".equals(method.getName()) && advancedPastBudget.compareAndSet(false, true)) {
                        try {
                            Thread.sleep(5_200);
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            throw interrupted;
                        }
                    }
                    try {
                        return method.invoke(delegate, arguments);
                    } catch (InvocationTargetException invocationFailure) {
                        throw invocationFailure.getCause();
                    }
                });
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

    private static ProjectDocumentQueryException queryFailure(ThrowingSupplier<?> operation) throws Exception {
        try {
            operation.get();
            fail("Expected the project document query to fail");
        } catch (ProjectDocumentQueryException exception) {
            return exception;
        }
        throw new AssertionError("The query failure assertion must return an exception");
    }

    private static <T> T background(ThrowingSupplier<T> supplier) throws Exception {
        CompletableFuture<T> future = new CompletableFuture<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception checked) {
                throw checked;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new AssertionError(cause);
        }
    }

    private static void deleteTree(Path root) throws Exception {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private record ApiFixture(String module,
                              String sourcePath,
                              String qualifiedClass,
                              String methodName,
                              String name,
                              String description,
                              String httpPath,
                              String framework,
                              String httpMethod) {
    }

    private record ObservedState(long inputEpoch, String snapshotId) {
    }
}

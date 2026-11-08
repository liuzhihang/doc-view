package com.liuzhihang.doc.view.service.document;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.EntryData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.FileData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.IdentityData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.IndexData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData.SourceData;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIdentity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/** Records bounded 10,000-entry query samples from a light fixture; this is not native IDE evidence. */
public final class ProjectDocumentStorePerformanceTest extends LightJavaCodeInsightFixtureTestCase {

    private static final int ENTRY_COUNT = 10_000;
    private static final int WARM_SEARCH_SAMPLES = 30;
    private static final int COLD_LOAD_SAMPLES = 7;
    private static final int CANCELLATION_SAMPLES = 15;
    private static final int MAX_CANCELLATION_ATTEMPTS = 90;
    private static final int PARSE_CANCELLATION_SAMPLES = 5;
    private static final int MAX_PARSE_CANCELLATION_ATTEMPTS = 15;
    private static final int PARSE_FIXTURE_ENTRIES = 31;
    private static final int PARSE_FIXTURE_DESCRIPTION_CHARS = 1_020_000;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Path documentRoot;
    private ProjectDocumentIndexService service;

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
        writeFixtureIndex();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            service.clearDerivedCache();
            deleteTree(documentRoot);
        } finally {
            super.tearDown();
        }
    }

    public void testTenThousandEntryQueryPerformanceSamples() throws Exception {
        List<Double> coldLoads = new ArrayList<>();
        List<Double> warmSearches = new ArrayList<>();
        for (int sample = 0; sample < COLD_LOAD_SAMPLES; sample++) {
            service.clearDerivedCache();
            Timed<ProjectDocumentIndexStatus> timed = backgroundTimed(() -> service.status(indicator()));
            assertEquals(ProjectDocumentIndexStatus.IndexState.READY, timed.value().indexState());
            assertEquals(ENTRY_COUNT, timed.value().entryCount().intValue());
            coldLoads.add(timed.elapsedMillis());
        }

        ProjectDocumentQuery query = new ProjectDocumentQuery("common endpoint", null, null, null, 20, null);
        ProjectDocumentQueryResult warmup = background(() -> service.search(query, indicator()));
        assertEquals(ENTRY_COUNT, warmup.totalMatched());
        for (int sample = 0; sample < WARM_SEARCH_SAMPLES; sample++) {
            Timed<ProjectDocumentQueryResult> timed = backgroundTimed(() -> service.search(query, indicator()));
            assertEquals(ENTRY_COUNT, timed.value().totalMatched());
            warmSearches.add(timed.elapsedMillis());
        }

        CancellationSummary warmCancellations = collectCancellationSamples(query, false);
        CancellationSummary coldCancellations = collectCancellationSamples(query, true);

        Map<String, Object> report = buildReport(coldLoads, warmSearches, warmCancellations, coldCancellations);
        String json = GSON.toJson(report);
        Path reportPath = Path.of("build/reports/project-document-store/performance.json")
                .toAbsolutePath().normalize();
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, json + "\n", StandardCharsets.UTF_8);
        System.out.println("DOC_VIEW_STORE_PERFORMANCE " + json.replace("\n", System.lineSeparator()));

        assertEquals("light-fixture", report.get("evidenceScope"));
        assertEquals(ENTRY_COUNT, ((Number) ((Map<?, ?>) report.get("fixture")).get("entries")).intValue());
        assertEquals("The report path is included in standard output for CI collection",
                reportPath.toString(), report.get("reportPath"));
    }

    public void testParseStageCancellationSamples() throws Exception {
        byte[] largeIndex = createParseCancellationIndex();
        assertTrue("The valid parser fixture must approach the 32 MiB limit",
                largeIndex.length >= ProjectDocumentFormat.MAX_INDEX_BYTES * 90L / 100L);
        assertTrue("The valid parser fixture must stay within the index byte limit",
                largeIndex.length <= ProjectDocumentFormat.MAX_INDEX_BYTES);
        Files.createDirectories(documentRoot);
        Files.write(documentRoot.resolve("index.json"), largeIndex);

        List<Double> latencies = new ArrayList<>();
        List<String> observedFrames = new ArrayList<>();
        List<String> unobservedOutcomes = new ArrayList<>();
        int attempts = 0;
        int unobservedParseStage = 0;
        int completedBeforeCancel = 0;
        int cancellationMissed = 0;
        while (latencies.size() < PARSE_CANCELLATION_SAMPLES
                && attempts < MAX_PARSE_CANCELLATION_ATTEMPTS) {
            attempts++;
            service.clearDerivedCache();
            ParseCancellationAttempt sample = cancelDuringFormatRead();
            if (sample.parseFrame() == null) {
                unobservedParseStage++;
                unobservedOutcomes.add(sample.completionCode());
            } else {
                observedFrames.add(sample.parseFrame());
            }
            if (sample.completedBeforeCancel()) {
                completedBeforeCancel++;
            } else if (sample.cancellationMissed()) {
                cancellationMissed++;
            } else if (sample.cancelled()) {
                latencies.add(sample.cancelLatencyMillis());
            }
        }

        Double p95 = percentile(latencies, 0.95);
        Path reportPath = Path.of("build/reports/project-document-store/parse-cancellation.json")
                .toAbsolutePath().normalize();
        Map<String, Object> cancellation = metric(latencies, 500.0);
        cancellation.put("parseStageObserved", latencies.size() == PARSE_CANCELLATION_SAMPLES);
        cancellation.put("observedParseFrames", List.copyOf(observedFrames));
        cancellation.put("attempts", attempts);
        cancellation.put("unobservedParseStageAttempts", unobservedParseStage);
        cancellation.put("unobservedAttemptOutcomes", List.copyOf(unobservedOutcomes));
        cancellation.put("completedBeforeCancel", completedBeforeCancel);
        cancellation.put("cancellationMissed", cancellationMissed);
        cancellation.put("cancelLatencyP95Millis", p95);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("evidenceScope", "light-fixture");
        report.put("nativeIdeValidated", false);
        report.put("measuredAtUtc", Instant.now().toString());
        report.put("reportPath", reportPath.toString());
        report.put("environment", Map.of(
                "os", System.getProperty("os.name", "unknown"),
                "osVersion", System.getProperty("os.version", "unknown"),
                "architecture", System.getProperty("os.arch", "unknown"),
                "javaVersion", System.getProperty("java.version", "unknown"),
                "javaVendor", System.getProperty("java.vendor", "unknown"),
                "ideaBuild", ideaBuild(),
                "availableProcessors", Runtime.getRuntime().availableProcessors()));
        report.put("fixture", Map.of(
                "entries", PARSE_FIXTURE_ENTRIES,
                "descriptionCodePointsPerEntry", PARSE_FIXTURE_DESCRIPTION_CHARS,
                "indexBytes", largeIndex.length,
                "indexByteLimit", ProjectDocumentFormat.MAX_INDEX_BYTES,
                "kind", "valid near-limit index-only fixture; status queries read no detail pages"));
        report.put("parseStageCancellation", cancellation);
        Files.createDirectories(reportPath.getParent());
        Files.writeString(reportPath, GSON.toJson(report) + "\n", StandardCharsets.UTF_8);
        System.out.println("DOC_VIEW_STORE_PARSE_CANCELLATION "
                + GSON.toJson(report).replace("\n", System.lineSeparator()));

        assertEquals("Five samples must observe ProjectDocumentFormat.readIndex before cancellation; "
                        + "unobserved parsing does not count as passing",
                PARSE_CANCELLATION_SAMPLES, latencies.size());
        assertTrue("The parse-stage cancellation p95 must meet the 500 ms target: " + p95,
                p95 != null && p95 <= 500.0);
    }

    private ParseCancellationAttempt cancelDuringFormatRead() throws Exception {
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        CountDownLatch started = new CountDownLatch(1);
        AtomicReference<Thread> workerThread = new AtomicReference<>();
        CompletableFuture<ProjectDocumentIndexStatus> finished = new CompletableFuture<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            workerThread.set(Thread.currentThread());
            started.countDown();
            try {
                finished.complete(service.status(indicator));
            } catch (Throwable failure) {
                finished.completeExceptionally(failure);
            }
        });
        assertTrue("The parse-stage query worker should start", started.await(5, TimeUnit.SECONDS));

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        String parseFrame = null;
        while (System.nanoTime() < deadline && !finished.isDone()) {
            Thread worker = workerThread.get();
            if (worker != null) {
                parseFrame = formatReadIndexFrame(worker);
                if (parseFrame != null) {
                    break;
                }
            }
            LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(100));
        }
        if (parseFrame == null) {
            if (!finished.isDone()) {
                indicator.cancel();
            }
            return ParseCancellationAttempt.unobserved(awaitParseQuery(finished));
        }
        if (finished.isDone()) {
            return ParseCancellationAttempt.completedBeforeCancel(parseFrame, awaitParseQuery(finished));
        }

        long cancellationRequestedAt = System.nanoTime();
        indicator.cancel();
        try {
            finished.get(20, TimeUnit.SECONDS);
            return ParseCancellationAttempt.missed(parseFrame, "completed");
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof ProjectDocumentQueryException queryException
                    && queryException.code() == ProjectDocumentQueryException.Code.CANCELLED) {
                double latencyMillis = (System.nanoTime() - cancellationRequestedAt) / 1_000_000.0;
                return ParseCancellationAttempt.cancelled(parseFrame, latencyMillis);
            }
            if (cause instanceof ProjectDocumentQueryException queryException
                    && queryException.code() == ProjectDocumentQueryException.Code.TIMEOUT) {
                return ParseCancellationAttempt.missed(parseFrame, queryException.code().name());
            }
            throw rethrow(cause);
        }
    }

    private static String awaitParseQuery(CompletableFuture<ProjectDocumentIndexStatus> finished) throws Exception {
        try {
            finished.get(20, TimeUnit.SECONDS);
            return "completed";
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof ProjectDocumentQueryException queryException) {
                if (queryException.code() == ProjectDocumentQueryException.Code.CANCELLED
                        || queryException.code() == ProjectDocumentQueryException.Code.TIMEOUT) {
                    return queryException.code().name();
                }
            }
            throw rethrow(cause);
        }
    }

    private static String formatReadIndexFrame(Thread worker) {
        for (StackTraceElement frame : worker.getStackTrace()) {
            if (ProjectDocumentFormat.class.getName().equals(frame.getClassName())
                    && "readIndex".equals(frame.getMethodName())) {
                return frame.getClassName() + "#" + frame.getMethodName();
            }
        }
        return null;
    }

    private static byte[] createParseCancellationIndex() {
        String description = "x".repeat(PARSE_FIXTURE_DESCRIPTION_CHARS);
        List<EntryData> entries = new ArrayList<>(PARSE_FIXTURE_ENTRIES);
        for (int index = 0; index < PARSE_FIXTURE_ENTRIES; index++) {
            String suffix = String.format(Locale.ROOT, "%04d", index);
            String sourcePath = "src/main/java/example/LargeEndpoint" + suffix + ".java";
            ProjectDocumentIdentity identity = ProjectDocumentIdentity.of("app", sourcePath,
                    "example.LargeEndpoint" + suffix, "findEndpoint", List.of());
            SourceData source = new SourceData(sourcePath, 1,
                    ProjectDocumentFormat.sha256(sourcePath.getBytes(StandardCharsets.UTF_8)), "ide-document-text");
            IdentityData identityData = new IdentityData(identity.moduleName(), identity.qualifiedClassName(),
                    identity.methodName(), identity.parameterTypes(), identity.displaySignature(),
                    identity.canonicalForm());
            String markdownPath = "apis/" + identity.pageFileName();
            String detailPath = markdownPath.substring(0, markdownPath.length() - 3) + ".json";
            entries.add(new EntryData(identity.sha256(), identityData, "Large endpoint " + suffix, description,
                    "spring", "GET", "/large/" + suffix, List.of("parse-cancellation"), source,
                    new FileData(markdownPath, "0".repeat(64)), new FileData(detailPath, "1".repeat(64))));
        }
        entries.sort(Comparator.comparing(entry -> entry.identity().canonicalForm()));
        byte[] markdownIndex = "# Parse cancellation fixture\n".getBytes(StandardCharsets.UTF_8);
        IndexData unsigned = new IndexData("0".repeat(64), Instant.now().toString(), "1.4.0",
                "2".repeat(64), false,
                new FileData("index.md", ProjectDocumentFormat.sha256(markdownIndex)), entries);
        IndexData index = new IndexData(ProjectDocumentFormat.computeSnapshotId(unsigned), unsigned.generatedAt(),
                unsigned.producerVersion(), unsigned.generatorFingerprint(), unsigned.hasUnsavedDocuments(),
                unsigned.markdownIndex(), unsigned.entries());
        return ProjectDocumentFormat.writeIndex(index);
    }

    private CancellationSample cancelOneQuery(ProjectDocumentQuery query) throws Exception {
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        CountDownLatch started = new CountDownLatch(1);
        CompletableFuture<Void> finished = new CompletableFuture<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            started.countDown();
            try {
                service.search(query, indicator);
                finished.complete(null);
            } catch (Throwable failure) {
                finished.completeExceptionally(failure);
            }
        });
        assertTrue("The query worker should start", started.await(5, TimeUnit.SECONDS));
        Thread.sleep(1);
        if (finished.isDone()) {
            try {
                finished.get();
                return CancellationSample.completed();
            } catch (ExecutionException exception) {
                throw rethrow(exception.getCause());
            }
        }
        long cancelRequestedAt = System.nanoTime();
        indicator.cancel();
        try {
            finished.get(5, TimeUnit.SECONDS);
            return CancellationSample.missed();
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof ProjectDocumentQueryException queryException
                    && queryException.code() == ProjectDocumentQueryException.Code.CANCELLED) {
                return CancellationSample.cancelled((System.nanoTime() - cancelRequestedAt) / 1_000_000.0);
            }
            throw rethrow(cause);
        }
    }

    private CancellationSummary collectCancellationSamples(ProjectDocumentQuery query, boolean clearBeforeQuery)
            throws Exception {
        List<Double> samples = new ArrayList<>();
        int completedBeforeSignal = 0;
        int missedCancellation = 0;
        int attempts = 0;
        while (samples.size() < CANCELLATION_SAMPLES && attempts < MAX_CANCELLATION_ATTEMPTS) {
            attempts++;
            if (clearBeforeQuery) {
                service.clearDerivedCache();
            }
            CancellationSample sample = cancelOneQuery(query);
            if (sample.completedBeforeSignal()) {
                completedBeforeSignal++;
            } else if (sample.cancellationMissed()) {
                missedCancellation++;
            } else {
                samples.add(sample.cancelLatencyMillis());
            }
        }
        return new CancellationSummary(List.copyOf(samples), completedBeforeSignal, missedCancellation, attempts);
    }

    private void writeFixtureIndex() throws Exception {
        List<EntryData> entries = new ArrayList<>(ENTRY_COUNT);
        for (int index = 0; index < ENTRY_COUNT; index++) {
            String suffix = String.format(Locale.ROOT, "%05d", index);
            String sourcePath = "src/main/java/example/Endpoint" + suffix + ".java";
            String qualifiedClass = "example.Endpoint" + suffix;
            ProjectDocumentIdentity identity = ProjectDocumentIdentity.of("app", sourcePath, qualifiedClass,
                    "findEndpoint", List.of("java.lang.String"));
            SourceData source = new SourceData(sourcePath, 1,
                    ProjectDocumentFormat.sha256(sourcePath.getBytes(StandardCharsets.UTF_8)), "ide-document-text");
            IdentityData identityData = new IdentityData(identity.moduleName(), identity.qualifiedClassName(),
                    identity.methodName(), identity.parameterTypes(), identity.displaySignature(),
                    identity.canonicalForm());
            String markdownPath = "apis/" + identity.pageFileName();
            String detailPath = markdownPath.substring(0, markdownPath.length() - 3) + ".json";
            entries.add(new EntryData(identity.sha256(), identityData, "Endpoint " + suffix,
                    "Common project document performance fixture", "spring", "GET", "/api/" + suffix,
                    List.of("performance", "fixture"), source,
                    new FileData(markdownPath, "0".repeat(64)), new FileData(detailPath, "1".repeat(64))));
        }
        entries.sort(Comparator.comparing(entry -> entry.identity().canonicalForm()));
        byte[] indexMarkdown = "# Performance fixture index\n".getBytes(StandardCharsets.UTF_8);
        IndexData unsigned = new IndexData("0".repeat(64), Instant.now().toString(), "1.4.0",
                "2".repeat(64), false,
                new FileData("index.md", ProjectDocumentFormat.sha256(indexMarkdown)), entries);
        IndexData index = new IndexData(ProjectDocumentFormat.computeSnapshotId(unsigned), unsigned.generatedAt(),
                unsigned.producerVersion(), unsigned.generatorFingerprint(), unsigned.hasUnsavedDocuments(),
                unsigned.markdownIndex(), unsigned.entries());
        Files.createDirectories(documentRoot);
        Files.write(documentRoot.resolve("index.md"), indexMarkdown);
        byte[] indexBytes = ProjectDocumentFormat.writeIndex(index);
        Files.write(documentRoot.resolve("index.json"), indexBytes);
        fixtureIndexBytes = indexBytes.length;
    }

    private int fixtureIndexBytes;

    private Map<String, Object> buildReport(List<Double> coldLoads,
                                            List<Double> warmSearches,
                                            CancellationSummary warmCancellations,
                                            CancellationSummary coldCancellations) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("evidenceScope", "light-fixture");
        report.put("nativeIdeValidated", false);
        report.put("measuredAtUtc", Instant.now().toString());
        report.put("reportPath", Path.of("build/reports/project-document-store/performance.json")
                .toAbsolutePath().normalize().toString());
        report.put("environment", Map.of(
                "os", System.getProperty("os.name", "unknown"),
                "osVersion", System.getProperty("os.version", "unknown"),
                "architecture", System.getProperty("os.arch", "unknown"),
                "javaVersion", System.getProperty("java.version", "unknown"),
                "javaVendor", System.getProperty("java.vendor", "unknown"),
                "ideaBuild", ideaBuild(),
                "availableProcessors", Runtime.getRuntime().availableProcessors()));
        report.put("fixture", Map.of(
                "entries", ENTRY_COUNT,
                "indexBytes", fixtureIndexBytes,
                "kind", "index-only summary fixture; detail pages are not read by these measurements"));
        report.put("warmSearch", metric(warmSearches, 200.0));
        report.put("coldIndexLoad", metric(coldLoads, 2_000.0));
        List<Double> allCancellationLatencies = new ArrayList<>(warmCancellations.samplesMillis());
        allCancellationLatencies.addAll(coldCancellations.samplesMillis());
        Map<String, Object> cancellation = metric(allCancellationLatencies, 500.0);
        cancellation.put("warmCache", cancellationMetric(warmCancellations));
        cancellation.put("coldCache", cancellationMetric(coldCancellations));
        report.put("queryCancellation", cancellation);
        return report;
    }

    private static Map<String, Object> cancellationMetric(CancellationSummary summary) {
        Map<String, Object> metric = metric(summary.samplesMillis(), 500.0);
        metric.put("attempts", summary.attempts());
        metric.put("completedBeforeSignal", summary.completedBeforeSignal());
        metric.put("missedAfterSignal", summary.missedCancellation());
        return metric;
    }

    private static Map<String, Object> metric(List<Double> samples, double targetMillis) {
        Map<String, Object> metric = new LinkedHashMap<>();
        metric.put("targetP95Millis", targetMillis);
        metric.put("sampleCount", samples.size());
        metric.put("samplesMillis", List.copyOf(samples));
        metric.put("p50Millis", percentile(samples, 0.50));
        metric.put("p95Millis", percentile(samples, 0.95));
        Double p95 = percentile(samples, 0.95);
        metric.put("targetMetInThisFixture", p95 != null && p95 <= targetMillis);
        return metric;
    }

    private static Double percentile(List<Double> values, double percentile) {
        if (values.isEmpty()) {
            return null;
        }
        List<Double> sorted = values.stream().sorted().toList();
        int index = Math.max(0, (int) Math.ceil(percentile * sorted.size()) - 1);
        return Math.round(sorted.get(index) * 1000.0) / 1000.0;
    }

    private static String ideaBuild() {
        try {
            return ApplicationInfo.getInstance().getBuild().asString();
        } catch (RuntimeException unavailable) {
            return System.getProperty("idea.build.number", "unknown");
        }
    }

    private static ProgressIndicator indicator() {
        return new EmptyProgressIndicator();
    }

    private static <T> Timed<T> backgroundTimed(ThrowingSupplier<T> supplier) throws Exception {
        CompletableFuture<Timed<T>> future = new CompletableFuture<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            long started = System.nanoTime();
            try {
                future.complete(new Timed<>(supplier.get(), (System.nanoTime() - started) / 1_000_000.0));
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        try {
            return future.get(15, TimeUnit.SECONDS);
        } catch (ExecutionException exception) {
            throw rethrow(exception.getCause());
        }
    }

    private static <T> T background(ThrowingSupplier<T> supplier) throws Exception {
        return backgroundTimed(supplier).value();
    }

    private static Exception rethrow(Throwable failure) throws Exception {
        if (failure instanceof Exception exception) {
            return exception;
        }
        if (failure instanceof Error error) {
            throw error;
        }
        throw new RuntimeException(failure);
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

    private record Timed<T>(T value, double elapsedMillis) {
    }

    private record CancellationSample(boolean completedBeforeSignal,
                                      boolean cancellationMissed,
                                      double cancelLatencyMillis) {
        static CancellationSample completed() {
            return new CancellationSample(true, false, 0.0);
        }

        static CancellationSample missed() {
            return new CancellationSample(false, true, 0.0);
        }

        static CancellationSample cancelled(double latencyMillis) {
            return new CancellationSample(false, false, latencyMillis);
        }
    }

    private record CancellationSummary(List<Double> samplesMillis,
                                       int completedBeforeSignal,
                                       int missedCancellation,
                                       int attempts) {
    }

    private record ParseCancellationAttempt(boolean cancelled,
                                            boolean completedBeforeCancel,
                                            boolean cancellationMissed,
                                            String parseFrame,
                                            String completionCode,
                                            double cancelLatencyMillis) {
        static ParseCancellationAttempt unobserved(String completionCode) {
            return new ParseCancellationAttempt(false, false, false, null, completionCode, 0.0);
        }

        static ParseCancellationAttempt completedBeforeCancel(String parseFrame, String completionCode) {
            return new ParseCancellationAttempt(false, true, false, parseFrame, completionCode, 0.0);
        }

        static ParseCancellationAttempt missed(String parseFrame, String completionCode) {
            return new ParseCancellationAttempt(false, false, true, parseFrame, completionCode, 0.0);
        }

        static ParseCancellationAttempt cancelled(String parseFrame, double latencyMillis) {
            return new ParseCancellationAttempt(true, false, false, parseFrame, "CANCELLED", latencyMillis);
        }
    }
}

package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.service.DtoSchemaCacheService;
import com.liuzhihang.doc.view.service.impl.DubboDocViewServiceImpl;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.ui.window.RootNode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Test-only baselines for generation, rendering, directory scanning, retention and cancellation. */
public final class GenerationPerformanceBaselineTest extends LightJavaCodeInsightFixtureTestCase {

    private static final int SAMPLE_COUNT = 7;
    private Boolean originalIncludeNormalInterface;
    private String originalPrefixSymbol1;
    private String originalDubboTemplate;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installSpringWeb(myFixture);
        AnnotationStubs.installDubbo(myFixture);
        Settings settings = Settings.getInstance(getProject());
        originalIncludeNormalInterface = settings.getIncludeNormalInterface();
        originalPrefixSymbol1 = settings.getPrefixSymbol1();
        originalDubboTemplate = TemplateSettings.getInstance(getProject()).getDubboTemplate();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Settings settings = Settings.getInstance(getProject());
            settings.setIncludeNormalInterface(originalIncludeNormalInterface);
            settings.setPrefixSymbol1(originalPrefixSymbol1);
            TemplateSettings.getInstance(getProject()).setDubboTemplate(originalDubboTemplate);
            DtoSchemaCacheService cacheService = DtoSchemaCacheService.getInstance(getProject());
            if (cacheService != null) {
                cacheService.clear();
            }
        } finally {
            super.tearDown();
        }
    }

    public void testSpringColdWarmGenerationAndMarkdownBaseline() {
        recordEnvironment();
        List<Workload> workloads = List.of(
                new Workload("small", 4, 3, 1),
                new Workload("medium", 12, 10, 3),
                new Workload("large", 30, 24, 6)
        );

        for (Workload workload : workloads) {
            PsiClass controller = addSpringFixture(workload);
            measure("spring", workload, controller, new SpringDocViewServiceImpl());
        }
    }

    public void testDubboColdWarmGenerationAndMarkdownBaseline() {
        recordEnvironment();
        Workload workload = new Workload("dubbo-medium", 16, 12, 4);
        PsiClass serviceClass = addDubboFixture(workload);
        measure("dubbo", workload, serviceClass, new DubboDocViewServiceImpl());
    }

    public void testDirectoryScanBaselineSeparatesNormalInterfaceSetting() throws Exception {
        addScanFixture();
        commitAndWaitForIndexes();

        Settings settings = Settings.getInstance(getProject());
        settings.setIncludeNormalInterface(false);
        ScanSnapshot annotatedOnly = measureScan("annotated-only");
        assertTrue(annotatedOnly.classNames().contains("performance.scan.ApiController0"));
        assertTrue(annotatedOnly.classNames().contains("performance.scan.DubboApi0"));
        assertFalse(annotatedOnly.classNames().contains("performance.scan.PlainApi0"));

        settings.setIncludeNormalInterface(true);
        ScanSnapshot includingNormalInterfaces = measureScan("including-normal-interfaces");
        assertTrue(includingNormalInterfaces.classNames().contains("performance.scan.PlainApi0"));
        assertEquals(annotatedOnly.moduleCount(), includingNormalInterfaces.moduleCount());
    }

    public void testSchemaCacheRetainsOnlyCurrentPsiSettingsAndTemplateEpoch() throws Exception {
        DtoSchemaCacheService cacheService = DtoSchemaCacheService.getInstance(getProject());
        cacheService.clear();
        Workload workload = new Workload("cache-epoch", 1, 3, 2);
        PsiClass serviceClass = addDubboFixture(workload);
        DocViewService service = new DubboDocViewServiceImpl();
        List<Integer> retainedEntries = new ArrayList<>();
        String expectedSchema = null;

        retainedEntries.add(generateAndCount(service, serviceClass, cacheService, expectedSchema));
        expectedSchema = schemaFingerprint(build(service, serviceClass).get(0).getRespBody());

        myFixture.addClass("""
                package performance.cache;

                public class PsiEpochMarker {
                    private String marker;
                }
                """);
        retainedEntries.add(generateAndCount(service, serviceClass, cacheService, expectedSchema));

        Settings.getInstance(getProject()).setPrefixSymbol1("epoch-setting");
        retainedEntries.add(generateAndCount(service, serviceClass, cacheService, expectedSchema));

        TemplateSettings templates = TemplateSettings.getInstance(getProject());
        templates.setDubboTemplate(templates.getDubboTemplate() + "\n## Epoch marker\n");
        retainedEntries.add(generateAndCount(service, serviceClass, cacheService, expectedSchema));

        System.out.printf(Locale.ROOT,
                "GENERATION_PERF_CACHE epochs=psi,settings,template entries=%s bodyNodes=%d%n",
                retainedEntries, cachedBodyNodeCount(cacheService));
        assertEquals("Only the current PSI/settings/template epoch should remain cached",
                List.of(1, 1, 1, 1), retainedEntries);
    }

    public void testLateOldEpochCacheWriteCannotRestoreAnExpiredSnapshot() {
        DtoSchemaCacheService cacheService = DtoSchemaCacheService.getInstance(getProject());
        cacheService.clear();
        DtoSchemaCacheService.DtoSchemaCacheKey oldKey = cacheService.key("response", "performance.cache.Late");
        Body oldSnapshot = new Body();
        oldSnapshot.setName("old");
        cacheService.putBody(oldKey, oldSnapshot);

        myFixture.addClass("""
                package performance.cache;

                public class LateEpochMarker {
                    private String marker;
                }
                """);
        DtoSchemaCacheService.DtoSchemaCacheKey currentKey = cacheService.key("response", "performance.cache.Late");
        Body currentSnapshot = new Body();
        currentSnapshot.setName("current");
        cacheService.putBody(currentKey, currentSnapshot);

        // Model a generation that captured oldKey before the PSI edit and finishes after the new epoch.
        cacheService.putBody(oldKey, oldSnapshot);
        System.out.printf(Locale.ROOT, "GENERATION_PERF_LATE_WRITE entries=%d oldSnapshotPresent=%s%n",
                cacheEntryCount(cacheService), cacheService.getBody(oldKey) != null);

        assertNull("A late write from an expired epoch must be rejected", cacheService.getBody(oldKey));
        assertEquals("Only the current epoch snapshot may remain", 1, cacheEntryCount(cacheService));
        assertEquals("current", cacheService.getBody(currentKey).getName());
    }

    public void testPreCancelledWholeClassGenerationDoesNotReturnPartialDocs() throws Exception {
        Workload workload = new Workload("cancel", 12, 8, 3);
        PsiClass serviceClass = addDubboFixture(workload);
        DocViewService service = new DubboDocViewServiceImpl();
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        Future<Boolean> cancelled = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ProgressManager.getInstance().runProcess(() ->
                        ApplicationManager.getApplication().runReadAction((Computable<List<DocView>>) () -> {
                            // Cancel after runProcess has installed this indicator, immediately before generation.
                            indicator.cancel();
                            return service.buildDoc(serviceClass, null);
                        }), indicator);
                return false;
            } catch (ProcessCanceledException expected) {
                return true;
            }
        });

        assertTrue("A pre-cancelled generation must stop before returning a document list",
                cancelled.get(10, TimeUnit.SECONDS));
    }

    public void testSpringCancellationAfterFirstMethodDoesNotReturnPartialDocs() throws Exception {
        PsiClass controller = addSpringFixture(new Workload("cancel-mid-spring", 4, 3, 1));
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        AtomicInteger builtMethods = new AtomicInteger();
        DocViewService service = new SpringDocViewServiceImpl() {
            @Override
            public DocView buildClassMethodDoc(PsiClass psiClass, PsiMethod method) {
                DocView result = super.buildClassMethodDoc(psiClass, method);
                builtMethods.incrementAndGet();
                indicator.cancel();
                return result;
            }
        };
        assertCancelledAfterOneMethod(service, controller, indicator, builtMethods);
    }

    public void testDubboCancellationAfterFirstMethodDoesNotReturnPartialDocs() throws Exception {
        PsiClass serviceClass = addDubboFixture(new Workload("cancel-mid-dubbo", 4, 3, 1));
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        AtomicInteger builtMethods = new AtomicInteger();
        DocViewService service = new DubboDocViewServiceImpl() {
            @Override
            public DocView buildClassMethodDoc(PsiClass psiClass, PsiMethod method) {
                DocView result = super.buildClassMethodDoc(psiClass, method);
                builtMethods.incrementAndGet();
                indicator.cancel();
                return result;
            }
        };
        assertCancelledAfterOneMethod(service, serviceClass, indicator, builtMethods);
    }

    private void assertCancelledAfterOneMethod(DocViewService service, PsiClass psiClass,
                                               EmptyProgressIndicator indicator,
                                               AtomicInteger builtMethods) throws Exception {
        Future<Boolean> cancelled = ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ProgressManager.getInstance().runProcess(() ->
                        ApplicationManager.getApplication().runReadAction((Computable<List<DocView>>) () ->
                                service.buildDoc(psiClass, null)), indicator);
                return false;
            } catch (ProcessCanceledException expected) {
                return true;
            }
        });
        assertTrue("Cancellation must prevent a partial list from escaping",
                cancelled.get(10, TimeUnit.SECONDS));
        assertEquals("Only the first real method may be built", 1, builtMethods.get());
    }

    private void measure(String framework, Workload workload, PsiClass targetClass, DocViewService service) {
        DtoSchemaCacheService cacheService = DtoSchemaCacheService.getInstance(getProject());
        List<String> expectedMarkdown = null;
        List<Long> coldBuildNanos = new ArrayList<>();
        List<Long> warmBuildNanos = new ArrayList<>();
        List<Long> markdownNanos = new ArrayList<>();

        cacheService.clear();
        List<DocView> warmupDocs = build(service, targetClass);
        renderAll(warmupDocs);

        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            cacheService.clear(); // Deliberately outside the timed section.
            long started = System.nanoTime();
            List<DocView> docs = build(service, targetClass);
            coldBuildNanos.add(System.nanoTime() - started);
            assertEquals(workload.methodCount(), docs.size());

            List<String> markdown = renderAll(docs);
            if (expectedMarkdown == null) {
                expectedMarkdown = markdown;
            } else {
                assertEquals("Cold generation output changed between samples", expectedMarkdown, markdown);
            }
        }

        List<String> coldExpectedMarkdown = expectedMarkdown;
        cacheService.clear();
        List<DocView> warmDocs = build(service, targetClass);
        expectedMarkdown = renderAll(warmDocs);
        assertEquals("Warm schema cache changed the generated Markdown", coldExpectedMarkdown, expectedMarkdown);
        long bodyNodes = warmDocs.stream().map(DocView::getRespBody).mapToLong(this::countNodes).sum();

        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            long started = System.nanoTime();
            List<DocView> docs = build(service, targetClass);
            warmBuildNanos.add(System.nanoTime() - started);
            assertEquals(workload.methodCount(), docs.size());

            started = System.nanoTime();
            List<String> markdown = renderAll(docs);
            markdownNanos.add(System.nanoTime() - started);
            assertEquals("Warm generation/rendering changed the generated document", expectedMarkdown, markdown);
        }

        System.out.printf(Locale.ROOT,
                "GENERATION_PERF workload=%s framework=%s methods=%d fieldsPerDto=%d nestedLevels=%d bodyNodes=%d " +
                        "coldBuildP50Ms=%.3f coldBuildP95Ms=%.3f warmBuildP50Ms=%.3f warmBuildP95Ms=%.3f " +
                        "markdownP50Ms=%.3f markdownP95Ms=%.3f markdownChars=%d cacheEntries=%d%n",
                workload.name(), framework, workload.methodCount(), workload.fieldsPerLevel(), workload.nestedLevels(),
                bodyNodes, percentileMillis(coldBuildNanos, 0.50), percentileMillis(coldBuildNanos, 0.95),
                percentileMillis(warmBuildNanos, 0.50), percentileMillis(warmBuildNanos, 0.95),
                percentileMillis(markdownNanos, 0.50), percentileMillis(markdownNanos, 0.95),
                expectedMarkdown.stream().mapToInt(String::length).sum(), cacheEntryCount(cacheService));
    }

    private List<DocView> build(DocViewService service, PsiClass targetClass) {
        return ReadAction.computeBlocking(() -> service.buildDoc(targetClass, null));
    }

    private List<String> renderAll(List<DocView> docs) {
        return ReadAction.computeBlocking(() -> docs.stream()
                .map(doc -> DocViewData.markdownText(getProject(), doc))
                .toList());
    }

    private PsiClass addSpringFixture(Workload workload) {
        String suffix = identifierSuffix(workload.name());
        String packageName = "performance.spring." + suffix.toLowerCase(Locale.ROOT);
        addPayloadTypes(packageName, "Payload" + suffix, workload.fieldsPerLevel(), workload.nestedLevels());

        String className = "Controller" + suffix;
        String payloadName = "Payload" + suffix;
        StringBuilder source = new StringBuilder("package ").append(packageName).append(";\n")
                .append("@org.springframework.web.bind.annotation.RestController\n")
                .append("public class ").append(className).append(" {\n");
        for (int index = 0; index < workload.methodCount(); index++) {
            source.append("  @org.springframework.web.bind.annotation.GetMapping(\"/")
                    .append(workload.name()).append('/').append(index).append("\")\n")
                    .append("  public ").append(payloadName).append(" operation").append(index)
                    .append("() { return null; }\n");
        }
        source.append("}\n");
        return myFixture.addClass(source.toString());
    }

    private PsiClass addDubboFixture(Workload workload) {
        String suffix = identifierSuffix(workload.name());
        String packageName = "performance.dubbo." + suffix.toLowerCase(Locale.ROOT);
        addPayloadTypes(packageName, "Payload" + suffix, workload.fieldsPerLevel(), workload.nestedLevels());

        String className = "DubboApi" + suffix;
        String payloadName = "Payload" + suffix;
        StringBuilder source = new StringBuilder("package ").append(packageName).append(";\n")
                .append("@org.apache.dubbo.config.annotation.DubboService\n")
                .append("public interface ").append(className).append(" {\n");
        for (int index = 0; index < workload.methodCount(); index++) {
            source.append("  ").append(payloadName).append(" operation").append(index).append("();\n");
        }
        source.append("}\n");
        return myFixture.addClass(source.toString());
    }

    private void addPayloadTypes(String packageName, String payloadName, int fieldsPerLevel, int nestedLevels) {
        for (int level = 0; level <= nestedLevels; level++) {
            String typeName = level == 0 ? payloadName : "Level" + level + payloadName;
            StringBuilder source = new StringBuilder("package ").append(packageName).append(";\n")
                    .append("public class ").append(typeName).append(" {\n");
            for (int field = 0; field < fieldsPerLevel; field++) {
                source.append("  private String field").append(level).append('_').append(field).append(";\n");
            }
            if (level < nestedLevels) {
                source.append("  private Level").append(level + 1).append(payloadName).append(" nested;\n");
            }
            source.append("}\n");
            myFixture.addClass(source.toString());
        }
    }

    private String identifierSuffix(String workloadName) {
        StringBuilder suffix = new StringBuilder();
        for (String part : workloadName.split("[^A-Za-z0-9_$]+")) {
            if (!part.isEmpty()) {
                suffix.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }
        return suffix.toString();
    }

    private void addScanFixture() {
        for (int index = 0; index < 4; index++) {
            myFixture.addClass("""
                    package performance.scan;

                    @org.springframework.web.bind.annotation.RestController
                    public class ApiController%d {
                        @org.springframework.web.bind.annotation.GetMapping("/scan/%d")
                        public String get%d() { return ""; }
                    }
                    """.formatted(index, index, index));
            myFixture.addClass("""
                    package performance.scan;

                    @org.apache.dubbo.config.annotation.DubboService
                    public interface DubboApi%d {
                        String get%d();
                    }
                    """.formatted(index, index));
        }
        for (int index = 0; index < 12; index++) {
            myFixture.addClass("""
                    package performance.scan;

                    public interface PlainApi%d {
                        String get%d();
                    }
                    """.formatted(index, index));
        }
    }

    private ScanSnapshot measureScan(String label) throws Exception {
        List<Long> samples = new ArrayList<>();
        ScanSnapshot expected = null;
        for (int sample = 0; sample < SAMPLE_COUNT; sample++) {
            long started = System.nanoTime();
            ScanSnapshot current = scanOnce();
            samples.add(System.nanoTime() - started);
            if (expected == null) {
                expected = current;
            } else {
                assertEquals("Directory membership/order changed between refreshes", expected, current);
            }
        }
        assertNotNull(expected);
        System.out.printf(Locale.ROOT,
                "GENERATION_PERF_SCAN setting=%s modules=%d classes=%d methods=%d classNames=%s " +
                        "refreshP50Ms=%.3f refreshP95Ms=%.3f%n",
                label, expected.moduleCount(), expected.classNames().size(), expected.methodCount(),
                expected.classNames(), percentileMillis(samples, 0.50), percentileMillis(samples, 0.95));
        return expected;
    }

    private ScanSnapshot scanOnce() throws Exception {
        return ReadAction.computeBlocking(() -> {
            RootNode root = new RootNode();
            root.updateNode(getProject());
            try {
                List<?> modules = listField(root, "moduleNodes");
                List<String> classNames = new ArrayList<>();
                int methodCount = 0;
                for (Object module : modules) {
                    for (Object classNode : listField(module, "classNodes")) {
                        PsiClass psiClass = (PsiClass) field(classNode, "psiClass");
                        if (psiClass.getQualifiedName() != null) {
                            classNames.add(psiClass.getQualifiedName());
                        }
                        methodCount += listField(classNode, "methodNodes").size();
                    }
                }
                return new ScanSnapshot(modules.size(), classNames, methodCount);
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    private void commitAndWaitForIndexes() {
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
    }

    private int generateAndCount(DocViewService service, PsiClass targetClass,
                                 DtoSchemaCacheService cacheService, String expectedSchema) {
        List<DocView> docs = build(service, targetClass);
        assertEquals(1, docs.size());
        String currentSchema = schemaFingerprint(docs.get(0).getRespBody());
        if (expectedSchema != null) {
            assertEquals("PSI/settings/template epoch changes must not alter this DTO schema", expectedSchema, currentSchema);
        }
        return cacheEntryCount(cacheService);
    }

    private String schemaFingerprint(Body root) {
        if (root == null) {
            return "<no-body>";
        }
        StringBuilder result = new StringBuilder();
        appendSchema(root, result, 0);
        return result.toString();
    }

    private void appendSchema(Body node, StringBuilder result, int depth) {
        result.append("  ".repeat(depth)).append(node.getName()).append(':').append(node.getType()).append('\n');
        for (Body child : node.getChildList()) {
            appendSchema(child, result, depth + 1);
        }
    }

    private long countNodes(Body root) {
        if (root == null) {
            return 0;
        }
        return 1 + root.getChildList().stream().mapToLong(this::countNodes).sum();
    }

    private int cacheEntryCount(DtoSchemaCacheService cacheService) {
        return cacheMap(cacheService).size();
    }

    private long cachedBodyNodeCount(DtoSchemaCacheService cacheService) {
        return cacheMap(cacheService).values().stream().map(Body.class::cast).mapToLong(this::countNodes).sum();
    }

    private Map<?, ?> cacheMap(DtoSchemaCacheService cacheService) {
        try {
            Field field = DtoSchemaCacheService.class.getDeclaredField("bodyCache");
            field.setAccessible(true);
            return (Map<?, ?>) field.get(cacheService);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static List<?> listField(Object target, String name) throws ReflectiveOperationException {
        return (List<?>) field(target, name);
    }

    private static Object field(Object target, String name) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private double percentileMillis(List<Long> samples, double percentile) {
        List<Long> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        int index = Math.max(0, (int) Math.ceil(percentile * sorted.size()) - 1);
        return sorted.get(index) / 1_000_000.0;
    }

    private void recordEnvironment() {
        System.out.printf(Locale.ROOT,
                "GENERATION_PERF_ENV ideBuild=%s javaVendor=%s javaVm=%s java=%s processors=%d samples=%d%n",
                ApplicationInfo.getInstance().getBuild().asString(),
                System.getProperty("java.vendor", "unknown"),
                System.getProperty("java.vm.name", "unknown"),
                System.getProperty("java.runtime.version", "unknown"),
                Runtime.getRuntime().availableProcessors(), SAMPLE_COUNT);
    }

    private record Workload(String name, int methodCount, int fieldsPerLevel, int nestedLevels) {
    }

    private record ScanSnapshot(int moduleCount, List<String> classNames, int methodCount) {
    }
}

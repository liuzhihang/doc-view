package com.liuzhihang.doc.view.contract;

import com.intellij.psi.PsiClass;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.dto.DocViewParamData;
import com.liuzhihang.doc.view.service.DtoSchemaCacheService;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;

import java.util.List;

/** Regression coverage for the synthetic collection/map rows reported in issue #96. */
public final class ContainerMarkdownHierarchyTest extends LightJavaCodeInsightFixtureTestCase {

    private Boolean originalSeparate;
    private String originalPrefix1;
    private String originalPrefix2;

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installSpringWeb(myFixture);
        myFixture.addClass("package org.springframework.web.bind.annotation; public @interface RequestBody {}");
        Settings settings = Settings.getInstance(getProject());
        originalSeparate = settings.getSeparateParam();
        originalPrefix1 = settings.getPrefixSymbol1();
        originalPrefix2 = settings.getPrefixSymbol2();
        settings.setSeparateParam(false);
        settings.setPrefixSymbol1("");
        settings.setPrefixSymbol2("-->");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Settings settings = Settings.getInstance(getProject());
            settings.setSeparateParam(originalSeparate);
            settings.setPrefixSymbol1(originalPrefix1);
            settings.setPrefixSymbol2(originalPrefix2);
            DtoSchemaCacheService.getInstance(getProject()).clear();
        } finally {
            super.tearDown();
        }
    }

    public void testNestedCollectionsUseActualFieldDepth() {
        DocViewData data = new DocViewData(getProject(), fixture());
        assertRow(data.getResponseParam(), "directories", "List<Directory>");
        assertRow(data.getResponseParam(), "-->dictList", "List<Dict>");
        assertRow(data.getResponseParam(), "-->-->dictDirectoryId", "Long");
        assertRow(data.getResponseParam(), "-->-->description", "String");
        assertFalse(data.getResponseParam(), data.getResponseParam().contains("element|Directory|"));
        assertFalse(data.getResponseParam(), data.getResponseParam().contains("element|Dict|"));
        // Custom-template data still contains the original parsing tree.
        assertTrue(data.getResponseParamDataList().get(0).getChildList().get(0).isCollection());
    }

    public void testMapsHideSyntheticRowsButRetainRealFieldsWithTheSameNames() {
        String markdown = new DocViewData(getProject(), fixture()).getResponseParam();
        assertRow(markdown, "byId", "Map<String, Directory>");
        assertRow(markdown, "-->dictList", "List<Dict>");
        assertRow(markdown, "-->-->element", "String");
        assertRow(markdown, "-->-->key", "String");
        assertRow(markdown, "-->-->value", "String");
        assertRow(markdown, "labels", "Map<String, String>");
        assertRow(markdown, "names", "List<String>");
        assertFalse(markdown, markdown.contains("|-->key|String|"));
        assertFalse(markdown, markdown.contains("value|Directory|"));
    }

    public void testSeparateTablesUseContainerFieldNames() {
        Settings.getInstance(getProject()).setSeparateParam(true);
        String markdown = new DocViewData(getProject(), fixture()).getResponseParam();
        assertTrue(markdown, markdown.contains("\n- List<Directory> directories\n\n"));
        assertTrue(markdown, markdown.contains("\n- Map<String, Directory> byId\n\n"));
        assertTrue(markdown, markdown.contains("\n- List<Dict> dictList\n\n"));
        assertRow(markdown, "dictDirectoryId", "Long");
        assertRow(markdown, "element", "String");
        assertFalse(markdown, markdown.contains("- Directory element"));
        assertFalse(markdown, markdown.contains("- Directory value"));
        assertFalse(markdown, markdown.contains("- Map<String, String> labels"));
    }

    public void testRequestAndResponseTablesShareTheSameProjection() {
        DocViewData data = new DocViewData(getProject(), fixture());
        assertEquals(data.getResponseParam(), data.getRequestBody());
        assertRow(data.getRequestBody(), "-->dictList", "List<Dict>");
        assertFalse(data.getRequestBody(), data.getRequestBody().contains("element|Directory|"));
    }

    public void testCustomPrefixTracksVisibleDepthWithoutMutatingTreesOrExamples() {
        Settings settings = Settings.getInstance(getProject());
        settings.setPrefixSymbol1("branch:");
        settings.setPrefixSymbol2(" / ");
        DocView doc = fixture();
        String bodyBefore = bodyFingerprint(doc.getRespBody());
        String jsonBefore = doc.getRespExample();
        String templateTreeBefore = dataFingerprint(DocViewData.buildBodyDataList(doc.getRespBody().getChildList()));
        DocViewData data = new DocViewData(getProject(), doc);
        assertRow(data.getResponseParam(), "branch: / dictList", "List<Dict>");
        assertRow(data.getResponseParam(), "branch: /  / description", "String");
        assertEquals(bodyBefore, bodyFingerprint(doc.getRespBody()));
        assertEquals(jsonBefore, doc.getRespExample());
        assertEquals(templateTreeBefore, dataFingerprint(data.getResponseParamDataList()));
        assertTrue(data.getResponseExample(), data.getResponseExample().contains("\"dictList\""));
        assertFalse(data.getResponseExample(), data.getResponseExample().contains("\"element\": {"));
    }

    public void testRecursiveCollectionsRemainBoundedAndDoNotRenderSyntheticNodes() {
        myFixture.addClass("""
                package backlog;
                import java.util.List;
                public class RecursiveDirectory {
                    public List<RecursiveDirectory> childList;
                    public String name;
                }
                """);
        PsiClass controller = myFixture.addClass("""
                package backlog;
                import org.springframework.web.bind.annotation.*;
                @RestController public class RecursiveController {
                    @GetMapping("/recursive") public RecursiveDirectory get() { return null; }
                }
                """);
        DocView doc = new SpringDocViewServiceImpl().buildClassMethodDoc(controller,
                controller.findMethodsByName("get", false)[0]);
        String before = bodyFingerprint(doc.getRespBody());
        String markdown = new DocViewData(getProject(), doc).getResponseParam();
        assertRow(markdown, "childList", "List<RecursiveDirectory>");
        assertRow(markdown, "name", "String");
        assertFalse(markdown, markdown.contains("element|RecursiveDirectory|"));
        assertTrue("Recursive schema must remain bounded", countNodes(doc.getRespBody()) < 20);
        assertEquals(before, bodyFingerprint(doc.getRespBody()));
    }

    private DocView fixture() {
        myFixture.addClass("""
                package backlog;
                import java.util.List;
                import java.util.Map;
                public class ContainerResponse {
                    public List<Directory> directories;
                    public Map<String, Directory> byId;
                    public Map<String, String> labels;
                    public List<String> names;
                    public static class Directory {
                        public List<Dict> dictList;
                    }
                    public static class Dict {
                        public Long dictDirectoryId;
                        public String description;
                        public String element;
                        public String key;
                        public String value;
                    }
                }
                """);
        PsiClass controller = myFixture.addClass("""
                package backlog;
                import org.springframework.web.bind.annotation.*;
                @RestController public class ContainerController {
                    @GetMapping("/containers")
                    public ContainerResponse get(@RequestBody ContainerResponse request) { return null; }
                }
                """);
        return new SpringDocViewServiceImpl().buildClassMethodDoc(controller,
                controller.findMethodsByName("get", false)[0]);
    }

    private static void assertRow(String markdown, String name, String type) {
        assertTrue(markdown, markdown.contains("|" + name + "|" + type + "|"));
    }

    private static int countNodes(Body body) {
        return 1 + body.getChildList().stream().mapToInt(ContainerMarkdownHierarchyTest::countNodes).sum();
    }

    private static String bodyFingerprint(Body body) {
        return body.getName() + ":" + body.getType() + ":" + body.isCollection() + ":" + body.isMap()
                + body.getChildList().stream().map(ContainerMarkdownHierarchyTest::bodyFingerprint).toList();
    }

    private static String dataFingerprint(List<DocViewParamData> data) {
        return data.stream().map(row -> row.getName() + ":" + row.getPrefixSymbol1() + ":"
                + row.getPrefixSymbol2() + ":" + row.getType() + dataFingerprint(row.getChildList())).toList().toString();
    }
}

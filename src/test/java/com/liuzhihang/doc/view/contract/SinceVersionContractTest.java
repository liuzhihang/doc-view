package com.liuzhihang.doc.view.contract;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.dto.Param;
import com.liuzhihang.doc.view.enums.FrameworkEnum;
import com.liuzhihang.doc.view.service.DtoSchemaCacheService;
import com.liuzhihang.doc.view.service.impl.DubboDocViewServiceImpl;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.utils.VelocityUtils;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/** Issue #89: method and field Javadoc metadata must reach generated Markdown. */
public final class SinceVersionContractTest extends LightJavaCodeInsightFixtureTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installSpringWeb(myFixture);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;
                public @interface PostMapping { String[] value() default {}; }
                """);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;
                public @interface RequestBody {}
                """);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            DtoSchemaCacheService.getInstance(getProject()).clear();
        } finally {
            super.tearDown();
        }
    }

    public void testSpringMethodMetadataReachesDefaultsAndCustomTemplate() {
        PsiClass controller = myFixture.addClass("""
                package example;
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class VersionedController {
                    /**
                     * Query user.
                     * @param id User identifier
                     * @since 2024-12-01
                     * @version 2.1
                     */
                    @GetMapping("/users")
                    public void find(@RequestParam String id) {}
                }
                """);
        DocView doc = spring(controller, "find");
        assertEquals("2024-12-01", doc.getReqParamList().get(0).getSince());
        assertEquals("2.1", doc.getReqParamList().get(0).getVersion());
        assertEquals("2024-12-01|2.1", customMetadata(doc));
        assertLocalizedDefaults(doc, "template.spring.init", "2024-12-01", "2.1");
    }

    public void testRequestAndResponseFieldsKeepOwnMetadataAcrossCacheHits() {
        installPayload();
        PsiClass controller = myFixture.addClass("""
                package example;
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class PayloadController {
                    /**
                     * Save payload.
                     * @since method-date
                     * @version method-version
                     */
                    @PostMapping("/payload")
                    public Payload save(@RequestBody Payload payload) { return payload; }
                }
                """);
        for (int attempt = 0; attempt < 2; attempt++) {
            DocView doc = spring(controller, "save");
            assertFieldMetadata(doc.getReqBody(), "name", "2024-10-02", "1.5");
            assertFieldMetadata(doc.getRespBody(), "name", "2024-10-02", "1.5");
            DocViewData data = new DocViewData(getProject(), doc);
            assertTrue(data.getRequestBody(), data.getRequestBody().contains("|2024-10-02-1.5|"));
            assertTrue(data.getResponseParam(), data.getResponseParam().contains("|2024-10-02-1.5|"));
            assertEquals("method-date|method-version", customMetadata(doc));
        }
    }

    public void testQueryEntityFieldsExposeTheirOwnTags() {
        installPayload();
        PsiClass controller = myFixture.addClass("""
                package example;
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class QueryController {
                    @GetMapping("/search")
                    public void find(Payload query) {}
                }
                """);
        DocView doc = spring(controller, "find");
        Param field = doc.getReqParamList().stream()
                .filter(param -> "name".equals(param.getName())).findFirst().orElseThrow();
        assertEquals("2024-10-02", field.getSince());
        assertEquals("1.5", field.getVersion());
        assertTrue(new DocViewData(getProject(), doc).getRequestParam().contains("|2024-10-02-1.5|"));
    }

    public void testMultilineMetadataPreservesWordsWithoutCommentMarkers() {
        myFixture.addClass("""
                package example;
                public class MultilinePayload {
                    /**
                     * Display name.
                     * @since 2024-10-02
                     *        public beta
                     * @version 1.5
                     *          stable release
                     */
                    public String name;
                }
                """);
        PsiClass controller = myFixture.addClass("""
                package example;
                import org.springframework.web.bind.annotation.*;
                @RestController
                public class MultilineController {
                    /**
                     * Query payload.
                     * @param id User identifier
                     * @since 2025-01-01
                     *        public beta
                     * @version 2.0
                     *          stable release
                     */
                    @GetMapping("/multiline")
                    public MultilinePayload find(@RequestParam String id) { return null; }
                }
                """);

        DocView doc = spring(controller, "find");
        assertEquals("2025-01-01 public beta|2.0 stable release", customMetadata(doc));
        assertEquals("2025-01-01 public beta", doc.getReqParamList().get(0).getSince());
        assertEquals("2.0 stable release", doc.getReqParamList().get(0).getVersion());
        assertFieldMetadata(doc.getRespBody(), "name", "2024-10-02 public beta", "1.5 stable release");
        String table = new DocViewData(getProject(), doc).getResponseParam();
        assertTrue(table, table.contains("|2024-10-02 public beta-1.5 stable release|"));
    }

    public void testDubboMultilineMethodMetadataMatchesScalarParameterMetadata() {
        PsiClass service = myFixture.addClass("""
                package example;
                public interface MultilineService {
                    /**
                     * Query user.
                     * @param id User identifier
                     * @since 2025-01-01
                     *        public beta
                     * @version 2.0
                     *          stable release
                     */
                    void find(String id);
                }
                """);
        DocView doc = new DubboDocViewServiceImpl().buildClassMethodDoc(service,
                service.findMethodsByName("find", false)[0]);
        assertEquals("2025-01-01 public beta|2.0 stable release", customMetadata(doc));
        assertFieldMetadata(doc.getReqBody(), "id", "2025-01-01 public beta", "2.0 stable release");
    }

    public void testDubboMethodParametersAndEntityFieldsExposeTags() {
        installPayload();
        PsiClass service = myFixture.addClass("""
                package example;
                public interface VersionedService {
                    /**
                     * Query payload.
                     * @param id User identifier
                     * @param payload Query fields
                     * @since 2025-02-01
                     * @version 3.0
                     */
                    Payload query(String id, Payload payload);
                }
                """);
        PsiMethod method = service.findMethodsByName("query", false)[0];
        for (int attempt = 0; attempt < 2; attempt++) {
            DocView doc = new DubboDocViewServiceImpl().buildClassMethodDoc(service, method);
            assertEquals("2025-02-01|3.0", customMetadata(doc));
            assertFieldMetadata(doc.getReqBody(), "id", "2025-02-01", "3.0");
            assertFieldMetadata(child(doc.getReqBody(), "payload"), "name", "2024-10-02", "1.5");
            assertFieldMetadata(doc.getRespBody(), "name", "2024-10-02", "1.5");
            assertLocalizedDefaults(doc, "template.dubbo.init", "2025-02-01", "3.0");
        }
    }

    public void testAbsentMethodTagsDoNotInheritClassTagsOrCreateEmptySections() {
        PsiClass controller = myFixture.addClass("""
                package example;
                import org.springframework.web.bind.annotation.*;
                /**
                 * @since class-date
                 * @version class-version
                 */
                @RestController
                public class UnversionedController {
                    @GetMapping("/plain")
                    public void plain() {}
                }
                """);
        DocView doc = spring(controller, "plain");
        assertEquals("|", customMetadata(doc));
        for (Locale locale : List.of(Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE)) {
            String markdown = VelocityUtils.render(bundle(locale).getString("template.spring.init"),
                    new DocViewData(getProject(), doc));
            assertFalse(markdown, markdown.contains("生效时间"));
            assertFalse(markdown, markdown.contains("接口版本"));
            assertFalse(markdown, markdown.contains("class-date"));
            assertFalse(markdown, markdown.contains("${DocView.since}"));
            assertFalse(markdown, markdown.contains("${DocView.version}"));
        }
    }

    public void testDescriptionsMentioningTagNamesAreNotErased() {
        DocView doc = new DocView("Tags in text");
        doc.setType(FrameworkEnum.SPRING);
        Param param = new Param();
        param.setName("query");
        param.setType("String");
        param.setRequired(false);
        param.setDesc("Search literal @since text");
        param.setSince("param-date");
        param.setVersion("param-version");
        doc.setReqParamList(List.of(param));
        Body field = new Body();
        field.setName("content");
        field.setType("String");
        field.setRequired(false);
        field.setDesc("Preserve literal @version text");
        field.setSince("field-date");
        field.setVersion("field-version");
        doc.getRespBody().getChildList().add(field);

        DocViewData data = new DocViewData(getProject(), doc);
        assertTrue(data.getRequestParam(), data.getRequestParam().contains("Search literal @since text"));
        assertTrue(data.getRequestParam(), data.getRequestParam().contains("|param-date-param-version|"));
        assertTrue(data.getResponseParam(), data.getResponseParam().contains("Preserve literal @version text"));
        assertTrue(data.getResponseParam(), data.getResponseParam().contains("|field-date-field-version|"));
    }

    private void installPayload() {
        myFixture.addClass("""
                package example;
                public class Payload {
                    /**
                     * Display name.
                     * @since 2024-10-02
                     * @version 1.5
                     */
                    public String name;
                }
                """);
    }

    private DocView spring(PsiClass controller, String methodName) {
        return new SpringDocViewServiceImpl().buildClassMethodDoc(controller,
                controller.findMethodsByName(methodName, false)[0]);
    }

    private String customMetadata(DocView doc) {
        return VelocityUtils.render("${DocView.since}|${DocView.version}", new DocViewData(getProject(), doc));
    }

    private void assertLocalizedDefaults(DocView doc, String key, String since, String version) {
        for (Locale locale : List.of(Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE)) {
            String markdown = VelocityUtils.render(bundle(locale).getString(key), new DocViewData(getProject(), doc));
            assertTrue(markdown, markdown.contains("**生效时间:**\n\n" + since));
            assertTrue(markdown, markdown.contains("**接口版本:**\n\n" + version));
        }
    }

    private ResourceBundle bundle(Locale locale) {
        return ResourceBundle.getBundle("messages.DocViewBundle", locale, getClass().getClassLoader());
    }

    private static Body child(Body parent, String name) {
        return parent.getChildList().stream().filter(body -> name.equals(body.getName())).findFirst().orElseThrow();
    }

    private static void assertFieldMetadata(Body parent, String name, String since, String version) {
        Body field = child(parent, name);
        assertEquals(since, field.getSince());
        assertEquals(version, field.getVersion());
    }
}

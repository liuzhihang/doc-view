package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.utils.DocViewUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AnnotationTextFallbackTest extends LightJavaCodeInsightFixtureTestCase {

    private Boolean originalNameUseSwagger3;
    private Boolean originalNameUseSwagger;
    private Boolean originalNameUseCommentTag;
    private Boolean originalNameMethodComment;
    private Boolean originalDescUseSwagger3;
    private Boolean originalDescUseSwagger;
    private String originalSpringTemplate;

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installSpringWeb(myFixture);
        AnnotationStubs.installOpenApi3(myFixture);
        AnnotationStubs.installSwagger2(myFixture);
        PsiClass annotationValues = myFixture.addClass(annotationValuesSource());
        assertConstantField(annotationValues, "SWAGGER2_NAME", "  Swagger2 \"查询\"  ");
        assertConstantField(annotationValues, "OPENAPI3_NAME", "  OpenAPI3 \"名称\"  ");
        assertConstantField(annotationValues, "OPENAPI3_DESCRIPTION", "  OpenAPI \"说明\"\n第二行  ");
        assertConstantField(annotationValues, "SWAGGER2_NOTES", "  Swagger2 \"备注\"\n后续  ");

        Settings settings = Settings.getInstance(getProject());
        originalNameUseSwagger3 = settings.getNameUseSwagger3();
        originalNameUseSwagger = settings.getNameUseSwagger();
        originalNameUseCommentTag = settings.getNameUseCommentTag();
        originalNameMethodComment = settings.getNameMethodComment();
        originalDescUseSwagger3 = settings.getDescUseSwagger3();
        originalDescUseSwagger = settings.getDescUseSwagger();

        settings.setNameUseSwagger3(true);
        settings.setNameUseSwagger(true);
        settings.setNameUseCommentTag(true);
        settings.setNameMethodComment(true);
        settings.setDescUseSwagger3(true);
        settings.setDescUseSwagger(true);

        TemplateSettings templateSettings = TemplateSettings.getInstance(getProject());
        originalSpringTemplate = templateSettings.getSpringTemplate();
        templateSettings.setSpringTemplate("${DocView.name}|NAME_END\n---DESCRIPTION---\n${DocView.desc}|DESC_END\n");
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Settings settings = Settings.getInstance(getProject());
            settings.setNameUseSwagger3(originalNameUseSwagger3);
            settings.setNameUseSwagger(originalNameUseSwagger);
            settings.setNameUseCommentTag(originalNameUseCommentTag);
            settings.setNameMethodComment(originalNameMethodComment);
            settings.setDescUseSwagger3(originalDescUseSwagger3);
            settings.setDescUseSwagger(originalDescUseSwagger);
            TemplateSettings.getInstance(getProject()).setSpringTemplate(originalSpringTemplate);
        } finally {
            super.tearDown();
        }
    }

    public void testEvaluatedAnnotationValuesReachSpringMarkdownGolden() throws Exception {
        PsiClass controller = controller("""
                package contract.openapi3;

                import io.swagger.annotations.ApiOperation;
                import io.swagger.v3.oas.annotations.Operation;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class ResolvedAnnotationController {
                    @Operation(summary = "   ", description = AnnotationValues.OPENAPI3_DESCRIPTION)
                    @ApiOperation(value = AnnotationValues.SWAGGER2_NAME, notes = AnnotationValues.SWAGGER2_NOTES)
                    @GetMapping("/resolved")
                    public void resolved() {
                    }
                }
                """);
        PsiMethod method = method(controller, "resolved");

        assertConstantAnnotationValue(method, "io.swagger.annotations.ApiOperation", "value",
                "SWAGGER2_NAME", "  Swagger2 \"查询\"  ");
        assertConstantAnnotationValue(method, "io.swagger.v3.oas.annotations.Operation", "description",
                "OPENAPI3_DESCRIPTION", "  OpenAPI \"说明\"\n第二行  ");
        assertConstantAnnotationValue(method, "io.swagger.annotations.ApiOperation", "notes",
                "SWAGGER2_NOTES", "  Swagger2 \"备注\"\n后续  ");

        assertEquals("  Swagger2 \"查询\"  ", DocViewUtils.getName(method));
        assertEquals("  OpenAPI \"说明\"\n第二行  ", DocViewUtils.getMethodDesc(method));

        DocView docView = new SpringDocViewServiceImpl().buildClassMethodDoc(controller, method);
        String markdown = DocViewData.markdownText(getProject(), docView);
        Path goldenFile = Path.of(getTestDataPath(), "contracts/markdown/annotation-text-fallback.md");
        String expectedMarkdown = StringUtil.convertLineSeparators(
                Files.readString(goldenFile, StandardCharsets.UTF_8));
        assertEquals(expectedMarkdown, markdown);
    }

    public void testBlankDefaultAndUnresolvedNamesContinueTheSourceOrder() {
        PsiClass controller = controller("""
                package contract.openapi3;

                import io.swagger.annotations.ApiOperation;
                import io.swagger.v3.oas.annotations.Operation;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class NameFallbackController {
                    /**
                     * @DocView.Name TagFallback
                     */
                    @Operation(summary = "   ")
                    @ApiOperation(value = "   ")
                    @GetMapping("/tag")
                    public void tagFallback() {
                    }

                    /** abcdefghijklmnopqr */
                    @Operation(summary = MissingValues.SUMMARY)
                    @ApiOperation(value = MissingValues.NAME)
                    @GetMapping("/javadoc")
                    public void javadocFallback() {
                    }

                    /**
                     * @DocView.Name DefaultFallback
                     */
                    @Operation
                    @ApiOperation
                    @GetMapping("/default")
                    public void defaultFallback() {
                    }

                    @Operation
                    @ApiOperation
                    @GetMapping("/method")
                    public void methodFallback() {
                    }
                }
                """);

        assertEquals("TagFallback", name(controller, "tagFallback"));
        assertEquals("abcdefghijklmno", name(controller, "javadocFallback"));
        assertEquals("DefaultFallback", name(controller, "defaultFallback"));
        assertEquals("methodFallback", name(controller, "methodFallback"));
    }

    public void testNameSettingsKeepTheExistingPriorityAndGates() {
        PsiClass controller = controller("""
                package contract.openapi3;

                import io.swagger.annotations.ApiOperation;
                import io.swagger.v3.oas.annotations.Operation;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class NamePriorityController {
                    /**
                     * abcdefghijklmnopqr
                     * @DocView.Name TagFallback
                     */
                    @Operation(summary = AnnotationValues.OPENAPI3_NAME)
                    @ApiOperation(value = "Swagger2Fallback")
                    @GetMapping("/sources")
                    public void nameSources() {
                    }
                }
                """);
        PsiMethod method = method(controller, "nameSources");
        assertConstantAnnotationValue(method, "io.swagger.v3.oas.annotations.Operation", "summary",
                "OPENAPI3_NAME", "  OpenAPI3 \"名称\"  ");

        Settings settings = Settings.getInstance(getProject());
        assertEquals("  OpenAPI3 \"名称\"  ", DocViewUtils.getName(method));

        settings.setNameUseSwagger3(false);
        assertEquals("Swagger2Fallback", name(controller, "nameSources"));

        settings.setNameUseSwagger(false);
        assertEquals("TagFallback", name(controller, "nameSources"));

        settings.setNameUseCommentTag(false);
        assertEquals("abcdefghijklmno", name(controller, "nameSources"));

        settings.setNameMethodComment(false);
        assertEquals("nameSources", name(controller, "nameSources"));
    }

    public void testDescriptionSettingsFallbackFromBlankOrUnresolvedAnnotations() {
        PsiClass controller = controller("""
                package contract.openapi3;

                import io.swagger.annotations.ApiOperation;
                import io.swagger.v3.oas.annotations.Operation;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class DescriptionFallbackController {
                    /** JavadocDescription */
                    @Operation(description = AnnotationValues.OPENAPI3_DESCRIPTION)
                    @ApiOperation(notes = AnnotationValues.SWAGGER2_NOTES)
                    @GetMapping("/priority")
                    public void priority() {
                    }

                    @Operation(description = "   ")
                    @ApiOperation(notes = AnnotationValues.SWAGGER2_NOTES)
                    @GetMapping("/notes")
                    public void notesFallback() {
                    }

                    /** JavadocFallback */
                    @Operation(description = MissingValues.DESCRIPTION)
                    @ApiOperation(notes = "   ")
                    @GetMapping("/javadoc")
                    public void javadocFallback() {
                    }

                    @GetMapping("/empty")
                    public void emptyFallback() {
                    }
                }
                """);

        Settings settings = Settings.getInstance(getProject());
        assertEquals("  OpenAPI \"说明\"\n第二行  ", description(controller, "priority"));

        settings.setDescUseSwagger3(false);
        assertEquals("  Swagger2 \"备注\"\n后续  ", description(controller, "priority"));
        assertEquals("  Swagger2 \"备注\"\n后续  ", description(controller, "notesFallback"));

        settings.setDescUseSwagger(false);
        assertEquals("JavadocDescription", description(controller, "priority"));
        assertEquals("JavadocFallback", description(controller, "javadocFallback"));
        assertEquals("", description(controller, "emptyFallback"));
    }

    private PsiClass controller(String source) {
        return myFixture.addClass(source);
    }

    private String name(PsiClass controller, String methodName) {
        return DocViewUtils.getName(method(controller, methodName));
    }

    private String description(PsiClass controller, String methodName) {
        return DocViewUtils.getMethodDesc(method(controller, methodName));
    }

    private PsiMethod method(PsiClass psiClass, String name) {
        PsiMethod[] methods = psiClass.findMethodsByName(name, false);
        assertEquals("Expected one fixture method named " + name, 1, methods.length);
        return methods[0];
    }

    private void assertConstantAnnotationValue(PsiMethod method, String annotationQualifiedName,
                                               String attributeName, String expectedFieldName,
                                               String expectedValue) {
        PsiAnnotation annotation = method.getAnnotation(annotationQualifiedName);
        assertNotNull("Missing annotation " + annotationQualifiedName, annotation);
        PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
        assertTrue("Expected " + attributeName + " to reference a fixture constant",
                value instanceof PsiReferenceExpression);
        PsiElement resolved = ((PsiReferenceExpression) value).resolve();
        assertTrue("Expected fixture constant reference to resolve to a field", resolved instanceof PsiField);
        PsiField field = (PsiField) resolved;
        assertEquals(expectedFieldName, field.getName());
        assertEquals(expectedValue, ((PsiLiteralExpression) field.getInitializer()).getValue());
    }

    private String annotationValuesSource() {
        return """
                package contract.openapi3;

                public final class AnnotationValues {
                    public static final String SWAGGER2_NAME = %s;
                    public static final String OPENAPI3_NAME = %s;
                    public static final String OPENAPI3_DESCRIPTION = %s;
                    public static final String SWAGGER2_NOTES = %s;
                }
                """.formatted(
                javaStringLiteral("  Swagger2 \"查询\"  "),
                javaStringLiteral("  OpenAPI3 \"名称\"  "),
                javaStringLiteral("  OpenAPI \"说明\"\n第二行  "),
                javaStringLiteral("  Swagger2 \"备注\"\n后续  "));
    }

    private String javaStringLiteral(String value) {
        return "\"" + value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n") + "\"";
    }

    private void assertConstantField(PsiClass owner, String name, String expectedValue) {
        PsiField field = owner.findFieldByName(name, false);
        assertNotNull("Missing constant field " + name, field);
        assertTrue("Expected literal initializer for constant field " + name,
                field.getInitializer() instanceof PsiLiteralExpression);
        assertEquals("java.lang.String", field.getType().getCanonicalText());
        assertEquals(expectedValue, ((PsiLiteralExpression) field.getInitializer()).getValue());
        GlobalSearchScope scope = GlobalSearchScope.allScope(getProject());
        assertNotNull("Expected java.lang.String to resolve from the test JDK",
                JavaPsiFacade.getInstance(getProject()).findClass("java.lang.String", scope));
        assertEquals("java.lang.String",
                PsiType.getJavaLangString(field.getManager(), scope).getCanonicalText());
        assertEquals("Expected IDEA to evaluate compile-time string constant " + name,
                expectedValue, field.computeConstantValue());
    }

    @Override
    protected String getTestDataPath() {
        return Path.of("src/test/testData").toAbsolutePath().toString();
    }
}

package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.dto.DocViewParamData;
import com.liuzhihang.doc.view.exception.DocViewException;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.service.DtoSchemaCacheService;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class OpenApi3SchemaRequiredModeTest extends LightJavaCodeInsightFixtureTestCase {

    private Set<String> originalRequiredFieldAnnotations;
    private Boolean originalRequiredUseCommentTag;
    private String originalRequiredTag;
    private Boolean originalFieldNameJsonProperty;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installSpringWeb(myFixture);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface PostMapping {
                    String[] value() default {};
                }
                """);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface RequestBody {
                }
                """);
        myFixture.addClass("""
                package javax.validation.constraints;

                public @interface NotNull {
                }
                """);
        myFixture.addClass("""
                package io.swagger.annotations;

                public @interface ApiModelProperty {
                    boolean required() default false;

                    String value() default "";
                }
                """);
        myFixture.addClass("""
                package com.fasterxml.jackson.annotation;

                public @interface JsonProperty {
                    String value() default "";
                }
                """);

        Settings settings = Settings.getInstance(getProject());
        originalRequiredFieldAnnotations = new HashSet<>(settings.getRequiredFieldAnnotation());
        originalRequiredUseCommentTag = settings.getRequiredUseCommentTag();
        originalRequiredTag = settings.getRequired();
        originalFieldNameJsonProperty = settings.getFieldNameJsonProperty();
        settings.setRequiredFieldAnnotation(new HashSet<>(Set.of("javax.validation.constraints.NotNull")));
        settings.setRequiredUseCommentTag(true);
        settings.setRequired("DocView.Required");
        settings.setFieldNameJsonProperty(true);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Settings settings = Settings.getInstance(getProject());
            settings.setRequiredFieldAnnotation(new HashSet<>(originalRequiredFieldAnnotations));
            settings.setRequiredUseCommentTag(originalRequiredUseCommentTag);
            settings.setRequired(originalRequiredTag);
            settings.setFieldNameJsonProperty(originalFieldNameJsonProperty);

            DtoSchemaCacheService cacheService = getProject().getService(DtoSchemaCacheService.class);
            if (cacheService != null) {
                cacheService.clear();
            }
        } finally {
            super.tearDown();
        }
    }

    public void testRequiredModeRequiredForcesRequired() {
        installSchema(true);
        PsiClass dto = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.media.Schema;

                public class RequiredDto {
                    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                    private String value;
                }
                """);

        assertTrue(DocViewUtils.isRequired(field(dto, "value")));
    }

    public void testNotRequiredOverridesValidationSwagger2AndDocViewTag() {
        installSchema(true);
        PsiClass dto = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.annotations.ApiModelProperty;
                import io.swagger.v3.oas.annotations.media.Schema;
                import javax.validation.constraints.NotNull;

                public class OptionalDto {
                    /**
                     * @DocView.Required
                     */
                    @NotNull
                    @ApiModelProperty(required = true)
                    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    private String value;
                }
                """);

        assertFalse(DocViewUtils.isRequired(field(dto, "value")));
    }

    public void testAutoAndMissingModeUseExistingRequiredSources() {
        installSchema(true);
        PsiClass dto = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.annotations.ApiModelProperty;
                import io.swagger.v3.oas.annotations.media.Schema;
                import javax.validation.constraints.NotNull;

                public class AutoDto {
                    @Schema(requiredMode = Schema.RequiredMode.AUTO)
                    @NotNull
                    private String validation;

                    @ApiModelProperty(required = true)
                    private String swagger2;

                    /**
                     * @DocView.Required
                     */
                    private String docViewTag;

                    @Schema
                    private String optional;
                }
                """);

        assertTrue(DocViewUtils.isRequired(field(dto, "validation")));
        assertTrue(DocViewUtils.isRequired(field(dto, "swagger2")));
        assertTrue(DocViewUtils.isRequired(field(dto, "docViewTag")));
        assertFalse(DocViewUtils.isRequired(field(dto, "optional")));
    }

    public void testDeprecatedRequiredIsIgnoredAndUnknownModeUsesAuto() {
        installSchema(true);
        PsiClass dto = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.media.Schema;
                import javax.validation.constraints.NotNull;

                public class DeprecatedDto {
                    enum OtherMode { REQUIRED, NOT_REQUIRED }

                    @Schema(required = true)
                    private String deprecatedOnly;

                    @Schema(required = true, requiredMode = Schema.RequiredMode.UNRESOLVED)
                    private String unresolvedWithDeprecatedValue;

                    @Schema(requiredMode = Schema.RequiredMode.UNRESOLVED)
                    @NotNull
                    private String unresolvedWithValidation;

                    @Schema(requiredMode = OtherMode.REQUIRED)
                    private String foreignRequiredOnly;

                    @Schema(requiredMode = OtherMode.NOT_REQUIRED)
                    @NotNull
                    private String foreignNotRequiredWithValidation;
                }
                """);

        assertFalse(DocViewUtils.isRequired(field(dto, "deprecatedOnly")));
        assertFalse(DocViewUtils.isRequired(field(dto, "unresolvedWithDeprecatedValue")));
        assertTrue(DocViewUtils.isRequired(field(dto, "unresolvedWithValidation")));
        assertFalse(DocViewUtils.isRequired(field(dto, "foreignRequiredOnly")));
        assertTrue(DocViewUtils.isRequired(field(dto, "foreignNotRequiredWithValidation")));
    }

    public void testRequiredModesReachGeneratedRequestAndResponseBodiesAndFullMarkdownGolden() throws Exception {
        installSchema(true);
        PsiClass controller = myFixture.addClass("""
                package contract.openapi3;

                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/api")
                public class SchemaController {
                    @PostMapping("/schema")
                    public SchemaDto postSchema(@RequestBody SchemaDto request) {
                        return null;
                    }
                }
                """);
        PsiClass dto = myFixture.addClass("""
                package contract.openapi3;

                import com.fasterxml.jackson.annotation.JsonProperty;
                import io.swagger.v3.oas.annotations.media.Schema;

                public class SchemaDto {
                    /** 显示名称 */
                    @JsonProperty("displayName")
                    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "显示名称",
                            title = "保留标题", example = "示例")
                    private String label;

                    /** 可选内容 */
                    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "可选内容", name = "memo-name")
                    private String memo;
                }
                """);

        PsiMethod method = controller.findMethodsByName("postSchema", false)[0];
        DocView docView = DocViewService.getInstance(getProject(), controller).buildClassMethodDoc(controller, method);

        assertEquals(2, docView.getReqBody().getChildList().size());
        assertEquals("displayName", docView.getReqBody().getChildList().get(0).getName());
        assertEquals("String", docView.getReqBody().getChildList().get(0).getType());
        assertTrue(docView.getReqBody().getChildList().get(0).getRequired());
        assertEquals("显示名称", docView.getReqBody().getChildList().get(0).getDesc());
        assertEquals("memo", docView.getReqBody().getChildList().get(1).getName());
        assertFalse(docView.getReqBody().getChildList().get(1).getRequired());
        assertEquals("可选内容", docView.getReqBody().getChildList().get(1).getDesc());

        assertEquals(2, docView.getRespBody().getChildList().size());
        assertEquals("displayName", docView.getRespBody().getChildList().get(0).getName());
        assertEquals("String", docView.getRespBody().getChildList().get(0).getType());
        assertTrue(docView.getRespBody().getChildList().get(0).getRequired());
        assertEquals("显示名称", docView.getRespBody().getChildList().get(0).getDesc());
        assertEquals("memo", docView.getRespBody().getChildList().get(1).getName());
        assertFalse(docView.getRespBody().getChildList().get(1).getRequired());
        assertEquals("可选内容", docView.getRespBody().getChildList().get(1).getDesc());

        String markdown = DocViewData.markdownText(getProject(), docView);
        Path goldenPath = Path.of(getTestDataPath(), "contracts/markdown/schema-required-mode.md");
        String expected = StringUtil.convertLineSeparators(Files.readString(goldenPath, StandardCharsets.UTF_8)) + "\n";
        assertEquals(expected, markdown);
    }

    public void testRequiredModesReachSpringFlattenedFormParameters() {
        installSchema(true);
        PsiClass controller = myFixture.addClass("""
                package contract.openapi3;

                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class FilterController {
                    @GetMapping("/search")
                    public void search(Filter filter) {
                    }
                }
                """);
        myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.media.Schema;

                public class Filter {
                    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                    private String account;

                    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    private String memo;
                }
                """);

        PsiMethod method = controller.findMethodsByName("search", false)[0];
        var params = SpringPsiUtils.buildFormParam(method);

        assertEquals(2, params.size());
        assertEquals("account", params.get(0).getName());
        assertTrue(params.get(0).getRequired());
        assertEquals("memo", params.get(1).getName());
        assertFalse(params.get(1).getRequired());
    }

    public void testSchemaWriteBackUpdatesModesPreservesMembersAndEscapesDescription() {
        installSchema(true);
        PsiClass dto = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.media.Schema;

                public class EditableDto {
                    @Schema(required = true, requiredMode = Schema.RequiredMode.AUTO,
                            name = "api-name", title = "保留标题", example = "保留示例")
                    private String optional;

                    @Schema(required = true, requiredMode = Schema.RequiredMode.AUTO, title = "另一个标题")
                    private String required;
                }
                """);
        PsiField optional = field(dto, "optional");
        PsiField required = field(dto, "required");
        String escapedDescription = "引号 \"、反斜杠 \\ 和换行\n下一行";

        Map<PsiElement, DocViewParamData> changes = new LinkedHashMap<>();
        changes.put(optional, fieldEdit(optional, false, "可选说明"));
        changes.put(required, fieldEdit(required, true, escapedDescription));
        DocViewUtils.writeComment(getProject(), changes);

        PsiAnnotation optionalSchema = optional.getAnnotation("io.swagger.v3.oas.annotations.media.Schema");
        assertNotNull(optionalSchema);
        assertTrue(attributeText(optionalSchema, "requiredMode").endsWith("NOT_REQUIRED"));
        assertFalse(declaresAttribute(optionalSchema, "required"));
        assertEquals("api-name", stringAttribute(optionalSchema, "name"));
        assertEquals("保留标题", stringAttribute(optionalSchema, "title"));
        assertEquals("保留示例", stringAttribute(optionalSchema, "example"));
        assertEquals("可选说明", stringAttribute(optionalSchema, "description"));

        PsiAnnotation requiredSchema = required.getAnnotation("io.swagger.v3.oas.annotations.media.Schema");
        assertNotNull(requiredSchema);
        assertTrue(attributeText(requiredSchema, "requiredMode").endsWith("REQUIRED"));
        assertFalse(declaresAttribute(requiredSchema, "required"));
        assertEquals("另一个标题", stringAttribute(requiredSchema, "title"));
        assertEquals(escapedDescription, stringAttribute(requiredSchema, "description"));
    }

    public void testModeOnlyWriteBackPreservesSchemaDescriptionJavaStringValue() {
        installSchema(true);
        PsiClass dto = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.media.Schema;

                public class DescriptionDto {
                    @Schema(requiredMode = Schema.RequiredMode.AUTO,
                            name = "literal-name", title = "Literal title", example = "Literal example",
                            description = "Original \\\"quoted\\\" label; path C:\\\\api\\\\schema;\\nsecond line")
                    private String literal;

                    @Schema(requiredMode = Schema.RequiredMode.AUTO,
                            name = "constant-name", title = "Constant title", example = "Constant example",
                            description = "Constant " + "\\\"quoted\\\" path C:\\\\constant\\\\schema;\\nthird line")
                    private String constant;
                }
                """);

        PsiField literal = field(dto, "literal");
        PsiField constant = field(dto, "constant");
        String literalDescription = "Original \"quoted\" label; path C:\\api\\schema;\nsecond line";
        String constantDescription = "Constant \"quoted\" path C:\\constant\\schema;\nthird line";

        assertEquals(literalDescription, DocViewUtils.fieldDesc(literal));
        assertEquals(constantDescription, DocViewUtils.fieldDesc(constant));

        Map<PsiElement, DocViewParamData> changes = new LinkedHashMap<>();
        changes.put(literal, fieldEdit(literal, true, DocViewUtils.fieldDesc(literal)));
        changes.put(constant, fieldEdit(constant, false, DocViewUtils.fieldDesc(constant)));
        DocViewUtils.writeComment(getProject(), changes);

        PsiAnnotation literalSchema = literal.getAnnotation("io.swagger.v3.oas.annotations.media.Schema");
        assertNotNull(literalSchema);
        assertTrue(attributeText(literalSchema, "requiredMode").endsWith("REQUIRED"));
        assertEquals("literal-name", stringAttribute(literalSchema, "name"));
        assertEquals("Literal title", stringAttribute(literalSchema, "title"));
        assertEquals("Literal example", stringAttribute(literalSchema, "example"));
        assertEquals(literalDescription, stringAttribute(literalSchema, "description"));

        PsiAnnotation constantSchema = constant.getAnnotation("io.swagger.v3.oas.annotations.media.Schema");
        assertNotNull(constantSchema);
        assertTrue(attributeText(constantSchema, "requiredMode").endsWith("NOT_REQUIRED"));
        assertEquals("constant-name", stringAttribute(constantSchema, "name"));
        assertEquals("Constant title", stringAttribute(constantSchema, "title"));
        assertEquals("Constant example", stringAttribute(constantSchema, "example"));
        assertEquals(constantDescription, stringAttribute(constantSchema, "description"));
    }

    public void testModeOnlyWriteBackPreservesExplicitBlankSchemaDescriptionsAgainstJavadocFallback() {
        installSchema(true);
        PsiClass dto = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.media.Schema;

                public class BlankDescriptionDto {
                    /** Lower-priority description must not replace an explicit empty Schema value. */
                    @Schema(requiredMode = Schema.RequiredMode.AUTO, description = "")
                    private String empty;

                    /** Lower-priority description must not replace an explicit whitespace Schema value. */
                    @Schema(requiredMode = Schema.RequiredMode.AUTO, description = "   ")
                    private String whitespace;

                    @Schema(requiredMode = Schema.RequiredMode.AUTO)
                    @io.swagger.annotations.ApiModelProperty("Fallback from Swagger 2")
                    private String fallback;
                }
                """);

        PsiField empty = field(dto, "empty");
        PsiField whitespace = field(dto, "whitespace");
        PsiField fallback = field(dto, "fallback");
        assertEquals("", DocViewUtils.fieldDesc(empty));
        assertEquals("   ", DocViewUtils.fieldDesc(whitespace));
        assertEquals("Fallback from Swagger 2", DocViewUtils.fieldDesc(fallback));

        Map<PsiElement, DocViewParamData> changes = new LinkedHashMap<>();
        changes.put(empty, fieldEdit(empty, true, DocViewUtils.fieldDesc(empty)));
        changes.put(whitespace, fieldEdit(whitespace, false, DocViewUtils.fieldDesc(whitespace)));
        DocViewUtils.writeComment(getProject(), changes);

        PsiAnnotation emptySchema = empty.getAnnotation("io.swagger.v3.oas.annotations.media.Schema");
        assertNotNull(emptySchema);
        assertTrue(attributeText(emptySchema, "requiredMode").endsWith("REQUIRED"));
        assertEquals("", stringAttribute(emptySchema, "description"));
        assertEquals("", DocViewUtils.fieldDesc(empty));

        PsiAnnotation whitespaceSchema = whitespace.getAnnotation("io.swagger.v3.oas.annotations.media.Schema");
        assertNotNull(whitespaceSchema);
        assertTrue(attributeText(whitespaceSchema, "requiredMode").endsWith("NOT_REQUIRED"));
        assertEquals("   ", stringAttribute(whitespaceSchema, "description"));
        assertEquals("   ", DocViewUtils.fieldDesc(whitespace));
    }

    public void testWritingBlankSchemaDescriptionPreventsJavadocFallbackFromReturning() {
        installSchema(true);
        PsiClass dto = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.media.Schema;

                public class ClearedDescriptionDto {
                    /** Existing Schema description can be cleared. */
                    @Schema(requiredMode = Schema.RequiredMode.AUTO, description = "Existing description")
                    private String cleared;

                    /** Javadoc fallback should stay suppressed after whitespace is saved. */
                    @Schema(requiredMode = Schema.RequiredMode.AUTO)
                    private String whitespace;
                }
                """);

        PsiField cleared = field(dto, "cleared");
        PsiField whitespace = field(dto, "whitespace");
        assertEquals("Existing description", DocViewUtils.fieldDesc(cleared));
        assertFalse(DocViewUtils.fieldDesc(whitespace).isEmpty());

        Map<PsiElement, DocViewParamData> changes = new LinkedHashMap<>();
        changes.put(cleared, fieldEdit(cleared, true, ""));
        changes.put(whitespace, fieldEdit(whitespace, false, "   "));
        DocViewUtils.writeComment(getProject(), changes);

        PsiAnnotation clearedSchema = cleared.getAnnotation("io.swagger.v3.oas.annotations.media.Schema");
        assertNotNull(clearedSchema);
        assertTrue(declaresAttribute(clearedSchema, "description"));
        assertEquals("", stringAttribute(clearedSchema, "description"));
        assertEquals("", DocViewUtils.fieldDesc(cleared));

        PsiAnnotation whitespaceSchema = whitespace.getAnnotation("io.swagger.v3.oas.annotations.media.Schema");
        assertNotNull(whitespaceSchema);
        assertTrue(declaresAttribute(whitespaceSchema, "description"));
        assertEquals("   ", stringAttribute(whitespaceSchema, "description"));
        assertEquals("   ", DocViewUtils.fieldDesc(whitespace));
    }

    public void testUnsupportedRequiredModeIsPreflightedBeforeAnyFieldWrite() {
        installSchema(false);
        PsiClass dto = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.media.Schema;

                public class BatchDto {
                    /** Original ordinary field documentation. */
                    private String ordinary;

                    @Schema(required = true, description = "Original Schema description", name = "stable-name")
                    private String unsupported;
                }
                """);

        PsiField ordinary = field(dto, "ordinary");
        PsiField unsupported = field(dto, "unsupported");
        String originalOrdinaryText = ordinary.getText();
        String originalUnsupportedText = unsupported.getText();
        Map<PsiElement, DocViewParamData> changes = new LinkedHashMap<>();
        changes.put(ordinary, fieldEdit(ordinary, true, "Draft ordinary description"));
        changes.put(unsupported, fieldEdit(unsupported, false, "Draft Schema description"));

        DocViewException failure = null;
        try {
            DocViewUtils.writeComment(getProject(), changes);
        } catch (DocViewException exception) {
            failure = exception;
        }

        assertNotNull("Expected a user-readable unsupported dependency error", failure);
        assertTrue(failure.getMessage().contains("swagger-annotations"));
        assertTrue(failure.getMessage().contains("2.2.5"));
        assertEquals("A requiredMode capability failure must precede all batch writes",
                originalOrdinaryText, ordinary.getText());
        assertEquals(originalUnsupportedText, unsupported.getText());
    }

    public void testMissingRequiredModeMemberRejectsWriteBackWithVersionHintAndNoMutation() {
        installSchema(false);
        PsiClass dto = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.media.Schema;

                public class OldSchemaDto {
                    @Schema(required = true, name = "original-name")
                    private String value;
                }
                """);
        PsiField value = field(dto, "value");
        String originalFieldText = value.getText();
        String originalAnnotationText = value.getAnnotation("io.swagger.v3.oas.annotations.media.Schema").getText();
        DocViewException failure = null;

        try {
            DocViewUtils.writeComment(getProject(), Map.of(value, fieldEdit(value, false, "新描述")));
        } catch (DocViewException exception) {
            failure = exception;
        }

        assertNotNull("Expected a user-readable unsupported dependency error", failure);
        assertTrue(failure.getMessage().contains("swagger-annotations"));
        assertTrue(failure.getMessage().contains("2.2.5"));
        assertEquals(originalFieldText, value.getText());
        assertEquals(originalAnnotationText,
                value.getAnnotation("io.swagger.v3.oas.annotations.media.Schema").getText());
    }

    private void installSchema(boolean includeRequiredMode) {
        String requiredModeMember = includeRequiredMode
                ? "RequiredMode requiredMode() default RequiredMode.AUTO;\n"
                + "    enum RequiredMode { AUTO, REQUIRED, NOT_REQUIRED }\n"
                : "";
        myFixture.addClass("""
                package io.swagger.v3.oas.annotations.media;

                public @interface Schema {
                    boolean required() default false;
                    String description() default "";
                    String name() default "";
                    String title() default "";
                    String example() default "";
                %s}
                """.formatted(requiredModeMember));
    }

    private PsiField field(PsiClass owner, String name) {
        PsiField result = owner.findFieldByName(name, false);
        assertNotNull("Missing test field " + name, result);
        return result;
    }

    private DocViewParamData fieldEdit(PsiField field, boolean required, String description) {
        DocViewParamData data = new DocViewParamData();
        data.setPsiElement(field);
        data.setRequired(required);
        data.setDesc(description);
        return data;
    }

    private String attributeText(PsiAnnotation annotation, String name) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(name);
        assertNotNull("Missing annotation attribute " + name, value);
        return value.getText();
    }

    private boolean declaresAttribute(PsiAnnotation annotation, String name) {
        for (PsiNameValuePair pair : annotation.getParameterList().getAttributes()) {
            if (name.equals(pair.getName())) {
                return true;
            }
        }
        return false;
    }

    private String stringAttribute(PsiAnnotation annotation, String name) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(name);
        assertNotNull("Missing annotation attribute " + name, value);
        assertTrue("Expected a Java string literal for " + name, value instanceof PsiLiteralExpression);
        Object literalValue = ((PsiLiteralExpression) value).getValue();
        assertTrue("Expected a string value for " + name, literalValue instanceof String);
        return (String) literalValue;
    }

    @Override
    protected String getTestDataPath() {
        return Path.of("src/test/testData").toAbsolutePath().toString();
    }
}

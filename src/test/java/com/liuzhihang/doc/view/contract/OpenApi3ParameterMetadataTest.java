package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.dto.Header;
import com.liuzhihang.doc.view.dto.Param;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class OpenApi3ParameterMetadataTest extends LightJavaCodeInsightFixtureTestCase {

    private Set<String> originalRequiredFieldAnnotations;
    private String originalSpringTemplate;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        installAnnotationStubs();

        Settings settings = Settings.getInstance(getProject());
        originalRequiredFieldAnnotations = new HashSet<>(settings.getRequiredFieldAnnotation());
        settings.setRequiredFieldAnnotation(new HashSet<>(Set.of("contract.Required")));
        originalSpringTemplate = TemplateSettings.getInstance(getProject()).getSpringTemplate();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Settings.getInstance(getProject()).setRequiredFieldAnnotation(originalRequiredFieldAnnotations);
            TemplateSettings.getInstance(getProject()).setSpringTemplate(originalSpringTemplate);
        } finally {
            super.tearDown();
        }
    }

    public void testSpringBindingNamesWinAndOpenApiNameIsFallback() {
        PsiClass controller = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.Parameter;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class ParameterNamesController {
                    @GetMapping("/names")
                    public void requestParams(
                            @RequestParam(name = "spring-name") @Parameter(name = "openapi-name") String nameAlias,
                            @RequestParam(value = "spring-value") String valueAlias,
                            @RequestParam @Parameter(name = "openapi-fallback") String openApiFallback,
                            @RequestParam(name = "spring-name", value = "spring-value")
                                    @Parameter(name = "openapi-must-not-mask-conflict") String conflictingAlias,
                            @Parameter(name = "openapi-only") String openApiOnly,
                            @Parameter(name = "   ") String blankOpenApiName) {
                    }

                    @GetMapping("/paths/{first}/{second}")
                    public void pathVariables(
                            @PathVariable(name = "path-name") @Parameter(name = "openapi-path") String first,
                            @PathVariable(value = "path-value") String second,
                            @PathVariable @Parameter(name = "openapi-path-fallback") String fallback) {
                    }
                }
                """);

        List<Param> requestParams = SpringPsiUtils.buildFormParam(method(controller, "requestParams"));
        assertEquals(List.of("spring-name", "spring-value", "openapi-fallback", "conflictingAlias", "openapi-only", "blankOpenApiName"),
                requestParams.stream().map(Param::getName).toList());

        List<Param> pathVariables = SpringPsiUtils.buildFormParam(method(controller, "pathVariables"));
        assertEquals(List.of("path-name", "path-value", "openapi-path-fallback"),
                pathVariables.stream().map(Param::getName).toList());
    }

    public void testRequestHeaderNameAndValueAliasesOpenApiFallbackAndConflict() {
        PsiClass controller = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.Parameter;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestHeader;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class HeaderNamesController {
                    @GetMapping("/header-names")
                    public void headers(
                            @RequestHeader(name = "X-Name") String nameAlias,
                            @RequestHeader(value = "X-Value") String valueAlias,
                            @RequestHeader @Parameter(name = "X-OpenAPI") String openApiFallback,
                            @RequestHeader(name = "X-First", value = "X-Second")
                                    @Parameter(name = "X-OpenAPI-Must-Not-Mask") String conflictingAlias) {
                    }
                }
                """);

        List<Header> headers = SpringPsiUtils.buildHeader(method(controller, "headers"));
        assertEquals(List.of("X-Name", "X-Value", "X-OpenAPI", "conflictingAlias"),
                headers.subList(1, headers.size()).stream().map(Header::getName).toList());
    }

    public void testRequestHeaderNameAndParameterDescriptionExampleAreMapped() {
        PsiClass controller = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.Parameter;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestHeader;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class HeaderMetadataController {
                    @GetMapping("/headers")
                    public void headers(
                            @RequestHeader(name = "X-Trace")
                            @Parameter(name = "documented-name", description = "  追踪标识  ", example = "trace-123")
                            String traceId) {
                    }
                }
                """);

        List<Header> headers = SpringPsiUtils.buildHeader(method(controller, "headers"));
        Header traceHeader = headers.get(1);
        assertEquals("X-Trace", traceHeader.getName());
        assertEquals("  追踪标识  ", traceHeader.getDesc());
        assertEquals("trace-123", traceHeader.getValue());
    }

    public void testParameterDescriptionAndExampleReachDocViewAndRequestExample() {
        PsiClass controller = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.Parameter;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class ParameterDescriptionController {
                    @GetMapping("/search")
                    public void search(
                            @RequestParam
                            @Parameter(name = "search", description = "参数描述", example = "alice") String userName,
                            @RequestParam
                            @Parameter(description = "", example = "") String fallback) {
                    }
                }
                """);

        PsiMethod method = method(controller, "search");
        DocView docView = new SpringDocViewServiceImpl().buildClassMethodDoc(controller, method);
        assertEquals("search", docView.getReqParamList().get(0).getName());
        assertEquals("参数描述", docView.getReqParamList().get(0).getDesc());
        assertEquals("alice", docView.getReqParamList().get(0).getExample());
        assertEquals("search=alice&fallback=null", docView.getReqFormExample());
    }

    public void testNonEmptyCompoundParameterExamplesSuppressScalarExample() {
        PsiClass controller = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.Parameter;
                import io.swagger.v3.oas.annotations.media.ArraySchema;
                import io.swagger.v3.oas.annotations.media.Content;
                import io.swagger.v3.oas.annotations.media.ExampleObject;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestHeader;
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class CompoundParameterExampleController {
                    @GetMapping("/compound-examples")
                    public void examples(
                            @RequestParam @Parameter(example = "ignored-scalar", examples = {@ExampleObject(name = "full")})
                                    String exampleObject,
                            @RequestParam @Parameter(example = "ignored-scalar", content = {@Content(mediaType = "application/json")})
                                    String content,
                            @RequestParam @Parameter(example = "ignored-scalar", array = @ArraySchema(schema = @Schema(type = "string")))
                                    String array,
                            @RequestParam @Parameter(example = "retained-scalar") String scalar,
                            @RequestParam @Parameter(example = "retained-empty-examples", examples = {}) String emptyExamples,
                            @RequestParam @Parameter(example = "retained-empty-content", content = {}) String emptyContent,
                            @RequestParam @Parameter(example = "retained-empty-array", array = @ArraySchema) String emptyArray,
                            @RequestHeader @Parameter(example = "ignored-scalar", content = @Content(mediaType = "text/plain"))
                                    String header) {
                    }
                }
                """);

        List<Param> params = SpringPsiUtils.buildFormParam(method(controller, "examples"));
        assertEquals(7, params.size());
        assertNull(params.get(0).getExample());
        assertNull(params.get(1).getExample());
        assertNull(params.get(2).getExample());
        assertEquals("retained-scalar", params.get(3).getExample());
        assertEquals("retained-empty-examples", params.get(4).getExample());
        assertEquals("retained-empty-content", params.get(5).getExample());
        assertEquals("retained-empty-array", params.get(6).getExample());

        List<Header> headers = SpringPsiUtils.buildHeader(method(controller, "examples"));
        assertNull(headers.get(1).getValue());
    }

    public void testParameterMetadataRendersInSpringMarkdownGolden() throws Exception {
        PsiClass controller = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.Parameter;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestHeader;
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class ParameterMarkdownController {
                    @GetMapping("/metadata")
                    public void metadata(
                            @RequestParam(value = "user")
                            @Parameter(name = "openapi-name-is-lower-priority", description = "用户账号", example = "alice", required = false)
                            String query,
                            @Parameter(name = "manual", description = "手动参数", example = "selected", required = false)
                            String manual,
                            @RequestHeader(value = "X-Trace")
                            @Parameter(name = "openapi-header-name-is-lower-priority", description = "追踪请求", example = "trace-1", required = false)
                            String trace) {
                    }
                }
                """);

        TemplateSettings.getInstance(getProject()).setSpringTemplate(
                "${DocView.requestHeader}---PARAMS---${DocView.requestParam}---REQUEST---${DocView.requestExample}\n");
        DocView docView = new SpringDocViewServiceImpl().buildClassMethodDoc(controller, method(controller, "metadata"));
        String markdown = DocViewData.markdownText(getProject(), docView);
        Path goldenFile = Path.of(getTestDataPath(), "contracts/markdown/openapi3-parameter-metadata.md");
        String expectedMarkdown = StringUtil.convertLineSeparators(
                Files.readString(goldenFile, StandardCharsets.UTF_8));
        assertEquals(expectedMarkdown, markdown);
    }

    public void testEmptyParameterDescriptionRetainsJavadocFallback() {
        PsiClass controller = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.Parameter;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class ParameterFallbackController {
                    /**
                     * @param query Javadoc 参数说明
                     */
                    @GetMapping("/fallback")
                    public void fallback(@RequestParam @Parameter(description = "   ") String query) {
                    }
                }
                """);

        Param query = SpringPsiUtils.buildFormParam(method(controller, "fallback")).get(0);
        assertEquals("Javadoc 参数说明", query.getDesc());
    }

    public void testRequiredUsesConfiguredMarkerThenSpringResolverThenExplicitParameter() {
        PsiClass controller = myFixture.addClass("""
                package contract.openapi3;

                import contract.Required;
                import io.swagger.v3.oas.annotations.Parameter;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class RequiredMetadataController {
                    @GetMapping("/required/{path}")
                    public void required(
                            @RequestParam String defaultRequestParam,
                            @RequestParam(required = false) @Parameter(required = true) String optionalRequestParam,
                            @RequestParam(defaultValue = "fallback") String defaultedRequestParam,
                            @PathVariable String defaultPath,
                            @PathVariable(required = false) @Parameter(required = true) String optionalPath,
                            @Parameter(required = true) String openApiRequired,
                            @Parameter(required = false) String openApiOptional,
                            @RequestParam(required = false) @Required @Parameter(required = false) String configuredRequired) {
                    }
                }
                """);

        List<Param> params = SpringPsiUtils.buildFormParam(method(controller, "required"));
        assertEquals(List.of(true, false, false, true, false, true, false, true),
                params.stream().map(Param::getRequired).toList());
    }

    public void testRequestHeaderRequiredUsesSpringDefaultValueAndConfiguredMarker() {
        PsiClass controller = myFixture.addClass("""
                package contract.openapi3;

                import contract.Required;
                import io.swagger.v3.oas.annotations.Parameter;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestHeader;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class HeaderRequiredController {
                    @GetMapping("/header-required")
                    public void headers(
                            @RequestHeader String defaultHeader,
                            @RequestHeader(required = false) @Parameter(required = true) String optionalHeader,
                            @RequestHeader(defaultValue = "fallback") String defaultedHeader,
                            @RequestHeader(required = false) @Required @Parameter(required = false) String configuredHeader) {
                    }
                }
                """);

        List<Header> headers = SpringPsiUtils.buildHeader(method(controller, "headers"));
        assertEquals(List.of(true, false, false, true),
                headers.subList(1, headers.size()).stream().map(Header::getRequired).toList());
    }

    public void testMetadataDoesNotRenameFlattenedFieldsOrRequestBodyAndIgnoresMethodLevelAnnotations() {
        myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.Parameter;

                public class Filter {
                    @Parameter(name = "field-level")
                    public String status;
                }
                """);
        myFixture.addClass("""
                package contract.openapi3;

                public class Body {
                    public String value;
                }
                """);
        PsiClass controller = myFixture.addClass("""
                package contract.openapi3;

                import io.swagger.v3.oas.annotations.Operation;
                import io.swagger.v3.oas.annotations.Parameter;
                import io.swagger.v3.oas.annotations.Parameters;
                import io.swagger.v3.oas.annotations.media.Schema;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RequestParam;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                public class MetadataBoundaryController {
                    @GetMapping("/boundary")
                    @Parameter(name = "method-level")
                    @Parameters({@Parameter(name = "parameters-container")})
                    @Operation(parameters = {@Parameter(name = "operation-parameter")})
                    public void boundary(
                            @Parameter(name = "object-level") Filter filter,
                            @RequestBody @Parameter(name = "body-level") Body body,
                            @RequestParam @Schema(name = "schema-level") String direct) {
                    }
                }
                """);

        List<Param> params = SpringPsiUtils.buildFormParam(method(controller, "boundary"));
        assertEquals(List.of("status", "direct"), params.stream().map(Param::getName).toList());
    }

    private void installAnnotationStubs() {
        myFixture.addClass("""
                package io.swagger.v3.oas.annotations;

                import io.swagger.v3.oas.annotations.media.ArraySchema;
                import io.swagger.v3.oas.annotations.media.Content;
                import io.swagger.v3.oas.annotations.media.ExampleObject;

                public @interface Parameter {
                    String name() default "";
                    String description() default "";
                    String example() default "";
                    boolean required() default false;
                    ExampleObject[] examples() default {};
                    Content[] content() default {};
                    ArraySchema array() default @ArraySchema;
                }
                """);
        myFixture.addClass("""
                package io.swagger.v3.oas.annotations;

                public @interface Parameters {
                    Parameter[] value() default {};
                }
                """);
        myFixture.addClass("""
                package io.swagger.v3.oas.annotations;

                public @interface Operation {
                    Parameter[] parameters() default {};
                }
                """);
        myFixture.addClass("""
                package io.swagger.v3.oas.annotations.media;

                public @interface Schema {
                    String name() default "";
                    String type() default "";
                }
                """);
        myFixture.addClass("""
                package io.swagger.v3.oas.annotations.media;

                public @interface ExampleObject {
                    String name() default "";
                }
                """);
        myFixture.addClass("""
                package io.swagger.v3.oas.annotations.media;

                public @interface Content {
                    String mediaType() default "";
                }
                """);
        myFixture.addClass("""
                package io.swagger.v3.oas.annotations.media;

                public @interface ArraySchema {
                    Schema schema() default @Schema;
                }
                """);
        myFixture.addClass("""
                package org.springframework.stereotype;

                public @interface Controller {
                }
                """);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface RestController {
                }
                """);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface RequestMapping {
                    String[] value() default {};
                }
                """);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface GetMapping {
                    String[] value() default {};
                }
                """);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface RequestParam {
                    String name() default "";
                    String value() default "";
                    String defaultValue() default "\\n\\t\\t\\n\\t\\t\\n\\uE000\\uE001\\uE002";
                    boolean required() default true;
                }
                """);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface PathVariable {
                    String name() default "";
                    String value() default "";
                    boolean required() default true;
                }
                """);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface RequestHeader {
                    String name() default "";
                    String value() default "";
                    String defaultValue() default "\\n\\t\\t\\n\\t\\t\\n\\uE000\\uE001\\uE002";
                    boolean required() default true;
                }
                """);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;

                public @interface RequestBody {
                }
                """);
        myFixture.addClass("""
                package contract;

                public @interface Required {
                }
                """);
    }

    private PsiMethod method(PsiClass psiClass, String name) {
        PsiMethod[] methods = psiClass.findMethodsByName(name, false);
        assertEquals("Expected one fixture method named " + name, 1, methods.length);
        return methods[0];
    }

    @Override
    protected String getTestDataPath() {
        return Path.of("src/test/testData").toAbsolutePath().toString();
    }
}

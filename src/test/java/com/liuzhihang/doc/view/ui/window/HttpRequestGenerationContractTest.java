package com.liuzhihang.doc.view.ui.window;

import com.intellij.ide.extensionResources.ExtensionsRootType;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.treeStructure.SimpleNode;
import com.liuzhihang.doc.view.service.http.HttpRequestBuilder;
import com.liuzhihang.doc.view.service.http.HttpRequestException;
import com.liuzhihang.doc.view.service.http.HttpRequestSnapshot;
import com.liuzhihang.doc.view.service.http.HttpRequestTestSupport;
import com.liuzhihang.doc.view.utils.CustomFileUtils;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Captures the current HTTP request behavior through the real Scratch-file entry point.
 * These assertions intentionally describe the target contract and expose the old generator's
 * parameter-location, header, form Content-Type, and missing-example defects.
 */
public final class HttpRequestGenerationContractTest extends LightJavaCodeInsightFixtureTestCase {

    private static final String SCRATCH_MARKER = "HttpRequestGenerationContract";
    private final List<VirtualFile> createdScratchFiles = new ArrayList<>();
    private int fixtureSequence;

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_21;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        installHttpAnnotations();
        installOpenApiParameter();
        HttpRequestTestSupport.install(getProject(), getTestRootDisposable());
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Set<VirtualFile> scratchFiles = new HashSet<>(createdScratchFiles);
            EdtTestUtil.runInEdtAndWait(() -> {
                FileEditorManager manager = FileEditorManager.getInstance(getProject());
                for (VirtualFile file : scratchFiles) {
                    if (file.isValid()) {
                        manager.closeFile(file);
                    }
                }
            });
            WriteCommandAction.runWriteCommandAction(getProject(), () -> {
                for (VirtualFile file : scratchFiles) {
                    if (isTestScratch(file) && file.isValid()) {
                        try {
                            if (!file.isWritable()) {
                                file.setWritable(true);
                            }
                            file.delete(getProject());
                        } catch (IOException ignored) {
                            // The light test project is discarded after this fixture.
                        }
                    }
                }
            });
        } finally {
            super.tearDown();
        }
    }

    public void testPathVariableIsSubstitutedAndNotRepeatedInQuery() throws Exception {
        String text = generate("PathController", "lookup", """
                @org.springframework.web.bind.annotation.PostMapping("/users/{userId}")
                public void lookup(
                        @org.springframework.web.bind.annotation.PathVariable("userId")
                        @io.swagger.v3.oas.annotations.Parameter(example = "42") String userId) {
                }
                """);

        assertTrue("The concrete path example must replace the Spring path placeholder: " + text,
                text.contains("POST {{host}}/api/users/42"));
        assertFalse("A path variable must not be appended as a query parameter: " + text,
                text.contains("?userId=") || text.contains("&userId="));
    }

    public void testOptionalPathVariableWithoutExampleStillGetsHttpClientVariable() throws Exception {
        String text = buildDirect("""
                @org.springframework.web.bind.annotation.GetMapping("/items/{id}")
                public void call(
                        @org.springframework.web.bind.annotation.PathVariable(value = "id", required = false) String id) { }
                """);
        assertTrue(text, text.contains("GET {{host}}/api/items/{{path_id}}"));
    }

    public void testQueryNameIsEncodedAsDataEvenWhenItLooksLikeHttpClientVariable() throws Exception {
        String text = buildDirect("""
                @org.springframework.web.bind.annotation.GetMapping("/items")
                public void call(
                        @org.springframework.web.bind.annotation.RequestParam(name = "{{tenant}}")
                        @io.swagger.v3.oas.annotations.Parameter(example = "blue") String tenant) { }
                """);
        assertTrue(text, text.contains("?%7B%7Btenant%7D%7D=blue"));
        assertFalse(text, text.contains("?{{tenant}}="));
    }

    public void testFormParametersUseFormContentTypeAndRequestBody() throws Exception {
        String text = generate("FormController", "submit", """
                @org.springframework.web.bind.annotation.PostMapping(
                        path = "/form", consumes = "application/x-www-form-urlencoded")
                public void submit(
                        @org.springframework.web.bind.annotation.RequestParam(name = "display")
                        @io.swagger.v3.oas.annotations.Parameter(example = "Alice & Bob") String display) {
                }
                """);

        assertTrue("A form request must declare its effective form media type: " + text,
                text.contains("Content-Type: application/x-www-form-urlencoded"));
        assertFalse("An explicitly form-bound value must not be duplicated in the query: " + text,
                text.contains("?display=") || text.contains("&display="));
        assertTrue("The form value must be present after the request headers: " + text,
                hasNonEmptyRequestBody(text));
    }

    public void testRequestHeaderIsWrittenAsAHeader() throws Exception {
        String text = generate("HeaderController", "read", """
                @org.springframework.web.bind.annotation.GetMapping("/headers")
                public void read(
                        @org.springframework.web.bind.annotation.RequestHeader(name = "X-Trace")
                        @io.swagger.v3.oas.annotations.Parameter(example = "trace-1") String trace) {
                }
                """);

        assertTrue("A Spring RequestHeader must be emitted as a header line: " + text,
                text.contains("X-Trace: trace-1"));
        assertFalse("A header binding must not be moved into the URL: " + text,
                text.contains("?X-Trace=") || text.contains("&X-Trace="));
    }

    public void testMissingRequiredQueryExampleUsesVariableInsteadOfNull() throws Exception {
        String text = generate("NullController", "search", """
                @org.springframework.web.bind.annotation.GetMapping("/search")
                public void search(
                        @org.springframework.web.bind.annotation.RequestParam(name = "q") String query) {
                }
                """);

        assertFalse("An absent example must never become the Java text `null`: " + text,
                text.contains("q=null"));
        assertTrue("A required query without an example must remain editable through an HTTP Client variable: " + text,
                text.matches("(?s).*q=\\{\\{[^{}\\s]+}}.*"));
    }

    public void testBindingNameAndLocationIgnoreOpenApiDisplayMetadata() throws Exception {
        String text = buildDirect("""
                @org.springframework.web.bind.annotation.GetMapping("/items")
                public void call(
                        @org.springframework.web.bind.annotation.RequestParam(name = "id")
                        @io.swagger.v3.oas.annotations.Parameter(name = "用户编号", in = "HEADER", example = "42")
                        String userId) { }
                """);

        assertTrue(text, text.contains("GET {{host}}/api/items?id=42"));
        assertFalse(text, text.contains("用户编号"));
        assertFalse(text, text.contains("id=42&"));
    }

    public void testPathValueAliasesAndCompileTimeConstantsProduceOneEndpoint() throws Exception {
        String text = buildDirect("""
                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping(path = Endpoint.BASE)
                class Endpoint {
                    public static final String BASE = "/api";
                    public static final String USER_PATH = "/users";
                    @org.springframework.web.bind.annotation.RequestMapping(
                            value = Endpoint.USER_PATH + "/{id}",
                            method = org.springframework.web.bind.annotation.RequestMethod.GET)
                    public void call(
                            @org.springframework.web.bind.annotation.PathVariable(name = "id")
                            @io.swagger.v3.oas.annotations.Parameter(example = "a/b") String userId) { }
                }
                """, "Endpoint", "call");

        assertTrue(text, text.contains("GET {{host}}/api/users/a%2Fb"));
        assertFalse(text, text.contains("?id="));

        String classMethod = buildDirect("""
                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping(method =
                        org.springframework.web.bind.annotation.RequestMethod.GET)
                class Endpoint {
                    @org.springframework.web.bind.annotation.RequestMapping(path = "/health")
                    public void call() { }
                }
                """, "Endpoint", "call");
        assertTrue(classMethod, classMethod.contains("GET {{host}}/health"));
    }

    public void testTrailingSlashIsPreservedAndBlankArrayEntryDoesNotHideMultiplePaths() throws Exception {
        String text = buildDirect("""
                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping(path = "/api/")
                class Endpoint {
                    @org.springframework.web.bind.annotation.GetMapping(path = "/items/")
                    public void call() { }
                }
                """, "Endpoint", "call");
        assertTrue(text, text.contains("GET {{host}}/api/items/"));

        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.GetMapping(path = {"", "/items"})
                public void call() { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.GetMapping(value = {"", "/items"})
                public void call() { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.GetMapping("/items with-space")
                public void call() { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.GetMapping("/items#fragment")
                public void call() { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.GetMapping("/items\\r\\nX-Injected: yes")
                public void call() { }
                """);
    }

    public void testCandidateCheckUsesAnnotationSyntaxWithoutResolvingNames() {
        PsiClass endpoint = myFixture.addClass("""
                package contract.httprequest;
                import org.springframework.web.bind.annotation.RestController;
                import org.springframework.web.bind.annotation.GetMapping;
                @RestController
                class CandidateEndpoint {
                    @GetMapping("/items") void call() { }
                }
                """);
        assertTrue(HttpRequestBuilder.isCandidate(endpoint.findMethodsByName("call", false)[0]));
    }

    public void testQueryAndFormValuesAreEncodedOnceAndVariablesRemainEditable() throws Exception {
        String query = buildDirect("""
                @org.springframework.web.bind.annotation.GetMapping("/search")
                public void call(
                        @org.springframework.web.bind.annotation.RequestParam(name = "q")
                        @io.swagger.v3.oas.annotations.Parameter(example = "a&b 空格") String query) { }
                """);
        assertTrue(query, query.contains("?q=a%26b%20%E7%A9%BA%E6%A0%BC"));
        assertFalse(query, query.contains("%2526"));

        String variable = buildDirect("""
                @org.springframework.web.bind.annotation.GetMapping("/search")
                public void call(@org.springframework.web.bind.annotation.RequestParam String query) { }
                """);
        assertTrue(variable, variable.matches("(?s).*query=\\{\\{query_query}}.*"));
    }

    public void testJsonBodyCoexistsWithQueryAndHeaderAndUsesEffectiveConsumes() throws Exception {
        String text = buildDirect("""
                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping(consumes = "application/x-www-form-urlencoded")
                class Endpoint {
                    static class Payload { String name; }
                    @org.springframework.web.bind.annotation.PostMapping(path = "/items", consumes = "application/json")
                    public void call(
                            @org.springframework.web.bind.annotation.RequestBody Payload body,
                            @org.springframework.web.bind.annotation.RequestParam(name = "dryRun")
                            @io.swagger.v3.oas.annotations.Parameter(example = "true") boolean dryRun,
                            @org.springframework.web.bind.annotation.RequestHeader(name = "X-Trace")
                            @io.swagger.v3.oas.annotations.Parameter(example = "trace-1") String trace) { }
                }
                """, "Endpoint", "call");

        assertTrue(text, text.contains("POST {{host}}/items?dryRun=true"));
        assertTrue(text, text.contains("X-Trace: trace-1"));
        assertEquals(1, countOccurrences(text, "Content-Type: application/json"));
        String body = requestBody(text);
        assertTrue(body, JsonParser.parseString(body).isJsonObject());
        assertFalse(text, text.contains("Content-Type: application/x-www-form-urlencoded"));
    }

    public void testMatchingContentTypeIsMergedAndConflictingTypeIsRejected() throws Exception {
        String matching = buildDirect("""
                @org.springframework.web.bind.annotation.RestController
                class Endpoint {
                    static class Payload { String name; }
                    @org.springframework.web.bind.annotation.PostMapping("/items")
                    public void call(
                            @org.springframework.web.bind.annotation.RequestBody Payload body,
                            @org.springframework.web.bind.annotation.RequestHeader(name = "Content-Type")
                            @io.swagger.v3.oas.annotations.Parameter(example = "application/json") String type) { }
                }
                """, "Endpoint", "call");

        String inferred = buildDirect("""
                @org.springframework.web.bind.annotation.RestController
                class Endpoint {
                    static class Payload { String name; }
                    @org.springframework.web.bind.annotation.PostMapping("/items")
                    public void call(
                            @org.springframework.web.bind.annotation.RequestBody Payload body,
                            @org.springframework.web.bind.annotation.RequestHeader(name = "Content-Type") String type) { }
                }
                """, "Endpoint", "call");
        assertEquals(1, countOccurrences(inferred, "Content-Type: application/json"));
        assertFalse(inferred, inferred.contains("{{header_content_type}}"));

        assertBuildFailure(HttpRequestException.CONTENT_TYPE_CONFLICT, """
                @org.springframework.web.bind.annotation.GetMapping("/items")
                public void call(
                        @org.springframework.web.bind.annotation.RequestHeader(name = "Content-Type") String first,
                        @org.springframework.web.bind.annotation.RequestHeader(name = "content-type") String second) { }
                """);
        assertEquals(1, countOccurrences(matching, "Content-Type: application/json"));

        assertBuildFailure(HttpRequestException.CONTENT_TYPE_CONFLICT, """
                @org.springframework.web.bind.annotation.RestController
                class Endpoint {
                    static class Payload { String name; }
                    @org.springframework.web.bind.annotation.PostMapping("/items")
                    public void call(
                            @org.springframework.web.bind.annotation.RequestBody Payload body,
                            @org.springframework.web.bind.annotation.RequestHeader(name = "Content-Type")
                            @io.swagger.v3.oas.annotations.Parameter(example = "application/x-www-form-urlencoded")
                            String type) { }
                }
                """, "Endpoint", "call");
    }

    public void testClassLevelFormConsumesIsInheritedAndFormParametersStayOutOfQuery() throws Exception {
        String text = buildDirect("""
                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping(path = "/api", consumes = "application/x-www-form-urlencoded")
                class Endpoint {
                    @org.springframework.web.bind.annotation.PostMapping("/items/{id}")
                    public void call(
                            @org.springframework.web.bind.annotation.PathVariable("id")
                            @io.swagger.v3.oas.annotations.Parameter(example = "42") String id,
                            @org.springframework.web.bind.annotation.RequestParam(name = "display")
                            @io.swagger.v3.oas.annotations.Parameter(example = "Alice & Bob") String display,
                            @org.springframework.web.bind.annotation.RequestHeader(name = "X-Trace")
                            @io.swagger.v3.oas.annotations.Parameter(example = "trace-1") String trace) { }
                }
                """, "Endpoint", "call");

        assertTrue(text, text.contains("POST {{host}}/api/items/42"));
        assertTrue(text, text.contains("Content-Type: application/x-www-form-urlencoded"));
        assertTrue(text, text.contains("X-Trace: trace-1"));
        assertTrue(text, text.endsWith("display=Alice+%26+Bob"));
        assertFalse(text, text.contains("?display="));
    }

    public void testGetWithoutBodyDoesNotInferContentTypeAndAuthenticationUsesVariables() throws Exception {
        String text = buildDirect("""
                @org.springframework.web.bind.annotation.GetMapping("/items")
                public void call(
                        @org.springframework.web.bind.annotation.RequestHeader(name = "Authorization")
                        @io.swagger.v3.oas.annotations.Parameter(example = "Bearer do-not-copy") String authorization,
                        @org.springframework.web.bind.annotation.RequestHeader(name = "X-Version")
                        @io.swagger.v3.oas.annotations.Parameter(example = "2") int version) { }
                """);

        assertTrue(text, text.contains("Authorization: {{header_authorization}}"));
        assertTrue(text, text.contains("X-Version: 2"));
        assertFalse(text, text.contains("Content-Type:"));
        assertFalse(text, text.contains("Bearer do-not-copy"));
    }

    public void testSpringRequiredDefaultAndEmptyDefaultControlOptionalValues() throws Exception {
        String text = buildDirect("""
                @org.springframework.web.bind.annotation.GetMapping("/search")
                public void call(
                        @org.springframework.web.bind.annotation.RequestParam(name = "required") String required,
                        @org.springframework.web.bind.annotation.RequestParam(name = "optional", required = false)
                        @io.swagger.v3.oas.annotations.Parameter(required = true) String optional,
                        @org.springframework.web.bind.annotation.RequestParam(name = "empty", defaultValue = "") String empty,
                        @org.springframework.web.bind.annotation.RequestParam(name = "priority", defaultValue = "10")
                        @io.swagger.v3.oas.annotations.Parameter(example = "20", required = false) String priority) { }
                """);

        assertTrue(text, text.contains("required={{query_required}}"));
        assertTrue(text, text.contains("empty="));
        assertTrue(text, text.contains("priority=20"));
        assertFalse(text, text.contains("optional="));
        assertFalse(text, text.contains("priority=10"));
        assertFalse(text, text.contains("null"));
        assertEquals(-1, text.indexOf('\uE000'));
        assertFalse("The Spring no-default sentinel must never become a URL parameter: " + text,
                text.contains("%0A%09%09"));
    }

    public void testSpringHeaderRequiredOptionalAndExplicitDefaults() throws Exception {
        String text = buildDirect("""
                @org.springframework.web.bind.annotation.GetMapping("/headers")
                public void call(
                        @org.springframework.web.bind.annotation.RequestHeader(name = "X-Required") String required,
                        @org.springframework.web.bind.annotation.RequestHeader(name = "X-Optional", required = false)
                        String optional,
                        @org.springframework.web.bind.annotation.RequestHeader(name = "X-Default", defaultValue = "trace-1")
                        String withDefault,
                        @org.springframework.web.bind.annotation.RequestHeader(name = "X-Empty", defaultValue = "")
                        String emptyDefault) { }
                """);

        assertTrue("A required header without a default needs an editable variable: " + text,
                text.contains("X-Required: {{header_x_required}}"));
        assertFalse("An optional header without a default should be omitted: " + text,
                text.contains("X-Optional:"));
        assertTrue(text, text.contains("X-Default: trace-1"));
        assertTrue("An explicitly empty header default remains present: " + text,
                text.contains("X-Empty: \n"));
        assertEquals(-1, text.indexOf('\uE000'));
    }

    public void testPathBindingMismatchAndUnsafeHeaderAreRejected() {
        assertBuildFailure(HttpRequestException.PATH_PARAMETER_MISMATCH, """
                @org.springframework.web.bind.annotation.GetMapping("/items/{id}")
                public void call(@org.springframework.web.bind.annotation.PathVariable("other") String id) { }
                """);
        assertBuildFailure(HttpRequestException.UNSAFE_HEADER, """
                @org.springframework.web.bind.annotation.GetMapping("/items")
                public void call(
                        @org.springframework.web.bind.annotation.RequestHeader(name = "X-Trace")
                        @io.swagger.v3.oas.annotations.Parameter(example = "one\\r\\nX-Admin: yes") String trace) { }
                """);
        assertBuildFailure(HttpRequestException.UNSAFE_HEADER, """
                @org.springframework.web.bind.annotation.GetMapping("/items")
                public void call(
                        @org.springframework.web.bind.annotation.RequestHeader(name = "Content-Type")
                        @io.swagger.v3.oas.annotations.Parameter(example = "text/plain\\r\\nX-Admin: yes") String type) { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_PARAMETER, """
                @org.springframework.web.bind.annotation.GetMapping("/items")
                public void call(
                        @org.springframework.web.bind.annotation.RequestHeader(name = "X-Trace") String first,
                        @org.springframework.web.bind.annotation.RequestHeader(name = "x-trace") String second) { }
                """);
    }

    public void testAmbiguousMappingsAndUnboundParametersAreRejected() {
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.GetMapping({"/first", "/second"})
                public void call() { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.RequestMapping("/items")
                public void call() { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.GetMapping(path = "/items/{id:[0-9]+}")
                public void call(@org.springframework.web.bind.annotation.PathVariable("id") String id) { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.GetMapping(path = "/items", params = "mode=full")
                public void call() { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.RequestMapping(
                        value = "/items", method = {org.springframework.web.bind.annotation.RequestMethod.GET,
                                                     org.springframework.web.bind.annotation.RequestMethod.POST})
                public void call() { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping(method = {
                        org.springframework.web.bind.annotation.RequestMethod.GET,
                        org.springframework.web.bind.annotation.RequestMethod.POST})
                class Endpoint {
                    @org.springframework.web.bind.annotation.RequestMapping(value = "/items",
                            method = org.springframework.web.bind.annotation.RequestMethod.POST)
                    public void call() { }
                }
                """, "Endpoint", "call");
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping(method =
                        org.springframework.web.bind.annotation.RequestMethod.POST)
                class Endpoint {
                    @org.springframework.web.bind.annotation.RequestMapping(value = "/items", method = {
                            org.springframework.web.bind.annotation.RequestMethod.GET,
                            org.springframework.web.bind.annotation.RequestMethod.POST})
                    public void call() { }
                }
                """, "Endpoint", "call");
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.GetMapping("${api.path}")
                public void call() { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.RequestMapping(path = "/one", value = "/two",
                        method = org.springframework.web.bind.annotation.RequestMethod.GET)
                public void call() { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.GetMapping(path = "/items", headers = "X-Version=2")
                public void call() { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, """
                @org.springframework.web.bind.annotation.GetMapping("/items")
                @interface CustomGet { }
                @CustomGet public void call() { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_PARAMETER, """
                @org.springframework.web.bind.annotation.GetMapping("/items")
                public void call(String businessObject) { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_PARAMETER, """
                @org.springframework.web.bind.annotation.GetMapping("/items")
                public void call(@org.springframework.web.bind.annotation.RequestParam java.util.Map<String, String> filter) { }
                """);
        myFixture.addClass("package test.types; public class String { }");
        assertBuildFailure(HttpRequestException.UNSUPPORTED_PARAMETER, """
                @org.springframework.web.bind.annotation.GetMapping("/items")
                public void call(@org.springframework.web.bind.annotation.RequestParam test.types.String value) { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_PARAMETER, """
                @org.springframework.web.bind.annotation.GetMapping("/items")
                public void call(@org.springframework.web.bind.annotation.ModelAttribute String filter) { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_PARAMETER, """
                @org.springframework.web.bind.annotation.GetMapping("/items")
                public void call(@org.springframework.web.bind.annotation.CookieValue String token) { }
                """);
    }

    public void testUnsupportedConsumesAndMultipleBodiesAreRejected() {
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MEDIA_TYPE, """
                @org.springframework.web.bind.annotation.PostMapping(path = "/items", consumes = "application/xml")
                public void call(@org.springframework.web.bind.annotation.RequestBody String body) { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MEDIA_TYPE, """
                @org.springframework.web.bind.annotation.PostMapping(consumes = "application/json")
                public void call() { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_PARAMETER, """
                @org.springframework.web.bind.annotation.PostMapping("/items")
                public void call(
                        @org.springframework.web.bind.annotation.RequestBody String first,
                        @org.springframework.web.bind.annotation.RequestBody String second) { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_PARAMETER, """
                @org.springframework.web.bind.annotation.PostMapping(consumes = "multipart/form-data")
                public void call(@org.springframework.web.bind.annotation.RequestPart String file) { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MEDIA_TYPE, """
                @org.springframework.web.bind.annotation.PostMapping(consumes = {"application/json", "application/xml"})
                public void call(@org.springframework.web.bind.annotation.RequestBody String body) { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_PARAMETER, """
                @org.springframework.web.bind.annotation.PostMapping("/items")
                public void call(@org.springframework.web.bind.annotation.RequestBody java.io.InputStream body) { }
                """);
    }

    public void testUnsupportedArrayAndCollectionBodyElementsAreRejected() {
        myFixture.addClass("""
                package org.springframework.web.multipart;
                public interface MultipartFile { }
                """);

        assertBuildFailure(HttpRequestException.UNSUPPORTED_PARAMETER, """
                @org.springframework.web.bind.annotation.PostMapping("/items")
                public void call(@org.springframework.web.bind.annotation.RequestBody java.io.InputStream[] body) { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_PARAMETER, """
                @org.springframework.web.bind.annotation.PostMapping("/items")
                public void call(@org.springframework.web.bind.annotation.RequestBody
                                 org.springframework.web.multipart.MultipartFile[] files) { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_PARAMETER, """
                @org.springframework.web.bind.annotation.PostMapping("/items")
                public void call(@org.springframework.web.bind.annotation.RequestBody
                                 java.util.List<java.io.InputStream> body) { }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_PARAMETER, """
                @org.springframework.web.bind.annotation.PostMapping("/items")
                public void call(@org.springframework.web.bind.annotation.RequestBody
                                 java.util.List<org.springframework.web.multipart.MultipartFile> files) { }
                """);
    }

    public void testAllOptionalFormParametersStillProduceAnEmptyFormBody() throws Exception {
        String text = buildDirect("""
                @org.springframework.web.bind.annotation.PostMapping(
                        path = "/items", consumes = "application/x-www-form-urlencoded")
                public void call(@org.springframework.web.bind.annotation.RequestParam(name = "optional", required = false)
                                 String optional) { }
                """);
        assertTrue(text, text.contains("Content-Type: application/x-www-form-urlencoded"));
        assertEquals("", requestBody(text));
    }

    public void testCollectionArrayAndParameterizedDtoBodiesUseBoundedValidJson() throws Exception {
        String collection = buildDirect("""
                @org.springframework.web.bind.annotation.PostMapping("/items")
                public void call(@org.springframework.web.bind.annotation.RequestBody java.util.List<String> items) { }
                """);
        assertTrue(JsonParser.parseString(requestBody(collection)).isJsonArray());

        String array = buildDirect("""
                @org.springframework.web.bind.annotation.PostMapping("/items")
                public void call(@org.springframework.web.bind.annotation.RequestBody String[] items) { }
                """);
        assertEquals("[]", requestBody(array));

        String genericDto = buildDirect("""
                @org.springframework.web.bind.annotation.RestController
                class Endpoint {
                    static class Item { String name; }
                    static class Envelope<T> { T data; }
                    @org.springframework.web.bind.annotation.PostMapping("/items")
                    public void call(
                            @org.springframework.web.bind.annotation.RequestBody Envelope<java.util.List<Item>> body) { }
                }
                """, "Endpoint", "call");
        var body = JsonParser.parseString(requestBody(genericDto)).getAsJsonObject();
        assertTrue(body.has("data"));
        assertTrue(body.getAsJsonArray("data").get(0).getAsJsonObject().has("name"));
    }

    public void testInheritedMappingIsRejectedWithoutExpansion() {
        String apiName = nextFixtureClassName();
        myFixture.addClass("""
                package contract.httprequest;
                public interface %s {
                    @org.springframework.web.bind.annotation.GetMapping("/items")
                    void call();
                }
                """.formatted(apiName));
        String controllerName = nextFixtureClassName();
        PsiClass controller = myFixture.addClass("""
                package contract.httprequest;
                @org.springframework.web.bind.annotation.RestController
                public class %s implements %s {
                    @Override public void call() { }
                }
                """.formatted(controllerName, apiName));
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING, controller.findMethodsByName("call", false)[0]);
    }

    public void testFeignStandardMappingUsesVariableHostAndDubboIsNotCandidate() throws Exception {
        PsiClass feign = myFixture.addClass("""
                package contract.httprequest;
                @org.springframework.cloud.openfeign.FeignClient(name = "catalog")
                public interface CatalogClient {
                    @org.springframework.web.bind.annotation.GetMapping("/catalog")
                    String call();
                }
                """);
        PsiMethod method = feign.findMethodsByName("call", false)[0];
        assertTrue(HttpRequestBuilder.isCandidate(method));
        String text = build(method);
        assertTrue(text, text.contains("GET {{host}}/catalog"));
        assertFalse(text, text.contains("http://catalog"));

        PsiClass prefixedFeign = myFixture.addClass("""
                package contract.httprequest;
                @org.springframework.cloud.openfeign.FeignClient(name = "catalog", path = "/gateway/v1/")
                public interface CatalogPrefixedClient {
                    @org.springframework.web.bind.annotation.GetMapping("/catalog")
                    String call();
                }
                """);
        String prefixed = build(prefixedFeign.findMethodsByName("call", false)[0]);
        assertTrue(prefixed, prefixed.contains("GET {{host}}/gateway/v1/catalog"));
        assertFalse(prefixed, prefixed.contains("http://catalog"));

        PsiClass dynamicFeign = myFixture.addClass("""
                package contract.httprequest;
                @org.springframework.cloud.openfeign.FeignClient(name = "catalog", path = "${gateway.prefix}")
                public interface DynamicPathClient {
                    @org.springframework.web.bind.annotation.GetMapping("/catalog")
                    String call();
                }
                """);
        assertBuildFailure(HttpRequestException.UNSUPPORTED_MAPPING,
                dynamicFeign.findMethodsByName("call", false)[0]);

        PsiClass dubbo = myFixture.addClass("""
                package contract.httprequest;
                @org.apache.dubbo.config.annotation.DubboService
                public interface InventoryService {
                    @org.springframework.web.bind.annotation.GetMapping("/inventory")
                    String call();
                }
                """);
        assertFalse(HttpRequestBuilder.isCandidate(dubbo.findMethodsByName("call", false)[0]));
    }

    private String generate(String controllerSuffix, String methodName, String methodSource) throws Exception {
        String controllerName = SCRATCH_MARKER + controllerSuffix;
        var controller = myFixture.addClass("""
                package contract.httprequest;

                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping("/api")
                public class %s {
                    %s
                }
                """.formatted(controllerName, methodSource));

        ClassNode classNode = new ClassNode(new ModuleNode(new RootNode(), getModule()), controller);
        MethodNode methodNode = null;
        for (SimpleNode child : classNode.getChildren()) {
            if (child instanceof MethodNode candidate && methodName.equals(candidate.getPsiMethod().getName())) {
                methodNode = candidate;
                break;
            }
        }
        assertNotNull("The HTTP fixture method must appear as a Doc View method node", methodNode);

        Set<VirtualFile> openedBefore = openedFiles();
        CompletableFuture<VirtualFile> openedScratch = watchNewOpenedScratch(openedBefore);
        MethodNode selectedMethod = methodNode;
        EdtTestUtil.runInEdtAndWait(() -> CustomFileUtils.openHttp(getProject(), selectedMethod));
        VirtualFile scratch = awaitNewOpenedScratch(openedScratch);
        createdScratchFiles.add(scratch);
        return new String(scratch.contentsToByteArray(), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private CompletableFuture<VirtualFile> watchNewOpenedScratch(Set<VirtualFile> openedBefore) {
        CompletableFuture<VirtualFile> openedScratch = new CompletableFuture<>();
        getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(
                FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
                    @Override
                    public void fileOpened(FileEditorManager source, VirtualFile file) {
                        if (!openedBefore.contains(file) && isTestScratch(file)) {
                            openedScratch.complete(file);
                        }
                    }
                });
        return openedScratch;
    }

    private VirtualFile awaitNewOpenedScratch(CompletableFuture<VirtualFile> openedScratch) throws Exception {
        VirtualFile file = PlatformTestUtil.waitForFuture(openedScratch, 10_000);
        assertTrue("The production HTTP entry point should leave its Scratch request file open",
                FileEditorManager.getInstance(getProject()).isFileOpen(file));
        return file;
    }

    private Set<VirtualFile> openedFiles() {
        AtomicReference<Set<VirtualFile>> result = new AtomicReference<>();
        EdtTestUtil.runInEdtAndWait(() -> result.set(new HashSet<>(Arrays.asList(
                FileEditorManager.getInstance(getProject()).getOpenFiles()))));
        return result.get();
    }

    private boolean hasNonEmptyRequestBody(String requestText) {
        int bodyStart = requestText.indexOf("\n\n");
        return bodyStart >= 0 && !requestText.substring(bodyStart + 2).isBlank();
    }

    private boolean isTestScratch(VirtualFile file) {
        return file.isValid() && ExtensionsRootType.getInstance().containsFile(file)
                && file.getPath().replace('\\', '/').contains("doc-view-http/");
    }

    private String buildDirect(String methodSource) throws Exception {
        String ownerName = nextFixtureClassName();
        return buildDirect("""
                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping("/api")
                public class %s {
                    %s
                }
                """.formatted(ownerName, methodSource), ownerName, "call");
    }

    private String buildDirect(String source, String ownerName, String methodName) throws Exception {
        String uniqueOwnerName = nextFixtureClassName();
        String uniqueSource = source.replace(ownerName, uniqueOwnerName);
        PsiClass owner = myFixture.addClass("package contract.httprequest;\n" + uniqueSource);
        PsiMethod[] methods = owner.findMethodsByName(methodName, false);
        assertEquals("Each contract fixture should declare one selected method", 1, methods.length);
        return build(methods[0]);
    }

    private String build(PsiMethod method) throws HttpRequestException {
        BuildOutcome outcome = ReadAction.compute(() -> {
            try {
                return new BuildOutcome(new HttpRequestBuilder().build(method), null);
            } catch (HttpRequestException exception) {
                return new BuildOutcome(null, exception);
            }
        });
        if (outcome.failure() != null) {
            throw new AssertionError("HTTP request builder rejected input with " + outcome.failure().reasonKey(),
                    outcome.failure());
        }
        assertNotNull("Successful build must return an immutable text snapshot", outcome.snapshot());
        return outcome.snapshot().text();
    }

    private void assertBuildFailure(String expectedReason, String methodSource) {
        String ownerName = nextFixtureClassName();
        assertBuildFailure(expectedReason, """
                @org.springframework.web.bind.annotation.RestController
                @org.springframework.web.bind.annotation.RequestMapping("/api")
                public class %s {
                    %s
                }
                """.formatted(ownerName, methodSource), ownerName, "call");
    }

    private void assertBuildFailure(String expectedReason, String source, String ownerName, String methodName) {
        String uniqueOwnerName = nextFixtureClassName();
        PsiClass owner = myFixture.addClass("package contract.httprequest;\n"
                + source.replace(ownerName, uniqueOwnerName));
        PsiMethod[] methods = owner.findMethodsByName(methodName, false);
        assertEquals("Failure fixture should declare one selected method", 1, methods.length);
        BuildOutcome outcome = ReadAction.compute(() -> {
            try {
                return new BuildOutcome(new HttpRequestBuilder().build(methods[0]), null);
            } catch (HttpRequestException exception) {
                return new BuildOutcome(null, exception);
            }
        });
        assertNull("Unsupported input must not return a partial request", outcome.snapshot());
        assertNotNull("Unsupported input must return a reason", outcome.failure());
        assertEquals(expectedReason, outcome.failure().reasonKey());
    }

    private void assertBuildFailure(String expectedReason, PsiMethod method) {
        BuildOutcome outcome = ReadAction.compute(() -> {
            try {
                return new BuildOutcome(new HttpRequestBuilder().build(method), null);
            } catch (HttpRequestException exception) {
                return new BuildOutcome(null, exception);
            }
        });
        assertNull("Unsupported input must not return a partial request", outcome.snapshot());
        assertNotNull("Unsupported input must return a reason", outcome.failure());
        assertEquals(expectedReason, outcome.failure().reasonKey());
    }

    private String nextFixtureClassName() {
        return SCRATCH_MARKER + "Builder" + (++fixtureSequence);
    }

    private String requestBody(String text) {
        int bodyStart = text.indexOf("\n\n");
        assertTrue("Request body must follow a blank line: " + text, bodyStart >= 0);
        return text.substring(bodyStart + 2).strip();
    }

    private int countOccurrences(String text, String expected) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(expected, from)) >= 0) {
            count++;
            from += expected.length();
        }
        return count;
    }

    private record BuildOutcome(HttpRequestSnapshot snapshot, HttpRequestException failure) { }

    private void installHttpAnnotations() {
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;
                public @interface RestController { }
                """);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;
                public @interface RequestMapping {
                    String[] value() default {};
                    String[] path() default {};
                    String[] consumes() default {};
                    String[] params() default {};
                    String[] headers() default {};
                    RequestMethod[] method() default {};
                }
                """);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;
                public enum RequestMethod { GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD, TRACE }
                """);
        myFixture.addClass(mappingAnnotation("GetMapping"));
        myFixture.addClass(mappingAnnotation("PostMapping"));
        myFixture.addClass(mappingAnnotation("PutMapping"));
        myFixture.addClass(mappingAnnotation("DeleteMapping"));
        myFixture.addClass(mappingAnnotation("PatchMapping"));
        myFixture.addClass("""
                package org.springframework.cloud.openfeign;
                public @interface FeignClient {
                    String value() default "";
                    String name() default "";
                    String path() default "";
                }
                """);
        myFixture.addClass("""
                package org.apache.dubbo.config.annotation;
                public @interface DubboService { }
                """);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;
                public @interface PathVariable {
                    String value() default "";
                    String name() default "";
                    boolean required() default true;
                }
                """);
        myFixture.addClass(bindingAnnotation("RequestParam"));
        myFixture.addClass(bindingAnnotation("RequestHeader"));
        myFixture.addClass(bindingAnnotation("RequestPart"));
        myFixture.addClass(bindingAnnotation("ModelAttribute"));
        myFixture.addClass(bindingAnnotation("CookieValue"));
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;
                public @interface RequestBody { boolean required() default true; }
                """);
    }

    private String mappingAnnotation(String simpleName) {
        return """
                package org.springframework.web.bind.annotation;
                public @interface %s {
                    String[] value() default {};
                    String[] path() default {};
                    String[] consumes() default {};
                    String[] params() default {};
                    String[] headers() default {};
                }
                """.formatted(simpleName);
    }

    private String bindingAnnotation(String simpleName) {
        return """
                package org.springframework.web.bind.annotation;
                public @interface %s {
                    String value() default "";
                    String name() default "";
                    boolean required() default true;
                    String defaultValue() default "\\n\\t\\t\\n\\t\\t\\n" + (char) 0xE000 + (char) 0xE001
                            + (char) 0xE002 + "\\n\\t\\t\\t\\t\\n";
                }
                """.formatted(simpleName);
    }

    private void installOpenApiParameter() {
        myFixture.addClass("""
                package io.swagger.v3.oas.annotations;
                public @interface Parameter {
                    String name() default "";
                    String description() default "";
                    String example() default "";
                    String in() default "";
                    boolean required() default false;
                }
                """);
    }
}

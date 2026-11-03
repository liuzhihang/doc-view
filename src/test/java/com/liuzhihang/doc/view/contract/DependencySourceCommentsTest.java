package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.IndexingTestUtil;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.service.DtoSchemaCacheService;
import com.liuzhihang.doc.view.service.impl.DubboDocViewServiceImpl;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.utils.DocViewUtils;

import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/** Real binary dependencies distinguish attached source navigation from project source parsing. */
public final class DependencySourceCommentsTest extends LightJavaCodeInsightFixtureTestCase {

    private Path libraryDirectory;
    private Path sourcesJar;
    private Library library;

    @Override
    protected void tearDown() throws Exception {
        try {
            DtoSchemaCacheService.getInstance(getProject()).clear();
            if (library != null) {
                PsiTestUtil.removeLibrary(getModule(), library);
            }
        } finally {
            try {
                super.tearDown();
            } finally {
                if (libraryDirectory != null) {
                    FileUtil.delete(libraryDirectory.toFile());
                }
            }
        }
    }

    public void testAttachedSourcesProvideInheritedNestedAndGenericFieldComments() throws Exception {
        installDependency(true);
        PsiClass model = dependencyClass("external.docs.ExternalModel");
        PsiField data = model.findFieldByName("data", false);
        assertNotNull(data);
        assertTrue("The test must exercise a compiled field", data instanceof PsiCompiledElement);
        assertEquals("T", data.getType().getCanonicalText());
        assertEquals("外部数据", DocViewUtils.fieldDesc(data));
        assertEquals("继承编号", DocViewUtils.fieldDesc(model.findFieldByName("id", true)));
        assertEquals("内部字段", DocViewUtils.fieldDesc(
                dependencyClass("external.docs.ExternalModel.Nested").findFieldByName("name", false)));
        assertEquals("注解优先", DocViewUtils.fieldDesc(model.findFieldByName("annotated", false)));
        assertEquals("", DocViewUtils.fieldDesc(model.findFieldByName("empty", false)));
        assertTrue("Reading comments must not replace the original binary field", data instanceof PsiCompiledElement);
        assertEquals("T", data.getType().getCanonicalText());
    }

    public void testAttachedCommentsReachSpringAndDubboRequestsResponsesAndMarkdown() throws Exception {
        installDependency(true);
        PsiClass controller = springController();
        PsiMethod method = controller.findMethodsByName("load", false)[0];
        DocView spring = new SpringDocViewServiceImpl().buildClassMethodDoc(controller, method);
        assertDocumentedModel(spring.getReqBody());
        assertDocumentedModel(spring.getRespBody());
        assertTrue(DocViewData.markdownText(getProject(), spring).contains("外部数据"));

        PsiClass service = myFixture.addClass("""
                package sourcecomments;
                public interface ExternalService {
                    external.docs.ExternalModel<String> load(external.docs.ExternalModel<String> request);
                }
                """);
        DocView dubbo = new DubboDocViewServiceImpl().buildClassMethodDoc(
                service, service.findMethodsByName("load", false)[0]);
        Body request = child(dubbo.getReqBody(), "request");
        assertEquals("外部数据", child(request, "data").getDesc());
        assertEquals("继承编号", child(request, "id").getDesc());
        assertDocumentedModel(dubbo.getRespBody());
        assertTrue(DocViewData.markdownText(getProject(), dubbo).contains("继承编号"));
    }

    public void testMissingSourcesRetainAnnotationFallbackAndAttachingSourcesInvalidatesCachedBody() throws Exception {
        installDependency(false);
        PsiClass model = dependencyClass("external.docs.ExternalModel");
        assertEquals("", DocViewUtils.fieldDesc(model.findFieldByName("data", false)));
        assertEquals("注解优先", DocViewUtils.fieldDesc(model.findFieldByName("annotated", false)));
        PsiClass controller = springController();
        PsiMethod method = controller.findMethodsByName("load", false)[0];
        SpringDocViewServiceImpl service = new SpringDocViewServiceImpl();
        DocView withoutSources = service.buildClassMethodDoc(controller, method);
        assertEquals("", child(withoutSources.getRespBody(), "data").getDesc());
        assertEquals("", child(service.buildClassMethodDoc(controller, method).getRespBody(), "data").getDesc());

        VirtualFile sourceRoot = jarRoot(sourcesJar);
        WriteAction.run(() -> {
            Library.ModifiableModel edit = library.getModifiableModel();
            edit.addRoot(sourceRoot, OrderRootType.SOURCES);
            edit.commit();
        });
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());

        DocView withSources = service.buildClassMethodDoc(controller, method);
        assertDocumentedModel(withSources.getReqBody());
        assertDocumentedModel(withSources.getRespBody());
    }

    public void testProjectSourceCommentsStillUseTheExistingDescriptionRules() {
        PsiClass model = myFixture.addClass("""
                package sourcecomments;
                public class LocalModel {
                    /** 本地描述 */
                    public String value;
                }
                """);
        PsiField field = model.findFieldByName("value", false);
        assertNotNull(field);
        assertFalse(field instanceof PsiCompiledElement);
        assertEquals("本地描述", DocViewUtils.fieldDesc(field));
    }

    private void assertDocumentedModel(Body body) {
        assertEquals("外部数据", child(body, "data").getDesc());
        assertEquals("String", child(body, "data").getType());
        assertEquals("2026-11", child(body, "data").getSince());
        assertEquals("2.0", child(body, "data").getVersion());
        assertEquals("继承编号", child(body, "id").getDesc());
        assertEquals("内部字段", child(child(body, "nested"), "name").getDesc());
    }

    private Body child(Body parent, String name) {
        return parent.getChildList().stream().filter(body -> name.equals(body.getName()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing body field " + name));
    }

    private PsiClass springController() {
        AnnotationStubs.installSpringWeb(myFixture);
        myFixture.addClass("""
                package org.springframework.web.bind.annotation;
                public @interface RequestBody {}
                """);
        return myFixture.addClass("""
                package sourcecomments;
                @org.springframework.web.bind.annotation.RestController
                public class ExternalController {
                    @org.springframework.web.bind.annotation.GetMapping("/external")
                    public external.docs.ExternalModel<String> load(
                        @org.springframework.web.bind.annotation.RequestBody external.docs.ExternalModel<String> request) {
                        return request;
                    }
                }
                """);
    }

    private PsiClass dependencyClass(String qualifiedName) {
        PsiClass result = JavaPsiFacade.getInstance(getProject()).findClass(
                qualifiedName, GlobalSearchScope.allScope(getProject()));
        assertNotNull("Missing binary dependency " + qualifiedName, result);
        assertTrue("Dependency must resolve to binary PSI", result instanceof PsiCompiledElement);
        return result;
    }

    private void installDependency(boolean attachSources) throws Exception {
        libraryDirectory = Files.createTempDirectory("doc-view-dependency-comments-");
        VfsRootAccess.allowRootAccess(getTestRootDisposable(), libraryDirectory.toString());
        Path sourceDirectory = Files.createDirectories(libraryDirectory.resolve("src"));
        Path classesDirectory = Files.createDirectories(libraryDirectory.resolve("classes"));
        Map<String, String> sources = new LinkedHashMap<>();
        sources.put("io/swagger/v3/oas/annotations/media/Schema.java", """
                package io.swagger.v3.oas.annotations.media;
                @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.CLASS)
                @java.lang.annotation.Target(java.lang.annotation.ElementType.FIELD)
                public @interface Schema {
                    String description() default "";
                }
                """);
        sources.put("external/docs/Parent.java", """
                package external.docs;
                public class Parent {
                    /** 继承编号 */
                    public String id;
                }
                """);
        sources.put("external/docs/ExternalModel.java", """
                package external.docs;
                public class ExternalModel<T> extends Parent {
                    /** 外部数据
                     * @since 2026-11
                     * @version 2.0
                     */
                    public T data;
                    public Nested nested;
                    /** 不应替换注解 */
                    @io.swagger.v3.oas.annotations.media.Schema(description = "注解优先")
                    public String annotated;
                    /** 不应替换显式空描述 */
                    @io.swagger.v3.oas.annotations.media.Schema(description = "")
                    public String empty;
                    public static class Nested {
                        /** 内部字段 */
                        public String name;
                    }
                }
                """);
        List<String> compilerArguments = new ArrayList<>(List.of(
                "-proc:none", "--release", "21", "-encoding", "UTF-8", "-d", classesDirectory.toString()));
        for (Map.Entry<String, String> source : sources.entrySet()) {
            Path file = sourceDirectory.resolve(source.getKey());
            Files.createDirectories(file.getParent());
            Files.writeString(file, source.getValue(), StandardCharsets.UTF_8);
            compilerArguments.add(file.toString());
        }
        compileDependency(compilerArguments);

        Path binaryJar = libraryDirectory.resolve("dependency.jar");
        sourcesJar = libraryDirectory.resolve("dependency-sources.jar");
        createJar(classesDirectory, binaryJar);
        createJar(sourceDirectory, sourcesJar);
        var builder = PsiTestUtil.newLibrary("external-doc-comments").classesRoot(jarRoot(binaryJar));
        if (attachSources) {
            builder.sourceRoot(jarRoot(sourcesJar));
        }
        library = builder.addTo(getModule());
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
    }

    private void compileDependency(List<String> arguments) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler != null) {
            assertEquals("Fixture library compilation failed", 0,
                    compiler.run(null, null, null, arguments.toArray(String[]::new)));
            return;
        }
        // The IntelliJ test launcher can use a JBR without jdk.compiler even though Gradle uses a JDK.
        String executable = System.getProperty("os.name").startsWith("Windows") ? "javac.exe" : "javac";
        String configuredJdk = System.getenv("JAVA_HOME");
        if (configuredJdk != null) {
            Path candidate = Path.of(configuredJdk, "bin", executable);
            if (Files.isExecutable(candidate)) {
                executable = candidate.toString();
            }
        }
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.addAll(arguments);
        Path diagnostics = libraryDirectory.resolve("compiler.log");
        Process process = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(diagnostics.toFile()).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fail("Fixture compilation did not complete within 30 seconds");
        }
        assertEquals("Fixture library compilation failed: " + Files.readString(diagnostics), 0, process.exitValue());
    }

    private VirtualFile jarRoot(Path path) {
        VirtualFile root = JarFileSystem.getInstance().refreshAndFindFileByPath(path + "!/");
        assertNotNull("Missing test JAR root " + path, root);
        return root;
    }

    private void createJar(Path directory, Path target) throws Exception {
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(target));
             var paths = Files.walk(directory)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                output.putNextEntry(new JarEntry(directory.relativize(path).toString().replace('\\', '/')));
                Files.copy(path, output);
                output.closeEntry();
            }
        }
    }
}

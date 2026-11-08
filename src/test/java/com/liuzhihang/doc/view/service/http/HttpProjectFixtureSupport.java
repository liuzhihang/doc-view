package com.liuzhihang.doc.view.service.http;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleWithNameAlreadyExists;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Creates a real directory-based project for tests that need project isolation. */
public final class HttpProjectFixtureSupport {
    private final JavaCodeInsightTestFixture fixture;
    private final Path temporaryRoot;
    private boolean closed;

    private HttpProjectFixtureSupport(JavaCodeInsightTestFixture fixture, Path temporaryRoot) {
        this.fixture = fixture;
        this.temporaryRoot = temporaryRoot;
    }

    public static HttpProjectFixtureSupport create(String projectName) throws Exception {
        Path temporaryRoot = Files.createTempDirectory("doc-view-http-project-");
        Path sourceRoot = temporaryRoot.resolve(projectName).resolve("src");
        Files.createDirectories(sourceRoot);
        JavaCodeInsightTestFixture fixture = null;
        try {
            // Register Java module builders before adding a module, including standalone runs.
            JavaTestFixtureFactory javaFactory = JavaTestFixtureFactory.getFixtureFactory();
            TestFixtureBuilder<IdeaProjectTestFixture> builder = IdeaTestFixtureFactory.getFixtureFactory()
                    .createFixtureBuilder(projectName, temporaryRoot, true);
            JavaModuleFixtureBuilder<?> moduleBuilder = builder.addModule(JavaModuleFixtureBuilder.class);
            moduleBuilder.addSourceContentRoot(sourceRoot.toString());
            moduleBuilder.setMockJdkLevel(JavaModuleFixtureBuilder.MockJdkLevel.jdk15);

            fixture = javaFactory.createCodeInsightFixture(builder.getFixture());
            fixture.setUp();
            return new HttpProjectFixtureSupport(fixture, temporaryRoot);
        } catch (Exception | Error failure) {
            if (fixture != null) {
                try {
                    fixture.tearDown();
                } catch (Exception cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            try {
                deleteRecursively(temporaryRoot);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    public JavaCodeInsightTestFixture fixture() {
        return fixture;
    }

    public Project project() {
        return fixture.getProject();
    }

    public void renameModule(String moduleName) {
        WriteCommandAction.runWriteCommandAction(project(), () -> {
            ModifiableModuleModel model = ModuleManager.getInstance(project()).getModifiableModel();
            try {
                model.renameModule(fixture.getModule(), moduleName);
            } catch (ModuleWithNameAlreadyExists conflict) {
                model.dispose();
                throw new IllegalStateException("Could not assign a stable fixture module name", conflict);
            }
            model.commit();
        });
    }

    public HttpRequestService installHttpService(HttpRequestBuilder builder) {
        HttpRequestService service = new HttpRequestService(project(), builder, () -> true);
        ServiceContainerUtil.replaceService(project(), HttpRequestService.class, service,
                fixture.getTestRootDisposable());
        return service;
    }

    public void close() throws Exception {
        if (closed) {
            return;
        }
        closed = true;
        try {
            fixture.tearDown();
        } finally {
            deleteRecursively(temporaryRoot);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}

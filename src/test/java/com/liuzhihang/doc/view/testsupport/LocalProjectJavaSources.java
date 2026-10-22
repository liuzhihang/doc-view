package com.liuzhihang.doc.view.testsupport;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Creates local-filesystem Java PSI under a test project's actual base path. */
public final class LocalProjectJavaSources {

    private final Project project;
    private final Module module;
    private final Path projectRoot;
    private final Path sourceRoot;
    private final Set<Path> createdFiles = new LinkedHashSet<>();
    private final Set<Path> createdDirectories = new LinkedHashSet<>();
    private final VirtualFile sourceRootFile;
    private final boolean addedSourceRoot;

    public LocalProjectJavaSources(@NotNull Project project, @NotNull Module module) throws IOException {
        this.project = project;
        this.module = module;
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            throw new IllegalStateException("Test project has no base path");
        }
        projectRoot = Path.of(basePath).toAbsolutePath().normalize();
        createTrackedDirectory(projectRoot);
        sourceRoot = projectRoot.resolve("src");
        createTrackedDirectory(sourceRoot);
        sourceRootFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(sourceRoot);
        if (sourceRootFile == null || !sourceRootFile.isInLocalFileSystem()) {
            throw new IllegalStateException("Test source root is not a local filesystem directory");
        }
        addedSourceRoot = !ModuleRootManager.getInstance(module).getFileIndex().isInSourceContent(sourceRootFile);
        if (addedSourceRoot) {
            PsiTestUtil.addSourceRoot(module, sourceRootFile);
        }
    }

    public @NotNull PsiClass addClass(@NotNull String relativePath, @NotNull String source) throws IOException {
        Path javaFile = sourceRoot.resolve(relativePath).normalize();
        if (!javaFile.startsWith(sourceRoot) || !javaFile.getFileName().toString().endsWith(".java")) {
            throw new IllegalArgumentException("Java fixture path must stay under src and end in .java");
        }
        if (Files.exists(javaFile)) {
            throw new IllegalStateException("Java fixture path already exists: " + relativePath);
        }
        createTrackedDirectories(javaFile.getParent());
        Files.writeString(javaFile, source, StandardCharsets.UTF_8);
        createdFiles.add(javaFile);
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(javaFile);
        if (virtualFile == null || !virtualFile.isInLocalFileSystem()) {
            throw new IllegalStateException("Java fixture did not resolve to a local filesystem file");
        }
        if (!(PsiManager.getInstance(project).findFile(virtualFile) instanceof PsiJavaFile psiJavaFile)
                || psiJavaFile.getClasses().length == 0) {
            throw new IllegalStateException("Java fixture did not produce a Java PSI class: " + relativePath);
        }
        return psiJavaFile.getClasses()[0];
    }

    public @NotNull Path projectRoot() {
        return projectRoot;
    }

    public void cleanup() throws IOException {
        if (addedSourceRoot && !module.isDisposed()) {
            PsiTestUtil.removeSourceRoot(module, sourceRootFile);
        }
        List<Path> files = new ArrayList<>(createdFiles);
        files.sort(Comparator.comparingInt(Path::getNameCount).reversed());
        for (Path file : files) {
            Files.deleteIfExists(file);
        }
        List<Path> directories = new ArrayList<>(createdDirectories);
        directories.sort(Comparator.comparingInt(Path::getNameCount).reversed());
        for (Path directory : directories) {
            if (Files.isDirectory(directory) && isEmpty(directory)) {
                Files.deleteIfExists(directory);
            }
        }
    }

    private void createTrackedDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            createdDirectories.add(directory);
        }
    }

    private void createTrackedDirectories(Path directory) throws IOException {
        List<Path> missing = new ArrayList<>();
        for (Path current = directory; current != null && current.startsWith(projectRoot) && !Files.exists(current);
             current = current.getParent()) {
            missing.add(current);
        }
        Files.createDirectories(directory);
        createdDirectories.addAll(missing);
    }

    private static boolean isEmpty(Path directory) throws IOException {
        try (var children = Files.list(directory)) {
            return children.findAny().isEmpty();
        }
    }

    public void installApiAnnotationStubs() throws IOException {
        addClass("org/springframework/web/bind/annotation/RestController.java", """
                package org.springframework.web.bind.annotation;
                public @interface RestController {}
                """);
        addClass("org/springframework/web/bind/annotation/RequestMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface RequestMapping { String[] value() default {}; }
                """);
        addClass("org/springframework/web/bind/annotation/GetMapping.java", """
                package org.springframework.web.bind.annotation;
                public @interface GetMapping { String[] value() default {}; }
                """);
        addClass("org/springframework/web/bind/annotation/RequestParam.java", """
                package org.springframework.web.bind.annotation;
                public @interface RequestParam { String value() default ""; boolean required() default true; }
                """);
        addClass("org/springframework/cloud/openfeign/FeignClient.java", """
                package org.springframework.cloud.openfeign;
                public @interface FeignClient {}
                """);
        addClass("org/apache/dubbo/config/annotation/DubboService.java", """
                package org.apache.dubbo.config.annotation;
                public @interface DubboService {}
                """);
    }
}

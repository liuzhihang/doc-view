package com.liuzhihang.doc.view.service.export;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.DocViewBuildInfo;
import com.liuzhihang.doc.view.config.TemplateSettings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentData;
import com.liuzhihang.doc.view.service.document.ProjectDocumentFormat;
import com.liuzhihang.doc.view.service.document.ProjectDocumentIndexService;
import com.liuzhihang.doc.view.service.document.ProjectDocumentProjection;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;
import com.liuzhihang.doc.view.utils.FeignPsiUtil;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Scans the project's configured module scope and renders PSI-free documents. */
@Service(Service.Level.PROJECT)
public final class ProjectDocumentSnapshotService {

    private final Project project;

    public ProjectDocumentSnapshotService(@NotNull Project project) {
        this.project = project;
    }

    public static @NotNull ProjectDocumentSnapshotService getInstance(@NotNull Project project) {
        return project.getService(ProjectDocumentSnapshotService.class);
    }

    public @NotNull ProjectDocumentSnapshot createSnapshot(@NotNull ProgressIndicator indicator) {
        if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
            throw new IllegalStateException("Project document snapshots require a read action");
        }
        ensureIndexesReady();
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            throw new IllegalStateException("Project base path is unavailable");
        }
        Path projectRoot;
        try {
            projectRoot = Path.of(basePath).toRealPath();
        } catch (IOException exception) {
            throw new IllegalStateException("Project root is not accessible", exception);
        }
        Settings settings = Settings.getInstance(project);
        TemplateSettings templateSettings = TemplateSettings.getInstance(project);
        String generatorFingerprint = generatorFingerprint(settings, templateSettings);
        String producerVersion = DocViewBuildInfo.version();
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        ProjectFileIndex projectFileIndex = ProjectFileIndex.getInstance(project);
        TreeMap<String, ProjectDocumentPage> pagesByIdentity = new TreeMap<>();
        Map<String, CapturedSource> capturedSources = new HashMap<>();
        int skippedCount = 0;
        boolean hasUnsavedDocuments = hasUnsavedProjectInputDocuments(
                psiDocumentManager, fileDocumentManager, projectFileIndex);

        for (Module module : ModuleManager.getInstance(project).getModules()) {
            indicator.checkCanceled();
            ensureIndexesReady();
            if (module.isDisposed()) {
                continue;
            }
            Map<String, CandidateClass> candidates = findCandidateClasses(module, settings.getIncludeNormalInterface(), indicator);
            for (CandidateClass candidate : candidates.values()) {
                indicator.checkCanceled();
                PsiClass psiClass = candidate.psiClass();
                String qualifiedClassName = psiClass.getQualifiedName();
                Path sourceFile = sourceFilePath(psiClass);
                String sourcePath = sourceFile == null ? null : projectRelativeSourcePath(projectRoot, sourceFile);
                if (qualifiedClassName == null || sourcePath == null) {
                    for (PsiMethod psiMethod : psiClass.getMethods()) {
                        indicator.checkCanceled();
                        if (DocViewUtils.isDocViewMethod(psiMethod)) {
                            skippedCount++;
                        }
                    }
                    continue;
                }
                PsiFile psiFile = psiClass.getContainingFile();
                Document document = psiFile == null ? null : psiDocumentManager.getDocument(psiFile);
                if (document == null) {
                    for (PsiMethod psiMethod : psiClass.getMethods()) {
                        indicator.checkCanceled();
                        if (DocViewUtils.isDocViewMethod(psiMethod)) {
                            throw new IllegalStateException("Cannot capture the Document for a documented source file");
                        }
                    }
                    continue;
                }
                CapturedSource capturedSource = capturedSources.get(sourcePath);
                if (capturedSource == null) {
                    if (!psiDocumentManager.isCommitted(document)) {
                        throw new IllegalStateException("Cannot capture an uncommitted source Document");
                    }
                    String capturedText = document.getText();
                    if (!psiDocumentManager.isCommitted(document)) {
                        throw new IllegalStateException("Source Document changed while the project snapshot was captured");
                    }
                    capturedSource = new CapturedSource(document, capturedText,
                            ProjectDocumentFormat.sha256(capturedText.getBytes(StandardCharsets.UTF_8)));
                    capturedSources.put(sourcePath, capturedSource);
                } else if (capturedSource.document() != document || !psiDocumentManager.isCommitted(document)) {
                    throw new IllegalStateException("Source Document changed while the project snapshot was captured");
                }
                String capturedText = capturedSource.text();
                String capturedContentSha256 = capturedSource.sha256();
                hasUnsavedDocuments |= isProjectInputFile(projectFileIndex, psiFile.getVirtualFile())
                        && fileDocumentManager.isDocumentUnsaved(document);
                for (PsiMethod psiMethod : psiClass.getMethods()) {
                    indicator.checkCanceled();
                    ensureIndexesReady();
                    if (!DocViewUtils.isDocViewMethod(psiMethod)) {
                        continue;
                    }
                    ProjectDocumentIdentity identity;
                    try {
                        List<String> parameterTypes = canonicalParameterTypes(psiMethod);
                        if (parameterTypes == null) {
                            skippedCount++;
                            continue;
                        }
                        identity = ProjectDocumentIdentity.of(module.getName(), sourcePath, qualifiedClassName,
                                psiMethod.getName(), parameterTypes);
                    } catch (IllegalArgumentException exception) {
                        skippedCount++;
                        continue;
                    }

                    DocView docView = DocViewService.getInstance(project, psiClass)
                            .buildClassMethodDoc(psiClass, psiMethod);
                    int methodOffset = psiMethod.getTextOffset();
                    Integer line = methodOffset >= 0 && methodOffset <= capturedText.length()
                            ? document.getLineNumber(methodOffset) + 1 : null;
                    ProjectDocumentData.SourceData source = new ProjectDocumentData.SourceData(
                            sourcePath, line, capturedContentSha256, "ide-document-text");
                    ProjectDocumentData.ApiData data = ProjectDocumentProjection.fromDocView(
                            docView, identity, candidate.framework(), source);
                    String markdown = DocViewData.markdownText(project, docView);
                    ProjectDocumentPage page = new ProjectDocumentPage(identity, candidate.framework(), markdown, data);
                    ProjectDocumentPage previous = pagesByIdentity.putIfAbsent(identity.canonicalForm(), page);
                    if (previous != null && (!previous.markdown().equals(markdown) || !previous.data().equals(data))) {
                        throw new IllegalStateException("One project document identity resolved to different content");
                    }
                }
            }
        }
        ensureIndexesReady();
        return new ProjectDocumentSnapshot(new ArrayList<>(pagesByIdentity.values()), skippedCount,
                producerVersion, generatorFingerprint, hasUnsavedDocuments);
    }

    static String generatorFingerprint(Settings settings, TemplateSettings templates) {
        Map<String, List<String>> values = new TreeMap<>();
        addScalar(values, "projectionVersion", "1");
        addScalar(values, "commentTitleMaxLength", settings.getCommentTitleMaxLength());
        addScalar(values, "nameCommentFirstLine", settings.isNameCommentFirstLine());
        addScalar(values, "descCommentBody", settings.isDescCommentBody());
        addScalar(values, "titleTag", settings.getTitleTag());
        addScalar(values, "titleUseCommentTag", settings.getTitleUseCommentTag());
        addScalar(values, "titleClassComment", settings.getTitleClassComment());
        addScalar(values, "titleUseSimpleClassName", settings.getTitleUseSimpleClassName());
        addScalar(values, "titleUseFullClassName", settings.getTitleUseFullClassName());
        addScalar(values, "nameTag", settings.getNameTag());
        addScalar(values, "nameUseSwagger3", settings.getNameUseSwagger3());
        addScalar(values, "nameUseSwagger", settings.getNameUseSwagger());
        addScalar(values, "nameUseCommentTag", settings.getNameUseCommentTag());
        addScalar(values, "nameMethodComment", settings.getNameMethodComment());
        addScalar(values, "descUseSwagger3", settings.getDescUseSwagger3());
        addScalar(values, "descUseSwagger", settings.getDescUseSwagger());
        addScalar(values, "required", settings.getRequired());
        addScalar(values, "requiredUseCommentTag", settings.getRequiredUseCommentTag());
        addScalar(values, "fieldNameJsonProperty", settings.getFieldNameJsonProperty());
        addScalar(values, "includeNormalInterface", settings.getIncludeNormalInterface());
        addScalar(values, "prefixSymbol1", settings.getPrefixSymbol1());
        addScalar(values, "prefixSymbol2", settings.getPrefixSymbol2());
        addScalar(values, "separateParam", settings.getSeparateParam());
        addSet(values, "containClassAnnotationName", settings.getContainClassAnnotationName());
        addSet(values, "containMethodAnnotationName", settings.getContainMethodAnnotationName());
        addSet(values, "requiredFieldAnnotation", settings.getRequiredFieldAnnotation());
        addSet(values, "fieldNameAnnotation", settings.getFieldNameAnnotation());
        addSet(values, "excludeFieldNames", settings.getExcludeFieldNames());
        addSet(values, "excludeParameterType", settings.getExcludeParameterType());
        addSet(values, "excludeFieldAnnotation", settings.getExcludeFieldAnnotation());
        addSet(values, "excludeClassPackage", settings.getExcludeClassPackage());
        addScalar(values, "springTemplate", templates.getSpringTemplate());
        addScalar(values, "dubboTemplate", templates.getDubboTemplate());
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                for (Map.Entry<String, List<String>> entry : values.entrySet()) {
                    writeFingerprintString(output, entry.getKey());
                    output.writeInt(entry.getValue().size());
                    for (String value : entry.getValue()) writeFingerprintString(output, value);
                }
            }
            return ProjectDocumentFormat.sha256(bytes.toByteArray());
        } catch (IOException impossible) {
            throw new IllegalStateException("Could not encode generator fingerprint", impossible);
        }
    }

    private static void addScalar(Map<String, List<String>> values, String name, Object value) {
        String text = value == null ? null : value.toString();
        values.put(name, java.util.Collections.singletonList(text));
    }

    private static void addSet(Map<String, List<String>> values, String name, Collection<String> value) {
        if (value == null) {
            values.put(name, java.util.Collections.singletonList(null));
            return;
        }
        List<String> sorted = new ArrayList<>(value);
        sorted.sort(Comparator.nullsFirst(Comparator.naturalOrder()));
        values.put(name, sorted);
    }

    private static void writeFingerprintString(DataOutputStream output, String value) throws IOException {
        if (value == null) {
            output.writeInt(-1);
            return;
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(bytes.length);
        output.write(bytes);
    }

    private Map<String, CandidateClass> findCandidateClasses(Module module,
                                                               Boolean includeNormalInterface,
                                                               ProgressIndicator indicator) {
        Map<String, CandidateClass> candidates = new LinkedHashMap<>();
        for (PsiClass psiClass : DubboPsiUtils.findDocViewFromModule(module)) {
            indicator.checkCanceled();
            addCandidate(candidates, psiClass, "DUBBO");
        }
        for (PsiClass psiClass : FeignPsiUtil.findDocViewFromModule(module)) {
            indicator.checkCanceled();
            addCandidate(candidates, psiClass, "FEIGN");
        }
        if (Boolean.TRUE.equals(includeNormalInterface)) {
            AllClassesSearch.search(GlobalSearchScope.moduleScope(module), project).forEach(psiClass -> {
                indicator.checkCanceled();
                if (psiClass.isInterface() && !psiClass.isAnnotationType()) {
                    addCandidate(candidates, psiClass, "INTERFACE");
                }
                return true;
            });
        }
        for (PsiClass psiClass : SpringPsiUtils.findDocViewFromModule(module)) {
            indicator.checkCanceled();
            addCandidate(candidates, psiClass, "SPRING");
        }
        return candidates;
    }

    private static void addCandidate(Map<String, CandidateClass> candidates, PsiClass psiClass, String framework) {
        VirtualFile file = psiClass.getContainingFile() == null ? null : psiClass.getContainingFile().getVirtualFile();
        String qualifiedName = psiClass.getQualifiedName();
        String key = (file == null ? "" : file.getPath()) + "\n" + (qualifiedName == null ? "" : qualifiedName);
        CandidateClass existing = candidates.get(key);
        if (existing == null || "INTERFACE".equals(existing.framework())) {
            candidates.put(key, new CandidateClass(psiClass, framework));
        }
    }

    private static Path sourceFilePath(PsiClass psiClass) {
        if (psiClass.getContainingFile() == null) {
            return null;
        }
        VirtualFile virtualFile = psiClass.getContainingFile().getVirtualFile();
        if (virtualFile == null || !virtualFile.isInLocalFileSystem()) {
            return null;
        }
        try {
            return Path.of(virtualFile.getPath()).toAbsolutePath().normalize();
        } catch (RuntimeException invalidPath) {
            return null;
        }
    }

    static String projectRelativeSourcePath(Path projectRoot, Path sourceFile) {
        try {
            Path realSource = sourceFile.toRealPath();
            if (!realSource.startsWith(projectRoot)) {
                return null;
            }
            return projectRoot.relativize(realSource).toString().replace('\\', '/');
        } catch (IOException | RuntimeException invalidPath) {
            return null;
        }
    }

    private static List<String> canonicalParameterTypes(PsiMethod psiMethod) {
        List<String> types = new ArrayList<>();
        for (PsiParameter parameter : psiMethod.getParameterList().getParameters()) {
            PsiType type = parameter.getType();
            if (!type.isValid()) {
                return null;
            }
            String canonicalText = type.getCanonicalText();
            if (canonicalText == null || canonicalText.isBlank()) {
                return null;
            }
            types.add(canonicalText);
        }
        return List.copyOf(types);
    }

    private void ensureIndexesReady() {
        if (project.isDisposed()) {
            throw new IllegalStateException("Project is closed");
        }
        if (DumbService.getInstance(project).isDumb()) {
            throw new ProjectDocumentIndexNotReadyException();
        }
    }

    private boolean hasUnsavedProjectInputDocuments(PsiDocumentManager psiDocumentManager,
                                                    FileDocumentManager fileDocumentManager,
                                                    ProjectFileIndex projectFileIndex) {
        boolean hasUnsavedDocuments = false;
        for (Document document : fileDocumentManager.getUnsavedDocuments()) {
            VirtualFile file = fileDocumentManager.getFile(document);
            if (!isProjectInputFile(projectFileIndex, file)) {
                continue;
            }
            if (!psiDocumentManager.isCommitted(document)) {
                throw new IllegalStateException("Cannot capture an uncommitted Java or XML source Document");
            }
            hasUnsavedDocuments = true;
        }
        return hasUnsavedDocuments;
    }

    private boolean isProjectInputFile(ProjectFileIndex projectFileIndex, @Nullable VirtualFile file) {
        if (file == null) {
            return false;
        }
        String extension = file.getExtension();
        if ((!"java".equalsIgnoreCase(extension) && !"xml".equalsIgnoreCase(extension))
                || !projectFileIndex.isInContent(file)) {
            return false;
        }
        String basePath = project.getBasePath();
        if (basePath != null && !basePath.isBlank()) {
            try {
                Path root = Path.of(basePath).toAbsolutePath().normalize();
                Path source = Path.of(file.getPath()).toAbsolutePath().normalize();
                if (source.startsWith(root.resolve(ProjectDocumentIndexService.DOCUMENT_ROOT))) {
                    return false;
                }
            } catch (RuntimeException ignored) {
                // Preserve content-root tracking when a path cannot be normalized locally.
            }
        }
        return true;
    }

    private record CandidateClass(PsiClass psiClass, String framework) {
    }

    private record CapturedSource(Document document, String text, String sha256) {
    }
}

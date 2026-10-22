package com.liuzhihang.doc.view.service.export;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;
import com.liuzhihang.doc.view.utils.FeignPsiUtil;
import com.liuzhihang.doc.view.utils.SpringPsiUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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
        TreeMap<String, ProjectDocumentPage> pagesByIdentity = new TreeMap<>();
        int skippedCount = 0;

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
                    String markdown = DocViewData.markdownText(project, docView);
                    ProjectDocumentPage page = new ProjectDocumentPage(identity, candidate.framework(), markdown);
                    ProjectDocumentPage previous = pagesByIdentity.putIfAbsent(identity.canonicalForm(), page);
                    if (previous != null && !previous.markdown().equals(markdown)) {
                        throw new IllegalStateException("One project document identity resolved to different content");
                    }
                }
            }
        }
        ensureIndexesReady();
        return new ProjectDocumentSnapshot(new ArrayList<>(pagesByIdentity.values()), skippedCount);
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

    private record CandidateClass(PsiClass psiClass, String framework) {
    }
}

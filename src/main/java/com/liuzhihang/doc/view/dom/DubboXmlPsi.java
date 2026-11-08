package com.liuzhihang.doc.view.dom;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.TextOccurenceProcessor;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds Dubbo service declarations from XML PSI without creating cached DOM models.
 */
public final class DubboXmlPsi {

    private static final String CANDIDATE_WORD = "dubbo";
    private static final String BEANS_ROOT_NAME = "beans";
    private static final String SERVICE_TAG_NAME = "dubbo:service";

    private DubboXmlPsi() {
    }

    /**
     * Processes direct {@code dubbo:service} children of {@code beans} XML roots in the given scope.
     *
     * @return {@code true} if every declaration was accepted; {@code false} when the consumer stopped
     */
    public static boolean processServiceTags(@NotNull Project project,
                                             @NotNull GlobalSearchScope scope,
                                             @NotNull Processor<? super XmlTag> consumer) {
        Set<VirtualFile> candidateFiles = findCandidateFiles(project, scope);
        PsiManager psiManager = PsiManager.getInstance(project);

        for (VirtualFile candidateFile : candidateFiles.stream()
                .sorted(Comparator.comparing(VirtualFile::getPath)).toList()) {
            ProgressManager.checkCanceled();
            if (!candidateFile.isValid() || !scope.contains(candidateFile)) {
                continue;
            }

            PsiFile psiFile = psiManager.findFile(candidateFile);
            if (!(psiFile instanceof XmlFile xmlFile)) {
                continue;
            }

            XmlTag rootTag = xmlFile.getRootTag();
            if (rootTag == null || !BEANS_ROOT_NAME.equals(rootTag.getName())) {
                continue;
            }

            for (XmlTag childTag : rootTag.getSubTags()) {
                ProgressManager.checkCanceled();
                if (SERVICE_TAG_NAME.equals(childTag.getName()) && !consumer.process(childTag)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Returns the decoded interface attribute, or {@code null} when it is absent. */
    public static @Nullable String getInterfaceName(@NotNull XmlTag serviceTag) {
        XmlAttribute interfaceAttribute = serviceTag.getAttribute("interface");
        return interfaceAttribute == null ? null : interfaceAttribute.getDisplayValue();
    }

    private static @NotNull Set<VirtualFile> findCandidateFiles(@NotNull Project project,
                                                                 @NotNull GlobalSearchScope scope) {
        Set<VirtualFile> candidateFiles = ConcurrentHashMap.newKeySet();
        TextOccurenceProcessor collectXmlFile = (element, offsetInElement) -> {
            ProgressManager.checkCanceled();
            PsiFile containingFile = element.getContainingFile();
            if (containingFile instanceof XmlFile) {
                VirtualFile virtualFile = containingFile.getVirtualFile();
                if (virtualFile != null && scope.contains(virtualFile)) {
                    candidateFiles.add(virtualFile);
                }
            }
            return true;
        };

        ProgressManager.checkCanceled();
        GlobalSearchScope xmlScope = GlobalSearchScope.getScopeRestrictedByFileTypes(scope, XmlFileType.INSTANCE);
        PsiSearchHelper.getInstance(project).processElementsWithWord(
                collectXmlFile, xmlScope, CANDIDATE_WORD, UsageSearchContext.IN_FOREIGN_LANGUAGES, true);
        return candidateFiles;
    }
}

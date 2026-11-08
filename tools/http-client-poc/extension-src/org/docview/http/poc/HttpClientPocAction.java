package org.docview.http.poc;

import com.intellij.httpClient.actions.generation.HttpRequestGenerationManager;
import com.intellij.httpClient.actions.generation.HttpRequestUrlsGenerationRequest;
import com.intellij.httpClient.http.request.microservices.OpenInHttpClientLineMarkerBuilder;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Computable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.microservices.url.references.UrlPathContext;
import com.intellij.spring.mvc.navigation.requests.rest.UrlPathContextUtilsKt;

import java.util.List;

/**
 * Isolated, non-production bridge probe. It inspects URL context only and never invokes generation.
 */
public final class HttpClientPocAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(HttpClientPocAction.class);

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent event) {
        PsiMethod method = findMethod(event);
        event.getPresentation().setEnabledAndVisible(method != null && !method.isConstructor());
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        PsiMethod selectedMethod = findMethod(event);
        if (project == null || selectedMethod == null || selectedMethod.isConstructor()) {
            return;
        }

        Snapshot snapshot = ApplicationManager.getApplication().runReadAction((Computable<Snapshot>) () -> {
            UrlPathContext context = UrlPathContextUtilsKt.findUrlPathContext(project, selectedMethod);
            if (context == null) {
                return new Snapshot(false, 0, 0, false);
            }
            OpenInHttpClientLineMarkerBuilder nativeBuilder =
                    OpenInHttpClientLineMarkerBuilder.Companion.fromUrlPathContext(project, context, true);
            return new Snapshot(true, context.getSelfPaths().size(), context.getMethods().size(), nativeBuilder != null);
        });

        String message = "URL context found: " + snapshot.contextFound()
                + "\nPath entries: " + snapshot.pathCount()
                + "\nHTTP methods: " + snapshot.methodCount()
                + "\nNative request builder available: " + snapshot.nativeBuilderAvailable()
                + "\nGenerator intentionally not invoked; see the POC lifecycle gate.";
        LOG.info("HTTP_CLIENT_POC contextFound=" + snapshot.contextFound()
                + " pathCount=" + snapshot.pathCount()
                + " methodCount=" + snapshot.methodCount()
                + " nativeBuilderAvailable=" + snapshot.nativeBuilderAvailable());
        Messages.showInfoMessage(project, message, "HTTP Client PoC");
    }

    private static PsiMethod findMethod(AnActionEvent event) {
        PsiElement element = event.getData(CommonDataKeys.PSI_ELEMENT);
        if (element != null) {
            PsiMethod method = PsiTreeUtil.getParentOfType(element, PsiMethod.class, false);
            if (method != null) {
                return method;
            }
        }
        PsiFile file = event.getData(CommonDataKeys.PSI_FILE);
        var editor = event.getData(CommonDataKeys.EDITOR);
        if (file == null || editor == null) {
            return null;
        }
        PsiElement atCaret = file.findElementAt(editor.getCaretModel().getOffset());
        return atCaret == null ? null : PsiTreeUtil.getParentOfType(atCaret, PsiMethod.class, false);
    }

    /** Compile-time link to the candidate publisher; deliberately not called by this probe. */
    @SuppressWarnings("unused")
    private static void compileOnlyNativeGenerationCall(
            Project project,
            List<HttpRequestUrlsGenerationRequest> requests) {
        new HttpRequestGenerationManager(project).generateRequestsInHttpEditor(requests);
    }

    private record Snapshot(boolean contextFound, int pathCount, int methodCount, boolean nativeBuilderAvailable) {}
}

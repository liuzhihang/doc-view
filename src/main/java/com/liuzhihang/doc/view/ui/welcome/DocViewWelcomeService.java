package com.liuzhihang.doc.view.ui.welcome;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorOpenRequest;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.utils.FileEditorOpenSupport;

@Service(Service.Level.PROJECT)
public final class DocViewWelcomeService {
    private final Project project;
    private final DocViewWelcomeFile file = new DocViewWelcomeFile();
    private long requestGeneration;

    public DocViewWelcomeService(Project project) {
        this.project = project;
    }

    public static DocViewWelcomeService getInstance(Project project) {
        return project.getService(DocViewWelcomeService.class);
    }

    public void open(String page) {
        ApplicationManager.getApplication().assertIsDispatchThread();
        if (project.isDisposed() || !WelcomeDocuments.PAGES.contains(page)) return;
        FileEditorManager manager = FileEditorManager.getInstance(project);
        boolean alreadyOpen = manager.isFileOpen(file);
        long request = ++requestGeneration;
        file.setRequestedPage(page);
        try {
            var openResult = manager.requestOpenFile(file,
                    FileEditorOpenRequest.withFocus(true).withSelectAsCurrent(true));
            FileEditorOpenSupport.whenOpened(project, openResult,
                    () -> request == requestGeneration && page.equals(file.getRequestedPage()),
                    composite -> {
                        if (!alreadyOpen) return;
                        for (FileEditor editor : composite.getAllEditors()) {
                            if (editor instanceof DocViewWelcomeEditor welcome) welcome.showPage(page);
                        }
                    }, error -> DocViewNotification.notifyError(project,
                            DocViewBundle.message("welcome.error.open.failed")));
        } catch (RuntimeException error) {
            if (!FileEditorOpenSupport.isCancellation(error)
                    && !project.isDisposed() && request == requestGeneration && page.equals(file.getRequestedPage())) {
                DocViewNotification.notifyError(project, DocViewBundle.message("welcome.error.open.failed"));
            }
        }
    }
}

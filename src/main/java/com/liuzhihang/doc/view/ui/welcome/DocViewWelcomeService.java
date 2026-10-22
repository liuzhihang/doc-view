package com.liuzhihang.doc.view.ui.welcome;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;

@Service(Service.Level.PROJECT)
public final class DocViewWelcomeService {
    private final Project project;
    private final DocViewWelcomeFile file = new DocViewWelcomeFile();

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
        file.setRequestedPage(page);
        for (FileEditor editor : manager.openFile(file, true)) {
            if (alreadyOpen && editor instanceof DocViewWelcomeEditor welcome) welcome.showPage(page);
        }
    }
}

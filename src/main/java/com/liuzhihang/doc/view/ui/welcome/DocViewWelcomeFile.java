package com.liuzhihang.doc.view.ui.welcome;

import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.testFramework.LightVirtualFile;

public final class DocViewWelcomeFile extends LightVirtualFile {
    private String requestedPage = "welcome";

    public DocViewWelcomeFile() {
        super("Doc View", PlainTextFileType.INSTANCE, "");
        setWritable(false);
    }

    String getRequestedPage() {
        return requestedPage;
    }

    void setRequestedPage(String requestedPage) {
        this.requestedPage = requestedPage;
    }
}

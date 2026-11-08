package com.liuzhihang.doc.view.service.http;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;

/** Light fixtures don't load HTTP Client. Native availability is verified separately. */
public final class HttpRequestTestSupport {
    private HttpRequestTestSupport() {}

    public static void install(Project project, Disposable parent) {
        ServiceContainerUtil.replaceService(project, HttpRequestService.class,
                new HttpRequestService(project, new HttpRequestBuilder(), () -> true), parent);
    }
}

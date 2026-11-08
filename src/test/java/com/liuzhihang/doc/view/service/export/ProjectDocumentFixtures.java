package com.liuzhihang.doc.view.service.export;

import com.liuzhihang.doc.view.service.document.ProjectDocumentData.*;
import com.liuzhihang.doc.view.service.document.ProjectDocumentFormat;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Fictional, PSI-free source data shared by publisher regression tests. */
public final class ProjectDocumentFixtures {
    private ProjectDocumentFixtures() {}

    public static ProjectDocumentPage page(ProjectDocumentIdentity identity, String framework, String markdown) {
        var data = new ApiData(identity.sha256(), new IdentityData(identity.moduleName(),
                identity.qualifiedClassName(), identity.methodName(), identity.parameterTypes(),
                identity.displaySignature(), identity.canonicalForm()), identity.methodName(), "Fixture",
                framework.toLowerCase(java.util.Locale.ROOT), "GET", "/fixture", List.of(),
                new SourceData(identity.sourcePath(), 1, ProjectDocumentFormat.sha256(
                        "fictional source".getBytes(StandardCharsets.UTF_8)), "ide-document-text"),
                List.of(), List.of(), null, null, new ExamplesData(null, null), "");
        return new ProjectDocumentPage(identity, framework, markdown, data);
    }

    public static ProjectDocumentSnapshot snapshot(List<ProjectDocumentPage> pages) {
        return new ProjectDocumentSnapshot(pages, 0, "1.4.0", "f".repeat(64), false);
    }
}

package com.liuzhihang.doc.view.service.export;

import junit.framework.TestCase;

import java.util.List;

public final class ProjectDocumentIdentityTest extends TestCase {

    public void testIdentityIsStableAndSeparatesOverloadsModulesAndPaths() {
        ProjectDocumentIdentity first = identity("api", "src/main/java/example/UserApi.java", "example.UserApi",
                "find", List.of("java.lang.String"));
        ProjectDocumentIdentity same = identity("api", "src/main/java/example/UserApi.java", "example.UserApi",
                "find", List.of("java.lang.String"));

        assertEquals(first.canonicalForm(), same.canonicalForm());
        assertEquals(first.sha256(), same.sha256());
        assertEquals(first.pageFileName(), same.pageFileName());
        assertEquals("find(java.lang.String)", first.displaySignature());

        assertFalse(first.sha256().equals(identity("api", "src/main/java/example/UserApi.java", "example.UserApi",
                "find", List.of("long")).sha256()));
        assertFalse(first.sha256().equals(identity("worker", "src/main/java/example/UserApi.java", "example.UserApi",
                "find", List.of("java.lang.String")).sha256()));
        assertFalse(first.sha256().equals(identity("api", "worker/src/main/java/example/UserApi.java", "example.UserApi",
                "find", List.of("java.lang.String")).sha256()));
    }

    public void testCanonicalEncodingDoesNotConflateFieldBoundaries() {
        ProjectDocumentIdentity left = identity("a|b", "c", "d", "e", List.of("f"));
        ProjectDocumentIdentity right = identity("a", "b|c", "d", "e", List.of("f"));

        assertFalse(left.canonicalForm().equals(right.canonicalForm()));
        assertFalse(left.sha256().equals(right.sha256()));
    }

    public void testSourcePathIsNormalizedToProjectRelativeSlashForm() {
        ProjectDocumentIdentity identity = identity("api", "src\\main\\java\\example\\UserApi.java",
                "example.UserApi", "find", List.of());

        assertEquals("src/main/java/example/UserApi.java", identity.sourcePath());
    }

    private ProjectDocumentIdentity identity(String module,
                                             String path,
                                             String qualifiedClassName,
                                             String methodName,
                                             List<String> parameterTypes) {
        return ProjectDocumentIdentity.of(module, path, qualifiedClassName, methodName, parameterTypes);
    }
}

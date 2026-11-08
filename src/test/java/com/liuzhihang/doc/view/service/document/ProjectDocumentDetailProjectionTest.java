package com.liuzhihang.doc.view.service.document;

import com.intellij.psi.PsiClass;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.Header;
import com.liuzhihang.doc.view.dto.Param;
import com.liuzhihang.doc.view.enums.FrameworkEnum;
import com.liuzhihang.doc.view.service.export.ProjectDocumentIdentity;

import java.util.List;

public final class ProjectDocumentDetailProjectionTest extends LightJavaCodeInsightFixtureTestCase {

    public void testProjectionCopiesDocumentationMetadataWithoutPsiReferences() {
        PsiClass psiClass = myFixture.addClass("package projection; public class UserApi {}");
        ProjectDocumentIdentity identity = ProjectDocumentIdentity.of("main", "src/main/java/projection/UserApi.java",
                "projection.UserApi", "search", List.of("java.lang.String"));
        DocView docView = springDocView(psiClass);
        ProjectDocumentData.SourceData source = new ProjectDocumentData.SourceData(identity.sourcePath(), 12,
                "a".repeat(64), "ide-document-text");

        ProjectDocumentData.ApiData api = ProjectDocumentProjection.fromDocView(docView, identity, "SPRING", source);

        assertEquals(identity.sha256(), api.apiId());
        assertEquals("spring", api.framework());
        assertEquals("GET", api.httpMethod());
        assertEquals("/users", api.httpPath());
        assertEquals(List.of("用户", "共享", "查询"), api.tags());
        assertEquals("Token 123", api.headers().get(0).example());
        assertEquals(Boolean.TRUE, api.headers().get(0).required());
        assertEquals("1.2", api.requestParameters().get(0).since());
        assertEquals("v3", api.requestParameters().get(0).version());
        assertEquals("projection.Query", api.requestBody().type());
        assertTrue(api.requestBody().fields().get(0).isCollection());
        assertTrue(api.requestBody().fields().get(0).isMap());
        assertEquals("1.1", api.requestBody().fields().get(0).since());
        assertTrue(api.examples().request().contains("```Form"));
        assertTrue(api.examples().request().contains("id=8"));
        assertTrue(api.examples().request().contains("{\"active\":true}"));
        assertTrue(api.examples().response().contains("{\"id\":8}"));
        assertEquals(api, ProjectDocumentFormat.readApi(ProjectDocumentFormat.writeApi(api)));
    }

    public void testNormalInterfaceRemainsSeparateFromDubboAndHasNoHttpMetadata() {
        PsiClass psiClass = myFixture.addClass("package projection; public interface SearchContract {}");
        ProjectDocumentIdentity identity = ProjectDocumentIdentity.of("main", "src/main/java/projection/SearchContract.java",
                "projection.SearchContract", "find", List.of("long"));
        DocView docView = new DocView();
        docView.setPsiClass(psiClass);
        docView.setType(FrameworkEnum.DUBBO);
        docView.setName("find");
        docView.setMethod("Dubbo");
        docView.setPath("projection.SearchContract#find");
        Body requestRoot = new Body();
        requestRoot.setQualifiedNameForClassType("projection.SearchContract");
        Body requestField = new Body();
        requestField.setName("id");
        requestField.setType("long");
        requestField.setRequired(Boolean.FALSE);
        requestRoot.getChildList().add(requestField);
        docView.setReqBody(requestRoot);

        ProjectDocumentData.ApiData api = ProjectDocumentProjection.fromDocView(docView, identity, "INTERFACE",
                new ProjectDocumentData.SourceData(identity.sourcePath(), 1, "b".repeat(64), "ide-document-text"));

        assertEquals("interface", api.framework());
        assertNull(api.httpMethod());
        assertNull(api.httpPath());
        assertEquals(identity.displaySignature(), api.requestBody().type());
    }

    private DocView springDocView(PsiClass psiClass) {
        DocView docView = new DocView();
        docView.setPsiClass(psiClass);
        docView.setType(FrameworkEnum.SPRING);
        docView.setName("检索用户");
        docView.setDesc("按条件检索用户");
        docView.setMethod("get");
        docView.setPath("/users");
        docView.setClassTags(new String[]{"用户", "共享"});
        docView.setTags(new String[]{"查询", "共享"});
        docView.setRemark("仅用于 projection fixture");

        Header header = new Header();
        header.setName("Authorization");
        header.setValue("Token 123");
        header.setRequired(true);
        header.setDesc("认证令牌");
        docView.setHeaderList(List.of(header));

        Param parameter = new Param();
        parameter.setName("id");
        parameter.setType("java.lang.String");
        parameter.setRequired(true);
        parameter.setExample("8");
        parameter.setDesc("用户标识");
        parameter.setSince("1.2");
        parameter.setVersion("v3");
        docView.setReqParamList(List.of(parameter));

        Body requestRoot = new Body();
        requestRoot.setQualifiedNameForClassType("projection.Query");
        Body requestField = new Body();
        requestField.setName("filters");
        requestField.setType("java.util.List<projection.Filter>");
        requestField.setDesc("过滤条件");
        requestField.setRequired(true);
        requestField.setSince("1.1");
        requestField.setCollection(true);
        requestField.setMap(true);
        requestRoot.getChildList().add(requestField);
        docView.setReqBody(requestRoot);
        docView.setReqFormExample("id=8");
        docView.setReqBodyExample("{\"active\":true}");

        Body responseRoot = new Body();
        responseRoot.setQualifiedNameForClassType("projection.User");
        Body responseField = new Body();
        responseField.setName("id");
        responseField.setType("long");
        responseField.setRequired(true);
        responseRoot.getChildList().add(responseField);
        docView.setRespBody(responseRoot);
        docView.setRespExample("{\"id\":8}");
        return docView;
    }

    public void testProjectionRejectsExcessiveFieldDepthBeforeBuildingTemplateData() {
        PsiClass psiClass = myFixture.addClass("package projection; public class DeepApi {} ");
        DocView docView = springDocView(psiClass);
        Body requestRoot = new Body();
        requestRoot.setQualifiedNameForClassType("projection.DeepRequest");
        Body current = requestRoot;
        for (int depth = 1; depth <= ProjectDocumentFormat.MAX_FIELD_DEPTH + 1; depth++) {
            Body child = new Body();
            child.setPsiElement(psiClass);
            child.setName("field" + depth);
            child.setType("java.lang.String");
            child.setRequired(Boolean.FALSE);
            current.getChildList().add(child);
            current = child;
        }
        docView.setReqBody(requestRoot);
        ProjectDocumentIdentity identity = ProjectDocumentIdentity.of("main", "src/main/java/projection/DeepApi.java",
                "projection.DeepApi", "search", List.of());

        try {
            ProjectDocumentProjection.fromDocView(docView, identity, "SPRING",
                    new ProjectDocumentData.SourceData(identity.sourcePath(), 1, "c".repeat(64), "ide-document-text"));
            fail("Projection must reject field depth above the format bound");
        } catch (ProjectDocumentFormat.ProjectDocumentFormatException expected) {
            assertEquals(ProjectDocumentFormat.Reason.LIMIT_EXCEEDED, expected.reason());
        }
    }

    public void testMaximumSupportedFieldDepthCanRoundTripThroughStrictJsonParser() {
        PsiClass psiClass = myFixture.addClass("package projection; public class MaxDepthApi {} ");
        DocView docView = springDocView(psiClass);
        Body requestRoot = new Body();
        requestRoot.setQualifiedNameForClassType("projection.DeepRequest");
        Body current = requestRoot;
        for (int depth = 1; depth <= ProjectDocumentFormat.MAX_FIELD_DEPTH; depth++) {
            Body child = new Body();
            child.setPsiElement(psiClass);
            child.setName("field" + depth);
            child.setType("java.lang.String");
            child.setRequired(Boolean.FALSE);
            current.getChildList().add(child);
            current = child;
        }
        docView.setReqBody(requestRoot);
        ProjectDocumentIdentity identity = ProjectDocumentIdentity.of("main", "src/main/java/projection/MaxDepthApi.java",
                "projection.MaxDepthApi", "search", List.of());
        ProjectDocumentData.ApiData projected = ProjectDocumentProjection.fromDocView(docView, identity, "SPRING",
                new ProjectDocumentData.SourceData(identity.sourcePath(), 1, "9".repeat(64), "ide-document-text"));

        ProjectDocumentData.ApiData roundTrip = ProjectDocumentFormat.readApi(ProjectDocumentFormat.writeApi(projected));
        ProjectDocumentData.FieldData field = roundTrip.requestBody().fields().get(0);
        for (int depth = 1; depth < ProjectDocumentFormat.MAX_FIELD_DEPTH; depth++) {
            assertEquals(1, field.children().size());
            field = field.children().get(0);
        }
        assertTrue(field.children().isEmpty());
    }

    public void testProjectionRejectsExcessiveTotalFieldCountBeforeAllocatingProjection() {
        PsiClass psiClass = myFixture.addClass("package projection; public class WideApi {} ");
        DocView docView = new DocView();
        docView.setPsiClass(psiClass);
        docView.setType(FrameworkEnum.SPRING);
        docView.setName("wide");
        docView.setReqBody(new Body());
        docView.setRespBody(new Body());
        docView.setReqParamList(new java.util.ArrayList<>());
        for (int index = 0; index <= ProjectDocumentFormat.MAX_FIELDS_PER_API; index++) {
            docView.getReqParamList().add(new Param());
        }
        ProjectDocumentIdentity identity = ProjectDocumentIdentity.of("main", "src/main/java/projection/WideApi.java",
                "projection.WideApi", "wide", List.of());

        try {
            ProjectDocumentProjection.fromDocView(docView, identity, "SPRING",
                    new ProjectDocumentData.SourceData(identity.sourcePath(), 1, "d".repeat(64), "ide-document-text"));
            fail("Projection must reject more than the per-API field bound");
        } catch (ProjectDocumentFormat.ProjectDocumentFormatException expected) {
            assertEquals(ProjectDocumentFormat.Reason.LIMIT_EXCEEDED, expected.reason());
        }
    }

    public void testAnonymousStringAndCollectionResponseRootsRoundTripWithoutInventingNames() {
        PsiClass psiClass = myFixture.addClass("package projection; public class ReturnApi {} ");
        ProjectDocumentIdentity identity = ProjectDocumentIdentity.of("main", "src/main/java/projection/ReturnApi.java",
                "projection.ReturnApi", "get", List.of());
        DocView scalarDocView = springDocView(psiClass);
        scalarDocView.setRespBody(anonymousReturn("java.lang.String", false));
        ProjectDocumentData.ApiData scalar = ProjectDocumentProjection.fromDocView(scalarDocView, identity, "SPRING",
                new ProjectDocumentData.SourceData(identity.sourcePath(), 1, "e".repeat(64), "ide-document-text"));

        ProjectDocumentData.ApiData scalarRoundTrip = ProjectDocumentFormat.readApi(ProjectDocumentFormat.writeApi(scalar));
        assertEquals("", scalarRoundTrip.responseBody().fields().get(0).name());
        assertEquals("java.lang.String", scalarRoundTrip.responseBody().fields().get(0).type());
        assertFalse(scalarRoundTrip.responseBody().fields().get(0).isCollection());

        DocView collectionDocView = springDocView(psiClass);
        collectionDocView.setRespBody(anonymousReturn("java.util.List<java.lang.String>", true));
        ProjectDocumentData.ApiData collection = ProjectDocumentProjection.fromDocView(collectionDocView, identity, "SPRING",
                new ProjectDocumentData.SourceData(identity.sourcePath(), 1, "f".repeat(64), "ide-document-text"));

        ProjectDocumentData.ApiData collectionRoundTrip = ProjectDocumentFormat.readApi(
                ProjectDocumentFormat.writeApi(collection));
        assertEquals("", collectionRoundTrip.responseBody().fields().get(0).name());
        assertEquals("java.util.List<java.lang.String>", collectionRoundTrip.responseBody().fields().get(0).type());
        assertTrue(collectionRoundTrip.responseBody().fields().get(0).isCollection());
        assertEquals("匿名返回根", collectionRoundTrip.responseBody().fields().get(0).description());
    }

    private Body anonymousReturn(String type, boolean collection) {
        Body responseRoot = new Body();
        responseRoot.setQualifiedNameForClassType(type);
        Body anonymous = new Body();
        anonymous.setName(null);
        anonymous.setType(type);
        anonymous.setDesc("匿名返回根");
        anonymous.setRequired(Boolean.FALSE);
        anonymous.setCollection(collection);
        responseRoot.getChildList().add(anonymous);
        return responseRoot;
    }
}

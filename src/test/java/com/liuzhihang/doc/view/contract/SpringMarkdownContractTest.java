package com.liuzhihang.doc.view.contract;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.dto.Header;
import com.liuzhihang.doc.view.dto.Param;
import com.liuzhihang.doc.view.enums.ContentTypeEnum;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.service.DtoSchemaCacheService;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SpringMarkdownContractTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installSpringWeb(myFixture);
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            DtoSchemaCacheService cacheService = getProject().getService(DtoSchemaCacheService.class);
            if (cacheService != null) {
                cacheService.clear();
            }
        } finally {
            super.tearDown();
        }
    }

    public void testBasicGetControllerBuildsExpectedDocView() throws Exception {
        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByFile("contracts/spring/BasicController.java");
        PsiClass controller = javaFile.getClasses()[0];
        PsiMethod method = controller.findMethodsByName("getUser", false)[0];

        DocViewService service = DocViewService.getInstance(getProject(), controller);
        assertTrue(service instanceof SpringDocViewServiceImpl);

        DocView docView = service.buildClassMethodDoc(controller, method);

        assertEquals("用户接口", docView.getDocTitle());
        assertEquals("查询用户", docView.getName());
        assertEquals("按编号查询用户。", docView.getDesc());
        assertEquals("/api/users", docView.getPath());
        assertEquals("GET", docView.getMethod());
        assertEquals(ContentTypeEnum.FORM, docView.getContentType());

        assertEquals(1, docView.getHeaderList().size());
        Header contentType = docView.getHeaderList().get(0);
        assertEquals("Content-Type", contentType.getName());
        assertEquals("application/x-www-form-urlencoded", contentType.getValue());
        assertTrue(contentType.getRequired());

        assertEquals(1, docView.getReqParamList().size());
        Param id = docView.getReqParamList().get(0);
        assertEquals("id", id.getName());
        assertEquals("String", id.getType());
        assertTrue(id.getRequired());
        assertEquals("用户编号", id.getDesc());

        assertEquals(1, docView.getRespBody().getChildList().size());
        Body name = docView.getRespBody().getChildList().get(0);
        assertEquals("name", name.getName());
        assertEquals("String", name.getType());
        assertFalse(name.getRequired());
        assertEquals("用户名称", name.getDesc());

        String markdown = DocViewData.markdownText(getProject(), docView);
        Path goldenFile = Path.of(getTestDataPath(), "contracts/markdown/spring-basic.md");
        String expectedMarkdown = StringUtil.convertLineSeparators(
                Files.readString(goldenFile, StandardCharsets.UTF_8)) + "\n";
        assertEquals(expectedMarkdown, markdown);
    }

    @Override
    protected String getTestDataPath() {
        return Path.of("src/test/testData").toAbsolutePath().toString();
    }
}

package com.liuzhihang.doc.view.contract;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.service.DtoSchemaCacheService;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.utils.DocViewUtils;

import java.nio.file.Path;

public final class OpenApi3OperationSummaryTest extends LightJavaCodeInsightFixtureTestCase {

    private PsiClass controller;
    private Boolean originalNameUseSwagger3;
    private Boolean originalNameUseSwagger;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AnnotationStubs.installSpringWeb(myFixture);
        AnnotationStubs.installOpenApi3(myFixture);
        AnnotationStubs.installSwagger2(myFixture);

        Settings settings = Settings.getInstance(getProject());
        originalNameUseSwagger3 = settings.getNameUseSwagger3();
        originalNameUseSwagger = settings.getNameUseSwagger();
        settings.setNameUseSwagger3(true);
        settings.setNameUseSwagger(true);

        PsiJavaFile javaFile = (PsiJavaFile) myFixture.configureByFile(
                "contracts/openapi3/OperationSummaryController.java");
        controller = javaFile.getClasses()[0];
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            Settings settings = Settings.getInstance(getProject());
            settings.setNameUseSwagger3(originalNameUseSwagger3);
            settings.setNameUseSwagger(originalNameUseSwagger);

            DtoSchemaCacheService cacheService = getProject().getService(DtoSchemaCacheService.class);
            if (cacheService != null) {
                cacheService.clear();
            }
        } finally {
            controller = null;
            super.tearDown();
        }
    }

    public void testSummaryIsUsedAsDocViewName() {
        PsiMethod method = method("summaryOnly");

        DocViewService service = DocViewService.getInstance(getProject(), controller);
        DocView docView = service.buildClassMethodDoc(controller, method);

        assertEquals("查询用户", docView.getName());
        assertEquals("按 ID 查询用户", docView.getDesc());
    }

    public void testSummaryWinsOverSwagger2Value() {
        assertEquals("OpenAPI 3 名称", DocViewUtils.getName(method("summaryPriority")));
    }

    public void testBlankSummaryFallsBackToSwagger2Value() {
        assertEquals("空白 summary fallback", DocViewUtils.getName(method("blankSummary")));
    }

    public void testEmptySummaryFallsBackToSwagger2Value() {
        assertEquals("空 summary fallback", DocViewUtils.getName(method("emptySummary")));
    }

    public void testDefaultSummaryFallsBackToSwagger2Value() {
        assertEquals("默认 summary fallback", DocViewUtils.getName(method("defaultSummary")));
    }

    public void testDisabledSwagger3NameSourceIgnoresSummary() {
        Settings.getInstance(getProject()).setNameUseSwagger3(false);

        assertEquals("Swagger 3 已关闭", DocViewUtils.getName(method("swagger3Disabled")));
    }

    public void testOperationDescriptionRemainsUnchanged() {
        assertEquals("按 ID 查询用户", DocViewUtils.getMethodDesc(method("summaryOnly")));
    }

    private PsiMethod method(String name) {
        return controller.findMethodsByName(name, false)[0];
    }

    @Override
    protected String getTestDataPath() {
        return Path.of("src/test/testData").toAbsolutePath().toString();
    }
}

package com.liuzhihang.doc.view.contract;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.service.DocViewService;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;

public final class DocViewServiceDuplicateNameCompatibilityTest extends LightJavaCodeInsightFixtureTestCase {

    public void testWholeClassBuildKeepsFirstDuplicateAndSuffixesLaterDuplicatesWithGlobalIndexes() {
        List<DocView> result = buildWholeClassDocs(fixtureClass());

        assertEquals(5, result.size());
        assertEquals("查询用户", result.get(0).getName());
        assertTrue(result.get(2).getName().matches("查询用户_[A-Za-z]{5}2"));
        assertTrue(result.get(4).getName().matches("查询用户_[A-Za-z]{5}4"));
    }

    public void testWholeClassBuildKeepsUniqueNamesAndProducesDistinctNames() {
        List<DocView> result = buildWholeClassDocs(fixtureClass());

        assertEquals("健康检查", result.get(1).getName());
        assertEquals("更新用户", result.get(3).getName());
        List<String> names = result.stream().map(DocView::getName).toList();
        assertEquals(names.size(), new HashSet<>(names).size());
    }

    public void testSingleMethodBuildLeavesNameUnchanged() {
        PsiClass fixtureClass = fixtureClass();
        PsiMethod targetMethod = fixtureClass.findMethodsByName("queryUser", false)[0];
        DocView methodDoc = new DocView("查询用户");
        DocViewService service = minimalService(List.of(new DocView("错误路径")), methodDoc);

        List<DocView> result = service.buildDoc(fixtureClass, targetMethod);

        assertEquals(1, result.size());
        assertEquals("查询用户", result.get(0).getName());
    }

    private List<DocView> buildWholeClassDocs(PsiClass fixtureClass) {
        List<DocView> classDocs = List.of(
                new DocView("查询用户"),
                new DocView("健康检查"),
                new DocView("查询用户"),
                new DocView("更新用户"),
                new DocView("查询用户")
        );
        DocViewService service = minimalService(classDocs, new DocView("未使用"));

        return service.buildDoc(fixtureClass, null);
    }

    private DocViewService minimalService(List<DocView> classDocs, DocView methodDoc) {
        return new DocViewService() {
            @Override
            public boolean checkMethod(@NotNull PsiMethod targetMethod) {
                return targetMethod.isValid();
            }

            @Override
            public List<DocView> buildClassDoc(@NotNull PsiClass psiClass) {
                return classDocs;
            }

            @Override
            public @NotNull DocView buildClassMethodDoc(PsiClass psiClass, @NotNull PsiMethod psiMethod) {
                return methodDoc;
            }
        };
    }

    private PsiClass fixtureClass() {
        return myFixture.addClass("""
                package contract.fixture;

                public class DuplicateNameFixture {
                    public void queryUser() {
                    }
                }
                """);
    }
}

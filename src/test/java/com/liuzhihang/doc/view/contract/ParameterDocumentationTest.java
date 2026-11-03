package com.liuzhihang.doc.view.contract;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.liuzhihang.doc.view.dto.Body;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.service.impl.DubboDocViewServiceImpl;
import com.liuzhihang.doc.view.utils.CustomPsiCommentUtils;
import com.liuzhihang.doc.view.utils.DubboPsiUtils;

public final class ParameterDocumentationTest extends LightJavaCodeInsightFixtureTestCase {
    public void testOriginalDubboIssueMultipleParametersKeepDescriptions() {
        PsiClass api = source("""
                interface SearchApi {
                    /** 身份查询
                     * @param page 分页参数
                     * @param searchKey 关键字
                     * @param viewId 视图id
                     * @param displayType 显示类型
                     * @param fetchOrgsInfo 组装所在机构信息
                     * @param maxLevel 最大搜索等级
                     * @param displayLevel 机构显示级别
                     * @since 2024/9/3
                     */
                    String search(Page page, String searchKey, long viewId, String displayType,
                                  boolean fetchOrgsInfo, Integer maxLevel, Integer displayLevel);
                }
                class Page { long size; long current; }
                """);
        DocView doc = new DubboDocViewServiceImpl().buildClassMethodDoc(api, api.getMethods()[0]);
        String[] expected = {"分页参数", "关键字", "视图id", "显示类型", "组装所在机构信息", "最大搜索等级", "机构显示级别"};
        assertEquals(expected.length, doc.getReqBody().getChildList().size());
        String markdown = DocViewData.markdownText(getProject(), doc);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], doc.getReqBody().getChildList().get(i).getDesc().trim());
            assertTrue(markdown.contains(expected[i]));
        }
    }

    public void testParameterNamesMatchExactlyAndMultilineIsPreserved() {
        PsiMethod method = source("""
                interface QueryApi {
                    /** 查询
                     * @param id first line
                     *           second line
                     * @param identifier another description
                     */
                    void query(String id, String identifier);
                }
                """).getMethods()[0];
        assertEquals("first line second line", CustomPsiCommentUtils.paramDocComment(
                method.getDocComment(), method.getParameterList().getParameters()[0]));
        assertEquals("another description", CustomPsiCommentUtils.paramDocComment(
                method.getDocComment(), method.getParameterList().getParameters()[1]));
        assertEquals("first line second line", DubboPsiUtils.buildBody(method).getChildList().get(0).getDesc());
    }

    public void testMalformedParamTagDoesNotBreakDubboGeneration() {
        PsiMethod method = source("""
                interface QueryApi {
                    /** 查询
                     * @param
                     * @param id identifier
                     * @param empty
                     */
                    void query(String id, String empty);
                }
                """).getMethods()[0];
        Body body = DubboPsiUtils.buildBody(method);
        assertEquals("identifier", body.getChildList().get(0).getDesc());
        assertEquals("", body.getChildList().get(1).getDesc());
    }

    private PsiClass source(String text) {
        return ((PsiJavaFile) myFixture.configureByText("QueryApi.java", text)).getClasses()[0];
    }
}

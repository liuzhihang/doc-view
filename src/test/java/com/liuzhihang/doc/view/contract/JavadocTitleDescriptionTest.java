package com.liuzhihang.doc.view.contract;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.psi.PsiClass;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import com.liuzhihang.doc.view.config.Settings;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.dto.DocViewData;
import com.liuzhihang.doc.view.service.impl.SpringDocViewServiceImpl;
import com.liuzhihang.doc.view.testsupport.AnnotationStubs;
import com.liuzhihang.doc.view.utils.VelocityUtils;
import com.intellij.util.xmlb.XmlSerializer;

public final class JavadocTitleDescriptionTest extends LightJavaCodeInsightFixtureTestCase {
    @Override protected void setUp() throws Exception {
        super.setUp();
        Settings.getInstance(getProject()).loadState(new Settings());
        AnnotationStubs.installSpringWeb(myFixture);
    }
    @Override protected void tearDown() throws Exception {
        try { Settings.getInstance(getProject()).loadState(new Settings()); }
        finally { super.tearDown(); }
    }
    private PsiClass sample() {
        return myFixture.addClass("""
                package demo;
                /** User API title
                 * Class description in English.
                 *
                 * Second paragraph.
                 * @since 1.0
                 */
                @org.springframework.web.bind.annotation.RestController
                public class Titles {
                    /** Find user details
                     * Method description in English.
                     *
                     * Keep this paragraph.
                     * @param id user id
                     * @return result
                     */
                    @org.springframework.web.bind.annotation.GetMapping("/users")
                    public String find(String id) { return id; }
                }
                """);
    }
    public void testClassTitleIsFirstPhysicalLineWithSpaces() {
        assertEquals("User API title", DocViewUtils.getTitle(sample()));
    }
    public void testMethodTitlePreservesSpacesAndDoesNotUseLegacyFifteenCharacterCutoff() {
        assertEquals("Find user details", DocViewUtils.getName(sample().getMethods()[0]));
    }
    public void testDescriptionExcludesTitleAndBlockTagsPreservingParagraphs() {
        assertEquals("Method description in English.\n\nKeep this paragraph.",
                DocViewUtils.getMethodDesc(sample().getMethods()[0]));
    }
    public void testClassDescriptionHasIndependentTemplateVariable() {
        PsiClass type = sample();
        DocView doc = new SpringDocViewServiceImpl().buildClassMethodDoc(type, type.getMethods()[0]);
        String output = VelocityUtils.render("${DocView.classDesc}|${DocView.desc}", new DocViewData(getProject(), doc));
        assertEquals("Class description in English.\n\nSecond paragraph.|Method description in English.\n\nKeep this paragraph.", output);
    }
    public void testSingleLineHasNoRepeatedDescription() {
        PsiClass type = myFixture.addClass("class Single { /** Just a title */ public void go() {} }");
        assertEquals("Just a title", DocViewUtils.getName(type.getMethods()[0]));
        assertEquals("", DocViewUtils.getMethodDesc(type.getMethods()[0]));
    }
    public void testUnicodeLimitAndSettingsRoundTrip() {
        var state = XmlSerializer.serialize(new Settings());
        state.addContent(new org.jdom.Element("option").setAttribute("name", "commentTitleMaxLength").setAttribute("value", "3"));
        Settings.getInstance(getProject()).loadState(XmlSerializer.deserialize(state, Settings.class));
        PsiClass type = myFixture.addClass("/** A😀BC\n * Keep complete class description */ class Emoji { /** A😀BC\n * Keep complete method description */ void go() {} }");
        assertEquals("A😀B", DocViewUtils.getTitle(type));
        assertEquals("A😀B", DocViewUtils.getName(type.getMethods()[0]));
        assertEquals("Keep complete method description", DocViewUtils.getMethodDesc(type.getMethods()[0]));
    }
    public void testBlankLinesInlineTagsAndCrLfDoNotBecomeTitlesOrBlockTags() {
        PsiClass type = myFixture.addClass("/**\r\n *\r\n * User {@code API}\r\n * Has {@link String} details.\r\n * @author example\r\n */ class Inline {}");
        assertEquals("User {@code API}", DocViewUtils.getTitle(type));
        assertEquals("Has {@link String} details.", DocViewUtils.getClassDesc(type));
        PsiClass tags = myFixture.addClass("/** @author example */ class Tags { /** @return ignored */ String go() { return null; } }");
        assertEquals("Tags", DocViewUtils.getTitle(tags));
        assertEquals("go", DocViewUtils.getName(tags.getMethods()[0]));
        assertEquals("", DocViewUtils.getMethodDesc(tags.getMethods()[0]));
    }

    public void testDisabledFirstLineAndDescriptionOptions() {
        Settings settings = Settings.getInstance(getProject());
        settings.setNameCommentFirstLine(false);
        settings.setCommentTitleMaxLength(200);
        PsiClass type = sample();
        assertEquals("Find user details Method description in English. Keep this paragraph.", DocViewUtils.getName(type.getMethods()[0]));
        assertEquals("User API title", DocViewUtils.getTitle(type));
        settings.setDescCommentBody(false);
        assertEquals("", DocViewUtils.getMethodDesc(type.getMethods()[0]));
        assertEquals("", DocViewUtils.getClassDesc(type));
    }

    public void testExplicitTagsAreNotTruncated() {
        Settings.getInstance(getProject()).setCommentTitleMaxLength(1);
        PsiClass type = myFixture.addClass("/** Class title\n * @DocView.Title Explicit class title */ class Explicit { /** Method title\n * @DocView.Name Explicit method name */ void go() {} }");
        assertEquals("Explicit class title", DocViewUtils.getTitle(type));
        assertEquals("Explicit method name", DocViewUtils.getName(type.getMethods()[0]));
    }

    public void testDubboAndFeignKeepIndependentClassAndMethodDescriptions() {
        AnnotationStubs.installDubbo(myFixture);
        PsiClass dubbo = myFixture.addClass("/** RPC title\n * RPC class description */ public interface Rpc { /** Call title\n * Call description */ String call(); }");
        DocView rpc = new com.liuzhihang.doc.view.service.impl.DubboDocViewServiceImpl().buildClassMethodDoc(dubbo, dubbo.getMethods()[0]);
        assertEquals("RPC title", rpc.getDocTitle());
        assertEquals("RPC class description", rpc.getClassDesc());
        assertEquals("Call description", rpc.getDesc());
        assertTrue(DocViewData.markdownText(getProject(), rpc).contains("RPC class description"));
        myFixture.addClass("package org.springframework.cloud.openfeign; public @interface FeignClient { String name(); }");
        PsiClass feign = myFixture.addClass("/** Client title\n * Client class description */ @org.springframework.cloud.openfeign.FeignClient(name=\"sample\") public interface Client { /** Get title\n * Get description */ @org.springframework.web.bind.annotation.GetMapping(\"/sample\") String get(); }");
        DocView client = new SpringDocViewServiceImpl().buildClassMethodDoc(feign, feign.getMethods()[0]);
        assertEquals("Client title", client.getDocTitle());
        assertEquals("Client class description", client.getClassDesc());
        assertEquals("Get description", client.getDesc());
    }

}

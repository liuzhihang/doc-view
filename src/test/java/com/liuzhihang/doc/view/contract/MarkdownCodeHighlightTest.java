package com.liuzhihang.doc.view.contract;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil;
public final class MarkdownCodeHighlightTest extends LightJavaCodeInsightFixtureTestCase {
    public void testJsonFenceHasColoredTokens() {
        for (String language : new String[]{"json", "JSON"}) {
            String markdown = "```" + language + "\n{\"name\": \"demo\", \"count\": 42, \"active\": true}\n```\n";
            String html = com.liuzhihang.doc.view.utils.MarkdownPreviewHtml.render(getProject(), markdown);
            assertTrue("JSON must contain syntax colors: " + html, html.contains("color:"));
        }
    }
}

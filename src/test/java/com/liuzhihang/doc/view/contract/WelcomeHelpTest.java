package com.liuzhihang.doc.view.contract;

import junit.framework.TestCase;
import com.liuzhihang.doc.view.ui.welcome.WelcomeDocuments;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Contracts for the small bundled welcome pages, not a second copy of the user manual. */
public final class WelcomeHelpTest extends TestCase {
    public void testPublicDocumentationUsesMasterInBothLanguages() throws Exception {
        String guide = "https://github.com/liuzhihang/doc-view/tree/master/docs/user-guide";
        String changelog = "https://github.com/liuzhihang/doc-view/blob/master/CHANGELOG.md";
        assertEquals(guide, WelcomeDocuments.GUIDE);
        assertTrue(WelcomeDocuments.isExternalLink(guide));
        assertTrue(WelcomeDocuments.isExternalLink(changelog));
        assertFalse(WelcomeDocuments.isExternalLink(guide.replace("/master/", "/develop/")));
        assertFalse(WelcomeDocuments.isExternalLink(changelog.replace("/master/", "/develop/")));
        for (String name : new String[]{"welcome.html", "updates.html", "welcome_zh_CN.html", "updates_zh_CN.html"}) {
            try (var stream = getClass().getResourceAsStream("/welcome/" + name)) {
                assertNotNull(name, stream);
                String html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(name, html.contains("href=\"" + guide + "\""));
                assertTrue(name, html.contains("href=\"" + changelog + "\""));
                assertFalse(name, html.contains("/develop/"));
            }
        }
    }

    public void testOnlyWelcomeAssetsAreBundled() throws Exception {
        for (String name : new String[]{"welcome.html", "updates.html", "welcome_zh_CN.html", "updates_zh_CN.html", "welcome.css"}) {
            try (var stream = getClass().getResourceAsStream("/welcome/" + name)) {
                assertNotNull("Missing welcome asset: " + name, stream);
                String source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                assertFalse(source.contains("<script"));
                assertFalse(source.contains("<iframe"));
                assertFalse(source.contains("@import"));
                assertFalse(source.matches("(?is).*<(?:img|link)[^>]*(?:src|href)=.*"));
            }
        }
        assertNull("User guides belong on GitHub", getClass().getResource("/doc-view-help/templates.md"));
        assertNull("The entire changelog must not be embedded", getClass().getResource("/doc-view-help/CHANGELOG.md"));
    }

    public void testUnknownActionsAndNonPublicUrlsCannotNavigate() throws Exception {
        var action = WelcomeDocuments.class.getMethod("action", String.class);
        for (String name : new String[]{"home", "updates", "directory", "templates", "integrations", "notifications"}) {
            assertEquals(name, action.invoke(null, "docview://action/" + name));
        }
        for (String value : new String[]{"docview://action/upload", "docview://action/templates?apply=true", "file:///private/a", "javascript:alert(1)", "https://evil.test/"}) {
            assertNull(value, action.invoke(null, value));
        }
        var external = WelcomeDocuments.class.getMethod("isExternalLink", String.class);
        assertEquals(true, external.invoke(null, "https://github.com/liuzhihang/doc-view"));
        for (String value : new String[]{"file:///private/a", "javascript:alert(1)", "https://github.com.evil.test/", "http://localhost:8000/", "https://github.com/liuzhihang/doc-view?token=a"}) {
            assertEquals(value, false, external.invoke(null, value));
        }
    }

    public void testRenderedPagesEscapeVersionAndShowReleaseHighlights() throws Exception {
        var render = WelcomeDocuments.class.getDeclaredMethod("render", String.class, String.class, Map.class, boolean.class,
                java.util.Locale.class);
        render.setAccessible(true);
        Map<String, String> theme = Map.of("THEME", "light", "FONT_SIZE", "16", "BG", "#ffffff", "FG", "#111111", "MUTED", "#666666", "BORDER", "#dddddd", "LINK", "#315cbb");
        for (java.util.Locale locale : new java.util.Locale[]{java.util.Locale.US, java.util.Locale.SIMPLIFIED_CHINESE}) {
            for (String page : new String[]{"welcome", "updates"}) {
                String html = (String) render.invoke(null, page, "1.4.0<&\"", theme, true, locale);
                assertTrue(html.contains("1.4.0&lt;&amp;&quot;"));
                assertFalse("Release highlights must not be labelled as an unreleased preview", html.contains("Not yet released"));
                assertFalse(html.contains("未发布"));
                assertTrue(html.contains("Content-Security-Policy"));
                assertTrue(html.contains("default-src 'none'"));
                assertFalse("No unexpanded placeholders", html.contains("@@"));
                assertFalse(html.contains("<script"));
                assertTrue(html.contains("https://github.com/liuzhihang/doc-view"));
            }
        }
    }
}

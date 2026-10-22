package com.liuzhihang.doc.view.ui.welcome;

import junit.framework.TestCase;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WelcomeLocaleTest extends TestCase {
    private static final Map<String, String> THEME = Map.of(
            "THEME", "light", "FONT_SIZE", "16", "BG", "#ffffff", "FG", "#111111",
            "MUTED", "#666666", "BORDER", "#dddddd", "LINK", "#315cbb");
    private static final Pattern HREF = Pattern.compile("href=\"([^\"]+)\"");

    public void testOnlySimplifiedChineseSelectsChinesePageResources() throws Exception {
        assertEquals("welcome_zh_CN.html", WelcomeDocuments.pageResourceName("welcome", Locale.SIMPLIFIED_CHINESE));
        assertEquals("updates_zh_CN.html", WelcomeDocuments.pageResourceName("updates", Locale.SIMPLIFIED_CHINESE));
        for (Locale locale : List.of(Locale.US, Locale.JAPAN, Locale.TRADITIONAL_CHINESE, Locale.CHINESE)) {
            assertEquals("welcome.html", WelcomeDocuments.pageResourceName("welcome", locale));
            assertEquals("updates.html", WelcomeDocuments.pageResourceName("updates", locale));
        }
    }

    public void testEnglishAndChineseReleasePagesKeepTheSameNavigationAndSecurityBoundary() throws Exception {
        for (String page : new String[]{"welcome", "updates"}) {
            String english = WelcomeDocuments.render(page, "1.4.0", THEME, true, Locale.US);
            String chinese = WelcomeDocuments.render(page, "1.4.0", THEME, true, Locale.SIMPLIFIED_CHINESE);

            assertTrue(english.startsWith("<!doctype html>\n<html lang=\"en\">"));
            assertTrue(chinese.startsWith("<!doctype html>\n<html lang=\"zh-CN\">"));
            assertTrue(english.contains("1.4.0"));
            assertTrue(chinese.contains("1.4.0"));
            assertTrue(english.contains("Highlights included in version 1.4.0."));
            assertTrue(chinese.contains("版本 1.4.0 更新重点"));
            assertFalse(english.contains("Not yet released"));
            assertFalse(english.contains("未发布"));
            assertFalse(chinese.contains("未发布"));
            assertTrue(english.contains("Update notifications are enabled."));
            assertTrue(english.contains("Turn off update notifications"));
            assertTrue(chinese.contains("更新提醒当前已开启。"));
            assertTrue(chinese.contains("关闭更新提醒"));
            assertFalse(english.contains("@@"));
            assertFalse(chinese.contains("@@"));
            assertTrue(english.contains("Content-Security-Policy"));
            assertTrue(chinese.contains("Content-Security-Policy"));
            assertTrue(english.contains("default-src 'none'"));
            assertTrue(chinese.contains("default-src 'none'"));
            assertEquals("Navigation targets must not change with the page language", hrefs(english), hrefs(chinese));
        }
    }

    public void testNotificationStateLabelsFollowThePageLanguage() throws Exception {
        String english = WelcomeDocuments.render("welcome", "1.3.14", THEME, false, Locale.US);
        String chinese = WelcomeDocuments.render("welcome", "1.3.14", THEME, false, Locale.SIMPLIFIED_CHINESE);

        assertTrue(english.contains("Update notifications are disabled."));
        assertTrue(english.contains("Turn on update notifications"));
        assertTrue(chinese.contains("更新提醒当前已关闭。"));
        assertTrue(chinese.contains("开启更新提醒"));
    }

    private static List<String> hrefs(String html) {
        Matcher matcher = HREF.matcher(html);
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        while (matcher.find()) result.add(matcher.group(1));
        return List.copyOf(result);
    }
}

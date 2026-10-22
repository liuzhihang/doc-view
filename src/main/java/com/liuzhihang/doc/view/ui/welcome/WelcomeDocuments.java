package com.liuzhihang.doc.view.ui.welcome;

import com.intellij.DynamicBundle;
import com.intellij.openapi.util.text.StringUtil;
import com.liuzhihang.doc.view.DocViewBundle;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/** Trusted, bundled presentation resources and the small set of welcome-page destinations. */
public final class WelcomeDocuments {
    public static final Set<String> PAGES = Set.of("welcome", "updates");
    public static final String GUIDE = "https://github.com/liuzhihang/doc-view/tree/master/docs/user-guide";
    private static final Set<String> ACTIONS = Set.of("home", "updates", "directory", "templates", "integrations", "notifications");
    private static final Set<String> EXTERNAL_LINKS = Set.of(GUIDE,
            "https://github.com/liuzhihang/doc-view",
            "https://github.com/liuzhihang/doc-view/issues",
            "https://github.com/liuzhihang/doc-view/blob/master/CHANGELOG.md");
    private static final String ACTION_PREFIX = "docview://action/";

    private WelcomeDocuments() {}

    public static String action(String url) {
        if (url == null || !url.startsWith(ACTION_PREFIX)) return null;
        String action = url.substring(ACTION_PREFIX.length());
        return ACTIONS.contains(action) ? action : null;
    }

    public static boolean isExternalLink(String url) {
        return url != null && EXTERNAL_LINKS.contains(url);
    }

    public static String render(String page, String version, Map<String, String> theme, boolean notifications) throws IOException {
        return render(page, version, theme, notifications, DynamicBundle.getLocale());
    }

    static String render(String page, String version, Map<String, String> theme, boolean notifications, Locale locale) throws IOException {
        if (!PAGES.contains(page)) throw new IOException(DocViewBundle.message("welcome.error.page.missing"));
        boolean simplifiedChinese = Locale.SIMPLIFIED_CHINESE.equals(locale);
        String html = read(pageResourceName(page, locale)).replace("@@CSS@@", read("welcome.css"));
        for (String key : new String[]{"THEME", "FONT_SIZE", "BG", "FG", "MUTED", "BORDER", "LINK"}) {
            html = html.replace("@@" + key + "@@", StringUtil.escapeXmlEntities(theme.get(key)));
        }
        String updateState = simplifiedChinese
                ? notifications ? "开启" : "关闭"
                : notifications ? "enabled" : "disabled";
        String updateLabel = simplifiedChinese
                ? notifications ? "关闭更新提醒" : "开启更新提醒"
                : notifications ? "Turn off update notifications" : "Turn on update notifications";
        html = html.replace("@@VERSION@@", StringUtil.escapeXmlEntities(version == null ? "development" : version))
                .replace("@@UPDATE_STATE@@", updateState)
                .replace("@@UPDATE_LABEL@@", updateLabel);
        // No subresources, scripts, frames, forms or remote connections are needed by these pages.
        return html.replace("<head>", "<head><meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'\">");
    }

    static String pageResourceName(String page, Locale locale) throws IOException {
        if (!PAGES.contains(page)) throw new IOException(DocViewBundle.message("welcome.error.page.missing"));
        return Locale.SIMPLIFIED_CHINESE.equals(locale) ? page + "_zh_CN.html" : page + ".html";
    }

    private static String read(String name) throws IOException {
        try (var stream = WelcomeDocuments.class.getResourceAsStream("/welcome/" + name)) {
            if (stream == null) throw new IOException(DocViewBundle.message("welcome.error.resource.missing"));
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

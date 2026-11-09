package com.liuzhihang.doc.view.utils;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.ui.ColorUtil;
import org.intellij.plugins.markdown.ui.preview.html.MarkdownUtil;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Renders Markdown with JSON token colors even when no JSON fence provider is installed. */
public final class MarkdownPreviewHtml {
    private static final Pattern JSON_FENCE = Pattern.compile(
            "(?is)(<code\\b[^>]*class=[\"']language-json[\"'][^>]*>)(.*?)(</code>)");
    private static final Pattern JSON_TOKEN = Pattern.compile(
            "\"(?:\\\\.|[^\"\\\\])*\"|-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|\\b(?:true|false|null)\\b");

    private MarkdownPreviewHtml() { }

    public static String render(Project project, String markdown) {
        String html = MarkdownUtil.INSTANCE.generateMarkdownHtml(new LightVirtualFile("doc-view-preview.md", markdown), markdown, project);
        EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
        Matcher fences = JSON_FENCE.matcher(html);
        StringBuilder result = new StringBuilder();
        while (fences.find()) {
            String text = StringUtil.unescapeXmlEntities(fences.group(2).replaceAll("<[^>]*>", ""));
            fences.appendReplacement(result, Matcher.quoteReplacement(fences.group(1) + colorJson(text, scheme) + fences.group(3)));
        }
        fences.appendTail(result);
        return result.toString();
    }

    private static String colorJson(String text, EditorColorsScheme scheme) {
        Matcher tokens = JSON_TOKEN.matcher(text);
        StringBuilder html = new StringBuilder();
        int end = 0;
        while (tokens.find()) {
            html.append(StringUtil.escapeXmlEntities(text.substring(end, tokens.start())));
            String token = tokens.group();
            TextAttributesKey key;
            if (token.startsWith("\"")) {
                int next = tokens.end();
                while (next < text.length() && Character.isWhitespace(text.charAt(next))) next++;
                key = next < text.length() && text.charAt(next) == ':'
                        ? DefaultLanguageHighlighterColors.INSTANCE_FIELD : DefaultLanguageHighlighterColors.STRING;
            } else {
                key = Character.isLetter(token.charAt(0)) ? DefaultLanguageHighlighterColors.KEYWORD : DefaultLanguageHighlighterColors.NUMBER;
            }
            var attributes = scheme.getAttributes(key);
            Color color = attributes == null ? null : attributes.getForegroundColor();
            if (color == null) color = scheme.getDefaultForeground();
            html.append("<span style=\"color: #").append(ColorUtil.toHex(color)).append("\">")
                    .append(StringUtil.escapeXmlEntities(token)).append("</span>");
            end = tokens.end();
        }
        return html.append(StringUtil.escapeXmlEntities(text.substring(end))).toString();
    }
}

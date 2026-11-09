package com.liuzhihang.doc.view.ui;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Adds theme-aware Velocity tokens over the existing Markdown lexer. */
final class TemplateSyntaxHighlighting {
    private static final Pattern TOKENS = Pattern.compile(
            "(?<!\\\\)(\\$!?\\{[\\w.]+}|\\$!?[A-Za-z_][\\w.]*)|(?<!\\\\)#(?:if|elseif|else|end|foreach|set|macro|parse|include|stop|break|evaluate)\\b");

    static void install(EditorEx editor, Disposable lifetime) {
        List<RangeHighlighter> ranges = new ArrayList<>();
        Runnable refresh = () -> {
            ranges.forEach(editor.getMarkupModel()::removeHighlighter);
            ranges.clear();
            var matcher = TOKENS.matcher(editor.getDocument().getImmutableCharSequence());
            while (matcher.find()) {
                TextAttributesKey key = matcher.group(1) == null
                        ? DefaultLanguageHighlighterColors.KEYWORD : DefaultLanguageHighlighterColors.INSTANCE_FIELD;
                ranges.add(editor.getMarkupModel().addRangeHighlighter(key, matcher.start(), matcher.end(),
                        HighlighterLayer.SYNTAX + 1, HighlighterTargetArea.EXACT_RANGE));
            }
        };
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void documentChanged(@NotNull DocumentEvent event) { refresh.run(); }
        }, lifetime);
        refresh.run();
    }
}

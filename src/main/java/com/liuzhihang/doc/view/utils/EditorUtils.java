package com.liuzhihang.doc.view.utils;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;

/**
 * 编辑器工具
 *
 * @author liuzhihang
 * @since 2023/8/5 18:56
 */
public class EditorUtils {

    /**
     * 对 markdown 编辑器进行渲染
     *
     * @param editor 编辑器
     */
    public static void renderMarkdownEditor(EditorEx editor) {

        EditorSettings editorSettings = editor.getSettings();
        editorSettings.setAdditionalLinesCount(0);
        editorSettings.setAdditionalColumnsCount(0);
        editorSettings.setLineMarkerAreaShown(false);
        editorSettings.setLineNumbersShown(false);
        editorSettings.setVirtualSpace(false);
        editorSettings.setFoldingOutlineShown(false);

        editorSettings.setLanguageSupplier(() -> Language.findLanguageByID("Markdown"));

    }

}

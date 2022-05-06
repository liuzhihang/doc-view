package com.liuzhihang.doc.view.ui.editor;

import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @version EditorWidgetActionProvider.java, v 0.1 2022/4/20 15:28 liuzhihan
 */
public class EditorFloatingToolbarProvider extends AbstractFloatingToolbarProvider {


    public EditorFloatingToolbarProvider() {
        super("liuzhihang.doc.preview.editor.floating");
    }

    @Override
    public boolean getAutoHideable() {
        return true;
    }
}

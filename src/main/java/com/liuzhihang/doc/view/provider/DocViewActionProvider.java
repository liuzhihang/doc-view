package com.liuzhihang.doc.view.provider;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.InspectionWidgetActionProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 编辑窗口，右上角
 *
 * @author liuzhihang
 * @since 2023/5/22 23:10
 */
public class DocViewActionProvider implements InspectionWidgetActionProvider {
    @Nullable
    @Override
    public AnAction createAction(@NotNull Editor editor) {

        Project project = editor.getProject();
        if (project == null || project.isDefault()) {
            return null;
        }

        return new AnAction("Refresh", "Refresh", AllIcons.Actions.Refresh) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {

            }
        };
    }
}

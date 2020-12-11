package com.liuzhihang.doc.view.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author liuzhihang
 * @date 2020/2/28 18:07
 */
public class ProjectViewPreviewAction extends AnAction {


    @Override
    public void actionPerformed(AnActionEvent e) {

        // 获取当前project对象
        Project project = e.getData(PlatformDataKeys.PROJECT);
        // 获取当前编辑的文件, 可以进而获取 PsiClass, PsiField 对象
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        //
        // DocViewForm form = new DocViewForm(project);
        //
        // JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(form.getContentPanel(), form.getContentPanel())
        //         .setProject(project)
        //         .setResizable(true)
        //         .setMovable(true)
        //         .setFocusable(true)
        //         .setCancelOnClickOutside(true)
        //         .setModalContext(false)
        //         // .setDimensionServiceKey(project, DOCUMENTATION_POPUP_SIZE, false)
        //         .createPopup();
        // popup.showCenteredInCurrentWindow(project);


    }


    /**
     * 设置在哪里可以使用
     *
     * @param e
     */
    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
    }
}

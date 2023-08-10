package com.liuzhihang.doc.view.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.exception.DocViewException;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.utils.CustomPsiUtils;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import org.jetbrains.annotations.NotNull;

/**
 * 抽象类，进行一些公共的验证操作
 *
 * @author liuzhihang
 * @since 2023/7/11 23:04
 */
public class AbstractAction extends AnAction {

    /**
     * 日志
     */
    private static final Logger LOG = Logger.getInstance(AbstractAction.class);

    /**
     * 工程
     */
    protected Project project;
    /**
     * 文件
     */
    protected PsiFile psiFile;

    /**
     * 编辑器
     */
    protected Editor editor;

    /**
     * 当前类
     */
    protected PsiClass targetClass;

    /**
     * 当前方法
     */
    protected PsiMethod targetMethod;

    /**
     * @see AnAction#actionPerformed(AnActionEvent)
     */
    @Override
    public void actionPerformed(AnActionEvent e) {

        try {
            // 对所有未保存的文件进行保存
            FileDocumentManager.getInstance().saveAllDocuments();

            project = e.getData(PlatformDataKeys.PROJECT);
            psiFile = e.getData(CommonDataKeys.PSI_FILE);
            editor = e.getData(CommonDataKeys.EDITOR);

            // 验证是否为空
            if (editor == null || project == null || psiFile == null || DumbService.isDumb(project)) {
                throw new DocViewException("当前位置不合法");
            }
            // 获取Java类或者接口
            targetClass = CustomPsiUtils.getTargetClass(editor, psiFile);

            if (targetClass == null || targetClass.isAnnotationType() || targetClass.isEnum()) {
                throw new DocViewException(DocViewBundle.message("notify.error.class"));
            }

            // 目标类没有方法
            PsiMethod[] methods = targetClass.getMethods();

            if (methods.length == 0) {
                throw new DocViewException(DocViewBundle.message("notify.error.class.no.method"));
            }

            // 当前方法
            targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);

        } catch (DocViewException ex) {
            DocViewNotification.notifyError(project, ex.getMessage());
            LOG.info("DocView 执行错误：" + ex.getMessage());
        }

    }

    /**
     * 判断是否支持 Doc View
     */
    @Override
    public void update(@NotNull AnActionEvent e) {

        Project project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);
        Editor editor = e.getData(CommonDataKeys.EDITOR);

        Presentation presentation = e.getPresentation();

        if (editor == null || project == null || psiFile == null || DumbService.isDumb(project)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        PsiClass targetClass = CustomPsiUtils.getTargetClass(editor, psiFile);

        if (!DocViewUtils.isDocViewClass(targetClass)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        PsiMethod targetMethod = CustomPsiUtils.getTargetMethod(editor, psiFile);

        if (targetMethod != null && !DocViewUtils.isDocViewMethod(targetMethod)) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        presentation.setEnabledAndVisible(true);
    }

}

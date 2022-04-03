package com.liuzhihang.doc.view.action;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.liuzhihang.doc.view.DocViewBundle;
import com.liuzhihang.doc.view.config.*;
import com.liuzhihang.doc.view.dto.DocView;
import com.liuzhihang.doc.view.notification.DocViewNotification;
import com.liuzhihang.doc.view.service.DocViewService;
import com.liuzhihang.doc.view.service.DocViewUploadService;
import com.liuzhihang.doc.view.utils.CustomPsiUtils;
import com.liuzhihang.doc.view.utils.DocViewUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author liuzhihang
 * @date 2021/10/23 23:13
 */
public abstract class AbstractUploadAction extends AnAction {

    protected Project project;
    protected PsiFile psiFile;
    protected Editor editor;
    protected PsiClass targetClass;

    protected List<DocView> docViewList;

    public void actionPerformed(@NotNull AnActionEvent e) {

        // 获取当前project对象
        project = e.getData(PlatformDataKeys.PROJECT);
        // 获取当前编辑的文件, 可以进而获取 PsiClass, PsiField 对象
        psiFile = e.getData(CommonDataKeys.PSI_FILE);
        editor = e.getData(CommonDataKeys.EDITOR);


        if (editor == null || project == null || psiFile == null || DumbService.isDumb(project)) {
            return;
        }
        if (DumbService.isDumb(project)) {
            return;
        }

        // 获取Java类或者接口
        targetClass = CustomPsiUtils.getTargetClass(editor, psiFile);

        if (targetClass == null || targetClass.isAnnotationType() || targetClass.isEnum()) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.error.class"));
            return;
        }

        // 目标类没有方法
        PsiMethod[] methods = targetClass.getMethods();

        if (methods.length == 0) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.error.class.no.method"));
            return;
        }

        checkSettings();


        DocViewService docViewService = DocViewService.getInstance(project, targetClass);

        if (docViewService == null) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.error.not.support"));
            return;
        }

        docViewList = docViewService.buildDoc(project, psiFile, editor, targetClass);

        if (CollectionUtils.isEmpty(docViewList)) {
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.error.not.support"));
            return;
        }

        DocViewUploadService service = uploadService();

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Doc View upload", true) {
            @Override
            public void run(@NotNull ProgressIndicator progressIndicator) {

                ApplicationManager.getApplication().executeOnPooledThread(() -> ApplicationManager.getApplication().runReadAction(() -> {
                    service.upload(project, docViewList);
                }));

            }
        });

    }

    /**
     * 具体上传服务
     *
     * @return
     */
    protected abstract DocViewUploadService uploadService();

    /**
     * 检查配置
     */
    protected abstract void checkSettings();

    /**
     * 设置右键菜单是否隐藏 上传按钮
     *
     * @param e
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


    public void checkYApiSettings(@NotNull Project project) {
        YApiSettings apiSettings = YApiSettings.getInstance(project);

        if (StringUtils.isBlank(apiSettings.getUrl())
                || apiSettings.getProjectId() == null
                || StringUtils.isBlank(apiSettings.getToken())) {
            // 说明没有配置 YApi 上传地址, 跳转到配置页面
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.yapi.info.settings"));
            ShowSettingsUtil.getInstance().showSettingsDialog(project, YApiSettingsConfigurable.class);

        }
    }

    public void checkShowDocSettings(@NotNull Project project) {
        ShowDocSettings apiSettings = ShowDocSettings.getInstance(project);

        if (StringUtils.isBlank(apiSettings.getUrl())
                || StringUtils.isBlank(apiSettings.getApiKey())
                || StringUtils.isBlank(apiSettings.getApiToken())) {
            // 说明没有配置 ShowDoc 上传地址, 跳转到配置页面
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.showdoc.info.settings"));
            ShowSettingsUtil.getInstance().showSettingsDialog(project, ShowDocSettingsConfigurable.class);

        }
    }

    public void checkYuQueSettings(@NotNull Project project) {
        YuQueSettings apiSettings = YuQueSettings.getInstance(project);

        if (StringUtils.isBlank(apiSettings.getUrl())
                || StringUtils.isBlank(apiSettings.getApiUrl())
                || StringUtils.isBlank(apiSettings.getRepos())
                || StringUtils.isBlank(apiSettings.getLogin())
                || StringUtils.isBlank(apiSettings.getToken())) {
            // 说明没有配置语雀上传地址, 跳转到配置页面
            DocViewNotification.notifyError(project, DocViewBundle.message("notify.yuque.info.settings"));
            ShowSettingsUtil.getInstance().showSettingsDialog(project, ShowDocSettingsConfigurable.class);

        }
    }
}
